package local.z6panel;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One durable, elapsed-realtime sleep timer. All components must use the app's default process.
 * No Activity, panel preferences, or ADB bootstrap is retained or invoked here.
 */
public final class SleepTimer {
    public static final String ACTION_TIMER = "local.z6panel.action.SLEEP_TIMER";
    private static final String TAG = "Z6SleepTimer";
    private static final String PREFS = "z6panel_sleep_timer_v1";
    private static final String GENERATION = "generation";
    private static final String DEADLINE = "deadline_elapsed";
    private static final String BOOT = "boot_count";
    private static final String FAILURE = "last_failure";
    private static final String EXTRA_GENERATION = "generation";
    private static final Object LOCK = new Object();
    private static final long RECEIVER_MS = 7500L;
    private static final long ATTACH_MS = 2000L;
    private static final long PING_MS = 1200L;
    private static final long SLEEP_MS = 5000L;
    private static volatile String storageFailure;

    private static final ThreadFactory THREADS = runnable -> {
        Thread thread = new Thread(runnable, "Z6SleepTimer");
        thread.setDaemon(true);
        return thread;
    };
    // No queue: a wedged synchronous Binder must not accumulate future sleep commands.
    private static final ThreadPoolExecutor BRIDGE = new ThreadPoolExecutor(
            0, 1, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), THREADS);
    private static final ScheduledThreadPoolExecutor WATCHDOG =
            new ScheduledThreadPoolExecutor(1, THREADS);

    static {
        WATCHDOG.setRemoveOnCancelPolicy(true);
    }

    private SleepTimer() { }

    /** Returns exactly OK on success. Failed replacement leaves an existing timer intact. */
    public static String schedule(Context context, int minutes) {
        if (minutes != 30 && minutes != 60 && minutes != 90) {
            return schedulingFailure(context, "Elige 30, 60 o 90 minutos");
        }
        return scheduleMillis(context, minutes * 60000L);
    }

    /** QA entry point ONLY for the DUMP-protected provider; never expose in a menu/Intent. */
    public static String scheduleSeconds(Context context, int seconds) {
        if (seconds < 1 || seconds > 120) {
            return schedulingFailure(context, "La prueba requiere entre 1 y 120 segundos");
        }
        return scheduleMillis(context, seconds * 1000L);
    }

    private static String scheduleMillis(Context supplied, long duration) {
        Context context = appContext(supplied);
        try {
            ActivityInfo receiver = context.getPackageManager().getReceiverInfo(
                    new ComponentName(context, TimerReceiver.class), 0);
            if (!receiver.enabled || !receiver.applicationInfo.enabled) {
                return schedulingFailure(context, "El receptor .TimerReceiver está deshabilitado");
            }
            if (context.checkSelfPermission(Manifest.permission.WAKE_LOCK)
                    != PackageManager.PERMISSION_GRANTED) {
                return schedulingFailure(context, "Falta android.permission.WAKE_LOCK en el manifiesto");
            }
            AlarmManager alarms = alarms(context);
            if (Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms()) {
                return schedulingFailure(context, "Permite alarmas exactas para Z6 Panel en Ajustes");
            }
            // Bounded even when an attached helper's Binder stops answering. Never bootstrap ADB.
            String ping = bridgeCall("PING", PING_MS, SystemClock.elapsedRealtime() + PING_MS);
            if (!"OK".equals(ping)) {
                return schedulingFailure(context, "No se programó el temporizador (PING): " + ping);
            }
            synchronized (LOCK) {
                SharedPreferences prefs = prefs(context);
                int boot = bootCount(context); // Fail closed if reboot identity cannot be read.
                String old = prefs.getString(GENERATION, "");
                String token = UUID.randomUUID().toString();
                long due = SystemClock.elapsedRealtime() + duration;
                PendingIntent alarm = pendingIntent(context, token, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    // Arm the new alarm before replacing state. Its receiver must acquire LOCK.
                    // Thus an AlarmManager failure can preserve the previous timer unchanged.
                    alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, alarm);
                    commit(prefs.edit().putString(GENERATION, token).putLong(DEADLINE, due)
                            .putInt(BOOT, boot).remove(FAILURE));
                } catch (RuntimeException e) {
                    cancelAlarm(context, token);
                    // commit(false) still changes SharedPreferences' in-memory map. Fail closed.
                    if (token.equals(prefs.getString(GENERATION, ""))) {
                        invalidate(context, prefs, "No se pudo guardar el temporizador: " + detail(e));
                        cancelAlarm(context, old);
                    }
                    throw e;
                }
                cancelAlarm(context, old);
                return "OK";
            }
        } catch (PackageManager.NameNotFoundException e) {
            return schedulingFailure(context, "Registra .TimerReceiver en el manifiesto");
        } catch (RuntimeException e) {
            return schedulingFailure(context, "No se programó el temporizador: " + detail(e));
        }
    }

    /** Invalidates even an expiry currently waiting for automatic helper reattachment. */
    public static void cancel(Context supplied) {
        Context context = appContext(supplied);
        synchronized (LOCK) {
            try {
                invalidate(context, prefs(context), null);
            } catch (RuntimeException e) {
                storageFailure = "No se pudo guardar la cancelación: " + detail(e);
                Log.e(TAG, storageFailure, e);
            }
        }
    }

    /** Zero means no armed timer (including one already consumed by its receiver). */
    public static long remainingMillis(Context supplied) {
        Context context = appContext(supplied);
        synchronized (LOCK) {
            try {
                SharedPreferences prefs = prefs(context);
                if (!sameBoot(context, prefs)) return 0L;
                return Math.max(0L, prefs.getLong(DEADLINE, 0L) - SystemClock.elapsedRealtime());
            } catch (RuntimeException e) {
                storageFailure = "No se pudo leer el temporizador: " + detail(e);
                return 0L;
            }
        }
    }

    /** Spanish text suitable for a menu subtitle or Toast; preserves the actual last error. */
    public static String summary(Context supplied) {
        Context context = appContext(supplied);
        synchronized (LOCK) {
            try {
                SharedPreferences prefs = prefs(context);
                sameBoot(context, prefs);
                long due = prefs.getLong(DEADLINE, 0L);
                long left = Math.max(0L, due - SystemClock.elapsedRealtime());
                String text = due == 0L ? "Sin temporizador" : left == 0L
                        ? "Temporizador vencido; esperando suspensión"
                        : "Suspender en " + ((left + 59999L) / 60000L) + " min";
                String failure = storageFailure != null ? storageFailure : prefs.getString(FAILURE, "");
                return failure.isEmpty() ? text : text + " · Último error: " + failure;
            } catch (RuntimeException e) {
                return "Sin temporizador verificable: " + detail(e);
            }
        }
    }

    /** Takes ownership of result and always finishes it, including Binder timeout/startup failure. */
    public static void handleBroadcast(Context supplied, Intent intent,
                                       BroadcastReceiver.PendingResult result) {
        final long end = SystemClock.elapsedRealtime() + RECEIVER_MS;
        final Context context = appContext(supplied);
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicBoolean expiryStarted = new AtomicBoolean();
        final PowerManager.WakeLock[] wake = new PowerManager.WakeLock[1];
        final Runnable finish = () -> {
            if (!finished.compareAndSet(false, true)) return;
            try {
                if (result != null) result.finish();
            } finally {
                try {
                    if (wake[0] != null && wake[0].isHeld()) wake[0].release();
                } catch (RuntimeException e) {
                    Log.w(TAG, "WakeLock release", e);
                }
            }
        };
        final java.util.concurrent.ScheduledFuture<?> timeout = WATCHDOG.schedule(
                finish, Math.max(0L, end - SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS);
        try {
            // AlarmManager's onReceive wake lock does not cover arbitrary asynchronous work.
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power == null) throw new IllegalStateException("PowerManager no disponible");
            wake[0] = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Z6Panel:SleepTimer");
            wake[0].setReferenceCounted(false);
            wake[0].acquire(Math.max(1L, end - SystemClock.elapsedRealtime()));
            THREADS.newThread(() -> {
                try {
                    if (finished.get()) return;
                    String action = intent == null ? null : intent.getAction();
                    if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                        cancel(context);
                    } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                        restoreAfterUpdate(context, finished);
                    } else if (ACTION_TIMER.equals(action)) {
                        fire(context, intent, end, finished, expiryStarted);
                    }
                } catch (RuntimeException e) {
                    receiverFailure(context, intent, "Temporizador: " + detail(e), expiryStarted.get());
                } finally {
                    timeout.cancel(false);
                    finish.run();
                }
            }).start();
        } catch (RuntimeException e) {
            // Finish before any potentially slow preferences I/O on this error path.
            timeout.cancel(false);
            finish.run();
            THREADS.newThread(() -> receiverFailure(context, intent,
                    "No se pudo ejecutar el receptor: " + detail(e), false)).start();
        }
    }

    private static void fire(Context context, Intent intent, long end, AtomicBoolean finished,
                             AtomicBoolean expiryStarted) {
        String token = intent.getStringExtra(EXTRA_GENERATION);
        if (token == null || !timerUri(context, token).equals(intent.getData())) return;
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (finished.get() || !token.equals(prefs.getString(GENERATION, ""))
                    || prefs.getLong(DEADLINE, 0L) == 0L || !sameBoot(context, prefs)) return;
            long due = prefs.getLong(DEADLINE, 0L);
            if (due > SystemClock.elapsedRealtime()) {
                arm(context, token, due); // Defensive early delivery must not lose the alarm.
                return;
            }
            expiryStarted.set(true);
            // Durable consumption BEFORE waiting or IPC: duplicates/process death never retry SLEEP.
            // Retain the generation so cancel/replacement can revoke this worker during attach wait.
            commit(prefs.edit().remove(DEADLINE).putString(FAILURE,
                    "Suspensión sin confirmar: el receptor terminó antes de recibir respuesta"));
            cancelAlarm(context, token);
        }

        long attachEnd = Math.min(end - 500L, SystemClock.elapsedRealtime() + ATTACH_MS);
        while (!attached() && !finished.get() && SystemClock.elapsedRealtime() < attachEnd) {
            synchronized (LOCK) {
                if (!token.equals(prefs(context).getString(GENERATION, ""))) return;
            }
            try {
                Thread.sleep(Math.min(100L, Math.max(1L, attachEnd - SystemClock.elapsedRealtime())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (!token.equals(prefs.getString(GENERATION, "")) || !sameBoot(context, prefs)) return;
            String outcome;
            if (finished.get() || Thread.currentThread().isInterrupted()
                    || SystemClock.elapsedRealtime() >= end - 250L) {
                outcome = "Plazo del receptor agotado; no se envió SLEEP";
            } else if (!attached()) {
                outcome = "Servicio de sistema desconectado; el helper no se adjuntó en 2 s";
            } else {
                // This is the irrevocable dispatch boundary. Cancel/replacement share LOCK.
                // Hold it through the bounded call so neither can return before dispatch starts.
                // Once a Binder transaction is sent it cannot be recalled, even on timeout.
                long budget = Math.min(SLEEP_MS, Math.max(1L, end - SystemClock.elapsedRealtime() - 250L));
                outcome = bridgeCall("SLEEP", budget, end - 250L);
            }
            String failure = "No se pudo confirmar la suspensión: " + outcome;
            commit("OK".equals(outcome) ? prefs.edit().remove(FAILURE)
                    : prefs.edit().putString(FAILURE, failure));
            if (!"OK".equals(outcome)) postExpiryFailureToast(context, token, failure);
        }
    }

    private static void restoreAfterUpdate(Context context, AtomicBoolean finished) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (finished.get() || !sameBoot(context, prefs)) return;
            long due = prefs.getLong(DEADLINE, 0L);
            if (due == 0L) return;
            if (due <= SystemClock.elapsedRealtime()) {
                invalidate(context, prefs, "El temporizador venció durante la actualización; programa uno nuevo");
                return;
            }
            try {
                arm(context, prefs.getString(GENERATION, ""), due);
            } catch (RuntimeException e) {
                invalidate(context, prefs, "No se pudo restaurar el temporizador: " + detail(e));
            }
        }
    }

    private static void receiverFailure(Context context, Intent intent, String message,
                                        boolean actualExpiry) {
        Log.e(TAG, message);
        synchronized (LOCK) {
            try {
                SharedPreferences prefs = prefs(context);
                String token = intent == null ? null : intent.getStringExtra(EXTRA_GENERATION);
                if (token != null && token.equals(prefs.getString(GENERATION, ""))) {
                    invalidate(context, prefs, message);
                    if (actualExpiry) {
                        postExpiryFailureToast(context, prefs.getString(GENERATION, ""), message);
                    }
                } else if (intent != null && Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
                    // An update failure must not invalidate a timer installed concurrently afterwards.
                    Log.w(TAG, "No se restauró el temporizador: " + message);
                }
            } catch (RuntimeException e) {
                storageFailure = message + "; no se pudo guardar: " + detail(e);
            }
        }
    }

    private static void postExpiryFailureToast(Context context, String token, String failure) {
        try {
            // Never wait for the UI thread or extend the receiver's deadline for a Toast.
            new Handler(Looper.getMainLooper()).post(() -> {
                synchronized (LOCK) {
                    try {
                        SharedPreferences prefs = prefs(context);
                        // Cancel/replacement or a cleared error suppresses a queued notification.
                        if (!token.equals(prefs.getString(GENERATION, ""))
                                || prefs.getLong(DEADLINE, 0L) != 0L
                                || !failure.equals(prefs.getString(FAILURE, ""))) return;
                        Toast.makeText(context,
                                "Temporizador: suspensión sin confirmar. Revisa Equipo > Temporizador.",
                                Toast.LENGTH_LONG).show();
                    } catch (RuntimeException e) {
                        Log.w(TAG, "No se pudo mostrar el error del temporizador", e);
                    }
                }
            });
        } catch (RuntimeException e) {
            Log.w(TAG, "No se pudo publicar el error del temporizador", e);
        }
    }

    private static String bridgeCall(String operation, long budget, long latestStart) {
        FutureTask<String> task = new FutureTask<>(() -> {
            if (Thread.currentThread().isInterrupted() || SystemClock.elapsedRealtime() >= latestStart) {
                return "Plazo agotado; no se envió " + operation;
            }
            String response = SystemBridge.call(operation);
            return response == null || response.trim().isEmpty() ? "Respuesta vacía de " + operation : response;
        });
        try {
            BRIDGE.execute(task);
            return task.get(Math.max(1L, budget), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return "Servicio de sistema ocupado por una llamada anterior sin respuesta";
        } catch (TimeoutException e) {
            return operation + " sin respuesta en " + budget + " ms; resultado no confirmado";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return operation + " interrumpido; resultado no confirmado";
        } catch (ExecutionException e) {
            return operation + ": " + detail(e.getCause() == null ? e : e.getCause());
        } finally {
            // Binder itself is not interruptible. No retry, queue, or delayed follow-up command.
            task.cancel(true);
        }
    }

    private static boolean attached() {
        IBinder binder = SystemBridge.remote;
        return binder != null && binder.isBinderAlive();
    }

    private static void arm(Context context, String token, long due) {
        if (token.isEmpty()) throw new IllegalStateException("Generación del temporizador ausente");
        AlarmManager alarms = alarms(context);
        if (Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms()) {
            throw new SecurityException("Permite alarmas exactas para Z6 Panel en Ajustes");
        }
        alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, due,
                pendingIntent(context, token, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private static PendingIntent pendingIntent(Context context, String token, int flags) {
        Intent intent = new Intent(context, TimerReceiver.class).setAction(ACTION_TIMER)
                .setData(timerUri(context, token)).putExtra(EXTRA_GENERATION, token)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        // Token in data (not just extras): an old delivered Intent cannot acquire a new generation.
        return PendingIntent.getBroadcast(context, 0, intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Uri timerUri(Context context, String token) {
        return new Uri.Builder().scheme("z6panel-sleep").authority(context.getPackageName())
                .appendPath(token).build();
    }

    private static void cancelAlarm(Context context, String token) {
        if (token == null || token.isEmpty()) return;
        try {
            PendingIntent alarm = pendingIntent(context, token, PendingIntent.FLAG_NO_CREATE);
            if (alarm != null) {
                try {
                    alarms(context).cancel(alarm);
                } finally {
                    alarm.cancel();
                }
            }
        } catch (RuntimeException e) {
            // Generation invalidation remains authoritative even if the OS cancel call fails.
            Log.w(TAG, "No se pudo retirar una alarma obsoleta", e);
        }
    }

    private static boolean sameBoot(Context context, SharedPreferences prefs) {
        if (prefs.getInt(BOOT, -1) == -1) {
            if (prefs.getLong(DEADLINE, 0L) != 0L) {
                invalidate(context, prefs, "Temporizador sin identidad de arranque; programa uno nuevo");
                return false;
            }
            return true;
        }
        final int current;
        try {
            current = bootCount(context);
        } catch (RuntimeException e) {
            invalidate(context, prefs, "No se pudo verificar el arranque: " + detail(e));
            return false;
        }
        if (current != prefs.getInt(BOOT, -1)) {
            invalidate(context, prefs, "Temporizador cancelado tras reiniciar el dispositivo");
            return false;
        }
        return true;
    }

    private static int bootCount(Context context) {
        int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        if (count < 0) throw new IllegalStateException("Settings.Global.BOOT_COUNT no disponible");
        return count;
    }

    private static void invalidate(Context context, SharedPreferences prefs, String failure) {
        String old = prefs.getString(GENERATION, "");
        SharedPreferences.Editor edit = prefs.edit().putString(GENERATION, UUID.randomUUID().toString())
                .remove(DEADLINE).remove(BOOT);
        if (failure == null) edit.remove(FAILURE); else edit.putString(FAILURE, failure);
        try {
            commit(edit);
        } finally {
            cancelAlarm(context, old);
        }
    }

    private static String schedulingFailure(Context supplied, String failure) {
        synchronized (LOCK) {
            try {
                commit(prefs(appContext(supplied)).edit().putString(FAILURE, failure));
            } catch (RuntimeException e) {
                storageFailure = failure + "; no se pudo guardar: " + detail(e);
            }
        }
        return failure;
    }

    private static void commit(SharedPreferences.Editor edit) {
        if (!edit.commit()) {
            storageFailure = "No se pudo guardar el estado del temporizador en almacenamiento";
            throw new IllegalStateException(storageFailure);
        }
        storageFailure = null;
    }

    private static Context appContext(Context context) {
        Context app = context.getApplicationContext();
        return app == null ? context : app;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static AlarmManager alarms(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) throw new IllegalStateException("AlarmManager no disponible");
        return alarms;
    }

    private static String detail(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
