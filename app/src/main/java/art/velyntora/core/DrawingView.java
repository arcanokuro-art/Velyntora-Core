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
 private static native boolean nativeClear();
 private static native boolean nativeFlipActiveHorizontal();
 private static native boolean nativeFlipActiveVertical();
 private static native boolean nativeRotateActive180();
 private static native boolean nativeRotateActive90(boolean clockwise);
 private static native boolean nativeCropActiveSelection(int left,int top,int right,int bottom);
 private static native boolean nativeCropActiveEllipse(int left,int top,int right,int bottom);
 private static native boolean nativeTrimActiveMasked(int left,int top,int width,int height,byte[] mask);
 private static native boolean nativeInvertActiveColors();
 private static native boolean nativeGrayscaleActive();
 private static native boolean nativeSepiaActive();
 private static native boolean nativeBrightnessActive(int adjustment);
 private static native boolean nativeContrastActive(int adjustment);
 private static native boolean nativeThresholdActive(int threshold);
 private static native void nativeStroke(float x0,float y0,float x1,float y1,float radius,int color);
 private static native void nativeShape(int kind,int x0,int y0,int x1,int y1,int color);
 private static native void nativeFill(int x,int y,int color);
 private static native boolean nativeUndo();
 private static native boolean nativeRedo();
 private static native int[] nativePixels();
 private static native boolean nativeImport(int[] pixels);
 private static native int nativePickColor(int x,int y);
 private static native boolean nativeEraseSelection(int kind,int x0,int y0,int x1,int y1);
 private static native boolean nativeEraseMaskedSelection(int x,int y,int w,int h,byte[] mask);
 private static native boolean nativeMoveMaskedSelection(int x,int y,int w,int h,byte[] mask,int dx,int dy);
 private static native int[] nativeCopySelection(int kind,int x0,int y0,int x1,int y1);
 private static native boolean nativePasteSelection(int[] data,int x,int y);
 private static native boolean nativeMovePixels(int kind,int x0,int y0,int x1,int y1,int dx,int dy);
 private static native int nativeLayerCount();
 private static native int nativeActiveLayer();
 private static native boolean nativeAddLayer();
 private static native boolean nativeSelectLayer(int index);
 private static native boolean nativeDeleteLayer();
 private static native boolean nativeToggleLayer();
 private static native boolean nativeLayerVisible(int index);
 private static native boolean nativeMoveLayer(int direction);
 private static native boolean nativeSetLayerOpacity(float opacity);
 private static native float nativeLayerOpacity();
 private static native int[] nativeLayerThumbnail(int index);
 private static final int SIZE=800;
 public static final int BRUSH=0,RECTANGLE=1,ELLIPSE=2,LINE=3,BUCKET=4,ERASER=5,PICKER=6,FILLED_RECTANGLE=7,FILLED_ELLIPSE=8,SELECT_RECTANGLE=9,SELECT_ELLIPSE=10,MOVE_SELECTION=11,MOVE_PIXELS=12,SELECT_FREE=13,MAGIC_WAND=14;
 private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
 private final Bitmap bitmap=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
 private int color=0xFF202020,tool=BRUSH;
 private float brushRadius=4f;
 private float previousX,previousY,startX,startY;
 private boolean drawing;
 private boolean hasSelection;
 private boolean movingSelection;
 private boolean movingPixels;
 private final android.graphics.Path freePath=new android.graphics.Path();
 private boolean freeSelectionReady;
 private final android.graphics.Region freeRegion=new android.graphics.Region();
 private int wandTolerance=0;
 private final android.graphics.Path wandBoundary=new android.graphics.Path();
 public void setWandTolerance(int value){wandTolerance=Math.max(0,Math.min(255,value));}
 private float moveStartX,moveStartY,moveOriginalLeft,moveOriginalTop,moveOriginalRight,moveOriginalBottom;
 private int selectionTool;
 private float selectionLeft,selectionTop,selectionRight,selectionBottom;
 private final Paint selectionPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private Runnable canvasChangedListener;
 private java.util.function.IntConsumer pickedColorListener;
 public void setOnColorPickedListener(java.util.function.IntConsumer listener){pickedColorListener=listener;}
 public void setOnCanvasChangedListener(Runnable listener){canvasChangedListener=listener;}
 public DrawingView(Context context){super(context);if(!nativeCreate(SIZE,SIZE))throw new IllegalStateException("Canvas error");refresh();}
 public boolean setLayerOpacity(float opacity){boolean ok=nativeSetLayerOpacity(opacity);if(ok)refresh();return ok;}
 public float layerOpacity(){return nativeLayerOpacity();}
 public Bitmap layerThumbnail(int index){
  int[] pixels=nativeLayerThumbnail(index);
  if(pixels==null||pixels.length!=48*48)return null;
  return Bitmap.createBitmap(pixels,48,48,Bitmap.Config.ARGB_8888);
 }
 public int layerCount(){return nativeLayerCount();}
 public int activeLayer(){return nativeActiveLayer();}
 public boolean layerVisible(int index){return nativeLayerVisible(index);}
 public boolean addLayer(){boolean ok=nativeAddLayer();if(ok)refresh();return ok;}
 public boolean selectLayer(int index){boolean ok=nativeSelectLayer(index);if(ok)refresh();return ok;}
 public boolean deleteLayer(){boolean ok=nativeDeleteLayer();if(ok)refresh();return ok;}
 public boolean toggleLayer(){boolean ok=nativeToggleLayer();if(ok)refresh();return ok;}
 public boolean moveLayer(int direction){boolean ok=nativeMoveLayer(direction);if(ok)refresh();return ok;}
 public void setColor(int value){color=value;}
 public void setBrushRadius(float radius){if(Float.isFinite(radius)&&radius>=1f&&radius<=128f)brushRadius=radius;}
 public float brushRadius(){return brushRadius;}
 public void setTool(int value){
  if(value<BRUSH||value>MAGIC_WAND)return;
  tool=value;
  // Switching away from a selection tool must not leave a selection
  // permanently active as an accidental overlay on subsequent drawings.
  invalidate();
 }
 public boolean hasSelection(){return hasSelection&&selectionRight-selectionLeft>=1f&&selectionBottom-selectionTop>=1f;}
 public boolean shrinkSelectionOnePixel(){
  if(!hasSelection())return false;
  android.graphics.Region original=new android.graphics.Region();
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND)original.set(freeRegion);
  else if(selectionTool==SELECT_RECTANGLE)
   original.set((int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),
     (int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  else if(selectionTool==SELECT_ELLIPSE){
   android.graphics.Path ellipse=new android.graphics.Path();
   ellipse.addOval(new RectF(selectionLeft,selectionTop,selectionRight,selectionBottom),
     android.graphics.Path.Direction.CW);
   original.setPath(ellipse,new android.graphics.Region(0,0,SIZE,SIZE));
  }else return false;
  original.op(0,0,SIZE,SIZE,android.graphics.Region.Op.INTERSECT);
  android.graphics.Region eroded=new android.graphics.Region(original);
  android.graphics.Region shifted=new android.graphics.Region();
  for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){
   if(dx==0&&dy==0)continue;
   original.translate(dx,dy,shifted);
   eroded.op(shifted,android.graphics.Region.Op.INTERSECT);
  }
  // A fully eroded selection is a valid result: clear it.
  if(eroded.isEmpty()){deselect();return true;}
  freeRegion.set(eroded);freePath.reset();
  wandBoundary.set(freeRegion.getBoundaryPath());
  android.graphics.Rect bounds=freeRegion.getBounds();
  selectionLeft=bounds.left;selectionTop=bounds.top;
  selectionRight=bounds.right;selectionBottom=bounds.bottom;
  selectionTool=MAGIC_WAND;hasSelection=true;
  invalidate();return true;
 }
 public boolean expandSelectionOnePixel(){
  if(!hasSelection())return false;
  android.graphics.Region region=new android.graphics.Region();
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND)region.set(freeRegion);
  else if(selectionTool==SELECT_RECTANGLE)
   region.set((int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),
     (int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  else if(selectionTool==SELECT_ELLIPSE){
   android.graphics.Path ellipse=new android.graphics.Path();
   ellipse.addOval(new RectF(selectionLeft,selectionTop,selectionRight,selectionBottom),
     android.graphics.Path.Direction.CW);
   region.setPath(ellipse,new android.graphics.Region(0,0,SIZE,SIZE));
  }else return false;
  region.op(0,0,SIZE,SIZE,android.graphics.Region.Op.INTERSECT);
  android.graphics.Region grown=new android.graphics.Region(region);
  android.graphics.Region offset=new android.graphics.Region();
  for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){
   if(dx==0&&dy==0)continue;
   region.translate(dx,dy,offset);
   grown.op(offset,android.graphics.Region.Op.UNION);
  }
  grown.op(0,0,SIZE,SIZE,android.graphics.Region.Op.INTERSECT);
  if(grown.isEmpty())return false;
  freeRegion.set(grown);freePath.reset();
  wandBoundary.set(freeRegion.getBoundaryPath());
  android.graphics.Rect bounds=freeRegion.getBounds();
  selectionLeft=bounds.left;selectionTop=bounds.top;
  selectionRight=bounds.right;selectionBottom=bounds.bottom;
  selectionTool=MAGIC_WAND;hasSelection=true;
  invalidate();return true;
 }
 public boolean invertSelection(){
  if(!hasSelection())return false;
  android.graphics.Region selected=new android.graphics.Region();
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND){
   selected.set(freeRegion);
  }else if(selectionTool==SELECT_RECTANGLE){
   selected.set((int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),
     (int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  }else if(selectionTool==SELECT_ELLIPSE){
   android.graphics.Path ellipse=new android.graphics.Path();
   ellipse.addOval(new RectF(selectionLeft,selectionTop,selectionRight,selectionBottom),
     android.graphics.Path.Direction.CW);
   selected.setPath(ellipse,new android.graphics.Region(0,0,SIZE,SIZE));
  }else return false;
  selected.op(0,0,SIZE,SIZE,android.graphics.Region.Op.INTERSECT);
  android.graphics.Region inverted=new android.graphics.Region(0,0,SIZE,SIZE);
  inverted.op(selected,android.graphics.Region.Op.DIFFERENCE);
  if(inverted.isEmpty()){deselect();return true;}
  freeRegion.set(inverted);
  freePath.reset();
  wandBoundary.set(freeRegion.getBoundaryPath());
  android.graphics.Rect bounds=freeRegion.getBounds();
  selectionLeft=bounds.left;selectionTop=bounds.top;
  selectionRight=bounds.right;selectionBottom=bounds.bottom;
  selectionTool=MAGIC_WAND;hasSelection=true;
  invalidate();return true;
 }
 public boolean selectionContains(int px,int py){
  if(!hasSelection()||px<0||py<0||px>=SIZE||py>=SIZE||px<selectionLeft||py<selectionTop||px>=selectionRight||py>=selectionBottom)return false;
  if(selectionTool==SELECT_RECTANGLE)return true;
  if(selectionTool==SELECT_FREE)return freeSelectionReady&&freeRegion.contains(px,py);
  if(selectionTool==MAGIC_WAND)return freeRegion.contains(px,py);
  final float rx=(selectionRight-selectionLeft)/2f,ry=(selectionBottom-selectionTop)/2f;
  if(rx<=0f||ry<=0f)return false;
  final float cx=(selectionRight+selectionLeft)/2f,cy=(selectionBottom+selectionTop)/2f;
  final float dx=(px+0.5f-cx)/rx,dy=(py+0.5f-cy)/ry;
  return dx*dx+dy*dy<=1f;
 }
 public boolean flipActiveHorizontal(){if(!nativeFlipActiveHorizontal())return false;deselect();refresh();return true;}
 public boolean flipActiveVertical(){if(!nativeFlipActiveVertical())return false;deselect();refresh();return true;}
 public boolean rotateActive180(){if(!nativeRotateActive180())return false;deselect();refresh();return true;}
 public boolean rotateActive90(boolean clockwise){if(!nativeRotateActive90(clockwise))return false;deselect();refresh();return true;}
 public boolean thresholdActive(int threshold){if(!nativeThresholdActive(threshold))return false;deselect();refresh();return true;}
 public boolean contrastActive(int adjustment){if(!nativeContrastActive(adjustment))return false;deselect();refresh();return true;}
 public boolean brightnessActive(int adjustment){if(!nativeBrightnessActive(adjustment))return false;deselect();refresh();return true;}
 public boolean sepiaActive(){if(!nativeSepiaActive())return false;deselect();refresh();return true;}
 public boolean grayscaleActive(){if(!nativeGrayscaleActive())return false;deselect();refresh();return true;}
 public boolean invertActiveColors(){if(!nativeInvertActiveColors())return false;deselect();refresh();return true;}
 public boolean trimActiveToFreeSelection(){
  if(!hasSelection()||(selectionTool!=SELECT_FREE&&selectionTool!=MAGIC_WAND))return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>SIZE||bottom>SIZE||left>=right||top>=bottom)return false;
  int width=right-left,height=bottom-top;
  byte[] mask=new byte[width*height];
  for(int y=0;y<height;++y)for(int x=0;x<width;++x)
   if(freeRegion.contains(left+x,top+y))mask[y*width+x]=1;
  if(!nativeTrimActiveMasked(left,top,width,height,mask))return false;
  deselect();refresh();return true;
 }
 public boolean trimActiveToEllipseSelection(){
  if(!hasSelection()||selectionTool!=SELECT_ELLIPSE)return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>SIZE||bottom>SIZE||left>=right||top>=bottom)return false;
  if(!nativeCropActiveEllipse(left,top,right,bottom))return false;
  deselect();refresh();return true;
 }
 public boolean trimActiveToRectSelection(){
  if(!hasSelection()||selectionTool!=SELECT_RECTANGLE)return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>SIZE||bottom>SIZE||left>=right||top>=bottom)return false;
  if(!nativeCropActiveSelection(left,top,right,bottom))return false;
  deselect();refresh();return true;
 }
 public void clear(){if(nativeClear()){deselect();refresh();}}
 public boolean pasteBitmap(Bitmap source){
  if(source==null||source.isRecycled())return false;
  int w=source.getWidth(),h=source.getHeight();
  if(w<=0||h<=0||w>SIZE||h>SIZE||((long)w*h)>SIZE*SIZE)return false;
  int[] data=new int[2+w*h];
  data[0]=w;data[1]=h;
  source.getPixels(data,2,w,0,0,w,h);
  int x=hasSelection()?(int)selectionLeft:(SIZE-w)/2;
  int y=hasSelection()?(int)selectionTop:(SIZE-h)/2;
  x=Math.max(0,Math.min(SIZE-w,x));
  y=Math.max(0,Math.min(SIZE-h,y));
  boolean ok=nativePasteSelection(data,x,y);
  if(ok){deselect();refresh();}
  return ok;
 }
 public Bitmap copySelection(){
  if(!hasSelection())return null;
  int leftBound=(int)Math.floor(selectionLeft),topBound=(int)Math.floor(selectionTop);
  int rightBound=(int)Math.ceil(selectionRight),bottomBound=(int)Math.ceil(selectionBottom);
  if(leftBound<0||topBound<0||rightBound>SIZE||bottomBound>SIZE||rightBound<=leftBound||bottomBound<=topBound)return null;
  int[] data=nativeCopySelection((selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND)?SELECT_RECTANGLE:selectionTool,(int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),(int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  if(data==null||data.length<3)return null;
  int w=data[0],h=data[1];
  if(w<=0||h<=0||w>SIZE||h>SIZE||w!=rightBound-leftBound||h!=bottomBound-topBound||((long)w*h)!=data.length-2)return null;
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND){
   int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
   for(int row=0;row<h;++row)for(int col=0;col<w;++col){
    if(!freeRegion.contains(left+col,top+row))data[2+row*w+col]=0;
   }
  }
  return Bitmap.createBitmap(data,2,w,w,h,Bitmap.Config.ARGB_8888);
 }
 public boolean eraseSelection(){
  if(!hasSelection())return false;
  boolean ok;
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND){
   int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
   int w=(int)Math.ceil(selectionRight)-left,h=(int)Math.ceil(selectionBottom)-top;
   if(w<=0||h<=0||left<0||top<0||left+w>SIZE||top+h>SIZE||((long)w*h)>SIZE*SIZE)return false;
   byte[] mask=new byte[w*h];
   for(int row=0;row<h;++row)for(int col=0;col<w;++col)
    if(freeRegion.contains(left+col,top+row))mask[row*w+col]=1;
   ok=nativeEraseMaskedSelection(left,top,w,h,mask);
  }else ok=nativeEraseSelection(selectionTool,(int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),(int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  if(ok){deselect();refresh();}
  return ok;
 }
 public void selectAll(){
  movingSelection=false;movingPixels=false;drawing=false;
  selectionTool=SELECT_RECTANGLE;
  selectionLeft=0f;selectionTop=0f;selectionRight=SIZE;selectionBottom=SIZE;
  hasSelection=true;invalidate();
 }
 public void deselect(){hasSelection=false;movingSelection=false;movingPixels=false;freeSelectionReady=false;freeRegion.setEmpty();freePath.reset();wandBoundary.reset();invalidate();}
 public void enableSelectionMove(){tool=MOVE_SELECTION;invalidate();}
 public boolean moveSelectedPixels(int dx,int dy){
  if(!hasSelection()||(dx==0&&dy==0))return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>SIZE||bottom>SIZE||right<=left||bottom<=top)return false;
  dx=Math.max(-left,Math.min(SIZE-right,dx));
  dy=Math.max(-top,Math.min(SIZE-bottom,dy));
  if(dx==0&&dy==0)return false;
  boolean ok;
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND){
   int w=right-left,h=bottom-top;
   if(w<=0||h<=0||((long)w*h)>SIZE*SIZE)return false;
   byte[] mask=new byte[w*h];
   for(int row=0;row<h;++row)for(int col=0;col<w;++col)
    if(freeRegion.contains(left+col,top+row))mask[row*w+col]=1;
   ok=nativeMoveMaskedSelection(left,top,w,h,mask,dx,dy);
   if(ok){
    android.graphics.Matrix matrix=new android.graphics.Matrix();
    matrix.setTranslate(dx,dy);
    freePath.transform(matrix);
    freeRegion.translate(dx,dy);
    wandBoundary.transform(matrix);
   }
  }else ok=nativeMovePixels(selectionTool,left,top,right,bottom,dx,dy);
  if(ok){selectionLeft+=dx;selectionRight+=dx;selectionTop+=dy;selectionBottom+=dy;refresh();}
  return ok;
 }
 public void undo(){if(nativeUndo()){deselect();refresh();}}
 public void redo(){if(nativeRedo()){deselect();refresh();}}
 public void loadBitmap(Bitmap source){
  if(source==null||source.isRecycled())return;
  Bitmap scaled=Bitmap.createScaledBitmap(source,SIZE,SIZE,true);
  try{int[] pixels=new int[SIZE*SIZE];scaled.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE);if(nativeImport(pixels)){deselect();refresh();}}
  finally{if(scaled!=source)scaled.recycle();}
 }
 public Bitmap snapshot(){return bitmap.copy(Bitmap.Config.ARGB_8888,false);}
 private void refresh(){int[] pixels=nativePixels();if(pixels==null||pixels.length!=SIZE*SIZE)return;bitmap.setPixels(pixels,0,SIZE,0,0,SIZE,SIZE);invalidate();if(canvasChangedListener!=null)canvasChangedListener.run();}
 private final Paint checkerPaint=new Paint();
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  if(getWidth()<=0||getHeight()<=0)return;
  float scale=Math.min(getWidth()/(float)SIZE,getHeight()/(float)SIZE);
  float x=(getWidth()-SIZE*scale)/2f,y=(getHeight()-SIZE*scale)/2f;
  canvas.drawColor(0xFFE3E3E3);
  // The checkerboard is drawn beneath the image, so transparent eraser
  // strokes reveal transparency instead of looking like opaque gray paint.
  canvas.save();
  canvas.clipRect(x,y,x+SIZE*scale,y+SIZE*scale);
  checkerPaint.setColor(0xFFFFFFFF);
  canvas.drawRect(x,y,x+SIZE*scale,y+SIZE*scale,checkerPaint);
  final float tile=16f*scale;
  if(tile>0f){
   checkerPaint.setColor(0xFFD1D1D1);
   for(int row=0;row<50;++row){
    for(int col=(row&1);col<50;col+=2){
     float left=x+col*tile,top=y+row*tile;
     canvas.drawRect(left,top,left+tile,top+tile,checkerPaint);
    }
   }
  }
  canvas.drawBitmap(bitmap,null,new RectF(x,y,x+SIZE*scale,y+SIZE*scale),paint);
  if(hasSelection()){
   selectionPaint.setColor(0xFF202020);
   selectionPaint.setStyle(Paint.Style.STROKE);
   selectionPaint.setStrokeWidth(Math.max(1f,1f/scale));
   selectionPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{6f/scale,4f/scale},0));
   RectF bounds=new RectF(x+selectionLeft*scale,y+selectionTop*scale,x+selectionRight*scale,y+selectionBottom*scale);
   if(selectionTool==SELECT_ELLIPSE)canvas.drawOval(bounds,selectionPaint);
   else if(selectionTool==SELECT_FREE){canvas.save();canvas.translate(x,y);canvas.scale(scale,scale);canvas.drawPath(freePath,selectionPaint);canvas.restore();}
   else if(selectionTool==MAGIC_WAND){canvas.save();canvas.translate(x,y);canvas.scale(scale,scale);canvas.drawPath(wandBoundary,selectionPaint);canvas.restore();}
   else canvas.drawRect(bounds,selectionPaint);
   selectionPaint.setPathEffect(null);
  }
  canvas.restore();
 }
 private static boolean withinTolerance(int color,int target,int tolerance){
  int da=Math.abs((color>>>24)-(target>>>24));
  int dr=Math.abs(((color>>>16)&255)-((target>>>16)&255));
  int dg=Math.abs(((color>>>8)&255)-((target>>>8)&255));
  int db=Math.abs((color&255)-(target&255));
  return Math.max(Math.max(da,dr),Math.max(dg,db))<=tolerance;
 }
 private void selectMatchingRegion(int sx,int sy){
  if(sx<0||sy<0||sx>=SIZE||sy>=SIZE)return;
  int[] pixels=new int[SIZE*SIZE];
  bitmap.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE);
  int target=pixels[sy*SIZE+sx];
  byte[] visited=new byte[pixels.length];
  int[] queue=new int[pixels.length];
  int head=0,tail=0,start=sy*SIZE+sx;
  visited[start]=1;queue[tail++]=start;
  int minX=sx,maxX=sx,minY=sy,maxY=sy;
  freeRegion.setEmpty();freePath.reset();wandBoundary.reset();
  while(head<tail){
   int pos=queue[head++],px=pos%SIZE,py=pos/SIZE;
   visited[pos]=2;
   minX=Math.min(minX,px);maxX=Math.max(maxX,px);
   minY=Math.min(minY,py);maxY=Math.max(maxY,py);
   if(px>0){int q=pos-1;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(px<SIZE-1){int q=pos+1;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(py>0){int q=pos-SIZE;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(py<SIZE-1){int q=pos+SIZE;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
  }
  // Union horizontal runs, not individual pixels, to reduce Region operations.
  for(int row=minY;row<=maxY;++row){
   int col=minX;
   while(col<=maxX){
    while(col<=maxX&&visited[row*SIZE+col]!=2)++col;
    int left=col;
    while(col<=maxX&&visited[row*SIZE+col]==2)++col;
    if(left<col)freeRegion.op(left,row,col,row+1,android.graphics.Region.Op.UNION);
   }
  }
  selectionTool=MAGIC_WAND;
  selectionLeft=minX;selectionTop=minY;selectionRight=maxX+1;selectionBottom=maxY+1;
  hasSelection=!freeRegion.isEmpty();
  if(hasSelection)wandBoundary.set(freeRegion.getBoundaryPath());
  invalidate();
 }
 private void nativeBeginEditIfNeeded(){if(tool!=PICKER)nativeBeginEdit();}
 private void updateMovedSelection(float x,float y){
  float dx=x-moveStartX,dy=y-moveStartY;
  dx=Math.max(-moveOriginalLeft,Math.min(SIZE-moveOriginalRight,dx));
  dy=Math.max(-moveOriginalTop,Math.min(SIZE-moveOriginalBottom,dy));
  selectionLeft=moveOriginalLeft+dx;selectionRight=moveOriginalRight+dx;
  selectionTop=moveOriginalTop+dy;selectionBottom=moveOriginalBottom+dy;
  invalidate();
 }
 private void updateSelection(float x,float y){
  float a=Math.max(0f,Math.min(SIZE,startX)),b=Math.max(0f,Math.min(SIZE,startY));
  float c=Math.max(0f,Math.min(SIZE,x)),d=Math.max(0f,Math.min(SIZE,y));
  selectionLeft=Math.min(a,c);selectionRight=Math.max(a,c);
  selectionTop=Math.min(b,d);selectionBottom=Math.max(b,d);
  invalidate();
 }
 @Override public boolean onTouchEvent(MotionEvent event){float scale=Math.min(getWidth()/(float)SIZE,getHeight()/(float)SIZE);if(scale<=0)return false;float left=(getWidth()-SIZE*scale)/2f,top=(getHeight()-SIZE*scale)/2f;float x=(event.getX()-left)/scale,y=(event.getY()-top)/scale;
 switch(event.getActionMasked()){
 case MotionEvent.ACTION_DOWN:
  if(x<0||y<0||x>=SIZE||y>=SIZE)return false;
  drawing=true;startX=previousX=x;startY=previousY=y;
  if(tool==MAGIC_WAND){
   selectMatchingRegion((int)x,(int)y);drawing=false;return true;
  }
  if(tool==SELECT_FREE){
   selectionTool=SELECT_FREE;hasSelection=false;freeSelectionReady=false;freeRegion.setEmpty();
   freePath.reset();freePath.moveTo(x,y);
   selectionLeft=selectionRight=x;selectionTop=selectionBottom=y;
   invalidate();return true;
  }
  if(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE){
   selectionTool=tool;hasSelection=true;
   selectionLeft=selectionRight=x;selectionTop=selectionBottom=y;
   invalidate();return true;
  }
  if(tool==MOVE_SELECTION||tool==MOVE_PIXELS){
   movingPixels=tool==MOVE_PIXELS;
   if((selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND)&&tool==MOVE_SELECTION){movingSelection=false;drawing=false;return true;}
   movingSelection=hasSelection()&&selectionContains((int)x,(int)y);
   if(movingSelection){moveStartX=x;moveStartY=y;moveOriginalLeft=selectionLeft;moveOriginalTop=selectionTop;moveOriginalRight=selectionRight;moveOriginalBottom=selectionBottom;}
   else drawing=false;
   return true;
  }
  nativeBeginEditIfNeeded();
  if(tool==PICKER){
   color=nativePickColor((int)x,(int)y);
   drawing=false;
   if(pickedColorListener!=null)pickedColorListener.accept(color);
  }else if(tool==BUCKET){nativeFill((int)x,(int)y,color);drawing=false;refresh();}
  else if(tool==BRUSH||tool==ERASER){nativeStroke(x,y,x,y,brushRadius,tool==ERASER?0x00000000:color);refresh();}
  return true;
 case MotionEvent.ACTION_MOVE:
  if(!drawing)return true;
  if(tool==MOVE_SELECTION||tool==MOVE_PIXELS){if(movingSelection)updateMovedSelection(x,y);return true;}
  if(tool==SELECT_FREE){
   float px=Math.max(0f,Math.min(SIZE,x)),py=Math.max(0f,Math.min(SIZE,y));
   freePath.lineTo(px,py);
   selectionLeft=Math.min(selectionLeft,px);selectionRight=Math.max(selectionRight,px);
   selectionTop=Math.min(selectionTop,py);selectionBottom=Math.max(selectionBottom,py);
   invalidate();return true;
  }
  if(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE){
   updateSelection(x,y);return true;
  }
  if(tool==BRUSH||tool==ERASER){nativeStroke(previousX,previousY,x,y,brushRadius,tool==ERASER?0x00000000:color);refresh();}
  previousX=x;previousY=y;return true;
 case MotionEvent.ACTION_UP:
  if(drawing){
   if(tool==MOVE_SELECTION||tool==MOVE_PIXELS){
    if(movingSelection){
     if(movingPixels){
      final int dx=Math.round(x-moveStartX),dy=Math.round(y-moveStartY);
      // Restore the original bounds before moving the pixels. The native
      // operation changes both the image and selection position atomically.
      selectionLeft=moveOriginalLeft;selectionTop=moveOriginalTop;
      selectionRight=moveOriginalRight;selectionBottom=moveOriginalBottom;
      if(!moveSelectedPixels(dx,dy))invalidate();
     }else updateMovedSelection(x,y);
    }
    movingSelection=false;movingPixels=false;drawing=false;return true;
   }
   if(tool==SELECT_FREE){
    float px=Math.max(0f,Math.min(SIZE,x)),py=Math.max(0f,Math.min(SIZE,y));
    freePath.lineTo(px,py);freePath.close();
    selectionLeft=Math.min(selectionLeft,px);selectionRight=Math.max(selectionRight,px);
    selectionTop=Math.min(selectionTop,py);selectionBottom=Math.max(selectionBottom,py);
    hasSelection=(selectionRight-selectionLeft>=1f&&selectionBottom-selectionTop>=1f);
    if(hasSelection){
     freeSelectionReady=freeRegion.setPath(freePath,new android.graphics.Region(0,0,SIZE,SIZE));
     hasSelection=freeSelectionReady&&!freeRegion.isEmpty();
     if(hasSelection){android.graphics.Rect actual=freeRegion.getBounds();selectionLeft=actual.left;selectionTop=actual.top;selectionRight=actual.right;selectionBottom=actual.bottom;}
    }
    freeSelectionReady=hasSelection;
    drawing=false;invalidate();return true;
   }
   if(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE){updateSelection(x,y);hasSelection=hasSelection();drawing=false;return true;}
   if(tool==BRUSH||tool==ERASER)nativeStroke(previousX,previousY,x,y,brushRadius,tool==ERASER?0x00000000:color);
   else nativeShape(tool,(int)startX,(int)startY,(int)x,(int)y,color);
   drawing=false;refresh();
  }return true;
 case MotionEvent.ACTION_CANCEL:
  if(drawing&&tool==SELECT_FREE){hasSelection=false;freeSelectionReady=false;freePath.reset();freeRegion.setEmpty();invalidate();}
  if(drawing&&(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE)){hasSelection=false;invalidate();}
  if(movingSelection){selectionLeft=moveOriginalLeft;selectionTop=moveOriginalTop;selectionRight=moveOriginalRight;selectionBottom=moveOriginalBottom;invalidate();}
  movingSelection=false;movingPixels=false;drawing=false;return true;
 default:return true;}
 }
}
