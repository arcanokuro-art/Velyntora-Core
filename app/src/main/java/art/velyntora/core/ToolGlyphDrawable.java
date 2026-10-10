package art.velyntora.core;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Monochrome tool geometry drawn at a consistent 24 dp optical size. */
final class ToolGlyphDrawable extends Drawable {
 private final int tool;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 ToolGlyphDrawable(int tool){this.tool=tool;p.setColor(0xffeeeeee);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 private void line(Canvas c,float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
 private void node(Canvas c,float x,float y){c.drawRect(x-2,y-2,x+2,y+2,p);}
 @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
  switch(tool){
   case DrawingView.BRUSH:line(c,8,15,18,3,21,6,11,17);c.drawOval(3,15,11,21,p);break;
   case DrawingView.PENCIL:line(c,3,21,5,15,17,3,21,7,9,19,3,21);line(c,15,5,19,9);break;
   case DrawingView.ERASER:line(c,3,14,14,3,21,10,10,21,7,21,3,17,3,14);line(c,7,10,15,18);break;
   case DrawingView.PICKER:line(c,4,20,6,14,15,5,19,9,10,18,4,20);line(c,13,3,21,11);c.drawCircle(19,5,3,p);break;
   case DrawingView.BUCKET:line(c,3,10,11,2,20,11,12,19,3,10);line(c,3,11,20,11);line(c,7,2,15,10);line(c,3,22,21,22);c.drawCircle(21,16,1,p);break;
   case DrawingView.LINE:line(c,5,18,9,9,18,5);node(c,5,18);node(c,9,9);node(c,18,5);break;
   case DrawingView.SELECT_RECTANGLE:p.setPathEffect(new DashPathEffect(new float[]{1,3},0));c.drawRect(3,3,21,21,p);p.setPathEffect(null);break;
   case DrawingView.SELECT_ELLIPSE:p.setPathEffect(new DashPathEffect(new float[]{1,3},0));c.drawOval(3,3,21,21,p);p.setPathEffect(null);break;
   case DrawingView.MOVE_SELECTION:p.setPathEffect(new DashPathEffect(new float[]{1,3},0));c.drawRect(3,3,21,21,p);p.setPathEffect(null);move(c,6);break;
   case DrawingView.MOVE_PIXELS:move(c,9);break;
   case DrawingView.PAN:line(c,4,14,7,17,7,7,10,7,10,4,13,4,13,7,16,7,16,10,19,10,19,18,16,21,10,21,4,14);break;
   case DrawingView.ZOOM:c.drawCircle(10,10,6,p);line(c,15,15,21,21);break;
   case DrawingView.RECTANGLE:case DrawingView.FILLED_RECTANGLE:c.drawRect(3,3,21,21,p);break;
   case DrawingView.ROUNDED_RECTANGLE:case DrawingView.FILLED_ROUNDED_RECTANGLE:c.drawRoundRect(3,3,21,21,5,5,p);break;
   case DrawingView.ELLIPSE:case DrawingView.CIRCLE:case DrawingView.FILLED_ELLIPSE:c.drawOval(3,3,21,21,p);break;
   case DrawingView.TRIANGLE:case DrawingView.FILLED_TRIANGLE:line(c,12,3,22,21,2,21,12,3);break;
   case DrawingView.SELECT_FREE:c.drawOval(3,4,21,14,p);line(c,15,12,20,18,15,21,5,21);break;
   case DrawingView.FREEFORM:line(c,3,21,10,13,14,4,21,4,21,21,3,21);break;
   case DrawingView.TEXT:p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(21);p.setStyle(Paint.Style.FILL);c.drawText("T",2,20,p);p.setTextSize(15);c.drawText("T",13,20,p);p.setStyle(Paint.Style.STROKE);break;
   case DrawingView.CLONE:c.drawCircle(12,7,4,p);line(c,9,11,7,17,17,17,15,11);c.drawRect(4,18,20,22,p);break;
   case DrawingView.RECOLOR:c.drawCircle(8,15,5,p);c.drawCircle(17,6,3,p);line(c,15,12,20,17,15,17);break;
   case DrawingView.GRADIENT:for(int i=0;i<6;i++){p.setAlpha(40+i*40);p.setStrokeWidth(3);line(c,3+i*3.5f,3,3+i*3.5f,21);}p.setAlpha(255);p.setStrokeWidth(1.8f);break;
   case DrawingView.MAGIC_WAND:line(c,3,21,16,8,19,11,6,24);line(c,5,3,5,8);line(c,2,5,8,5);line(c,19,2,19,6);line(c,17,4,21,4);break;
  }
  if(tool==DrawingView.FILLED_RECTANGLE||tool==DrawingView.FILLED_ELLIPSE||tool==DrawingView.FILLED_ROUNDED_RECTANGLE||tool==DrawingView.FILLED_TRIANGLE){p.setStyle(Paint.Style.FILL);c.drawCircle(19,20,3,p);p.setStyle(Paint.Style.STROKE);}
  c.restore();
 }
 private void move(Canvas c,float reach){float a=12-reach,b=12+reach;line(c,a,12,b,12);line(c,12,a,12,b);line(c,a+3,9,a,12,a+3,15);line(c,b-3,9,b,12,b-3,15);line(c,9,a+3,12,a,15,a+3);line(c,9,b-3,12,b,15,b-3);}
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
