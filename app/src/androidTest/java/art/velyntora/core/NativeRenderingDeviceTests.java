package art.velyntora.core;
import static org.junit.Assert.*;
import android.graphics.*;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

/** Cold native tile caches must never expose artificial checkerboard gaps. */
public final class NativeRenderingDeviceTests {
  @Test public void navigationNeverMixesPartialDetailAndRetainsSharpZoomOut() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        DrawingView.nativeBeginEdit();
        for(int x=2650;x<3350;x+=17)DrawingView.nativeStroke(x,2650,x+100,3350,1,0xff112233);
      });
      CanvasRendering.Frame partial=CanvasRendering.prepareFrame(.8f,new Rect(2990,2990,3010,3010));
      Bitmap actual=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888),expected=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
      try {
        inst.runOnMainSync(()->{
          Canvas a=new Canvas(actual),e=new Canvas(expected);
          for(Canvas c:new Canvas[]{a,e}){c.translate(256,256);c.rotate(-28);c.scale(.8f,.8f);c.translate(-3000,-3000);}
          partial.display.draw(a,.8f);
          Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
          e.drawBitmap(partial.bitmap,null,new Rect(0,0,partial.bitmap.getWidth()*8,partial.bitmap.getHeight()*8),paint);
          int[] ap=new int[512*512],ep=new int[ap.length];actual.getPixels(ap,0,512,0,0,512,512);expected.getPixels(ep,0,512,0,0,512,512);
          assertArrayEquals("Partial sharp blocks were exposed",ep,ap);
        });
      }finally{inst.runOnMainSync(partial::dispose);actual.recycle();expected.recycle();}
      CanvasRendering.Frame sharp=CanvasRendering.prepareFrame(4f,new Rect(2600,2600,3400,3400));
      Bitmap output=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888),reference=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
      Bitmap source=Bitmap.createBitmap(DrawingView.nativeSampleRegion(2600,2600,1,800,800),800,800,Bitmap.Config.ARGB_8888);
      try {
        inst.runOnMainSync(()->{
          Canvas a=new Canvas(output),e=new Canvas(reference);
          for(Canvas c:new Canvas[]{a,e}){c.translate(64,64);c.rotate(-28);c.scale(.49f,.49f);c.translate(-3000,-3000);}
          sharp.display.draw(a,.49f);
          e.drawBitmap(source,null,new Rect(2600,2600,3400,3400),new Paint(Paint.FILTER_BITMAP_FLAG));
          int[] ap=new int[128*128],ep=new int[ap.length];output.getPixels(ap,0,128,0,0,128,128);reference.getPixels(ep,0,128,0,0,128,128);
          assertArrayEquals("Zoom-out discarded existing sharp detail",ep,ap);
        });
      }finally{inst.runOnMainSync(sharp::dispose);source.recycle();output.recycle();reference.recycle();}
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
  @Test public void preparedHistoryFrameIsSharpOnItsFirstDraw() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(2960,2960,3040,3040,1,0xff112233);
      });
      // Preparation, including detailed blocks, runs off the UI just like history operations.
      for(int step=0;step<3;step++){
        if(step==1)assertTrue(DrawingView.nativeUndo());
        if(step==2)assertTrue(DrawingView.nativeRedo());
        CanvasRendering.Frame frame=CanvasRendering.prepareFrame(4f,new Rect(2920,2920,3080,3080));
        Bitmap actual=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        Bitmap expected=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        Bitmap source=Bitmap.createBitmap(DrawingView.nativeSampleRegion(2920,2920,1,160,160),160,160,Bitmap.Config.ARGB_8888);
        try {
          Canvas a=new Canvas(actual),e=new Canvas(expected);
          for(Canvas c:new Canvas[]{a,e}){c.translate(128,128);c.rotate(-8);c.scale(4,4);c.translate(-3000,-3000);}
          frame.display.draw(a,4f);
          Paint exact=new Paint();exact.setFilterBitmap(false);
          e.drawBitmap(source,null,new Rect(2920,2920,3080,3080),exact);
          for(int y=0;y<256;y++)for(int x=0;x<256;x++)assertEquals("Coarse history flash, step "+step+" at "+x+","+y,expected.getPixel(x,y),actual.getPixel(x,y));
        }finally{frame.dispose();actual.recycle();expected.recycle();source.recycle();}
      }
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
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
