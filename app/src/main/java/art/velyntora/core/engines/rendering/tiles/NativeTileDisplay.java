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
  private final int width,height;
  private final LinkedHashMap<Key,Tile> cache=new LinkedHashMap<>(16,.75f,true);
  private final Set<Key> pending=new HashSet<>(),dirtyKeys=new HashSet<>();
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler ui=new Handler(Looper.getMainLooper());
  private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
  private Runnable redraw;
  private long generation;
  private boolean closed;
  NativeTileDisplay(int width,int height){this.width=width;this.height=height;}
  void redraw(Runnable action){redraw=action;}
  private int sample(float scale){int result=1;while(result<128 && scale*result<.5f)result*=2;return result;}
  private Tile create(Key key){
    int span=SIDE*key.sample,left=Math.max(0,key.x*span-key.sample),top=Math.max(0,key.y*span-key.sample);
    int right=Math.min(width,(key.x+1)*span+key.sample),bottom=Math.min(height,(key.y+1)*span+key.sample);
    int w=(right-left+key.sample-1)/key.sample,h=(bottom-top+key.sample-1)/key.sample;
    int[] pixels=DrawingView.nativeSampleRegion(left,top,key.sample,w,h);
    if(pixels==null || pixels.length!=w*h)throw new IllegalStateException("Incomplete native tile");
    return new Tile(Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888),new Rect(left,top,left+w*key.sample,top+h*key.sample));
  }
  private void put(Key key,Tile tile){
    Tile old=cache.put(key,tile);if(old!=null)old.bitmap.recycle();
    while(cache.size()>CAPACITY){Iterator<Map.Entry<Key,Tile>> it=cache.entrySet().iterator();Tile discarded=it.next().getValue();it.remove();discarded.bitmap.recycle();}
  }
  public void warm(float scale){
    int chosen=sample(scale);warmLevel(chosen);
    if(chosen>1 && (long)width*height<=16000000){int finer=chosen/2;
      if(count(chosen)+count(finer)<=CAPACITY)warmLevel(finer);
    }
  }
  private long count(int sample){int span=sample*SIDE;return (long)((width+span-1)/span)*((height+span-1)/span);}
  private void warmLevel(int sample){
    if(count(sample)>CAPACITY)return;int span=sample*SIDE;
    for(int y=0;y<height;y+=span)for(int x=0;x<width;x+=span){Key key=new Key(sample,x/span,y/span);put(key,create(key));}
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
    for(int y=top/span;y*span<bottom;y++)for(int x=left/span;x*span<right;x++){
      Key key=new Key(chosen,x,y);Tile tile=cache.get(key);
      Rect cell=new Rect(x*span,y*span,Math.min((x+1)*span,width),Math.min((y+1)*span,height));
      canvas.save();canvas.clipRect(cell);
      if(tile!=null){canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);if(dirtyKeys.contains(key))request(key);}
      else {
        for(var entry:cache.entrySet())if(entry.getKey().sample>chosen && Rect.intersects(cell,entry.getValue().bounds))
          canvas.drawBitmap(entry.getValue().bitmap,null,entry.getValue().bounds,paint);
        request(key);
      }
      canvas.restore();
    }
  }
  public void invalidate(int left,int top,int w,int h){
    generation++;Rect dirty=new Rect(left,top,left+w,top+h);
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
  public void clear(){closed=true;generation++;worker.shutdownNow();for(Tile tile:cache.values())tile.bitmap.recycle();cache.clear();}
}
