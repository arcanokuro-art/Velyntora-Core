package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import org.junit.*;

public final class PlacedImageDeviceTests {
  Instrumentation inst;MainActivity host;File input;
  @Before public void open(){
    inst=InstrumentationRegistry.getInstrumentation();host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    inst.runOnMainSync(()->host.readDrawing().newDocument(800,800));inst.waitForIdleSync();
  }
  @After public void close() throws Exception {
    LargeImportResponsivenessTests.waitResponsive(inst,host);
    inst.runOnMainSync(()->{host.readDrawing().placedImage().cancel();host.readDrawing().newDocument(64,64);host.finish();});if(input!=null)input.delete();
  }
  void load(int width,int height) throws Exception {
    Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);int[] row=new int[width];
    for(int y=0;y<height;y++){for(int x=0;x<width;x++)row[x]=Color.rgb(x&255,y&255,128);image.setPixels(row,0,width,0,y,width,1);}
    input=new File(host.getCacheDir(),"placed-input.png");try(OutputStream out=new FileOutputStream(input)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    inst.runOnMainSync(()->host.readDrawing().placedImage().load(Uri.fromFile(input)));LargeImportResponsivenessTests.waitResponsive(inst,host);
  }
  View find(View view,String label){if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
    if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),label);if(result!=null)return result;}return null;}
  void event(DrawingView view,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(time,time+(action==0?0:20),action,(float)view.readViewport().screenX(x,y),(float)view.readViewport().screenY(x,y),0);try{assertTrue(view.onTouchEvent(e));}finally{e.recycle();}}
  void gesture(float x,float y,float endX,float endY){inst.runOnMainSync(()->{long time=SystemClock.uptimeMillis();DrawingView v=host.readDrawing();event(v,time,0,x,y);event(v,time,2,endX,endY);event(v,time,1,endX,endY);});}
  void dialogButton(String text){inst.waitForIdleSync();AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
    for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(node.getText())){assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();return;}throw new AssertionError("Missing dialog button "+text);}
  @Test public void placementGesturesCancelWithoutChangingDocumentAndCropDialogIsIsolated() throws Exception {
    long[] revision={0};inst.runOnMainSync(()->revision[0]=DrawingView.nativeRevision());load(400,300);
    inst.runOnMainSync(()->{PlacedImageSession s=host.readDrawing().placedImage();assertTrue(s.active());assertEquals(1,host.readDrawing().layerCount());assertEquals(revision[0],DrawingView.nativeRevision());});
    gesture(400,400,430,420);
    inst.runOnMainSync(()->{PlacedImageSession s=host.readDrawing().placedImage();assertEquals(430,s.state.cx,.1);assertEquals(420,s.state.cy,.1);s.undo();assertEquals(400,s.state.cx,.1);s.redo();assertEquals(430,s.state.cx,.1);
      find(host.getWindow().getDecorView(),"Recortar imagen").performClick();});
    dialogButton("Cancelar");inst.runOnMainSync(()->assertEquals(new RectF(0,0,400,300),host.readDrawing().placedImage().state.crop));
    inst.runOnMainSync(()->{PlacedImageSession s=host.readDrawing().placedImage();s.cropTo(new RectF(50,25,350,275));assertEquals(300,s.state.crop.width(),.1);assertEquals(800,host.readDrawing().documentWidth());s.cancel();
      assertEquals(1,host.readDrawing().layerCount());assertEquals(revision[0],DrawingView.nativeRevision());assertEquals(0xffffffff,DrawingView.nativeRegionPixels(400,400,1,1)[0]);
      assertEquals(View.VISIBLE,find(host.getWindow().getDecorView(),"Anchura del pincel en píxeles").getVisibility());});
  }
  @Test public void tiledCommitMatchesSourceTransformAndUndoRedoIsOneLayer() throws Exception {
    load(1600,1200);Bitmap expected=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);
    try{
      inst.runOnMainSync(()->{
        PlacedImageSession s=host.readDrawing().placedImage();s.state.crop=new RectF(400,200,1200,1000);s.state.width=620;s.state.height=540;s.state.cx=400;s.state.cy=400;s.state.angle=17;s.state.opacity=.65f;
        Bitmap raw=BitmapFactory.decodeFile(input.getPath());expected.eraseColor(Color.WHITE);Canvas canvas=new Canvas(expected);canvas.concat(s.state.matrix());canvas.clipRect(s.state.crop);Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);paint.setAlpha(Math.round(.65f*255));canvas.drawBitmap(raw,0,0,paint);raw.recycle();
        assertEquals(1,host.readDrawing().layerCount());s.confirm();
      });LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        DrawingView v=host.readDrawing();assertFalse(v.placedImage().active());assertEquals(2,v.layerCount());assertEquals(.65f,v.layerOpacity(),.001);
        int[] result=DrawingView.nativeRegionPixels(0,0,800,800);for(int y:new int[]{150,255,256,257,400,511,512,513,650})for(int x:new int[]{150,255,256,257,400,511,512,513,650}){
          int a=expected.getPixel(x,y),b=result[y*800+x];for(int shift:new int[]{0,8,16,24})assertTrue("Tile mismatch at "+x+","+y,Math.abs((a>>>shift&255)-(b>>>shift&255))<=4);
        }
      });
      inst.runOnMainSync(host::undo);LargeImportResponsivenessTests.waitResponsive(inst,host);inst.runOnMainSync(()->{assertEquals(1,host.readDrawing().layerCount());assertEquals(0xffffffff,DrawingView.nativeRegionPixels(400,400,1,1)[0]);});
      inst.runOnMainSync(host::redo);LargeImportResponsivenessTests.waitResponsive(inst,host);inst.runOnMainSync(()->assertEquals(2,host.readDrawing().layerCount()));
    }finally{expected.recycle();}
  }
  @Test public void stagingFailureNeverAddsPartialLayer() {
    inst.runOnMainSync(()->{
      long revision=DrawingView.nativeRevision();assertTrue(DrawingView.nativeBeginPlacedImage("Prueba",1));
      assertFalse(DrawingView.nativePlacedImageTile(0,0,2,2,new int[1]));DrawingView.nativeCancelPlacedImage();assertFalse(DrawingView.nativeCommitPlacedImage());
      assertEquals(1,host.readDrawing().layerCount());assertEquals(revision,DrawingView.nativeRevision());
    });
  }
  @Test public void oversizedSourceRequestsReductionWithoutResizingCanvas() throws Exception {
    load(6001,17);inst.runOnMainSync(()->assertFalse(host.readDrawing().placedImage().active()));dialogButton("Reducir e insertar");
    inst.runOnMainSync(()->{PlacedImageSession s=host.readDrawing().placedImage();assertTrue(s.active());assertEquals(6000,s.source.size.displayWidth());assertEquals(800,host.readDrawing().documentWidth());s.cancel();});
  }
  @Test public void transformOrientationAndCropPreserveCoordinates(){
    for(int orientation=1;orientation<=8;orientation++){
      ImageImportSize size=new ImageImportSize(40,30,orientation);RectF result=new RectF(0,0,40,30);PlacedImageSource.orientation(size).mapRect(result);
      assertEquals(new RectF(0,0,size.displayWidth(),size.displayHeight()),result);
    }
    PlacedImageTransform t=new PlacedImageTransform(400,300,800,800);float[] point={100,100};t.matrix().mapPoints(point);t.cropTo(new RectF(100,50,350,275));float[] after={100,100};t.matrix().mapPoints(after);assertArrayEquals(point,after,.001f);
  }
}
