package local.z6panel;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.*;

public class PersonalizeActivity extends Activity {
 private SharedPreferences prefs;
 private LinearLayout list;

 public void onCreate(Bundle b){
  super.onCreate(b);
  prefs=getSharedPreferences("panel",0);
  ScrollView scroll=new ScrollView(this);
  list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(50,25,50,25);scroll.addView(list);setContentView(scroll);
  TextView title=new TextView(this);title.setText("Personalizar inicio");title.setTextSize(24);title.setTextColor(Color.WHITE);list.addView(title);
  TextView hint=new TextView(this);hint.setText("Estos cambios se guardan localmente y no dejan procesos activos.");hint.setTextSize(14);hint.setTextColor(0xFFAAAAAA);list.addView(hint);
  EditText name=new EditText(this);name.setSingleLine();name.setHint("Nombre mostrado");name.setText(prefs.getString("display_name",""));list.addView(name);
  Button save=new Button(this);save.setText("Guardar nombre");save.setOnClickListener(v->{prefs.edit().putString("display_name",name.getText().toString().trim()).apply();Toast.makeText(this,"Guardado",Toast.LENGTH_SHORT).show();});list.addView(save);
  Button apps=new Button(this);apps.setText("Personalizar aplicaciones");apps.setOnClickListener(v->{if(PanelService.active!=null)PanelService.active.openPage("manage");else Toast.makeText(this,"Activa Z6 Panel en Accesibilidad",Toast.LENGTH_SHORT).show();});list.addView(apps);
  CheckBox greeting=check("Mostrar saludo", "show_greeting", true);list.addView(greeting);
  CheckBox date=check("Mostrar fecha", "show_date", true);list.addView(date);
  CheckBox controls=check("Mostrar Wi‑Fi, Bluetooth y Ajustes", "show_controls", true);list.addView(controls);
  CheckBox time24=check("Usar formato de 24 horas", "time_24", false);list.addView(time24);
  Button wallpapers=new Button(this);wallpapers.setText("Fondos del inicio");wallpapers.setOnClickListener(v->startActivity(new android.content.Intent(this,WallpaperActivity.class)));list.addView(wallpapers);
  Button close=new Button(this);close.setText("Volver");close.setOnClickListener(v->finish());list.addView(close);
 }

 private CheckBox check(String label,String key,boolean defaultValue){
  CheckBox box=new CheckBox(this);box.setText(label);box.setTextColor(Color.WHITE);box.setTextSize(17);box.setChecked(prefs.getBoolean(key,defaultValue));box.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean(key,checked).apply());return box;
 }
}
