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
  private final Set<Key> dirtyKeys=new HashSet<>();
  private List<Key> wanted=List.of();
  private boolean refinementPending;
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
  private List<Key> keys(int sample,Rect visible){
    List<Key> result=new ArrayList<>();int span=sample*SIDE;
    if(visible.isEmpty())return result;
    for(int y=visible.top/span;y*span<visible.bottom;y++)for(int x=visible.left/span;x*span<visible.right;x++)result.add(new Key(sample,x,y));
    return result;
  }
  private boolean complete(List<Key> keys){
    if(keys.size()>CAPACITY)return false;
    for(Key key:keys)if(!cache.containsKey(key))return false;
    return true;
  }
  private void requestVisible(List<Key> keys){
    wanted=keys;
    if(closed || refinementPending)return;
    List<Key> missing=new ArrayList<>();
    for(Key key:keys)if(!cache.containsKey(key)||dirtyKeys.contains(key))missing.add(key);
    if(missing.isEmpty())return;
    refinementPending=true;long version=generation;
    List<Key> requested=List.copyOf(keys);
    worker.execute(()->{
      Map<Key,Tile> results=new LinkedHashMap<>();boolean succeeded=true;
      try{for(Key key:missing)results.put(key,create(key));}
      catch(RuntimeException|OutOfMemoryError error){succeeded=false;}
      boolean ready=succeeded;
      ui.post(()->{
        refinementPending=false;
        if(!closed && version==generation && ready && wanted.equals(requested)){
          // Protect reused visible tiles from LRU eviction while installing the batch.
          for(Key key:requested)cache.get(key);
          for(var entry:results.entrySet()){put(entry.getKey(),entry.getValue());dirtyKeys.remove(entry.getKey());}
        }else for(Tile tile:results.values())tile.bitmap.recycle();
        if(!closed){
          if(redraw!=null)redraw.run();
          if(version!=generation || !wanted.equals(requested))requestVisible(wanted);
        }
      });
    });
  }
  public void draw(Canvas canvas,float scale){
    if(closed)return;
    Rect clip=canvas.getClipBounds();
    Rect visible=new Rect(Math.max(0,clip.left),Math.max(0,clip.top),Math.min(width,clip.right),Math.min(height,clip.bottom));
    if(visible.isEmpty())return;
    int chosen=sample(scale);
    List<Key> desired=keys(chosen,visible);
    while(chosen<overviewSample && desired.size()>CAPACITY){chosen*=2;desired=keys(chosen,visible);}
    // A frame uses one complete resolution across the visible document. Reuse finer
    // cached pixels on zoom-out instead of dropping immediately to the overview.
    List<Key> ready=null;
    for(int level=1;level<overviewSample;level*=2){
      List<Key> candidates=keys(level,visible);
      if(complete(candidates)){ready=candidates;break;}
    }
    paint.setFilterBitmap(scale<1);
    int layer=canvas.saveLayer(null,null);
    canvas.drawBitmap(overview,null,new Rect(0,0,overview.getWidth()*overviewSample,overview.getHeight()*overviewSample),paint);
    if(ready!=null)for(Key key:ready){Tile tile=cache.get(key);canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);}
    canvas.restoreToCount(layer);
    if(ready!=null && !ready.isEmpty() && ready.get(0).sample<=chosen)desired=ready;
    if(chosen<overviewSample)requestVisible(desired);else wanted=List.of();
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
