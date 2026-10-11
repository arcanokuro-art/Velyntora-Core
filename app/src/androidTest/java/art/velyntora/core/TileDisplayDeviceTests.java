package art.velyntora.core;
import static org.junit.Assert.*;
import android.graphics.*;
import org.junit.Test;
public final class TileDisplayDeviceTests {
  @Test public void exactPixelsAcrossTilesAndDirtyGutters() {
    Bitmap source=Bitmap.createBitmap(513,517,Bitmap.Config.ARGB_8888);
    for(int y=0;y<517;y++)for(int x=0;x<513;x++)source.setPixel(x,y,0xff000000|((x*3&255)<<16)|((y*7&255)<<8)|(x+y&255));
    TileDisplay tiles=new TileDisplay(source);Bitmap output=Bitmap.createBitmap(513,517,Bitmap.Config.ARGB_8888);
    try {
      tiles.draw(new Canvas(output),1);
      for(int y=0;y<517;y++)for(int x=0;x<513;x++)assertEquals(source.getPixel(x,y),output.getPixel(x,y));
      source.setPixel(256,256,0xffabcdef);tiles.invalidate(256,256,1,1);tiles.draw(new Canvas(output),1);
      assertEquals(0xffabcdef,output.getPixel(256,256));
      assertEquals(source.getPixel(255,256),output.getPixel(255,256));
      assertEquals(source.getPixel(512,516),output.getPixel(512,516));
    } finally {tiles.clear();source.recycle();output.recycle();}
  }
}
