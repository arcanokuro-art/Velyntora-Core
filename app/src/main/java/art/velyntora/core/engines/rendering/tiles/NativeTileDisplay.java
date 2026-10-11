package art.velyntora.core;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import java.util.*;
import java.util.concurrent.*;

/** Native document tiles; refinement runs off the UI and keeps coarse tiles visible. */
final class NativeTileDisplay implements CanvasTileDisplay {
  private static final int SIDE=256,CAPACITY=128;
  private record Key(int sample,int x,int y){}
  private record Tile(Bitmap bitmap,Rect bounds){}
  private final int width,height,overviewSample;
  private final Bitmap overview;
  private final LinkedHashMap<Key,Tile> cache=new LinkedHashMap<>(16,.75f,true);
  private final Set<Key> pending=new HashSet<>(),dirtyKeys=new HashSet<>();
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler ui=new Handler(Looper.getMainLooper());
  private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
  private Runnable redraw;
  private long generation;
  private boolean closed,overviewPending;
  NativeTileDisplay(int width,int height,Bitmap overview){
    this.width=width;this.height=height;this.overview=overview;
    overviewSample=Math.max((width+overview.getWidth()-1)/overview.getWidth(),(height+overview.getHeight()-1)/overview.getHeight());
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
  }
  void redraw(Runnable action){redraw=action;}
  private int sample(float scale){int result=1;while(result<128 && scale*result<.5f)result*=2;return result;}
  private Tile create(Key key){
    int span=SIDE*key.sample,left=Math.max(0,key.x*span-key.sample),top=Math.max(0,key.y*span-key.sample);
    int right=Math.min(width,(key.x+1)*span+key.sample),bottom=Math.min(height,(key.y+1)*span+key.sample);
    int w=(right-left+key.sample-1)/key.sample,h=(bottom-top+key.sample-1)/key.sample;
    int[] pixels=DrawingView.nativeSampleRegion(left,top,key.sample,w,h);
    if(pixels==null || pixels.length!=w*h)throw new IllegalStateException("Incomplete native tile");
    Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    try{bitmap.setPixels(pixels,0,w,0,0,w,h);return new Tile(bitmap,new Rect(left,top,left+w*key.sample,top+h*key.sample));}
    catch(RuntimeException|OutOfMemoryError error){bitmap.recycle();throw error;}
  }
  private void put(Key key,Tile tile){
    Tile old=cache.put(key,tile);if(old!=null)old.bitmap.recycle();
    while(cache.size()>CAPACITY){Iterator<Map.Entry<Key,Tile>> it=cache.entrySet().iterator();Tile discarded=it.next().getValue();it.remove();discarded.bitmap.recycle();}
  }
  public void warm(float scale){ /* The prepared overview already covers the entire document. */ }
  /** Worker-only preparation before this display is published to the UI. */
  void warmVisible(float scale,Rect visible){
    int chosen=sample(scale);
    int left=Math.max(0,visible.left),top=Math.max(0,visible.top);
    int right=Math.min(width,visible.right),bottom=Math.min(height,visible.bottom);
    if(left>=right || top>=bottom)return;
    while(chosen<overviewSample){
      int span=chosen*SIDE;
      long count=(long)((right-1)/span-left/span+1)*((bottom-1)/span-top/span+1);
      if(count<=CAPACITY)break;
      chosen*=2;
    }
    if(chosen>=overviewSample)return;
    int span=chosen*SIDE;
    for(int y=top/span;y*span<bottom;y++)for(int x=left/span;x*span<right;x++){
      Key key=new Key(chosen,x,y);put(key,create(key));
    }
  }
  private void request(Key key){
    if(closed || !pending.add(key))return;long version=generation;
    worker.execute(()->{
      Tile result=null;try{result=create(key);}catch(RuntimeException|OutOfMemoryError ignored){}
      Tile completed=result;
      ui.post(()->{
        pending.remove(key);
        if(completed==null)return;
        if(closed || generation!=version){completed.bitmap.recycle();if(!closed && redraw!=null)redraw.run();return;}
        put(key,completed);dirtyKeys.remove(key);if(redraw!=null)redraw.run();
      });
    });
  }
  public void draw(Canvas canvas,float scale){
    if(closed)return;int chosen=sample(scale),span=chosen*SIDE;Rect clip=canvas.getClipBounds();
    int left=Math.max(0,clip.left),top=Math.max(0,clip.top),right=Math.min(width,clip.right),bottom=Math.min(height,clip.bottom);
    paint.setFilterBitmap(scale<1);
    // SRC inside an isolated layer avoids double alpha at overlapping gutters.
    int layer=canvas.saveLayer(null,null);
    canvas.drawBitmap(overview,null,new Rect(0,0,overview.getWidth()*overviewSample,overview.getHeight()*overviewSample),paint);
    // Retain prepared coarser detail on unusually wide/rotated viewports.
    for(int coarse=overviewSample/2;coarse>chosen;coarse/=2){
      int coarseSpan=coarse*SIDE;
      for(int y=top/coarseSpan;y*coarseSpan<bottom;y++)for(int x=left/coarseSpan;x*coarseSpan<right;x++){
        Tile tile=cache.get(new Key(coarse,x,y));
        if(tile!=null)canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);
      }
    }
    if(chosen<overviewSample)for(int y=top/span;y*span<bottom;y++)for(int x=left/span;x*span<right;x++){
      Key key=new Key(chosen,x,y);Tile tile=cache.get(key);
      if(tile!=null){canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);if(dirtyKeys.contains(key))request(key);}
      else request(key);
    }
    canvas.restoreToCount(layer);
  }

  public void invalidate(int left,int top,int w,int h){
    generation++;Rect dirty=new Rect(left,top,left+w,top+h);
    if((long)w*h<=1048576)updateOverview(dirty);else requestOverview();
    // Existing textures are updated in bounded sample cells by the stroke refresh path.
    for(var entry:cache.entrySet()){
      Tile tile=entry.getValue();if(!Rect.intersects(dirty,tile.bounds))continue;
      if((long)w*h>1048576){dirtyKeys.add(entry.getKey());continue;}
      int sample=entry.getKey().sample,l=Math.max(left,tile.bounds.left),t=Math.max(top,tile.bounds.top);
      int r=Math.min(left+w,Math.min(width,tile.bounds.right)),b=Math.min(top+h,Math.min(height,tile.bounds.bottom));
      int x=(l-tile.bounds.left)/sample,y=(t-tile.bounds.top)/sample;
      int columns=(r-tile.bounds.left+sample-1)/sample-x,rows=(b-tile.bounds.top+sample-1)/sample-y;
      if(columns<=0||rows<=0)continue;
      int[] pixels=DrawingView.nativeSampleRegion(tile.bounds.left+x*sample,tile.bounds.top+y*sample,sample,columns,rows);
      if(pixels!=null && pixels.length==columns*rows)tile.bitmap.setPixels(pixels,0,columns,x,y,columns,rows);
    }
  }
  private void updateOverview(Rect dirty){
    int x=Math.max(0,dirty.left/overviewSample),y=Math.max(0,dirty.top/overviewSample);
    int right=Math.min(overview.getWidth(),(dirty.right+overviewSample-1)/overviewSample);
    int bottom=Math.min(overview.getHeight(),(dirty.bottom+overviewSample-1)/overviewSample);
    int w=right-x,h=bottom-y;if(w<=0||h<=0)return;
    int[] pixels=DrawingView.nativeSampleRegion(x*overviewSample,y*overviewSample,overviewSample,w,h);
    if(pixels!=null && pixels.length==w*h)overview.setPixels(pixels,0,w,x,y,w,h);
  }
  private void requestOverview(){
    if(closed || overviewPending)return;overviewPending=true;long version=generation;
    int w=overview.getWidth(),h=overview.getHeight();
    worker.execute(()->{
      int[] pixels=null;try{pixels=DrawingView.nativeSampleRegion(0,0,overviewSample,w,h);}catch(RuntimeException|OutOfMemoryError ignored){}
      final int[] completed=pixels;
      ui.post(()->{
        overviewPending=false;if(closed)return;
        if(version!=generation){requestOverview();return;}
        if(completed!=null && completed.length==w*h){overview.setPixels(completed,0,w,0,0,w,h);if(redraw!=null)redraw.run();}
      });
    });
  }
  public void clear(){closed=true;generation++;worker.shutdownNow();for(Tile tile:cache.values())tile.bitmap.recycle();cache.clear();}
}
