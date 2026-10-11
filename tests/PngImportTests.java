package art.velyntora.core;
import java.io.*;
public final class PngImportTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  for(int orientation=1;orientation<=8;orientation++){
   ByteArrayOutputStream encoded=new ByteArrayOutputStream();
   PngWriter.write(encoded,37,29,(y,row)->{for(int x=0;x<row.length;x++)row[x]=0xff000000|(x<<16)|(y<<8)|123;});
   check(PngStripeImporter.load(new ByteArrayInputStream(encoded.toByteArray()),orientation));DrawingView.nativeAcceptImageImport();
   int w=orientation>=5?29:37,h=orientation>=5?37:29;
   check(DrawingView.nativeWidth()==w && DrawingView.nativeHeight()==h);
   for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    int source=ExifOrientation.sourceIndex(x,y,37,29,orientation);
    check(DrawingView.nativeRegionPixels(x,y,1,1)[0]==(0xff000000|((source%37)<<16)|((source/37)<<8)|123));
   }
  }
  ByteArrayOutputStream encoded=new ByteArrayOutputStream();
  PngWriter.write(encoded,10000,10000,(y,row)->java.util.Arrays.fill(row,0xff000000|y));
  check(PngStripeImporter.load(new ByteArrayInputStream(encoded.toByteArray()),1));DrawingView.nativeAcceptImageImport();
  check(DrawingView.nativeWidth()==10000 && DrawingView.nativeHeight()==10000);
  for(int y:new int[]{0,1,103,9999})check(DrawingView.nativeRegionPixels(9999,y,1,1)[0]==(0xff000000|y));
  byte[] broken=encoded.toByteArray();broken[broken.length-13]^=1;
  boolean rejected=false;try{PngStripeImporter.load(new ByteArrayInputStream(broken),1);}catch(IOException error){rejected=true;}finally{DrawingView.nativeCancelImageImport();}
  check(rejected);check(DrawingView.nativeWidth()==10000);check(DrawingView.nativeRegionPixels(9999,9999,1,1)[0]==(0xff000000|9999));
  check(DrawingView.nativeCreate(64,64));System.out.println("Original 10000x10000 PNG import, all EXIF orientations and corrupt-import rollback passed");
 }
}
