package art.velyntora.core;
import java.util.Arrays;
class DrawingView {
 static {System.loadLibrary("velyntora_host");}
 static native boolean nativeCreate(int w,int h);
 static native boolean nativeLoadBitmap(int w,int h,int[] pixels);
 static native boolean nativeAddLayer();
 static native boolean nativePasteSelection(int[] data,int x,int y);
 static native int[] nativePixels();
 static native boolean nativeUndo();
 static native boolean nativeRedo();
}
public final class NativeRasterTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  check(DrawingView.nativeLoadBitmap(4,3,new int[12]));
  check(DrawingView.nativeAddLayer());
  int[] before=DrawingView.nativePixels();
  check(DrawingView.nativePasteSelection(new int[]{2,2,0x80ff0000,0,0xff0000ff,0xff00ff00},1,1));
  int[] pasted=DrawingView.nativePixels();check(pasted[5]==0x80ff0000&&pasted[6]==0&&pasted[9]==0xff0000ff&&pasted[10]==0xff00ff00);
  check(DrawingView.nativeUndo());check(Arrays.equals(before,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(!DrawingView.nativePasteSelection(new int[]{2,2,1},0,0));check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(!DrawingView.nativeLoadBitmap(4,3,new int[1]));check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(DrawingView.nativePasteSelection(new int[]{1,1,0x800000ff},1,1));
  int blended=DrawingView.nativePixels()[5];check((blended>>>24)==192);check(((blended>>>16)&255)>=84&&((blended>>>16)&255)<=86);
  check((blended&255)>=169&&(blended&255)<=171);
  System.out.println("JNI raster: transparency, source-over composition, atomic import, and undo/redo passed");
 }
}
