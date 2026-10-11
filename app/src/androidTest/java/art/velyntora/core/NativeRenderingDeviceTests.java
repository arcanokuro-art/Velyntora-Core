package art.velyntora.core;
import static org.junit.Assert.*;
import android.graphics.*;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

/** Cold native tile caches must never expose artificial checkerboard gaps. */
public final class NativeRenderingDeviceTests {
  @Test public void largeColdCacheCoversZoomAndRotationAndHistoryBoundaries() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        assertEquals(0,DrawingView.nativeHistoryAvailability());
        host.undo();host.redo();assertNull(host.readProjectProgress());
        for(float zoom:new float[]{.15f,.8f,1.3f,4f}){
          CanvasRendering.Frame frame=CanvasRendering.prepareFrame(zoom);
          Bitmap output=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
          try {
            output.eraseColor(0xffff00ff);Canvas canvas=new Canvas(output);
            canvas.translate(256,256);canvas.rotate(19.7f);canvas.scale(zoom,zoom);canvas.translate(-3000,-3000);
            canvas.clipRect(0,0,6000,6000);frame.display.draw(canvas,zoom);
            for(int y=0;y<512;y++)for(int x=0;x<512;x++)assertEquals("Missing overview at zoom "+zoom,0xffffffff,output.getPixel(x,y));
          }finally{frame.dispose();output.recycle();}
        }
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(3000,3000,3000,3000,8,0xff111111);
        assertEquals(1,DrawingView.nativeHistoryAvailability());assertTrue(DrawingView.nativeUndo());
        assertEquals(2,DrawingView.nativeHistoryAvailability());assertFalse(DrawingView.nativeUndo());
        assertTrue(DrawingView.nativeRedo());assertEquals(1,DrawingView.nativeHistoryAvailability());
      });
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
}
