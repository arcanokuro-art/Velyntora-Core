package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;
import android.graphics.*;
import android.net.Uri;
import java.io.*;

/** One bounded decoder region and JNI stripe at a time; original dimensions are retained. */
final class RegionImageImporter {
  static void load(MainActivity host,Uri uri) throws IOException {
    int orientation=1;
    try(InputStream metadata=host.getContentResolver().openInputStream(uri)){
      if(metadata!=null)orientation=ExifOrientation.read(metadata);
    }catch(IOException ignored){}
    try(InputStream probe=host.getContentResolver().openInputStream(uri)){
      if(probe==null)throw new IOException("No se puede leer la imagen");
      BufferedInputStream buffered=new BufferedInputStream(probe);buffered.mark(8);
      boolean png=buffered.read()==137 && buffered.read()==80 && buffered.read()==78 && buffered.read()==71;
      buffered.reset();if(png && PngStripeImporter.load(buffered,orientation))return;
    }
    try(InputStream input=host.getContentResolver().openInputStream(uri)){
      if(input==null)throw new IOException("No se puede leer la imagen");
      BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(input,false);
      if(decoder==null)throw new IOException("Imagen no compatible con lectura por regiones");
      try {
        int width=decoder.getWidth(),height=decoder.getHeight();
        if(!CanvasLimits.accepts(width,height))throw new IOException("El límite es 10.000 × 10.000 px");
        boolean rotated=orientation>=5;int outputWidth=rotated?height:width,outputHeight=rotated?width:height;
        if(!DrawingView.nativeBeginImageImport(outputWidth,outputHeight))throw new IOException("No se pudo preparar el documento");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        int rows=Math.max(1,Math.min(256,1048576/width));
        for(int top=0;top<height;top+=rows){
          int count=Math.min(rows,height-top);Bitmap decoded=decoder.decodeRegion(new Rect(0,top,width,top+count),options);
          if(decoded==null || decoded.getWidth()!=width || decoded.getHeight()!=count)throw new IOException("Región incompleta");
          int[] source=new int[width*count];try{decoded.getPixels(source,0,width,0,0,width,count);}finally{decoded.recycle();}
          ImageImportStripe.write(width,height,orientation,top,count,source);
        }
        if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");
      }finally{decoder.recycle();}
    }
  }
  private RegionImageImporter(){}
}
