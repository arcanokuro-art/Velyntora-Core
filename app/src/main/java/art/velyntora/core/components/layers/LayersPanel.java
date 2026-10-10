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


/** Owns components/layers behavior; the host is the workspace composition facade. */
final class LayersPanel {
  private final MainActivity host;
  LayersPanel(MainActivity host) { this.host = host; }

  void refreshLayerPanel() {
    if (host.readLayerItems() == null) return;
    host.readLayerItems().removeAllViews();
    host.readLayerItems().setMinimumHeight(host.dp(48 * host.readDrawing().layerCount()));
    for (Bitmap old : host.readThumbnails()) old.recycle();
    host.readThumbnails().clear();
    if (host.readLayerOpacity() != null) host.readLayerOpacity().setProgress(Math.round(host.readDrawing().layerOpacity() * 100));
    for (int i = host.readDrawing().layerCount() - 1; i >= 0; --i) {
      final int index = i;
      String label = host.readDrawing().layerName(index) + (host.readDrawing().layerVisible(index) ? "" : " · Oculta");
      LinearLayout item = host.row();
      Bitmap preview = host.readDrawing().layerThumbnail(index);
      if (preview != null) {
        host.readThumbnails().add(preview);
        ImageView thumbnail = new ImageView(host);
        thumbnail.setImageBitmap(preview);
        thumbnail.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        item.addView(thumbnail, new LinearLayout.LayoutParams(host.dp(48), host.dp(48)));
      }
      Button layerButton =
          host.button(
              item,
              label,
              () -> {
                host.readDrawing().selectLayer(index);
                host.refreshLayerPanel();
              });
      layerButton.setSelected(index == host.readDrawing().activeLayer());
      layerButton.setBackgroundResource(R.drawable.tool_button_background);
      layerButton.setBackgroundTintList(null);
      layerButton.setOnLongClickListener(
          v -> {
            if (host.readProjectProgress() == null) host.renameLayer(index);
            return true;
          });
      layerButton.setContentDescription(
          host.readDrawing().layerName(index)
              + (index == host.readDrawing().activeLayer() ? ", activa" : "")
              + (host.readDrawing().layerVisible(index) ? ", visible" : ", oculta"));
      if (android.os.Build.VERSION.SDK_INT >= 30)
        layerButton.setStateDescription(index == host.readDrawing().activeLayer() ? "Activa" : "");
      item.setOnClickListener(
          view -> {
            if (host.readProjectProgress() != null) return;
            host.readDrawing().selectLayer(index);
            host.refreshLayerPanel();
          });
      host.readLayerItems().addView(item);
    }
  }

  void addLayer() {
    if (!host.readDrawing().addLayer()) host.message("Límite de capas o memoria alcanzado");
    else host.message("Capa creada: " + (host.readDrawing().activeLayer() + 1));
    host.refreshLayerPanel();
  }

  void renameLayer(int index) {
    android.widget.EditText name = new android.widget.EditText(host);
    name.setSingleLine(true);
    name.setText(host.readDrawing().layerName(index));
    name.setSelectAllOnFocus(true);
    name.setContentDescription("Nombre de capa");
    name.setFilters(
        new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(4096)});
    AlertDialog dialog =
        new AlertDialog.Builder(host)
            .setTitle("Renombrar capa")
            .setView(name)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      if (!host.readDrawing().renameLayer(index, name.getText().toString())) {
                        name.setError("Escribe un nombre de hasta 4096 bytes UTF-8");
                        return;
                      }
                      dialog.dismiss();
                      host.refreshLayerPanel();
                    }));
    dialog.show();
  }

  void chooseLayer() {
    int count = host.readDrawing().layerCount();
    String[] items = new String[count];
    for (int i = 0; i < count; ++i) {
      items[i] =
          (i == host.readDrawing().activeLayer() ? "● " : "  ")
              + host.readDrawing().layerName(i)
              + (host.readDrawing().layerVisible(i) ? "" : " (oculta)");
    }
    new AlertDialog.Builder(host)
        .setTitle("Capas")
        .setItems(
            items,
            (dialog, index) -> {
              host.readDrawing().selectLayer(index);
              host.refreshLayerPanel();
            })
        .show();
  }

  void deleteLayer() {
    if (!host.readDrawing().deleteLayer()) host.message("No se puede eliminar la única capa");
    host.refreshLayerPanel();
  }

  void toggleLayer() {
    if (!host.readDrawing().toggleLayer()) host.message("No se pudo cambiar la visibilidad");
    host.refreshLayerPanel();
  }

  void moveLayer(int direction) {
    if (!host.readDrawing().moveLayer(direction)) host.message("La capa ya está en el extremo");
    host.refreshLayerPanel();
  }

  void create(LinearLayout root) {
    LinearLayout layerPanel = new LinearLayout(host);
    layerPanel.setOrientation(LinearLayout.VERTICAL);
    layerPanel.setBackgroundColor(0xFF202020);
    TextView layerTitle = host.text("CAPAS");
    layerTitle.setPadding(host.dp(6), host.dp(8), host.dp(6), host.dp(8));
    layerPanel.addView(layerTitle);
    LinearLayout layerCommands = host.row();
    host.iconButton(layerCommands, "Añadir capa", "new", host::addLayer);
    host.iconButton(layerCommands, "Eliminar capa", "delete", host::deleteLayer);

    LinearLayout layerOrder = host.row();
    host.iconButton(layerOrder, "Subir capa", "up", () -> host.moveLayer(1));
    host.iconButton(layerOrder, "Bajar capa", "down", () -> host.moveLayer(-1));

    host.iconButton(layerCommands, "Mostrar / ocultar", "eye", host::toggleLayer);
    TextView opacityLabel = host.text("Opacidad: 100%");
    opacityLabel.setPadding(host.dp(6), host.dp(8), host.dp(6), host.dp(2));

    SeekBar opacity = new SeekBar(host);
    opacity.setContentDescription("Opacidad de la capa activa");
    opacity.setMax(100);
    opacity.setProgress(100);
    opacity.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            opacityLabel.setText("Opacidad: " + progress + "%");
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {
            // One history checkpoint per gesture, not one per slider tick.
            if (host.readProjectProgress() == null) host.readDrawing().setLayerOpacity(bar.getProgress() / 100f);
          }
        });

    host.writeLayerOpacity(opacity);
    LinearLayout layerItems = new LinearLayout(host);
    layerItems.setOrientation(LinearLayout.VERTICAL);
    layerPanel.addView(layerItems, new LinearLayout.LayoutParams(-1, 0, 1));
    layerPanel.addView(layerCommands);
    layerPanel.addView(layerOrder);
    layerPanel.addView(opacityLabel);
    layerPanel.addView(opacity);
    ScrollView layerScroll = new ScrollView(host);
    layerScroll.setFillViewport(true);
    layerScroll.addView(layerPanel);

    host.writeLayerItems(layerItems);
    host.writeLayerScroll(layerScroll);


  }
}
