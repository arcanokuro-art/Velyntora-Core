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
 static native boolean nativeTransformSelection(int left,int top,int width,int height,byte[] mask,float degrees,float sx,float sy);
 static native boolean nativeBlur(int kind,int amount,int angle,int cx,int cy);
 static native boolean nativeColorAdjustment(int kind,int[] values);
 static native boolean nativeEffect(int kind,int amount);
 static native void nativeSetBrushSelection(byte[] mask);
 static native void nativeStyledStroke(float x0,float y0,float x1,float y1,float radius,int color,float opacity,float hardness,boolean square,boolean eraser);
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
  check(DrawingView.nativeLoadBitmap(4,3,new int[12]));byte[] mask=new byte[12];mask[5]=1;
  DrawingView.nativeSetBrushSelection(mask);DrawingView.nativeStyledStroke(1.5f,1.5f,1.5f,1.5f,4,0xffffffff,1,1,false,false);
  int[] selected=DrawingView.nativePixels();check(selected[5]==0xffffffff&&selected[4]==0&&selected[6]==0);
  int[] red = {0,0xffff0000,0};check(DrawingView.nativeLoadBitmap(3,1,red));
  check(DrawingView.nativeEffect(0,1));check(DrawingView.nativePixels()[1]==0x55ff0000);
  check(DrawingView.nativeUndo());check(Arrays.equals(red,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[1]==0x55ff0000);
  int[] stripes={0xffff0000,0xff00ff00,0xff0000ff};check(DrawingView.nativeLoadBitmap(3,1,stripes));
  check(DrawingView.nativeTransformSelection(0,0,3,1,new byte[]{1,1,1},0,-1,1));
  check(DrawingView.nativePixels()[0]==stripes[2]&&DrawingView.nativePixels()[2]==stripes[0]);
  check(DrawingView.nativeUndo());check(Arrays.equals(stripes,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[0]==stripes[2]);
  int[] colors={0x80ff0000,0xff204080,0};check(DrawingView.nativeLoadBitmap(3,1,colors));
  int[] curve=new int[257];for(int i=0;i<256;i++)curve[i]=255-i;
  check(DrawingView.nativeColorAdjustment(0,curve));check(DrawingView.nativePixels()[0]==0x8000ffff);
  check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[0]==0x8000ffff);
  int[] adjusted=DrawingView.nativePixels();check(!DrawingView.nativeColorAdjustment(0,new int[2]));check(!DrawingView.nativeColorAdjustment(1,new int[15]));check(Arrays.equals(adjusted,DrawingView.nativePixels()));
  check(DrawingView.nativeLoadBitmap(3,1,colors));check(DrawingView.nativeColorAdjustment(4,new int[]{120,100,0}));check(DrawingView.nativePixels()[0]==0x8000ff00);check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(!DrawingView.nativeColorAdjustment(4,new int[]{0,100,0}));check(!DrawingView.nativeUndo());
  check(DrawingView.nativeColorAdjustment(1,new int[]{0,255,100,0,255,0,255,100,0,255,0,255,100,20,255}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(2,new int[]{0}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(3,new int[]{2,4,8}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(5,new int[]{15,20}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  for(int kind=0;kind<3;kind++){check(DrawingView.nativeColorAdjustment(6,new int[]{kind}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));}
  int[] impulse={0,0xffff0000,0};check(DrawingView.nativeLoadBitmap(3,1,impulse));
  for(int kind=0;kind<4;kind++){check(DrawingView.nativeBlur(kind,kind==2?45:4,35,25,50));check(DrawingView.nativeUndo());check(Arrays.equals(impulse,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  check(!DrawingView.nativeBlur(0,33,0,50,50));check(Arrays.equals(impulse,DrawingView.nativePixels()));
  System.out.println("JNI raster: transparency, source-over composition, atomic import, and undo/redo passed");
 }
}
