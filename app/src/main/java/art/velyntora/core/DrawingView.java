package art.velyntora.core;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
public final class DrawingView extends View {
 static {System.loadLibrary("velyntora_jni");}
 private static native boolean nativeCreate(int w,int h);
 private static native void nativeBeginEdit();
 private static native void nativeStroke(float x0,float y0,float x1,float y1,float radius,int color);
 private static native void nativeShape(int kind,int x0,int y0,int x1,int y1,int color);
 private static native void nativeFill(int x,int y,int color);
 private static native boolean nativeUndo();
 private static native boolean nativeRedo();
 private static native int[] nativePixels();
 private static final int SIZE=800;
 public static final int BRUSH=0,RECTANGLE=1,ELLIPSE=2,LINE=3,BUCKET=4;
 private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
 private final Bitmap bitmap=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
 private int color=0xFF202020,tool=BRUSH;
 private float previousX,previousY,startX,startY;
 private boolean drawing;
 public DrawingView(Context context){super(context);if(!nativeCreate(SIZE,SIZE))throw new IllegalStateException("Canvas error");refresh();}
 public void setColor(int value){color=value;}
 public void setTool(int value){tool=value;}
 public void clear(){if(nativeCreate(SIZE,SIZE))refresh();}
 public void undo(){if(nativeUndo())refresh();}
 public void redo(){if(nativeRedo())refresh();}
 public Bitmap snapshot(){return bitmap.copy(Bitmap.Config.ARGB_8888,false);}
 private void refresh(){int[] pixels=nativePixels();if(pixels!=null)bitmap.setPixels(pixels,0,SIZE,0,0,SIZE,SIZE);invalidate();}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float scale=Math.min(getWidth()/(float)SIZE,getHeight()/(float)SIZE);float x=(getWidth()-SIZE*scale)/2f,y=(getHeight()-SIZE*scale)/2f;canvas.drawColor(0xFFE3E3E3);canvas.drawBitmap(bitmap,null,new RectF(x,y,x+SIZE*scale,y+SIZE*scale),paint);}
 @Override public boolean onTouchEvent(MotionEvent event){float scale=Math.min(getWidth()/(float)SIZE,getHeight()/(float)SIZE);if(scale<=0)return false;float left=(getWidth()-SIZE*scale)/2f,top=(getHeight()-SIZE*scale)/2f;float x=(event.getX()-left)/scale,y=(event.getY()-top)/scale;
 switch(event.getActionMasked()){
 case MotionEvent.ACTION_DOWN:
  if(x<0||y<0||x>=SIZE||y>=SIZE)return false;
  drawing=true;startX=previousX=x;startY=previousY=y;nativeBeginEdit();
  if(tool==BUCKET){nativeFill((int)x,(int)y,color);drawing=false;refresh();}
  else if(tool==BRUSH){nativeStroke(x,y,x,y,4f,color);refresh();}
  return true;
 case MotionEvent.ACTION_MOVE:
  if(!drawing)return true;
  if(tool==BRUSH){nativeStroke(previousX,previousY,x,y,4f,color);refresh();}
  previousX=x;previousY=y;return true;
 case MotionEvent.ACTION_UP:
  if(drawing){
   if(tool==BRUSH)nativeStroke(previousX,previousY,x,y,4f,color);
   else nativeShape(tool,(int)startX,(int)startY,(int)x,(int)y,color);
   drawing=false;refresh();
  }return true;
 case MotionEvent.ACTION_CANCEL:drawing=false;return true;
 default:return true;}
 }
}
