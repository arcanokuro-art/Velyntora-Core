package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for line_curve. Shared document state belongs to DrawingView. */
final class LineCurveTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.setFocusableInTouchMode(true);
    v.requestFocus();
    v.curveHandle =
        v.curve == null
            ? -1
            : v.curve.hit(
                x, y, 24 * v.getResources().getDisplayMetrics().density / (float) v.viewport.scale);
    v.dragBackup = v.curve == null ? null : v.curve.copy();
    if (v.curveHandle < 0) {
      v.confirmCurve();
      v.curveHistory.clear();
      v.canceledCurve = null;
      v.curve = new CurveDraft(x, y, x, y);
      v.curveHandle = 3;
      v.dragBackup = null;
    }
    v.viewportChanged();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    if (v.dragBackup == null) v.curve = new CurveDraft(v.startX, v.startY, x, y);
    else v.curve.move(v.curveHandle, x, y);
    v.invalidate();
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (v.curve != null) {
      if (v.dragBackup == null) v.curve = new CurveDraft(v.startX, v.startY, x, y);
      else v.curve.move(v.curveHandle, x, y);
      v.curveHistory.record(v.dragBackup, v.curve);
      v.drawing = false;
      v.dragBackup = null;
      v.curveHandle = -1;
      v.viewportChanged();
    }
  }

  static android.graphics.Path curvePath(DrawingView v) {
    android.graphics.Path p = new android.graphics.Path();
    p.moveTo(v.curve.x[0], v.curve.y[0]);
    p.cubicTo(v.curve.x[1], v.curve.y[1], v.curve.x[2], v.curve.y[2], v.curve.x[3], v.curve.y[3]);
    return p;
  }

  static boolean confirmCurve(DrawingView v) {
    if (v.curve == null) return false;
    Bitmap image = Bitmap.createBitmap(v.canvasWidth, v.canvasHeight, Bitmap.Config.ARGB_8888);
    try {
      Paint style = new Paint(Paint.ANTI_ALIAS_FLAG);
      style.setColor(v.color);
      style.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
      style.setStyle(Paint.Style.STROKE);
      style.setStrokeWidth(v.brushRadius * 2);
      style.setStrokeCap(Paint.Cap.ROUND);
      new Canvas(image).drawPath(v.curvePath(), style);
      int[] data = new int[2 + v.canvasWidth * v.canvasHeight];
      data[0] = v.canvasWidth;
      data[1] = v.canvasHeight;
      image.getPixels(data, 2, v.canvasWidth, 0, 0, v.canvasWidth, v.canvasHeight);
      if (v.hasSelection())
        for (int row = 0; row < v.canvasHeight; row++)
          for (int col = 0; col < v.canvasWidth; col++)
            if (!v.selectionContains(col, row)) data[2 + row * v.canvasWidth + col] = 0;
      if (DrawingView.nativePasteSelection(data, 0, 0)) {
        int tag = v.nextCurveTag++;
        v.savedCurves.put(
            tag,
            new CurveSnapshot(
                v.curve, v.color, v.brushRadius, v.brushOpacity, new SelectionSnapshot(v)));
        while (v.savedCurves.size() > 15) v.savedCurves.removeAt(0);
        DrawingView.nativeMarkCurve(tag);
      }
      v.curve = null;
      v.canceledCurve = null;
      v.curveHistory.clear();
      v.curveHandle = -1;
      v.refresh();
      return true;
    } finally {
      image.recycle();
    }
  }

  static void cancelCurve(DrawingView v) {
    v.curve = null;
    v.canceledCurve = null;
    v.curveHistory.clear();
    v.curveHandle = -1;
    v.viewportChanged();
  }

  static void undo(DrawingView v) {
    if (v.curve != null) {
      CurveDraft previous = v.curveHistory.undo(v.curve);
      if (previous != null) v.curve = previous;
      else {
        v.canceledCurve = v.curve.copy();
        v.curve = null;
      }
      v.viewportChanged();
      return;
    }
    v.canceledCurve = null;
    v.curveHistory.clear();
    int tag = DrawingView.nativeCurveTag();
    if (DrawingView.nativeUndo()) {
      CurveSnapshot saved = v.savedCurves.get(tag);
      v.deselect();
      if (saved != null) {
        v.curve = saved.geometry.copy();
        v.color = saved.color;
        v.brushRadius = saved.radius;
        v.brushOpacity = saved.opacity;
        v.tool = DrawingView.LINE;
        v.curveHistory.clear();
        v.canceledCurve = null;
        saved.selection.restore(v);
        if (v.pickedColorListener != null) v.pickedColorListener.accept(v.color);
      }
      v.refresh();
    }
  }

  static void redo(DrawingView v) {
    if (v.canceledCurve != null) {
      v.curve = v.canceledCurve;
      v.canceledCurve = null;
      v.viewportChanged();
      return;
    }
    if (v.curve != null) {
      CurveDraft next = v.curveHistory.redo(v.curve);
      if (next != null) {
        v.curve = next;
        v.viewportChanged();
        return;
      }
    }
    v.cancelCurve();
    if (DrawingView.nativeRedo()) {
      v.deselect();
      v.refresh();
    }
  }

  public void cancel(DrawingView v) {
    if (v.drawing) {
      v.curve = v.dragBackup;
      v.dragBackup = null;
      v.curveHandle = -1;
    }
  }

  public void preview(DrawingView v, Canvas canvas, float scale) {
    if (v.curve == null) return;
    Paint preview = new Paint(Paint.ANTI_ALIAS_FLAG);
    preview.setColor(v.color);
    preview.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
    preview.setStyle(Paint.Style.STROKE);
    preview.setStrokeWidth(v.brushRadius * 2);
    preview.setStrokeCap(Paint.Cap.ROUND);
    canvas.drawPath(v.curvePath(), preview);
    preview.setStyle(Paint.Style.FILL);
    preview.setColor(0xff7040b0);
    preview.setAlpha(255);
    for (int i = 0; i < 4; i++) canvas.drawCircle(v.curve.x[i], v.curve.y[i], 6 / scale, preview);
  }
}
