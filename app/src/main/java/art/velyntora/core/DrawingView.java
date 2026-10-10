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
 private static native boolean nativeTransformSelection(int left,int top,int width,int height,byte[] mask,float degrees,float sx,float sy);
 private static native boolean nativeEffect(int kind,int amount);
 private static native boolean nativeUtility(int kind,int amount,int size,int parameter);
 private static native boolean nativeObject(int kind,int amount,int tolerance,int color,boolean option);
 public boolean utility(int kind,int amount,int size,int parameter){return nativeUtility(kind,amount,size,parameter);}
 public boolean objectEffect(int kind,int amount,int tolerance,boolean option){return nativeObject(kind,amount,tolerance,color,option);}
 private static native boolean nativeRender(int kind,int scale,int detail,int seed,int first,int second);
 public boolean render(int kind,int scale,int detail,int seed,int second){return nativeRender(kind,scale,detail,seed,color,second);}
 private static native boolean nativeArtistic(int kind,int strength,int radius,int threshold);
 public boolean artistic(int kind,int strength,int radius,int threshold){return nativeArtistic(kind,strength,radius,threshold);}
 private static native boolean nativeDistortion(int kind,int amount,int size,int angle,int cx,int cy);
 public boolean distortion(int kind,int amount,int size,int angle,int cx,int cy){return nativeDistortion(kind,amount,size,angle,cx,cy);}
 private static native boolean nativeBlur(int kind,int amount,int angle,int cx,int cy);
 public boolean blur(int kind,int amount,int angle,int cx,int cy){return nativeBlur(kind,amount,angle,cx,cy);}
 private static native boolean nativeColorAdjustment(int kind,int[] values);
 public boolean colorAdjustment(int kind,int[] values){return nativeColorAdjustment(kind,values);}
 public boolean applyEffect(int kind,int amount){return nativeEffect(kind,amount);}
 private static native boolean nativeSetEffectSelection(byte[] mask);
 public boolean prepareEffectSelection(){byte[] mask=null;if(hasSelection()){mask=new byte[canvasWidth*canvasHeight];int left=Math.max(0,(int)Math.floor(selectionLeft)),right=Math.min(canvasWidth,(int)Math.ceil(selectionRight)),top=Math.max(0,(int)Math.floor(selectionTop)),bottom=Math.min(canvasHeight,(int)Math.ceil(selectionBottom));for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)if(selectionContains(x,y))mask[y*canvasWidth+x]=1;}return nativeSetEffectSelection(mask);}
 public void effectApplied(){refresh();}
 private static native boolean nativeSaveProject(int fd);
 private static native boolean nativeOpenProject(int fd);
 public boolean writeProject(int fd){return nativeSaveProject(fd);}
 public boolean readProject(int fd){return nativeOpenProject(fd);}
 public void projectOpened(){cancelCurve();savedCurves.clear();deselect();refresh();fitCanvas();}
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
 private static native boolean nativePosterizeActive(int levels);
 private static native boolean nativeSolarizeActive(int threshold);
 private static native boolean nativeSaturationActive(int adjustment);
 private static native boolean nativeGammaActive(int percent);
 private static native boolean nativeTintActive(int redAdjustment,int greenAdjustment,int blueAdjustment);
 private static native boolean nativeSwapChannelsActive(int mode);
 private static native boolean nativeColorBalanceActive(int redPercent,int greenPercent,int bluePercent);
 private static native boolean nativeHueRotateActive(int degrees);
 private static native boolean nativeLevelsActive(int blackPoint,int whitePoint);
 private static native boolean nativeExposureActive(int percent);
 private static native boolean nativeDesaturateChannelActive(int channel);
 private static native boolean nativeAdjustAlphaActive(int percent);
 private static native boolean nativeRemoveChannelActive(int channel);
 private static native boolean nativeNormalizeActive();
 private static native boolean nativeQuantizeActive(int step);
 private static native boolean nativeClampHighlightsActive(int ceiling);
 private static native boolean nativeLiftShadowsActive(int floor);
 private static native boolean nativeAdjustChannelActive(int channel,int adjustment);
 private static native void nativeSetBrushSelection(byte[] mask);
 private static native void nativeStyledStroke(float x0,float y0,float x1,float y1,float radius,int color,float opacity,float hardness,boolean square,boolean eraser);
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
 private int canvasWidth=800,canvasHeight=800;
 private static native boolean nativeLoadBitmap(int w,int h,int[] pixels);
 private static native int nativeWidth();
 private static native int nativeHeight();
 private static native boolean nativeResizeDocument(int w,int h,boolean scalePixels);
 private static native boolean nativeResizeDocumentOptions(int w,int h,boolean scalePixels,boolean bilinear,int anchor);
 private static native boolean nativeCropDocument(int left,int top,int w,int h);
 public int documentWidth(){return canvasWidth;}
 public int documentHeight(){return canvasHeight;}
 public boolean resizeDocument(int w,int h,boolean scalePixels){return resizeDocument(w,h,scalePixels,false,0);}
 public boolean resizeDocument(int w,int h,boolean scalePixels,boolean bilinear,int anchor){if(!resizeDocumentPixels(w,h,scalePixels,bilinear,anchor))return false;documentResized();return true;}
 public boolean resizeDocumentPixels(int w,int h,boolean scalePixels,boolean bilinear,int anchor){return nativeResizeDocumentOptions(w,h,scalePixels,bilinear,anchor);}
 public void documentResized(){deselect();refresh();fitCanvas();}
 public boolean newDocument(int w,int h){if(w<=0||h<=0||w>8192||h>8192||(long)w*h>4000000||!nativeCreate(w,h))return false;deselect();refresh();fitCanvas();return true;}
 public boolean cropDocument(){if(!hasSelection())return false;int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);if(!nativeCropDocument(left,top,(int)Math.ceil(selectionRight)-left,(int)Math.ceil(selectionBottom)-top))return false;deselect();refresh();fitCanvas();return true;}
 private final Viewport viewport=new Viewport(canvasWidth,canvasHeight);
 private boolean navigating;
 private float gestureX,gestureY,gestureDistance,gestureAngle;
 private Runnable viewportChangedListener;
 public void setOnViewportChangedListener(Runnable listener){viewportChangedListener=listener;}
 public float zoomPercent(){return (float)(viewport.scale*100);}
 public float rotationDegrees(){return (float)viewport.angle;}
 private void viewportChanged(){invalidate();if(viewportChangedListener!=null)viewportChangedListener.run();}
 public void fitCanvas(){viewport.fit(getWidth(),getHeight());viewportChanged();}
 public void zoomBy(float factor){viewport.zoom(viewport.scale*factor,getWidth()/2.,getHeight()/2.);viewportChanged();}
 public void setZoomPercent(float percent){viewport.zoom(percent/100.,getWidth()/2.,getHeight()/2.);viewportChanged();}
 public void rotateView(float degrees){viewport.rotate(degrees,getWidth()/2.,getHeight()/2.);viewportChanged();}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
  super.onSizeChanged(w,h,oldw,oldh);
  if(oldw==0||oldh==0)viewport.fit(w,h);
  else{viewport.centerX+=(w-oldw)/2.;viewport.centerY+=(h-oldh)/2.;}
  viewportChanged();
 }
 private void recordGesture(MotionEvent event){
  gestureX=(event.getX(0)+event.getX(1))/2f;gestureY=(event.getY(0)+event.getY(1))/2f;
  float dx=event.getX(1)-event.getX(0),dy=event.getY(1)-event.getY(0);
  gestureDistance=(float)Math.hypot(dx,dy);gestureAngle=(float)Math.toDegrees(Math.atan2(dy,dx));
 }
 private void cancelToolGesture(){
  if(tool==LINE&&drawing){curve=dragBackup;dragBackup=null;curveHandle=-1;}
  if(drawing&&(tool==SELECT_FREE||tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE))deselect();
  if(movingSelection)restoreMovedSelection();
  drawing=false;movingSelection=false;movingPixels=false;
 }

 public static final int BRUSH=0,RECTANGLE=1,ELLIPSE=2,LINE=3,BUCKET=4,ERASER=5,PICKER=6,FILLED_RECTANGLE=7,FILLED_ELLIPSE=8,SELECT_RECTANGLE=9,SELECT_ELLIPSE=10,MOVE_SELECTION=11,MOVE_PIXELS=12,SELECT_FREE=13,MAGIC_WAND=14,TEXT=15,ROUNDED_RECTANGLE=16,FILLED_ROUNDED_RECTANGLE=17,TRIANGLE=18,FILLED_TRIANGLE=19,PENCIL=20,PAN=21,ZOOM=22,GRADIENT=23,FREEFORM=24,CIRCLE=25,CLONE=26,RECOLOR=27;
 private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
 private Bitmap bitmap=Bitmap.createBitmap(canvasWidth,canvasHeight,Bitmap.Config.ARGB_8888);
 private java.util.function.BiConsumer<Integer,Integer> textPositionListener;
 public void setOnTextPositionListener(java.util.function.BiConsumer<Integer,Integer> listener){textPositionListener=listener;}
 public boolean insertText(String text,int x,int y,int size,boolean bold,boolean italic,String family){
  if(text==null||text.trim().isEmpty()||text.length()>4096||x<0||y<0||x>=canvasWidth||y>=canvasHeight)return false;
  android.text.TextPaint textPaint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
  textPaint.setColor(color);textPaint.setTextSize(Math.max(4,Math.min(256,size)));
  textPaint.setTypeface(android.graphics.Typeface.create(family,(bold?android.graphics.Typeface.BOLD:0)|(italic?android.graphics.Typeface.ITALIC:0)));
  int width=canvasWidth-x;
  android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain(text,0,text.length(),textPaint,width).setIncludePad(true).build();
  int height=Math.min(canvasHeight-y,layout.getHeight());if(height<=0)return false;
  Bitmap glyphs=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
  try{layout.draw(new Canvas(glyphs));return pasteAt(glyphs,x,y);}finally{glyphs.recycle();}
 }
 private boolean pasteAt(Bitmap source,int x,int y){
  int w=source.getWidth(),h=source.getHeight();int[] data=new int[2+w*h];data[0]=w;data[1]=h;
  source.getPixels(data,2,w,0,0,w,h);if(!nativePasteSelection(data,x,y))return false;
  deselect();refresh();return true;
 }
 private void additionalShape(int kind,float x0,float y0,float x1,float y1){
  float left=Math.min(x0,x1),top=Math.min(y0,y1),right=Math.max(x0,x1),bottom=Math.max(y0,y1);
  int x=Math.max(0,(int)Math.floor(left-brushRadius)),y=Math.max(0,(int)Math.floor(top-brushRadius));
  int w=Math.min(canvasWidth-x,(int)Math.ceil(right+brushRadius)-x+1),h=Math.min(canvasHeight-y,(int)Math.ceil(bottom+brushRadius)-y+1);
  if(w<=0||h<=0)return;
  Bitmap shape=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
  try{
   Canvas target=new Canvas(shape);target.translate(-x,-y);
   Paint style=new Paint(Paint.ANTI_ALIAS_FLAG);style.setColor(color);style.setAlpha(Math.round((color>>>24)*brushOpacity));style.setStrokeWidth(brushRadius*2);
   style.setStrokeJoin(Paint.Join.ROUND);style.setStyle(kind==FILLED_ROUNDED_RECTANGLE||kind==FILLED_TRIANGLE?Paint.Style.FILL:Paint.Style.STROKE);
   if(kind==ROUNDED_RECTANGLE||kind==FILLED_ROUNDED_RECTANGLE){float corner=Math.min(right-left,bottom-top)/5f;target.drawRoundRect(left,top,right,bottom,corner,corner,style);}
   else{android.graphics.Path path=new android.graphics.Path();path.moveTo((left+right)/2,top);path.lineTo(right,bottom);path.lineTo(left,bottom);path.close();target.drawPath(path,style);}
   pasteAt(shape,x,y);
  }finally{shape.recycle();}
 }
 private CurveDraft curve;
 private CurveDraft dragBackup,canceledCurve;
 private final CurveHistory curveHistory=new CurveHistory();
 private int curveHandle=-1,nextCurveTag=1;
 private static native int nativeCurveTag();
 private static native void nativeMarkCurve(int tag);
 private final class SavedSelection {
  final boolean selected=hasSelection();final int kind=selectionTool;
  final float left=selectionLeft,top=selectionTop,right=selectionRight,bottom=selectionBottom;
  final android.graphics.Region region=new android.graphics.Region(freeRegion);
  final android.graphics.Path path=new android.graphics.Path(freePath);
  void restore(){if(!selected)return;selectionTool=kind;hasSelection=true;selectionLeft=left;selectionTop=top;selectionRight=right;selectionBottom=bottom;freeRegion.set(region);freePath.set(path);freeSelectionReady=kind==SELECT_FREE;wandBoundary.set(region.getBoundaryPath());}
 }
 private static final class SavedCurve {final CurveDraft geometry;final int color;final float radius,opacity;final SavedSelection selection;SavedCurve(CurveDraft g,int c,float r,float o,SavedSelection sel){geometry=g.copy();color=c;radius=r;opacity=o;selection=sel;}}
 private final android.util.SparseArray<SavedCurve> savedCurves=new android.util.SparseArray<>();
 private android.graphics.Path curvePath(){android.graphics.Path p=new android.graphics.Path();p.moveTo(curve.x[0],curve.y[0]);p.cubicTo(curve.x[1],curve.y[1],curve.x[2],curve.y[2],curve.x[3],curve.y[3]);return p;}
 public boolean confirmCurve(){
  if(curve==null)return false;
  Bitmap image=Bitmap.createBitmap(canvasWidth,canvasHeight,Bitmap.Config.ARGB_8888);
  try{
   Paint style=new Paint(Paint.ANTI_ALIAS_FLAG);style.setColor(color);style.setAlpha(Math.round((color>>>24)*brushOpacity));style.setStyle(Paint.Style.STROKE);style.setStrokeWidth(brushRadius*2);style.setStrokeCap(Paint.Cap.ROUND);
   new Canvas(image).drawPath(curvePath(),style);int[] data=new int[2+canvasWidth*canvasHeight];data[0]=canvasWidth;data[1]=canvasHeight;image.getPixels(data,2,canvasWidth,0,0,canvasWidth,canvasHeight);
   if(hasSelection())for(int row=0;row<canvasHeight;row++)for(int col=0;col<canvasWidth;col++)if(!selectionContains(col,row))data[2+row*canvasWidth+col]=0;
   if(nativePasteSelection(data,0,0)){int tag=nextCurveTag++;savedCurves.put(tag,new SavedCurve(curve,color,brushRadius,brushOpacity,new SavedSelection()));while(savedCurves.size()>15)savedCurves.removeAt(0);nativeMarkCurve(tag);}
   curve=null;canceledCurve=null;curveHistory.clear();curveHandle=-1;refresh();return true;
  }finally{image.recycle();}
 }
 public void cancelCurve(){curve=null;canceledCurve=null;curveHistory.clear();curveHandle=-1;invalidate();}
 @Override public boolean onKeyDown(int code,android.view.KeyEvent event){
  if(code==android.view.KeyEvent.KEYCODE_ENTER&&confirmCurve())return true;
  if(code==android.view.KeyEvent.KEYCODE_ESCAPE&&curve!=null){cancelCurve();return true;}
  if(event.isCtrlPressed()&&code==android.view.KeyEvent.KEYCODE_Z){if(event.isShiftPressed())redo();else undo();return true;}
  return super.onKeyDown(code,event);
 }
 private final android.graphics.Path shapePath=new android.graphics.Path();
 private void commitFreeform(){
  Bitmap shape=Bitmap.createBitmap(canvasWidth,canvasHeight,Bitmap.Config.ARGB_8888);
  try{
   Paint style=new Paint(Paint.ANTI_ALIAS_FLAG);style.setColor(color);style.setAlpha(Math.round((color>>>24)*brushOpacity));style.setStrokeWidth(brushRadius*2);style.setStrokeJoin(Paint.Join.ROUND);style.setStyle(Paint.Style.STROKE);
   shapePath.close();new Canvas(shape).drawPath(shapePath,style);
   int[] data=new int[2+canvasWidth*canvasHeight];data[0]=canvasWidth;data[1]=canvasHeight;shape.getPixels(data,2,canvasWidth,0,0,canvasWidth,canvasHeight);
   if(hasSelection())for(int row=0;row<canvasHeight;row++)for(int col=0;col<canvasWidth;col++)if(!selectionContains(col,row))data[2+row*canvasWidth+col]=0;
   nativePasteSelection(data,0,0);
  }finally{shape.recycle();shapePath.reset();}
 }
 private final NavigationTool navigationTool=new NavigationTool(4);
 private int color=0xFF202020,tool=BRUSH;
 private boolean radialGradient;
 public void setRadialGradient(boolean radial){radialGradient=radial;}
 private boolean cloneOriginReady;
 private int cloneX,cloneY;
 private static native void nativeBeginSampled(int x,int y);
 private static native void nativeSampledStroke(boolean clone,int ox,int oy,int replacement,int tolerance,float x0,float y0,float x1,float y1,float radius,float opacity,float hardness);
 private float brushRadius=4f,brushOpacity=1f,brushHardness=1f;
 private boolean squareBrush,pressureBrush=true,strokeEditing;
 public float brushOpacity(){return brushOpacity;}
 public float brushHardness(){return brushHardness;}
 public boolean squareBrush(){return squareBrush;}
 public boolean pressureBrush(){return pressureBrush;}
 public void configureBrush(float opacity,float hardness,boolean square,boolean pressure){brushOpacity=opacity;brushHardness=hardness;squareBrush=square;pressureBrush=pressure;}
 private void paintStroke(float x0,float y0,float x1,float y1,MotionEvent event){
  if(!strokeEditing){
   byte[] mask=null;
   if(hasSelection()){
    mask=new byte[canvasWidth*canvasHeight];
    int left=Math.max(0,(int)Math.floor(selectionLeft)),right=Math.min(canvasWidth,(int)Math.ceil(selectionRight));
    int top=Math.max(0,(int)Math.floor(selectionTop)),bottom=Math.min(canvasHeight,(int)Math.ceil(selectionBottom));
    for(int y=top;y<bottom;++y)for(int x=left;x<right;++x)if(selectionContains(x,y))mask[y*canvasWidth+x]=1;
   }
   nativeSetBrushSelection(mask);nativeBeginEdit();if(tool==CLONE||tool==RECOLOR)nativeBeginSampled((int)startX,(int)startY);strokeEditing=true;
  }
  float pressure=pressureBrush&&event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS?Math.max(.1f,Math.min(1f,event.getPressure())):1f;
  if(tool==CLONE||tool==RECOLOR)nativeSampledStroke(tool==CLONE,cloneX-(int)startX,cloneY-(int)startY,color,wandTolerance,x0,y0,x1,y1,brushRadius*pressure,brushOpacity,brushHardness);
  else if(tool==PENCIL)nativeStyledStroke((float)Math.floor(x0)+.5f,(float)Math.floor(y0)+.5f,(float)Math.floor(x1)+.5f,(float)Math.floor(y1)+.5f,.5f,color,1,1,true,false);
  else nativeStyledStroke(x0,y0,x1,y1,brushRadius*pressure,color,brushOpacity,brushHardness,squareBrush,tool==ERASER);
 }

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
 public DrawingView(Context context){super(context);if(nativeWidth()==0&&!nativeCreate(canvasWidth,canvasHeight))throw new IllegalStateException("Canvas error");refresh();}
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
 public boolean selectLayer(int index){confirmCurve();boolean ok=nativeSelectLayer(index);if(ok)refresh();return ok;}
 public boolean deleteLayer(){boolean ok=nativeDeleteLayer();if(ok)refresh();return ok;}
 public boolean toggleLayer(){boolean ok=nativeToggleLayer();if(ok)refresh();return ok;}
 public boolean moveLayer(int direction){boolean ok=nativeMoveLayer(direction);if(ok)refresh();return ok;}
 public void setColor(int value){color=value;}
 public void setBrushRadius(float radius){if(Float.isFinite(radius)&&radius>=1f&&radius<=128f)brushRadius=radius;}
 public float brushRadius(){return brushRadius;}
 public int currentTool(){return tool;}
 @Override public void setEnabled(boolean enabled){if(!enabled)confirmCurve();super.setEnabled(enabled);}
 public void setTool(int value){
  if(value<BRUSH||value>RECOLOR)return;
  if(value!=LINE){if(curve!=null)confirmCurve();else if(canceledCurve!=null)cancelCurve();}
  if(value==CLONE)cloneOriginReady=false;
  tool=value;
  // Switching away from a selection tool must not leave a selection
  // permanently active as an accidental overlay on subsequent drawings.
  invalidate();
 }
 public boolean transformSelection(float degrees,float sx,float sy){
  if(!hasSelection())return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop),right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  int w=right-left,h=bottom-top;if(w<=0||h<=0||left<0||top<0||right>canvasWidth||bottom>canvasHeight)return false;
  byte[] mask=new byte[w*h];android.graphics.Region selected=new android.graphics.Region();
  for(int y=0;y<h;++y){int run=-1;
   for(int x=0;x<=w;++x){boolean inside=x<w&&selectionContains(left+x,top+y);if(inside){mask[y*w+x]=1;if(run<0)run=x;}
    else if(run>=0){selected.op(left+run,top+y,left+x,top+y+1,android.graphics.Region.Op.UNION);run=-1;}
   }
  }
  android.graphics.Path transformed=selected.getBoundaryPath();float cx=left+w/2f,cy=top+h/2f;
  android.graphics.Matrix matrix=new android.graphics.Matrix();matrix.setTranslate(-cx,-cy);matrix.postScale(sx,sy);matrix.postRotate(degrees);matrix.postTranslate(cx,cy);transformed.transform(matrix);
  android.graphics.Region region=new android.graphics.Region();region.setPath(transformed,new android.graphics.Region(0,0,canvasWidth,canvasHeight));
  if(!nativeTransformSelection(left,top,w,h,mask,degrees,sx,sy))return false;
  deselect();if(!region.isEmpty()){freeRegion.set(region);wandBoundary.set(region.getBoundaryPath());android.graphics.Rect bounds=region.getBounds();selectionLeft=bounds.left;selectionTop=bounds.top;selectionRight=bounds.right;selectionBottom=bounds.bottom;selectionTool=MAGIC_WAND;hasSelection=true;}
  refresh();return true;
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
   original.setPath(ellipse,new android.graphics.Region(0,0,canvasWidth,canvasHeight));
  }else return false;
  original.op(0,0,canvasWidth,canvasHeight,android.graphics.Region.Op.INTERSECT);
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
   region.setPath(ellipse,new android.graphics.Region(0,0,canvasWidth,canvasHeight));
  }else return false;
  region.op(0,0,canvasWidth,canvasHeight,android.graphics.Region.Op.INTERSECT);
  android.graphics.Region grown=new android.graphics.Region(region);
  android.graphics.Region offset=new android.graphics.Region();
  for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){
   if(dx==0&&dy==0)continue;
   region.translate(dx,dy,offset);
   grown.op(offset,android.graphics.Region.Op.UNION);
  }
  grown.op(0,0,canvasWidth,canvasHeight,android.graphics.Region.Op.INTERSECT);
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
   selected.setPath(ellipse,new android.graphics.Region(0,0,canvasWidth,canvasHeight));
  }else return false;
  selected.op(0,0,canvasWidth,canvasHeight,android.graphics.Region.Op.INTERSECT);
  android.graphics.Region inverted=new android.graphics.Region(0,0,canvasWidth,canvasHeight);
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
  if(!hasSelection()||px<0||py<0||px>=canvasWidth||py>=canvasHeight||px<selectionLeft||py<selectionTop||px>=selectionRight||py>=selectionBottom)return false;
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
 public boolean adjustChannelActive(int channel,int adjustment){if(!nativeAdjustChannelActive(channel,adjustment))return false;deselect();refresh();return true;}
 public boolean liftShadowsActive(int floor){if(!nativeLiftShadowsActive(floor))return false;deselect();refresh();return true;}
 public boolean clampHighlightsActive(int ceiling){if(!nativeClampHighlightsActive(ceiling))return false;deselect();refresh();return true;}
 public boolean quantizeActive(int step){if(!nativeQuantizeActive(step))return false;deselect();refresh();return true;}
 public boolean normalizeActive(){if(!nativeNormalizeActive())return false;deselect();refresh();return true;}
 public boolean removeChannelActive(int channel){if(!nativeRemoveChannelActive(channel))return false;deselect();refresh();return true;}
 public boolean adjustAlphaActive(int percent){if(!nativeAdjustAlphaActive(percent))return false;deselect();refresh();return true;}
 public boolean grayscaleFromChannelActive(int channel){if(!nativeDesaturateChannelActive(channel))return false;deselect();refresh();return true;}
 public boolean exposureActive(int percent){if(!nativeExposureActive(percent))return false;deselect();refresh();return true;}
 public boolean levelsActive(int blackPoint,int whitePoint){if(!nativeLevelsActive(blackPoint,whitePoint))return false;deselect();refresh();return true;}
 public boolean hueRotateActive(int degrees){if(!nativeHueRotateActive(degrees))return false;deselect();refresh();return true;}
 public boolean colorBalanceActive(int redPercent,int greenPercent,int bluePercent){if(!nativeColorBalanceActive(redPercent,greenPercent,bluePercent))return false;deselect();refresh();return true;}
 public boolean swapChannelsActive(int mode){if(!nativeSwapChannelsActive(mode))return false;deselect();refresh();return true;}
 public boolean tintActive(int red,int green,int blue){if(!nativeTintActive(red,green,blue))return false;deselect();refresh();return true;}
 public boolean gammaActive(int percent){if(!nativeGammaActive(percent))return false;deselect();refresh();return true;}
 public boolean saturationActive(int adjustment){if(!nativeSaturationActive(adjustment))return false;deselect();refresh();return true;}
 public boolean solarizeActive(int threshold){if(!nativeSolarizeActive(threshold))return false;deselect();refresh();return true;}
 public boolean posterizeActive(int levels){if(!nativePosterizeActive(levels))return false;deselect();refresh();return true;}
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
  if(left<0||top<0||right>canvasWidth||bottom>canvasHeight||left>=right||top>=bottom)return false;
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
  if(left<0||top<0||right>canvasWidth||bottom>canvasHeight||left>=right||top>=bottom)return false;
  if(!nativeCropActiveEllipse(left,top,right,bottom))return false;
  deselect();refresh();return true;
 }
 public boolean trimActiveToRectSelection(){
  if(!hasSelection()||selectionTool!=SELECT_RECTANGLE)return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>canvasWidth||bottom>canvasHeight||left>=right||top>=bottom)return false;
  if(!nativeCropActiveSelection(left,top,right,bottom))return false;
  deselect();refresh();return true;
 }
 public void clear(){if(nativeClear()){deselect();refresh();}}
 public boolean pasteBitmap(Bitmap source){
  if(source==null||source.isRecycled())return false;
  int w=source.getWidth(),h=source.getHeight();
  if(w<=0||h<=0||w>canvasWidth||h>canvasHeight||((long)w*h)>canvasWidth*canvasHeight)return false;
  int[] data=new int[2+w*h];
  data[0]=w;data[1]=h;
  source.getPixels(data,2,w,0,0,w,h);
  int x=hasSelection()?(int)selectionLeft:(canvasWidth-w)/2;
  int y=hasSelection()?(int)selectionTop:(canvasHeight-h)/2;
  x=Math.max(0,Math.min(canvasWidth-w,x));
  y=Math.max(0,Math.min(canvasHeight-h,y));
  boolean ok=nativePasteSelection(data,x,y);
  if(ok){deselect();refresh();}
  return ok;
 }
 public Bitmap copySelection(){
  if(!hasSelection())return null;
  int leftBound=(int)Math.floor(selectionLeft),topBound=(int)Math.floor(selectionTop);
  int rightBound=(int)Math.ceil(selectionRight),bottomBound=(int)Math.ceil(selectionBottom);
  if(leftBound<0||topBound<0||rightBound>canvasWidth||bottomBound>canvasHeight||rightBound<=leftBound||bottomBound<=topBound)return null;
  int[] data=nativeCopySelection((selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND)?SELECT_RECTANGLE:selectionTool,(int)Math.floor(selectionLeft),(int)Math.floor(selectionTop),(int)Math.ceil(selectionRight),(int)Math.ceil(selectionBottom));
  if(data==null||data.length<3)return null;
  int w=data[0],h=data[1];
  if(w<=0||h<=0||w>canvasWidth||h>canvasHeight||w!=rightBound-leftBound||h!=bottomBound-topBound||((long)w*h)!=data.length-2)return null;
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
   if(w<=0||h<=0||left<0||top<0||left+w>canvasWidth||top+h>canvasHeight||((long)w*h)>canvasWidth*canvasHeight)return false;
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
  selectionLeft=0f;selectionTop=0f;selectionRight=canvasWidth;selectionBottom=canvasHeight;
  hasSelection=true;invalidate();
 }
 public void deselect(){hasSelection=false;movingSelection=false;movingPixels=false;freeSelectionReady=false;freeRegion.setEmpty();freePath.reset();wandBoundary.reset();invalidate();}
 public void enableSelectionMove(){tool=MOVE_SELECTION;invalidate();}
 public boolean moveSelectedPixels(int dx,int dy){
  if(!hasSelection()||(dx==0&&dy==0))return false;
  int left=(int)Math.floor(selectionLeft),top=(int)Math.floor(selectionTop);
  int right=(int)Math.ceil(selectionRight),bottom=(int)Math.ceil(selectionBottom);
  if(left<0||top<0||right>canvasWidth||bottom>canvasHeight||right<=left||bottom<=top)return false;
  dx=Math.max(-left,Math.min(canvasWidth-right,dx));
  dy=Math.max(-top,Math.min(canvasHeight-bottom,dy));
  if(dx==0&&dy==0)return false;
  boolean ok;
  if(selectionTool==SELECT_FREE||selectionTool==MAGIC_WAND){
   int w=right-left,h=bottom-top;
   if(w<=0||h<=0||((long)w*h)>canvasWidth*canvasHeight)return false;
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
 public void undo(){if(curve!=null){CurveDraft previous=curveHistory.undo(curve);if(previous!=null)curve=previous;else{canceledCurve=curve.copy();curve=null;}invalidate();return;}canceledCurve=null;curveHistory.clear();int tag=nativeCurveTag();if(nativeUndo()){SavedCurve saved=savedCurves.get(tag);deselect();if(saved!=null){curve=saved.geometry.copy();color=saved.color;brushRadius=saved.radius;brushOpacity=saved.opacity;tool=LINE;curveHistory.clear();canceledCurve=null;saved.selection.restore();if(pickedColorListener!=null)pickedColorListener.accept(color);}refresh();}}
 public void redo(){if(canceledCurve!=null){curve=canceledCurve;canceledCurve=null;invalidate();return;}if(curve!=null){CurveDraft next=curveHistory.redo(curve);if(next!=null){curve=next;invalidate();return;}}cancelCurve();if(nativeRedo()){deselect();refresh();}}
 public boolean loadBitmap(Bitmap source){
  if(source==null||source.isRecycled())return false;
  int w=source.getWidth(),h=source.getHeight();
  if(w>8192||h>8192||(long)w*h>4000000)return false;
  int[] pixels=new int[w*h];source.getPixels(pixels,0,w,0,0,w,h);
  if(!nativeLoadBitmap(w,h,pixels))return false;
  deselect();refresh();fitCanvas();return true;
 }
 public Bitmap snapshot(){confirmCurve();return bitmap.copy(Bitmap.Config.ARGB_8888,false);}
 private void refresh(){
  int width=nativeWidth(),height=nativeHeight();
  int[] pixels=nativePixels();if(width<=0||height<=0||pixels==null||pixels.length!=(long)width*height)return;
  if(width!=canvasWidth||height!=canvasHeight){
   Bitmap replacement=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
   bitmap.recycle();bitmap=replacement;canvasWidth=width;canvasHeight=height;
   viewport.documentSize(width,height);
  }
  bitmap.setPixels(pixels,0,width,0,0,width,height);invalidate();
  if(canvasChangedListener!=null)canvasChangedListener.run();viewportChanged();
 }
 private final Paint checkerPaint=new Paint();
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  if(getWidth()<=0||getHeight()<=0)return;
  float scale=(float)viewport.scale;
  canvas.drawColor(0xFFE3E3E3);
  canvas.save();
  canvas.translate((float)viewport.centerX,(float)viewport.centerY);
  canvas.rotate((float)viewport.angle);
  canvas.scale(scale,scale);
  canvas.translate(-canvasWidth/2f,-canvasHeight/2f);
  canvas.clipRect(0,0,canvasWidth,canvasHeight);
  checkerPaint.setColor(0xFFFFFFFF);
  canvas.drawRect(0,0,canvasWidth,canvasHeight,checkerPaint);
  checkerPaint.setColor(0xFFD1D1D1);
  for(int row=0;row<(canvasHeight+15)/16;++row)for(int col=(row&1);col<(canvasWidth+15)/16;col+=2)
   canvas.drawRect(col*16,row*16,(col+1)*16,(row+1)*16,checkerPaint);
  canvas.drawBitmap(bitmap,0,0,paint);
  if(curve!=null){Paint preview=new Paint(Paint.ANTI_ALIAS_FLAG);preview.setColor(color);preview.setAlpha(Math.round((color>>>24)*brushOpacity));preview.setStyle(Paint.Style.STROKE);preview.setStrokeWidth(brushRadius*2);preview.setStrokeCap(Paint.Cap.ROUND);canvas.drawPath(curvePath(),preview);preview.setStyle(Paint.Style.FILL);preview.setColor(0xff7040b0);preview.setAlpha(255);for(int i=0;i<4;i++)canvas.drawCircle(curve.x[i],curve.y[i],6/scale,preview);}

  if(drawing&&tool==FREEFORM){Paint preview=new Paint(Paint.ANTI_ALIAS_FLAG);preview.setColor(color);preview.setAlpha(Math.round((color>>>24)*brushOpacity));preview.setStyle(Paint.Style.STROKE);preview.setStrokeWidth(brushRadius*2);preview.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(shapePath,preview);}
  if(hasSelection()){
   selectionPaint.setColor(0xFF202020);
   selectionPaint.setStyle(Paint.Style.STROKE);
   selectionPaint.setStrokeWidth(Math.max(1f,1f/scale));
   selectionPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{6f/scale,4f/scale},0));
   RectF bounds=new RectF(selectionLeft,selectionTop,selectionRight,selectionBottom);
   if(selectionTool==SELECT_ELLIPSE)canvas.drawOval(bounds,selectionPaint);
   else if(selectionTool==SELECT_FREE){canvas.save();canvas.drawPath(freePath,selectionPaint);canvas.restore();}
   else if(selectionTool==MAGIC_WAND){canvas.save();canvas.drawPath(wandBoundary,selectionPaint);canvas.restore();}
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
  if(sx<0||sy<0||sx>=canvasWidth||sy>=canvasHeight)return;
  int[] pixels=new int[canvasWidth*canvasHeight];
  bitmap.getPixels(pixels,0,canvasWidth,0,0,canvasWidth,canvasHeight);
  int target=pixels[sy*canvasWidth+sx];
  byte[] visited=new byte[pixels.length];
  int[] queue=new int[pixels.length];
  int head=0,tail=0,start=sy*canvasWidth+sx;
  visited[start]=1;queue[tail++]=start;
  int minX=sx,maxX=sx,minY=sy,maxY=sy;
  freeRegion.setEmpty();freePath.reset();wandBoundary.reset();
  while(head<tail){
   int pos=queue[head++],px=pos%canvasWidth,py=pos/canvasWidth;
   visited[pos]=2;
   minX=Math.min(minX,px);maxX=Math.max(maxX,px);
   minY=Math.min(minY,py);maxY=Math.max(maxY,py);
   if(px>0){int q=pos-1;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(px<canvasWidth-1){int q=pos+1;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(py>0){int q=pos-canvasWidth;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
   if(py<canvasHeight-1){int q=pos+canvasWidth;if(visited[q]==0){visited[q]=1;if(withinTolerance(pixels[q],target,wandTolerance))queue[tail++]=q;}}
  }
  // Union horizontal runs, not individual pixels, to reduce Region operations.
  for(int row=minY;row<=maxY;++row){
   int col=minX;
   while(col<=maxX){
    while(col<=maxX&&visited[row*canvasWidth+col]!=2)++col;
    int left=col;
    while(col<=maxX&&visited[row*canvasWidth+col]==2)++col;
    if(left<col)freeRegion.op(left,row,col,row+1,android.graphics.Region.Op.UNION);
   }
  }
  selectionTool=MAGIC_WAND;
  selectionLeft=minX;selectionTop=minY;selectionRight=maxX+1;selectionBottom=maxY+1;
  hasSelection=!freeRegion.isEmpty();
  if(hasSelection)wandBoundary.set(freeRegion.getBoundaryPath());
  invalidate();
 }
 private void nativeBeginEditIfNeeded(){if(tool!=BRUSH&&tool!=ERASER&&tool!=PICKER&&tool<=FILLED_ELLIPSE)nativeBeginEdit();}
 private void translateSelectionMask(int dx,int dy){
  if(selectionTool!=SELECT_FREE&&selectionTool!=MAGIC_WAND)return;
  android.graphics.Matrix translation=new android.graphics.Matrix();translation.setTranslate(dx,dy);
  freePath.transform(translation);freeRegion.translate(dx,dy);wandBoundary.transform(translation);
 }
 private void restoreMovedSelection(){
  if(!movingPixels)translateSelectionMask(Math.round(moveOriginalLeft-selectionLeft),Math.round(moveOriginalTop-selectionTop));
  selectionLeft=moveOriginalLeft;selectionTop=moveOriginalTop;selectionRight=moveOriginalRight;selectionBottom=moveOriginalBottom;
 }
 private void updateMovedSelection(float x,float y){
  float dx=Math.round(x-moveStartX),dy=Math.round(y-moveStartY);
  dx=Math.max(-moveOriginalLeft,Math.min(canvasWidth-moveOriginalRight,dx));
  dy=Math.max(-moveOriginalTop,Math.min(canvasHeight-moveOriginalBottom,dy));
  if(!movingPixels)translateSelectionMask(Math.round(moveOriginalLeft+dx-selectionLeft),Math.round(moveOriginalTop+dy-selectionTop));
  selectionLeft=moveOriginalLeft+dx;selectionRight=moveOriginalRight+dx;
  selectionTop=moveOriginalTop+dy;selectionBottom=moveOriginalBottom+dy;
  invalidate();
 }
 private void updateSelection(float x,float y){
  float a=Math.max(0f,Math.min(canvasWidth,startX)),b=Math.max(0f,Math.min(canvasHeight,startY));
  float c=Math.max(0f,Math.min(canvasWidth,x)),d=Math.max(0f,Math.min(canvasHeight,y));
  selectionLeft=Math.min(a,c);selectionRight=Math.max(a,c);
  selectionTop=Math.min(b,d);selectionBottom=Math.max(b,d);
  invalidate();
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!isEnabled()||getWidth()<=0||getHeight()<=0)return false;
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_POINTER_DOWN&&event.getPointerCount()>=2){
   cancelToolGesture();navigating=true;recordGesture(event);
   getParent().requestDisallowInterceptTouchEvent(true);return true;
  }
  if(navigating){
   if(action==MotionEvent.ACTION_MOVE&&event.getPointerCount()==2){
    float oldX=gestureX,oldY=gestureY,oldDistance=gestureDistance,oldAngle=gestureAngle;
    recordGesture(event);
    float delta=gestureAngle-oldAngle;while(delta>180)delta-=360;while(delta< -180)delta+=360;
    if(oldDistance>1)viewport.gesture(oldX,oldY,gestureX,gestureY,gestureDistance/oldDistance,delta);
    viewportChanged();
   }else if(action==MotionEvent.ACTION_POINTER_UP&&event.getPointerCount()>2){gestureDistance=0;}
   else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
    navigating=false;getParent().requestDisallowInterceptTouchEvent(false);
   }
   return true;
  }
  if(tool==PAN||tool==ZOOM){
   if(action==MotionEvent.ACTION_DOWN){getParent().requestDisallowInterceptTouchEvent(true);navigationTool.begin(event.getX(),event.getY());}
   else if(action==MotionEvent.ACTION_MOVE){navigationTool.move(viewport,event.getX(),event.getY(),tool==ZOOM);viewportChanged();}
   else if(action==MotionEvent.ACTION_UP){navigationTool.finish(viewport,tool==ZOOM,event.getEventTime()-event.getDownTime()>=500);viewportChanged();getParent().requestDisallowInterceptTouchEvent(false);}
   else if(action==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);
   return true;
  }
  float x=(float)viewport.documentX(event.getX(),event.getY());
  float y=(float)viewport.documentY(event.getX(),event.getY());
 switch(event.getActionMasked()){
 case MotionEvent.ACTION_DOWN:
  if(x<0||y<0||x>=canvasWidth||y>=canvasHeight)return true;
  getParent().requestDisallowInterceptTouchEvent(true);
  drawing=true;strokeEditing=false;startX=previousX=x;startY=previousY=y;
  if(tool==LINE){setFocusableInTouchMode(true);requestFocus();curveHandle=curve==null?-1:curve.hit(x,y,24*getResources().getDisplayMetrics().density/(float)viewport.scale);dragBackup=curve==null?null:curve.copy();if(curveHandle<0){confirmCurve();curveHistory.clear();canceledCurve=null;curve=new CurveDraft(x,y,x,y);curveHandle=3;dragBackup=null;}invalidate();return true;}
  if(tool==CLONE&&!cloneOriginReady){cloneX=(int)x;cloneY=(int)y;cloneOriginReady=true;drawing=false;announceForAccessibility("Origen de clonación fijado");return true;}
  if(tool==FREEFORM){shapePath.reset();shapePath.moveTo(x,y);invalidate();return true;}
  if(tool==TEXT){drawing=false;if(textPositionListener!=null)textPositionListener.accept((int)x,(int)y);return true;}
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
  else if(tool==BRUSH||tool==ERASER||tool==PENCIL||tool==CLONE||tool==RECOLOR){invalidate();}
  return true;
 case MotionEvent.ACTION_MOVE:
  if(!drawing)return true;
  if(tool==LINE&&curve!=null){if(dragBackup==null)curve=new CurveDraft(startX,startY,x,y);else curve.move(curveHandle,x,y);invalidate();return true;}
  if(tool==FREEFORM){shapePath.lineTo(Math.max(0,Math.min(canvasWidth,x)),Math.max(0,Math.min(canvasHeight,y)));invalidate();return true;}
  if(tool==MOVE_SELECTION||tool==MOVE_PIXELS){if(movingSelection)updateMovedSelection(x,y);return true;}
  if(tool==SELECT_FREE){
   float px=Math.max(0f,Math.min(canvasWidth,x)),py=Math.max(0f,Math.min(canvasHeight,y));
   freePath.lineTo(px,py);
   selectionLeft=Math.min(selectionLeft,px);selectionRight=Math.max(selectionRight,px);
   selectionTop=Math.min(selectionTop,py);selectionBottom=Math.max(selectionBottom,py);
   invalidate();return true;
  }
  if(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE){
   updateSelection(x,y);return true;
  }
  if((tool==BRUSH||tool==ERASER||tool==PENCIL||tool==CLONE||tool==RECOLOR)&&(x!=previousX||y!=previousY)){paintStroke(previousX,previousY,x,y,event);refresh();}
  previousX=x;previousY=y;return true;
 case MotionEvent.ACTION_UP:
  if(drawing){
   if(tool==LINE&&curve!=null){if(dragBackup==null)curve=new CurveDraft(startX,startY,x,y);else curve.move(curveHandle,x,y);curveHistory.record(dragBackup,curve);drawing=false;dragBackup=null;curveHandle=-1;invalidate();return true;}
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
    float px=Math.max(0f,Math.min(canvasWidth,x)),py=Math.max(0f,Math.min(canvasHeight,y));
    freePath.lineTo(px,py);freePath.close();
    selectionLeft=Math.min(selectionLeft,px);selectionRight=Math.max(selectionRight,px);
    selectionTop=Math.min(selectionTop,py);selectionBottom=Math.max(selectionBottom,py);
    hasSelection=(selectionRight-selectionLeft>=1f&&selectionBottom-selectionTop>=1f);
    if(hasSelection){
     freeSelectionReady=freeRegion.setPath(freePath,new android.graphics.Region(0,0,canvasWidth,canvasHeight));
     hasSelection=freeSelectionReady&&!freeRegion.isEmpty();
     if(hasSelection){android.graphics.Rect actual=freeRegion.getBounds();selectionLeft=actual.left;selectionTop=actual.top;selectionRight=actual.right;selectionBottom=actual.bottom;}
    }
    freeSelectionReady=hasSelection;
    drawing=false;invalidate();return true;
   }
   if(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE){updateSelection(x,y);hasSelection=hasSelection();drawing=false;return true;}
   if(tool==FREEFORM){shapePath.lineTo(Math.max(0,Math.min(canvasWidth,x)),Math.max(0,Math.min(canvasHeight,y)));commitFreeform();}
   else if(tool==CIRCLE){
    float size=Math.min(Math.abs(x-startX),Math.abs(y-startY));
    nativeBeginEdit();nativeShape(ELLIPSE,(int)startX,(int)startY,(int)(startX+Math.copySign(size,x-startX)),(int)(startY+Math.copySign(size,y-startY)),color);
   }
   else if(tool==GRADIENT){
    int[] data=GradientRaster.create(canvasWidth,canvasHeight,startX,startY,x,y,color,radialGradient);
    if(hasSelection())for(int row=0;row<canvasHeight;row++)for(int col=0;col<canvasWidth;col++)if(!selectionContains(col,row))data[2+row*canvasWidth+col]=0;
    nativePasteSelection(data,0,0);
   }
   else if(tool==BRUSH||tool==ERASER||tool==PENCIL||tool==CLONE||tool==RECOLOR){if(!strokeEditing||x!=previousX||y!=previousY)paintStroke(previousX,previousY,x,y,event);}
   else if(tool>=ROUNDED_RECTANGLE&&tool<=FILLED_TRIANGLE)additionalShape(tool,startX,startY,x,y);
   else nativeShape(tool,(int)startX,(int)startY,(int)x,(int)y,color);
   drawing=false;refresh();
  }return true;
 case MotionEvent.ACTION_CANCEL:
  if(tool==LINE&&drawing){curve=dragBackup;dragBackup=null;curveHandle=-1;}
  shapePath.reset();invalidate();
  if(drawing&&tool==SELECT_FREE){hasSelection=false;freeSelectionReady=false;freePath.reset();freeRegion.setEmpty();invalidate();}
  if(drawing&&(tool==SELECT_RECTANGLE||tool==SELECT_ELLIPSE)){hasSelection=false;invalidate();}
  if(movingSelection){restoreMovedSelection();invalidate();}
  movingSelection=false;movingPixels=false;drawing=false;return true;
 default:return true;}
 }
}
