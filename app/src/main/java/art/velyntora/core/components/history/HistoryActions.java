package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;


/** Owns components/history behavior; the host is the workspace composition facade. */
final class HistoryActions {
  private final MainActivity host;
  HistoryActions(MainActivity host) { this.host = host; }

  void undo() {
    host.readDrawing().undo();
    host.refreshLayerPanel();
  }

  void redo() {
    host.readDrawing().redo();
    host.refreshLayerPanel();
  }
}
