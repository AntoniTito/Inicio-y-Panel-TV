package local.z6panel;
import android.content.SharedPreferences;
import org.json.*;
import java.net.*;
import java.io.*;
/** Explicit city only. No GPS, identifiers, accounts or analytics. */
final class Weather {
 static JSONObject get(String url)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);
  try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>262144)throw new IOException("Respuesta demasiado grande");}return new JSONObject(out.toString("UTF-8"));
  }}finally{c.disconnect();}
 }
 static JSONArray search(String city)throws Exception{return get("https://geocoding-api.open-meteo.com/v1/search?count=5&language=es&name="+URLEncoder.encode(city,"UTF-8")).optJSONArray("results");}
 static void refresh(SharedPreferences p)throws Exception{
  String lat=p.getString("city_lat", ""),lon=p.getString("city_lon", "");if(lat.isEmpty()||lon.isEmpty())return;
  long age=System.currentTimeMillis()-p.getLong("weather_time",0);if(age>=0&&age<1800000)return;
  JSONObject c=get("https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+"&current=temperature_2m,weather_code").getJSONObject("current");
  if(!lat.equals(p.getString("city_lat",""))||!lon.equals(p.getString("city_lon","")))return;
  p.edit().putString("weather",Math.round(c.getDouble("temperature_2m"))+" °C · "+condition(c.getInt("weather_code"))).putLong("weather_time",System.currentTimeMillis()).apply();
 }
 static String condition(int code){if(code==0)return "Despejado";if(code<=3)return "Nublado";if(code<=48)return "Niebla";if(code<=57)return "Llovizna";if(code<=67)return "Lluvia";if(code<=77)return "Nieve";if(code<=82)return "Chubascos";if(code<=86)return "Nieve";return "Tormenta";}
 static String display(SharedPreferences p){if(p.getString("city_lat","").isEmpty())return "";String v=p.getString("weather", "Clima no disponible");long age=System.currentTimeMillis()-p.getLong("weather_time",0);return v+(p.getLong("weather_time",0)>0&&(age<0||age>3600000)?" · guardado":"");}
}
