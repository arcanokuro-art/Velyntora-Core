package art.velyntora.core;

import android.app.ProgressDialog;
import java.util.function.BooleanSupplier;

/** Serial UI entry point: native composition and display preparation run on a worker. */
final class LayerWorkQueue {
  static void run(MainActivity host, BooleanSupplier operation, boolean redraw, Runnable completed) {
    if (host.readProjectProgress() != null) return;
    DrawingView drawing = host.readDrawing();
    drawing.confirmCurve();
    drawing.finishOpacityStroke();
    ProgressDialog progress = new ProgressDialog(host);
    progress.setMessage("Actualizando capas…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    drawing.setEnabled(false);
    progress.show();
    float scale = (float)drawing.readViewport().scale;
    new Thread(() -> {
      boolean result = false;
      CanvasRendering.Frame prepared = null;
      try {
        result = operation.getAsBoolean();
        if (result && redraw) prepared = CanvasRendering.prepareFrame(scale);
      } catch (RuntimeException | OutOfMemoryError error) {
        result = false;
      }
      final boolean success = result;
      final CanvasRendering.Frame frame = prepared;
      host.runOnUiThread(() -> {
        try {
          if (host.isDestroyed()) {
            if (frame != null) frame.dispose();
            return;
          }
          if (frame != null) drawing.installCanvasFrame(frame);
          else {drawing.viewportChanged();}
          if (!success) host.message("No se pudo completar la operación de capa");
        } finally {
          progress.dismiss();
          host.writeProjectProgress(null);
          drawing.setEnabled(true);
          if (!host.isDestroyed()) { host.refreshLayerPanel(); host.updateStatus(); }
        }
        if (success && !host.isDestroyed() && completed != null) completed.run();
      });
    }, "velyntora-layer-operation").start();
  }
  static void select(MainActivity host, int index, Runnable completed) {
    run(host, () -> DrawingView.nativeSelectLayer(index), false, completed);
  }
  private LayerWorkQueue() {}
}
