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
  TextView title=new TextView(this); title.setText("Z6 Panel · configuración inicial"); title.setTextSize(25); root.addView(title);
  TextView text=new TextView(this); text.setText("1. Activa Z6 Panel en Accesibilidad.\n2. Home abre el panel dentro de las apps; flechas y OK permiten navegar.\n3. Ajustes abre el panel derecho. Atrás vuelve un nivel; Home o Atrás largo cierran todo.\n4. Mantén OK sobre una app para favoritos, ocultar, ordenar y cambiar categoría.\n\nLos ajustes disponibles se muestran en paneles superpuestos. Wi-Fi y reinicio requieren el servicio local iniciado desde la PC. La búsqueda por voz se reserva para un control con micrófono.\n\nEl panel no solicita foco de ventana ni audio. El servicio recibe teclas y no lee la pantalla. Internet se usa solamente para el clima de la ciudad configurada. La búsqueda Bluetooth en Android 10 requiere permisos de ubicación; no consultamos GPS. Antes de desinstalar, restaura otro launcher con el script de recuperación."); text.setTextSize(17); root.addView(text);
  CheckBox home=new CheckBox(this); home.setText("Intentar abrir el panel con Home"); home.setChecked(getSharedPreferences("panel",0).getBoolean("home",true)); home.setOnCheckedChangeListener((v,c)->getSharedPreferences("panel",0).edit().putBoolean("home",c).apply()); root.addView(home);
  Button access=new Button(this); access.setText("Abrir Accesibilidad"); access.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Exception e){Toast.makeText(this,"Abre Ajustes → Accesibilidad manualmente",1).show();}});root.addView(access);
  Button test=new Button(this); test.setText("Probar panel aquí"); test.setOnClickListener(v->{if(PanelService.active!=null)PanelService.active.toggle();else Toast.makeText(this,"Activa primero Z6 Panel en Accesibilidad",1).show();});root.addView(test);
 }
}
