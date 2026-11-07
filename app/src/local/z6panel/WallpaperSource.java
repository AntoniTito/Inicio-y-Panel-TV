package local.z6panel;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import java.io.*;

/** A reopenable source; shared images are read without staging a second master. */
final class WallpaperSource {
 private long knownLength=-1;final Context context;final Uri uri;final File local;
 WallpaperSource(File f){local=f;uri=null;context=null;}
 WallpaperSource(Context c,Uri u){context=c.getApplicationContext();uri=u;local=null;}
 ParcelFileDescriptor open()throws IOException{ParcelFileDescriptor fd=local!=null?ParcelFileDescriptor.open(local,ParcelFileDescriptor.MODE_READ_ONLY):context.getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new FileNotFoundException();return fd;}
 ImageDecoder.Source decoder(){return local!=null?ImageDecoder.createSource(local):ImageDecoder.createSource(context.getContentResolver(),uri);}
 int[] bounds(){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(ParcelFileDescriptor fd=open()){if(fd!=null)BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(),null,o);}catch(Exception ignored){}return new int[]{o.outWidth,o.outHeight};}
 boolean isFile(){if(local!=null)return local.isFile();try(Cursor q=context.getContentResolver().query(uri,new String[]{"_id"},android.os.Build.VERSION.SDK_INT>=30?"is_trashed=0 AND is_pending=0":"is_pending=0",null,null)){return q!=null&&q.moveToFirst();}catch(Exception e){return false;}}
 long length(){if(local!=null)return local.length();if(knownLength>=0)return knownLength;try(ParcelFileDescriptor fd=open()){knownLength=Math.max(0,fd.getStatSize());return knownLength;}catch(Exception e){return 0;}}
 long lastModified(){return local!=null?local.lastModified():length();}
 String getPath(){return local!=null?local.getPath():uri.toString();}
 String getName(){return local!=null?local.getName():uri.toString();}
 boolean delete(){try{return local!=null?(!local.exists()||local.delete()):context.getContentResolver().delete(uri,null,null)>=0;}catch(Exception e){return false;}}
}
