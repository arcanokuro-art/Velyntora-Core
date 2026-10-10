package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for rectangle. Shared document state belongs to DrawingView. */
final class RectangleTool implements DrawingTool {
  public void up(DrawingView v, float x, float y, MotionEvent event) {
    v.additionalShape(v.tool, v.startX, v.startY, x, y);
  }
}
