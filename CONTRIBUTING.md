# Contribuir a Inicio y Panel TV

Gracias por ayudar a mejorar un entorno que se maneja con el control del televisor. La prioridad es conservar navegación, datos y fluidez.

## Reportar un problema

Abre un issue con la versión de la app, Android/API, modelo/firmware, control utilizado, pasos, resultado esperado y resultado observado. Indica si ocurrió en hardware o emulador. Adjunta únicamente registros y capturas revisados: no compartas claves, sesiones QR, cuentas o fotos personales.

## Proponer cambios

- Explica el problema y el comportamiento que resuelve el cambio.
- Conserva un APK común para las API admitidas y las comprobaciones de capacidades por firmware.
- No cambies identificadores, firma, preferencias, URI de fotos o mapas del control por un cambio de marca.
- Evita sondeos en reposo, tareas pesadas en el hilo de interfaz y reconstrucciones por cada flecha.
- Las pruebas de emulador exigen serial explícito y datos sintéticos. No ejecutes las suites destructivas sobre un TV físico.
- Documenta las verificaciones realmente realizadas y sus límites. Un cambio de documentación no necesita fingir pruebas de reproducción física.

Lee [AGENTS.md](AGENTS.md), [arquitectura](docs/ARQUITECTURA.md), [compilación](docs/COMPILACION.md) y [pruebas](pruebas/README.md). Para cambios del asistente ejecuta `powershell -File pruebas/verificar.ps1` con los APK de su entrega. Para enlaces, JSON y sintaxis ejecuta `powershell -File herramientas/verificar-documentacion.ps1`.

## Firmas y versiones

La clave oficial no se distribuye ni se requiere para revisar el código. Puedes compilar para desarrollo con tu propia firma mediante `-Keystore`; ese APK no actualiza directamente una instalación firmada por el autor. No desinstales una instalación con datos para sortear una incompatibilidad de firmas.

Cada cambio del APK requiere una nueva versión/código y una recuperación compatible. Las etiquetas publicadas son inmutables. Los APK oficiales y sus hashes se distribuyen en Releases; los binarios no se añaden al historial Git.

Las contribuciones al código propio se incorporan bajo [GPL-3.0-only](LICENSE). Conserva las licencias y atribuciones de terceros.
