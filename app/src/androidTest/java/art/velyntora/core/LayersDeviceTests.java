package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
public final class LayersDeviceTests {
 private static <T> T find(View view,Class<T> type){if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){T result=find(group.getChildAt(i),type);if(result!=null)return result;}}return null;}
 @Test public void propertiesCancelAcceptAndUndoAreAtomic(){
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);MainActivity activity=(MainActivity)inst.startActivitySync(intent);
  inst.runOnMainSync(()->{try{DrawingView drawing=activity.readDrawing();assertTrue(drawing.addLayer());int i=drawing.activeLayer();String original=drawing.layerName(i);AlertDialog dialog=LayerPropertiesDialog.show(activity,i);find(dialog.getWindow().getDecorView(),EditText.class).setText("Prueba");find(dialog.getWindow().getDecorView(),Spinner.class).setSelection(8);dialog.getButton(-2).performClick();assertEquals(original,drawing.layerName(i));assertEquals(0,drawing.layerBlendMode(i));
   dialog=LayerPropertiesDialog.show(activity,i);find(dialog.getWindow().getDecorView(),EditText.class).setText("Prueba");find(dialog.getWindow().getDecorView(),Spinner.class).setSelection(8);find(dialog.getWindow().getDecorView(),CheckBox.class).setChecked(false);find(dialog.getWindow().getDecorView(),SeekBar.class).setProgress(50); // programmatic slider updates the numeric field through user input below
   LinearLayout opacityRow=(LinearLayout)find(dialog.getWindow().getDecorView(),SeekBar.class).getParent();((EditText)opacityRow.getChildAt(0)).setText("50");dialog.getButton(-1).performClick();assertFalse(dialog.isShowing());assertEquals("Prueba",drawing.layerName(i));assertEquals(8,drawing.layerBlendMode(i));assertFalse(drawing.layerVisible(i));assertEquals(.5f,drawing.layerOpacity(),.001f);drawing.undo();assertEquals(original,drawing.layerName(i));assertEquals(0,drawing.layerBlendMode(i));assertTrue(drawing.layerVisible(i));assertEquals(1f,drawing.layerOpacity(),.001f);
   assertTrue(drawing.layerAction(0));assertEquals(3,drawing.layerCount());assertTrue(drawing.layerAction(1));assertEquals(2,drawing.layerCount());activity.refreshLayerPanel();LinearLayout items=activity.readLayerItems();assertEquals(2,items.getChildCount());LinearLayout row=(LinearLayout)items.getChildAt(0);assertEquals(3,row.getChildCount());assertTrue(row.getChildAt(0) instanceof ImageView);assertTrue(row.getChildAt(1) instanceof TextView);assertTrue(row.getChildAt(2) instanceof CheckBox);
  }finally{activity.finish();}});
 }

 @Test public void captureLayersAndProperties() throws Exception {
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);MainActivity activity=(MainActivity)inst.startActivitySync(intent);
  java.util.concurrent.atomic.AtomicReference<AlertDialog> dialog=new java.util.concurrent.atomic.AtomicReference<>();
  try{
   inst.runOnMainSync(()->{for(int i=activity.readDrawing().layerCount();i<12;i++)assertTrue(activity.readDrawing().addLayer());activity.refreshLayerPanel();activity.readToolScroll().setVisibility(View.GONE);activity.readLayerScroll().setVisibility(View.VISIBLE);});inst.waitForIdleSync();
   java.io.File folder=new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),"velyntora-core-workspace");folder.mkdirs();
   android.graphics.Bitmap image=inst.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,"layers-panel.png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}image.recycle();
   inst.runOnMainSync(()->dialog.set(LayerPropertiesDialog.show(activity,activity.readDrawing().activeLayer())));inst.waitForIdleSync();image=inst.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,"layers-properties.png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}image.recycle();
  }finally{inst.runOnMainSync(()->{if(dialog.get()!=null)dialog.get().dismiss();activity.finish();});}
 }
}
