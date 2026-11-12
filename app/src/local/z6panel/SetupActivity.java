package local.z6panel;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.widget.*;

public class SetupActivity extends Activity {
 public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(40,24,40,24);
  ScrollView scroll=new ScrollView(this); scroll.addView(root); setContentView(scroll);
  TextView title=new TextView(this); title.setText("Inicio y Panel TV · configuración inicial"); title.setTextSize(25); root.addView(title);
  TextView text=new TextView(this); text.setText("1. Activa Inicio y Panel TV en Accesibilidad.\n2. Home abre el panel dentro de las apps; flechas y OK permiten navegar.\n3. Ajustes abre el panel derecho. Atrás vuelve un nivel; Home o Atrás largo cierran todo.\n4. Mantén OK sobre una app para ordenar u ocultar. Si no hay apps, usa Añadir aplicación.\n\nFondos del inicio permite fotos, presentaciones y paisajes diarios. Mi control y arranque permite configurar teclas y destinos. Algunas acciones avanzadas necesitan el auxiliar autorizado desde Diagnóstico y permisos.\n\nEl servicio principal recibe teclas sin leer el contenido de las ventanas. La asistencia opcional reconoce controles de la app elegida solo durante una tarea. Internet se utiliza para el clima y los paisajes cuando corresponde; las fotos del celular se reciben en la red local durante una sesión temporal. Bluetooth en Android 10 requiere permisos de ubicación; no consultamos GPS. Antes de desinstalar, restaura otro inicio mediante el asistente de recuperación."); text.setTextSize(17); root.addView(text);
  if(android.os.Build.VERSION.SDK_INT>=31){
   Button nearby=new Button(this);nearby.setText("Autorizar dispositivos cercanos");nearby.setOnClickListener(v->requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT","android.permission.BLUETOOTH_SCAN"},310));root.addView(nearby);
   Button alarms=new Button(this);alarms.setText("Permitir temporizadores exactos");alarms.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}});root.addView(alarms);
  }
  CheckBox home=new CheckBox(this); home.setText("Intentar abrir el panel con Home"); home.setChecked(getSharedPreferences("panel",0).getBoolean("home",true)); home.setOnCheckedChangeListener((v,c)->getSharedPreferences("panel",0).edit().putBoolean("home",c).apply()); root.addView(home);
  Button access=new Button(this); access.setText("Abrir Accesibilidad"); access.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Exception e){Toast.makeText(this,"Abre Ajustes → Accesibilidad manualmente",1).show();}});root.addView(access);
  Button test=new Button(this); test.setText("Probar panel aquí"); test.setOnClickListener(v->{if(PanelService.active!=null)PanelService.active.toggle();else Toast.makeText(this,"Activa primero Inicio y Panel TV en Accesibilidad",1).show();});root.addView(test);
 }
}
