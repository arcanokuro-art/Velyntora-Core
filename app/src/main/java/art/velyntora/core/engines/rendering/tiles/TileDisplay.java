package art.velyntora.core;

import android.graphics.*;
import java.util.*;

/** Independent GPU textures: an edited tile does not re-upload the full document. */
final class TileDisplay {
  static final int SIDE=256, CAPACITY=128;
  final Bitmap source;
  private final LinkedHashMap<Key,Tile> cache=new LinkedHashMap<>(16,.75f,true);
  private final Paint paint=new Paint();
  private record Key(int sample,int x,int y) {}
  private record Tile(Bitmap bitmap,Rect bounds) {}
  TileDisplay(Bitmap source){this.source=source;}
  private int sample(float scale){
    int sample=1;
    while(sample<128 && scale*sample<.5f)sample*=2;
    return sample;
  }
  private Tile tile(Key key){
    Tile found=cache.get(key);if(found!=null)return found;
    int span=SIDE*key.sample,left=key.x*span,top=key.y*span;
    int right=Math.min(left+span+key.sample,source.getWidth()),bottom=Math.min(top+span+key.sample,source.getHeight());
    left=Math.max(0,left-key.sample);top=Math.max(0,top-key.sample);
    int width=right-left,height=bottom-top;
    Bitmap result=Bitmap.createBitmap((width+key.sample-1)/key.sample,
        (height+key.sample-1)/key.sample,Bitmap.Config.ARGB_8888);
    Paint resample=new Paint();resample.setFilterBitmap(key.sample>1);
    new Canvas(result).drawBitmap(source,new Rect(left,top,left+width,top+height),
        new Rect(0,0,result.getWidth(),result.getHeight()),resample);
    Tile generated=new Tile(result,new Rect(left,top,right,bottom));
    cache.put(key,generated);
    while(cache.size()>CAPACITY){
      Iterator<Map.Entry<Key,Tile>> it=cache.entrySet().iterator();
      Map.Entry<Key,Tile> old=it.next();it.remove();old.getValue().bitmap.recycle();
    }
    return generated;
  }
  void warm(float scale){
    int sample=sample(scale),span=sample*SIDE;
    long count=(long)((source.getWidth()+span-1)/span)*((source.getHeight()+span-1)/span);
    if(count>CAPACITY)return;
    for(int y=0;y<source.getHeight();y+=span)
      for(int x=0;x<source.getWidth();x+=span)tile(new Key(sample,x/span,y/span));
  }
  void draw(Canvas canvas,float scale){
    Rect clip=canvas.getClipBounds();int sample=sample(scale),span=sample*SIDE;
    int left=Math.max(0,clip.left),top=Math.max(0,clip.top);
    int right=Math.min(source.getWidth(),clip.right),bottom=Math.min(source.getHeight(),clip.bottom);
    paint.setFilterBitmap(scale<1);
    for(int y=top/span;y*span<bottom;y++)for(int x=left/span;x*span<right;x++){
      Tile tile=tile(new Key(sample,x,y));
      Rect destination=new Rect(x*span,y*span,Math.min((x+1)*span,source.getWidth()),
          Math.min((y+1)*span,source.getHeight()));
      canvas.save();canvas.clipRect(destination);
      canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);canvas.restore();
    }
  }
  void invalidate(int left,int top,int width,int height){
    Iterator<Map.Entry<Key,Tile>> it=cache.entrySet().iterator();
    while(it.hasNext()){
      Map.Entry<Key,Tile> e=it.next();Key k=e.getKey();int span=SIDE*k.sample;
      if(k.x*span-k.sample<left+width && (k.x+1)*span+k.sample>left && k.y*span-k.sample<top+height && (k.y+1)*span+k.sample>top){
        it.remove();e.getValue().bitmap.recycle();
      }
    }
  }
  void clear(){for(Tile t:cache.values())t.bitmap.recycle();cache.clear();}
}
