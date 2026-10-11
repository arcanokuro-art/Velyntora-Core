package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns engines/rendering behavior; the host is the workspace composition facade. */
final class CanvasRendering {
  private final DrawingView host;
  CanvasRendering(DrawingView host) { this.host = host; }

  void noteStrokeBounds(float x0,float y0,float x1,float y1,float radius) {
    float margin=Math.min(radius,2048)+2;
    host.writeDirtyLeft(Math.min(host.readDirtyLeft(),Math.max(0,(int)Math.floor(Math.min(x0,x1)-margin))));
    host.writeDirtyTop(Math.min(host.readDirtyTop(),Math.max(0,(int)Math.floor(Math.min(y0,y1)-margin))));
    host.writeDirtyRight(Math.max(host.readDirtyRight(),Math.min(host.readCanvasWidth(),(int)Math.ceil(Math.max(x0,x1)+margin))));
    host.writeDirtyBottom(Math.max(host.readDirtyBottom(),Math.min(host.readCanvasHeight(),(int)Math.ceil(Math.max(y0,y1)+margin))));
  }

  void refreshStrokePixels() {
    int w=host.readDirtyRight()-host.readDirtyLeft(),h=host.readDirtyBottom()-host.readDirtyTop();
    if(w>0 && h>0) {
      int[] pixels=host.nativeRegionPixels(host.readDirtyLeft(),host.readDirtyTop(),w,h);
      if(pixels!=null && pixels.length==w*h)
        host.readBitmap().setPixels(pixels,0,w,host.readDirtyLeft(),host.readDirtyTop(),w,h);
    }
    host.writeDirtyLeft(host.writeDirtyTop(Integer.MAX_VALUE));host.writeDirtyRight(host.writeDirtyBottom(0));
    host.invalidate();
  }

  void requestStrokeRefresh() {
    if (!host.readStrokeRefreshPending()) {
      host.writeStrokeRefreshPending(true);
      host.postOnAnimation(host.readStrokeRefresh());
    }
  }

  void finishStrokeRefresh() {
    host.removeCallbacks(host.readStrokeRefresh());
    host.writeStrokeRefreshPending(false);
    refreshStrokePixels();
    if (host.readCanvasChangedListener() != null) host.readCanvasChangedListener().run();
    host.viewportChanged();
  }

  void refresh() {
    host.removeCallbacks(host.readStrokeRefresh());
    host.writeStrokeRefreshPending(false);
    host.writeDirtyLeft(host.writeDirtyTop(Integer.MAX_VALUE));host.writeDirtyRight(host.writeDirtyBottom(0));
    host.refreshPixels();
    if (host.readCanvasChangedListener() != null) host.readCanvasChangedListener().run();
    host.viewportChanged();
  }

  void refreshPixels() {
    int width = host.nativeWidth(), height = host.nativeHeight();
    if (width <= 0 || height <= 0) return;
    if (width != host.readCanvasWidth() || height != host.readCanvasHeight()) {
      Bitmap replacement = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      host.readBitmap().recycle();
      host.writeBitmap(replacement);
      host.writeCanvasWidth(width);
      host.writeCanvasHeight(height);
      host.readViewport().documentSize(width, height);
    }
    // Bounded stripes avoid a document-sized Java array and native flattened copy.
    int rows = Math.max(1, 262144 / width);
    for (int y=0; y<height; y+=rows) {
      int count=Math.min(rows,height-y);
      int[] pixels=host.nativeRegionPixels(0,y,width,count);
      if(pixels==null || pixels.length!=(long)width*count) return;
      host.readBitmap().setPixels(pixels,0,width,0,y,width,count);
    }
    host.invalidate();
  }

  void onDraw(Canvas canvas) {
    host.defaultDraw(canvas);
    WorkspaceRenderer.draw(host, canvas);
  }
}
