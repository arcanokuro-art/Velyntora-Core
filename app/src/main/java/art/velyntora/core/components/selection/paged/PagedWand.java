package art.velyntora.core;
import android.graphics.Region;
import java.util.*;

/** Scanline flood selection: bounded source rows, bitset visitation, no full ARGB image. */
final class PagedWand {
  private static boolean matches(int a,int b,int tolerance){
    for(int shift=0;shift<=24;shift+=8)if(Math.abs((a>>>shift&255)-(b>>>shift&255))>tolerance)return false;
    return true;
  }
  static Region select(int width,int height,int sx,int sy,int tolerance){
    if(sx<0 || sy<0 || sx>=width || sy>=height)return new Region();
    LinkedHashMap<Integer,int[]> rows=new LinkedHashMap<Integer,int[]>(16,.75f,true){
      protected boolean removeEldestEntry(Map.Entry<Integer,int[]> e){return size()>8;}
    };
    java.util.function.IntFunction<int[]> row=y->rows.computeIfAbsent(y,key->DrawingView.nativeRegionPixels(0,key,width,1));
    int target=row.apply(sy)[sx];BitSet visited=new BitSet(width*height);
    ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(sy*width+sx);Region result=new Region();
    while(!queue.isEmpty()){
      int pos=queue.removeLast(),y=pos/width,x=pos%width;
      if(visited.get(pos))continue;
      int[] current=row.apply(y);if(!matches(current[x],target,tolerance)){visited.set(pos);continue;}
      int left=x,right=x+1;
      while(left>0 && !visited.get(y*width+left-1) && matches(current[left-1],target,tolerance))left--;
      while(right<width && !visited.get(y*width+right) && matches(current[right],target,tolerance))right++;
      visited.set(y*width+left,y*width+right);result.op(left,y,right,y+1,Region.Op.UNION);
      for(int next:new int[]{y-1,y+1}){
        if(next<0 || next>=height)continue;
        int[] neighbor=row.apply(next);boolean run=false;
        for(int col=left;col<right;col++){
          boolean accepted=!visited.get(next*width+col) && matches(neighbor[col],target,tolerance);
          if(accepted && !run)queue.add(next*width+col);
          run=accepted;
        }
      }
    }
    return result;
  }
  static void run(DrawingView drawing,int x,int y){
    if(!(drawing.getContext() instanceof MainActivity host))return;
    int width=drawing.documentWidth(),height=drawing.documentHeight(),tolerance=drawing.readWandTolerance();
    Region[] result={null};
    LayerWorkQueue.run(host,()->{result[0]=select(width,height,x,y,tolerance);return true;},false,()->drawing.installWandRegion(result[0]));
  }
  private PagedWand(){}
}
