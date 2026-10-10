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


/** Owns components/text behavior; the host is the workspace composition facade. */
final class TextDialog {
  private final MainActivity host;
  TextDialog(MainActivity host) { this.host = host; }

  void configureText(int x, int y) {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    android.widget.EditText input = new android.widget.EditText(host);
    input.setHint("Escribe tu texto");
    input.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setMinLines(3);
    input.setFilters(
        new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(4096)});
    form.addView(input);
    TextView sizeLabel = host.text("Tamaño: 32 px");
    form.addView(sizeLabel);
    SeekBar size = new SeekBar(host);
    size.setMax(252);
    size.setProgress(28);
    form.addView(size);
    size.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean u) {
            sizeLabel.setText("Tamaño: " + (n + 4) + " px");
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    android.widget.CheckBox bold = new android.widget.CheckBox(host),
        italic = new android.widget.CheckBox(host);
    bold.setText("Negrita");
    italic.setText("Cursiva");
    form.addView(bold);
    form.addView(italic);
    android.widget.Spinner font = new android.widget.Spinner(host);
    String[] families = {"sans-serif", "serif", "monospace"};
    font.setAdapter(
        new android.widget.ArrayAdapter<String>(
            host, android.R.layout.simple_spinner_dropdown_item, families));
    form.addView(font);
    form.addView(
        host.text("Se inserta en la capa activa. Puedes deshacer, seleccionar y mover el resultado."));
    AlertDialog dialog =
        new AlertDialog.Builder(host)
            .setTitle("Texto en " + x + ", " + y)
            .setView(host.scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Insertar", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    view -> {
                      if (host.readDrawing().insertText(
                          input.getText().toString(),
                          x,
                          y,
                          size.getProgress() + 4,
                          bold.isChecked(),
                          italic.isChecked(),
                          families[font.getSelectedItemPosition()])) dialog.dismiss();
                      else host.message("Introduce texto visible dentro del lienzo");
                    }));
    dialog.show();
  }
}
