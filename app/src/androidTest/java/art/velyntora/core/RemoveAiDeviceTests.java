package art.velyntora.core;
import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.Arrays;
/** Runs the actual bundled model on Android, not a mock inpainting substitute. */
public final class RemoveAiDeviceTests {
 @Test public void officialModelRectangularImageAlphaAndHistory()throws Exception{
  android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  int width=160,height=96,count=width*height;int[] original=new int[count];byte[] mask=new byte[count];Arrays.fill(original,0xff969696);
  for(int y=25;y<55;y++)for(int x=70;x<95;x++){int i=y*width+x;original[i]=0x80000000;mask[i]=1;}
  int[] result;
  try(RemoveAiBackend backend=new RemoveAiBackend()){
   result=backend.run(context,original,mask,width,height);
   int changed=0;for(int i=0;i<count;i++){assertEquals(original[i]>>>24,result[i]>>>24);if(mask[i]==0)assertEquals(original[i],result[i]);else if(result[i]!=original[i])changed++;}
   assertTrue("Real model must reconstruct masked pixels",changed>100);
   int[] repeated=backend.run(context,original,mask,width,height);assertArrayEquals("Reused session must be deterministic",result,repeated);
   try{backend.run(context,original,new byte[count],width,height);fail("Empty mask");}catch(IllegalArgumentException expected){}
   byte[] full=new byte[count];Arrays.fill(full,(byte)1);try{backend.run(context,original,full,width,height);fail("No background");}catch(IllegalArgumentException expected){}
  }
  System.loadLibrary("velyntora_jni");
  assertTrue(DrawingView.nativeCreate(width,height));assertTrue(DrawingView.nativeImport(original));
  long revision=DrawingView.nativeRevision();int layer=DrawingView.nativeActiveLayer();
  assertFalse(DrawingView.nativeRemoveCommit(result,mask,width,height,layer,revision-1));assertArrayEquals(original,DrawingView.nativeRemoveSource());
  assertFalse(DrawingView.nativeRemoveCommit(result,mask,width,height,layer+1,revision));
  assertTrue(DrawingView.nativeRemoveCommit(result,mask,width,height,layer,revision));assertArrayEquals(result,DrawingView.nativeRemoveSource());
  assertTrue(DrawingView.nativeUndo());assertArrayEquals(original,DrawingView.nativeRemoveSource());
  assertTrue(DrawingView.nativeRedo());assertArrayEquals(result,DrawingView.nativeRemoveSource());
 }
 @Test public void maskDoesNotEditDocumentAndClearsOnToolSwitch(){
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);MainActivity activity=(MainActivity)inst.startActivitySync(intent);
  try{inst.runOnMainSync(()->{
   DrawingView v=activity.readDrawing();v.setTool(DrawingView.REMOVE_AI);long revision=DrawingView.nativeRevision();int[] source=DrawingView.nativeRemoveSource();
   v.removeAi().down(30,30);v.removeAi().cancelStroke();assertEquals(revision,DrawingView.nativeRevision());assertArrayEquals(source,DrawingView.nativeRemoveSource());
   v.removeAi().down(40,40);v.setTool(DrawingView.BRUSH);v.setTool(DrawingView.REMOVE_AI);assertEquals(revision,DrawingView.nativeRevision());
   android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(v.readCanvasWidth(),v.readCanvasHeight(),android.graphics.Bitmap.Config.ARGB_8888);
   try{v.removeAi().preview(new android.graphics.Canvas(bitmap));assertEquals(0,bitmap.getPixel(40,40));}finally{bitmap.recycle();}
   assertNotNull(Tools.icon(DrawingView.REMOVE_AI,activity));
  });}finally{inst.runOnMainSync(activity::finish);}
 }
}
