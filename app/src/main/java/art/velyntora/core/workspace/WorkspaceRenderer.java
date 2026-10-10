package art.velyntora.core;

import android.graphics.*;

/** Viewport, checkerboard and sharp source pixels; tool overlays are delegated. */
final class WorkspaceRenderer {
  static void draw(DrawingView v, Canvas canvas) {

    if (v.getWidth() <= 0 || v.getHeight() <= 0) return;
    float scale = (float) v.viewport.scale;
    canvas.drawColor(0xFF2B2B2B);
    canvas.save();
    canvas.translate((float) v.viewport.centerX, (float) v.viewport.centerY);
    canvas.rotate((float) v.viewport.angle);
    canvas.scale(scale, scale);
    canvas.translate(-v.canvasWidth / 2f, -v.canvasHeight / 2f);
    canvas.clipRect(0, 0, v.canvasWidth, v.canvasHeight);
    if(v.tool==DrawingView.LINE || v.tool==DrawingView.FREEFORM) {
    v.checkerPaint.setColor(0xFFFFFFFF);
    canvas.drawRect(0, 0, v.canvasWidth, v.canvasHeight, v.checkerPaint);
    v.checkerPaint.setColor(0xFFD1D1D1);
    for (int row = 0; row < (v.canvasHeight + 15) / 16; ++row)
      for (int col = (row & 1); col < (v.canvasWidth + 15) / 16; col += 2)
        canvas.drawRect(col * 16, row * 16, (col + 1) * 16, (row + 1) * 16, v.checkerPaint);
    } else Checkerboard.draw(canvas,v.canvasWidth,v.canvasHeight);
    // Interpolate only when reducing the image. Enlarged source pixels stay sharp.
    v.paint.setFilterBitmap(scale < 1f);
    canvas.drawBitmap(v.bitmap, 0, 0, v.paint);

    Tools.controller(v.tool).preview(v, canvas, scale);
    SelectionSupport.draw(v, canvas, scale);
    canvas.restore();
  }
}
