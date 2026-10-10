package art.velyntora.core;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;

/** Runs against the actual Android widgets, JNI engine, and lifecycle on an emulator. */
public final class WorkspaceDeviceTests {
 private Instrumentation instrumentation;private MainActivity activity;
 @Before public void open(){
  instrumentation=InstrumentationRegistry.getInstrumentation();
  Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  activity=(MainActivity)instrumentation.startActivitySync(intent);instrumentation.waitForIdleSync();
 }
 @After public void close(){instrumentation.runOnMainSync(()->activity.finish());instrumentation.waitForIdleSync();}
 private DrawingView drawing(){return findDrawing(activity.getWindow().getDecorView());}
 private DrawingView findDrawing(View view){if(view instanceof DrawingView)return (DrawingView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){DrawingView found=findDrawing(group.getChildAt(i));if(found!=null)return found;}}return null;}
 private Button findButton(View view,String label){if(view instanceof Button&&label.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 @Test public void rotationRetainsLayersAndGivesCanvasSpace(){
  instrumentation.runOnMainSync(()->{assertTrue(drawing().newDocument(64,32));assertTrue(drawing().addLayer());});
  int count=drawing().layerCount();long revision=drawing().revision();
  for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}){
   instrumentation.runOnMainSync(()->activity.setRequestedOrientation(orientation));
   instrumentation.waitForIdleSync();
   long end=android.os.SystemClock.uptimeMillis()+5000;
   while(android.os.SystemClock.uptimeMillis()<end){int wanted=orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?2:1;if(activity.getResources().getConfiguration().orientation==wanted)break;android.os.SystemClock.sleep(50);}
   instrumentation.waitForIdleSync();
   instrumentation.runOnMainSync(()->{assertNotNull(drawing());assertTrue(drawing().getWidth()>0);assertTrue(drawing().getHeight()>48*activity.getResources().getDisplayMetrics().density);assertEquals(count,drawing().layerCount());assertEquals(revision,drawing().revision());});
  }
 }
 @Test public void unsavedBackOffersCancelWithoutLosingLayers(){
  instrumentation.runOnMainSync(()->{assertTrue(drawing().newDocument(16,16));assertTrue(drawing().addLayer());activity.onBackPressed();});instrumentation.waitForIdleSync();
  // Dialog windows are separate roots; use UiAutomation to observe the user-facing choice.
  android.view.accessibility.AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
  java.util.List<android.view.accessibility.AccessibilityNodeInfo> choices=root.findAccessibilityNodeInfosByText("Cancelar");assertFalse(choices.isEmpty());
  long revision=drawing().revision();assertTrue(choices.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));instrumentation.waitForIdleSync();
  assertFalse(activity.isFinishing());assertEquals(2,drawing().layerCount());assertEquals(revision,drawing().revision());
 }
 @Test public void projectAndRecoveryPreserveDimensionsAndRejectStaleRevision()throws Exception{
  java.io.File file=new java.io.File(instrumentation.getTargetContext().getCacheDir(),"device-recovery-test.vlycore");
  instrumentation.runOnMainSync(()->assertTrue(drawing().newDocument(23,17)));
  long revision=drawing().revision();
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_CREATE|android.os.ParcelFileDescriptor.MODE_READ_WRITE|android.os.ParcelFileDescriptor.MODE_TRUNCATE)){
   assertFalse(drawing().writeRecovery(fd.getFd(),revision-1));assertEquals(0,file.length());assertTrue(drawing().writeRecovery(fd.getFd(),revision));
  }
  instrumentation.runOnMainSync(()->assertTrue(drawing().newDocument(3,2)));
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)){assertTrue(drawing().readProject(fd.getFd()));}
  instrumentation.runOnMainSync(()->{drawing().projectOpened();assertEquals(23,drawing().documentWidth());assertEquals(17,drawing().documentHeight());});assertTrue(file.delete());
 }
 @Test public void quickCommandsHaveAccessibleTouchTargets(){
  instrumentation.runOnMainSync(()->{float density=activity.getResources().getDisplayMetrics().density;for(String label:new String[]{"Archivo","Editar","Ver","Nuevo","Guardar","Deshacer","Rehacer"}){
   Button button=findButton(activity.getWindow().getDecorView(),label);assertNotNull(label,button);assertNotNull(label,button.getContentDescription());assertTrue(label,button.getMeasuredHeight()>=Math.round(48*density));
  }});
 }
}
