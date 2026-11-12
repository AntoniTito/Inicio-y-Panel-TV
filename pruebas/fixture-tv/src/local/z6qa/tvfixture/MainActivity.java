package local.z6qa.tvfixture;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Emulator-only fixture: no network, account, media downloads or actual credentials. */
public final class MainActivity extends Activity {
 private String mode="normal";private LinearLayout root;private boolean wrongChannels,delayedReady;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);readMode(getIntent());home();}
 protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);readMode(i);home();}
 private void readMode(Intent i){android.content.SharedPreferences p=getSharedPreferences("fixture",0);if(i.hasExtra("mode"))p.edit().putString("mode",i.getStringExtra("mode")).remove("retry_seen").commit();mode=p.getString("mode","normal");wrongChannels=mode.equals("retry-never")||mode.equals("retry-once")&&!p.getBoolean("retry_seen",false);if(mode.equals("retry-once")&&!i.getBooleanExtra("stage_only",false))p.edit().putBoolean("retry_seen",true).commit();}
 private void screen(String name){root=new LinearLayout(this);root.setOrientation(1);root.setPadding(32,24,32,24);root.setBackgroundColor(0xFF18252B);TextView t=new TextView(this);t.setText(name+" · PID "+android.os.Process.myPid());t.setTextSize(24);t.setTextColor(-1);root.addView(t);setContentView(root);}
 private void button(int id,String title,Runnable run){Button b=new Button(this);b.setId(id);b.setText(title);root.addView(b,new LinearLayout.LayoutParams(500,80));b.setOnClickListener(v->run.run());}
 private void home(){
  screen("Inicio TV de prueba");
  if(mode.equals("library")){screen("Biblioteca de prueba");button(R.id.television,"Biblioteca",this::library);return;}
  if(mode.equals("early-player")||mode.equals("other-player")){player();return;}
  if(mode.equals("login")){EditText secret=new EditText(this);secret.setId(R.id.password);secret.setInputType(0x81);secret.setText("secret-fixture-never-record");root.addView(secret);button(0,"Iniciar sesión",()->{});return;}
  if(mode.equals("inaccessible")){View canvas=new View(this);canvas.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);root.addView(canvas,new LinearLayout.LayoutParams(600,300));return;}
  button(R.id.television,mode.equals("changed")?"Contenido nuevo":"Televisión",this::countries);
  if(mode.equals("ambiguous"))button(R.id.television,"Televisión",this::countries);
 }
 private void library(){screen("Mi biblioteca");button(R.id.peru,"Mis listas",()->{screen("Mis listas");TextView marker=new TextView(this);marker.setId(R.id.playing);marker.setText("Mis listas · sección de prueba");marker.setTextColor(-1);root.addView(marker);});}
 private void list(String[] labels,int chosen,Runnable action){ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels));root.addView(list,new LinearLayout.LayoutParams(600,300));list.setOnItemClickListener((p,v,i,id)->{if(list.hasFocus()&&i==chosen)action.run();});}
 private void countries(){if(mode.equals("advanced")){channels();return;}screen("Países");if(mode.startsWith("list")){list(new String[]{"Argentina","Perú"},1,this::channels);return;}button(R.id.peru,"Perú",this::channels);button(0,"Chile",()->{});}
 private void channels(){screen("Canales de Perú");if(mode.equals("delayed")&&!delayedReady){button(0,"Cargando canales",()->{});new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()){delayedReady=true;channels();}},7000);return;}if(wrongChannels){button(R.id.channel,"Canal no disponible todavía",()->{});return;}if(mode.equals("list-alt")){
  ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new String[]{"Otro canal","TV Perú"}){
   public View getView(int position,View convert,ViewGroup parent){LinearLayout row=new LinearLayout(MainActivity.this);TextView text=new TextView(MainActivity.this);text.setText(getItem(position));text.setPadding(12,12,12,12);row.addView(text);return row;}
  });root.addView(list,new LinearLayout.LayoutParams(600,300));list.setOnItemClickListener((p,v,i,id)->{if(list.hasFocus()&&i==1)player();});return;
 }if(mode.startsWith("list")){list(mode.equals("list-ambiguous")?new String[]{"TV Perú","TV Perú"}:new String[]{"Otro canal","TV Perú"},1,this::player);return;}button(R.id.channel,"TV Perú",this::player);button(0,"Otro canal",()->{});}
 private void player(){screen("Canal elegido");TextView t=new TextView(this);t.setId(R.id.playing);t.setText(mode.equals("other-player")?"Otro canal · reproducción de prueba":"TV Perú · reproducción de prueba");t.setTextColor(-1);root.addView(t);SurfaceView surface=new SurfaceView(this);surface.setId(R.id.player);surface.setContentDescription("Reproductor de prueba");root.addView(surface,new LinearLayout.LayoutParams(500,220));}
}
