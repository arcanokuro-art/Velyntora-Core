package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;

/** Android workspace modeled after Pinta's tool, canvas, palette and status regions. */
public final class MainActivity extends Activity {
    private static final int SAVE_PROJECT = 43, OPEN_PROJECT = 44, SAVE_JPEG = 45, SAVE_WEBP = 46, SAVE_BMP = 47, SAVE_TGA = 48, OPEN_TGA = 49, SAVE_ORA = 50, OPEN_ORA = 51, SAVE_TIFF = 52, OPEN_TIFF = 53, SAVE_GIF = 54, OPEN_ICO = 55, SAVE_ICO = 56, OPEN_PPM = 57, SAVE_PPM = 58;
    private static final int SAVE_PNG = 41;
    private static final int OPEN_IMAGE = 42;
    private DrawingView drawing;
    private TextView status;
    private TextView selectedTool;
    private LinearLayout layerItems;
    private ScrollView layerScroll,toolScroll;
    private android.widget.FrameLayout workspaceFrame;
    private boolean panelsInline;
    private HorizontalScrollView commandScroll,paletteScroll;
    private Button currentColorButton;
    private final android.util.SparseArray<Button> toolButtons=new android.util.SparseArray<>();
    private final java.util.ArrayList<Bitmap> thumbnails = new java.util.ArrayList<>();
    private android.os.Handler layerRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private SeekBar layerOpacity;
    private int activeColor = Color.BLACK;
    private Bitmap selectionClipboard;
    private android.app.ProgressDialog projectProgress;

    private void copySelection() {
        Bitmap copy = drawing.copySelection();
        if (copy == null) { message("No hay selección válida"); return; }
        if (selectionClipboard != null && !selectionClipboard.isRecycled()) selectionClipboard.recycle();
        selectionClipboard = copy;
        message("Selección copiada a la memoria de Velyntora");
    }

    private void moveSelectedContent() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        android.widget.EditText horizontal = new android.widget.EditText(this);
        horizontal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        horizontal.setHint("Desplazamiento horizontal (px)");
        horizontal.setText("0");
        android.widget.EditText vertical = new android.widget.EditText(this);
        vertical.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        vertical.setHint("Desplazamiento vertical (px)");
        vertical.setText("0");
        form.addView(horizontal);
        form.addView(vertical);
        new AlertDialog.Builder(this).setTitle("Mover contenido seleccionado")
            .setView(scrollForm(form)).setNegativeButton("Cancelar", null)
            .setPositiveButton("Mover", (dialog, which) -> {
                try {
                    int dx = Integer.parseInt(horizontal.getText().toString().trim());
                    int dy = Integer.parseInt(vertical.getText().toString().trim());
                    if (!drawing.moveSelectedPixels(dx,dy)) message("No se pudo mover la selección");
                } catch (NumberFormatException error) {
                    message("Introduce desplazamientos numéricos válidos");
                }
            }).show();
    }

    /** User-configurable brightness, with one native undo checkpoint per application. */
    private void configureBrightness() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(510);
        slider.setProgress(255);
        value.setText("Brillo: 0");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Brillo: " + (progress - 255));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Ajustar brillo de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int adjustment = slider.getProgress() - 255;
                if (adjustment == 0) return;
                if (!drawing.brightnessActive(adjustment)) message("No hay cambios de brillo");
            }).show();
    }

    /** Configure contrast without changing pixels until Apply is pressed. */
    private void configureContrast() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(200);
        slider.setProgress(100);
        value.setText("Contraste: 0");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Contraste: " + (progress - 100));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Ajustar contraste de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int adjustment = slider.getProgress() - 100;
                if (adjustment == 0) return;
                if (!drawing.contrastActive(adjustment)) message("No hay cambios de contraste");
            }).show();
    }

    /** Choose a binary threshold without applying it during slider movement. */
    private void configureThreshold() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(255);
        slider.setProgress(128);
        value.setText("Umbral: 128");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Umbral: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Blanco y negro por umbral").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                if (!drawing.thresholdActive(slider.getProgress())) message("No hay cambios de umbral");
            }).show();
    }

    /** Configure saturation with a single undoable apply operation. */
    private void configureSaturation() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(200);
        slider.setProgress(100);
        value.setText("Saturación: 0");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Saturación: " + (progress - 100));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Ajustar saturación de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int adjustment = slider.getProgress() - 100;
                if (adjustment == 0) return;
                if (!drawing.saturationActive(adjustment)) message("No hay cambios de saturación");
            }).show();
    }

    /** Configure gamma percentage; no change is made until Apply. */
    private void configureGamma() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(290);
        slider.setProgress(90);
        value.setText("Gamma: 100 %");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Gamma: " + (progress + 10) + " %");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Ajustar gamma de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int percent = slider.getProgress() + 10;
                if (percent == 100) return;
                if (!drawing.gammaActive(percent)) message("No hay cambios de gamma");
            }).show();
    }

    /** Adjustable posterization without modifying the document until confirmation. */
    private void configurePosterization() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(30);
        slider.setProgress(2);
        value.setText("Niveles por canal: 4");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Niveles por canal: " + (progress + 2));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Posterizar capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                if (!drawing.posterizeActive(slider.getProgress() + 2)) message("No hay cambios al posterizar");
            }).show();
    }

    /** Set the solarization threshold; applying is a single undoable operation. */
    private void configureSolarization() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(255);
        slider.setProgress(128);
        value.setText("Umbral de solarización: 128");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Umbral de solarización: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Solarizar capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                if (!drawing.solarizeActive(slider.getProgress())) message("No hay cambios al solarizar");
            }).show();
    }

    /** Set individual red, green, and blue offsets on the active layer. */
    private void configureRgbOffsets() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        final SeekBar[] sliders = new SeekBar[3];
        final String[] names = {"Rojo", "Verde", "Azul"};
        for (int i = 0; i < sliders.length; i++) {
            TextView value = new TextView(this);
            value.setText(names[i] + ": 0");
            SeekBar slider = new SeekBar(this);
            slider.setMax(510);
            slider.setProgress(255);
            final String name = names[i];
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    value.setText(name + ": " + (progress - 255));
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
            form.addView(value);
            form.addView(slider);
            sliders[i] = slider;
        }
        new AlertDialog.Builder(this).setTitle("Ajustar canales RGB").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int red = sliders[0].getProgress() - 255;
                int green = sliders[1].getProgress() - 255;
                int blue = sliders[2].getProgress() - 255;
                if (red == 0 && green == 0 && blue == 0) return;
                if (!drawing.tintActive(red, green, blue)) message("No hay cambios en canales RGB");
            }).show();
    }

    /** Edit black and white points before applying the levels operation. */
    private void configureLevels() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView blackValue = new TextView(this);
        TextView whiteValue = new TextView(this);
        SeekBar black = new SeekBar(this);
        SeekBar white = new SeekBar(this);
        black.setMax(254);
        white.setMax(254);
        black.setProgress(16);
        white.setProgress(238);
        blackValue.setText("Punto negro: 16");
        whiteValue.setText("Punto blanco: 239");
        black.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { blackValue.setText("Punto negro: " + progress); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        white.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { whiteValue.setText("Punto blanco: " + (progress + 1)); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(blackValue);
        form.addView(black);
        form.addView(whiteValue);
        form.addView(white);
        new AlertDialog.Builder(this).setTitle("Ajustar niveles de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int blackPoint = black.getProgress();
                int whitePoint = white.getProgress() + 1;
                if (blackPoint >= whitePoint) { message("El punto blanco debe ser mayor que el punto negro"); return; }
                if (blackPoint == 0 && whitePoint == 255) return;
                if (!drawing.levelsActive(blackPoint, whitePoint)) message("No hay cambios de niveles");
            }).show();
    }

    /** Configure exposure without modifying the document until Apply. */
    private void configureExposure() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(290);
        slider.setProgress(90);
        value.setText("Exposición: 100 %");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Exposición: " + (progress + 10) + " %");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Ajustar exposición de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int percent = slider.getProgress() + 10;
                if (percent == 100) return;
                if (!drawing.exposureActive(percent)) message("No hay cambios de exposición");
            }).show();
    }

    /** Rotate hue by a selected angle without applying during slider movement. */
    private void configureHue() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(360);
        slider.setProgress(180);
        value.setText("Rotación de tono: 0°");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Rotación de tono: " + (progress - 180) + "°");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Rotar tono de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int degrees = slider.getProgress() - 180;
                if (degrees == 0) return;
                if (!drawing.hueRotateActive(degrees)) message("No hay cambios de tono");
            }).show();
    }

    /** Independently scale red, green and blue channels using one undoable operation. */
    private void configureRgbBalance() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        SeekBar[] sliders = new SeekBar[3];
        String[] names = {"Rojo", "Verde", "Azul"};
        for (int i = 0; i < sliders.length; i++) {
            TextView value = new TextView(this);
            value.setText(names[i] + ": 100 %");
            SeekBar slider = new SeekBar(this);
            slider.setMax(300);
            slider.setProgress(100);
            String name = names[i];
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    value.setText(name + ": " + progress + " %");
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
            form.addView(value);
            form.addView(slider);
            sliders[i] = slider;
        }
        new AlertDialog.Builder(this).setTitle("Balance de canales RGB").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int red = sliders[0].getProgress();
                int green = sliders[1].getProgress();
                int blue = sliders[2].getProgress();
                if (red == 100 && green == 100 && blue == 100) return;
                if (!drawing.colorBalanceActive(red, green, blue)) message("No hay cambios de balance");
            }).show();
    }

    /** Choose quantization step; only apply on confirmation. */
    private void configureQuantization() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(63);
        slider.setProgress(15);
        value.setText("Paso de cuantización: 16");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Paso de cuantización: " + (progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Cuantizar colores de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                if (!drawing.quantizeActive(slider.getProgress() + 1)) message("No hay cambios al cuantizar");
            }).show();
    }

    /** Choose the maximum RGB channel value for highlight limiting. */
    private void configureHighlightCeiling() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        SeekBar slider = new SeekBar(this);
        slider.setMax(255);
        slider.setProgress(224);
        value.setText("Límite máximo RGB: 224");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Límite máximo RGB: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Limitar luces de capa").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int ceiling = slider.getProgress();
                if (ceiling == 255) return;
                if (!drawing.clampHighlightsActive(ceiling)) message("No hay cambios al limitar luces");
            }).show();
    }

    private int wandTolerance = 0;

    private void configureWandTolerance() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        TextView value = new TextView(this);
        value.setText("Tolerancia: " + wandTolerance + " / 255");
        SeekBar slider = new SeekBar(this);
        slider.setMax(255);
        slider.setProgress(wandTolerance);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText("Tolerancia: " + progress + " / 255");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(value);
        form.addView(slider);
        new AlertDialog.Builder(this).setTitle("Varita mágica").setView(scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                wandTolerance = slider.getProgress();
                drawing.setWandTolerance(wandTolerance);
            }).show();
    }

    private void duplicateSelection() {
        Bitmap copy = drawing.copySelection();
        if (copy == null) { message("No hay selección válida para duplicar"); return; }
        try {
            if (!drawing.pasteBitmap(copy)) message("No se pudo duplicar la selección");
            else message("Selección duplicada");
        } finally {
            copy.recycle();
        }
    }

    private void pasteSelection() {
        if (selectionClipboard == null || selectionClipboard.isRecycled()) {
            message("No hay contenido copiado"); return;
        }
        if (!drawing.pasteBitmap(selectionClipboard)) message("No se pudo pegar la selección");
        else message("Selección pegada");
    }

    private void cutSelection() {
        Bitmap copy = drawing.copySelection();
        if (copy == null) { message("No hay selección válida"); return; }
        if (!drawing.eraseSelection()) { copy.recycle(); message("No se pudo cortar la selección"); return; }
        if (selectionClipboard != null && !selectionClipboard.isRecycled()) selectionClipboard.recycle();
        selectionClipboard = copy;
        message("Selección cortada");
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        drawing = new DrawingView(this);
        drawing.setOnTextPositionListener(this::configureText);
        drawing.setOnColorPickedListener(color -> {setActiveColor(color);message("Color seleccionado");});
        drawing.setOnCanvasChangedListener(() -> {
            layerRefreshHandler.removeCallbacks(layerRefreshTask);
            layerRefreshHandler.postDelayed(layerRefreshTask, 120);
        });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F1F1);

        LinearLayout menus = row();
        menu(menus, "Archivo", new String[]{"Nuevo…", "Abrir imagen", "Abrir TGA", "Abrir TIFF", "Abrir ICO", "Abrir PPM", "Abrir OpenRaster", "Guardar OpenRaster", "Guardar PNG", "Guardar JPEG", "Guardar WebP", "Guardar BMP", "Guardar TGA", "Guardar TIFF", "Guardar GIF (imagen fija)", "Guardar ICO (hasta 256 px)", "Guardar PPM", "Abrir proyecto", "Guardar proyecto"},
            new Runnable[]{() -> configureDimensions(0), this::openImage, this::openTga, this::openTiff, this::openIco, this::openPpm, () -> openRasterPicker(false), () -> openRasterPicker(true), this::savePng, () -> saveImage("image/jpeg", "dibujo.jpg", SAVE_JPEG), () -> saveImage("image/webp", "dibujo.webp", SAVE_WEBP), () -> saveImage("image/bmp", "dibujo.bmp", SAVE_BMP), () -> saveImage("image/x-tga", "dibujo.tga", SAVE_TGA), () -> saveImage("image/tiff", "dibujo.tiff", SAVE_TIFF), () -> {message("GIF: colores reducidos y transparencia sin semitransparencias");saveImage("image/gif", "dibujo.gif", SAVE_GIF);}, () -> saveImage("image/vnd.microsoft.icon", "dibujo.ico", SAVE_ICO), () -> saveImage("image/x-portable-pixmap", "dibujo.ppm", SAVE_PPM), () -> projectPicker(false), () -> projectPicker(true)});
        menu(menus, "Editar", new String[]{"Deshacer", "Rehacer", "Copiar selección", "Cortar selección", "Pegar selección", "Duplicar selección", "Mover contenido…", "Transformar selección…", "Seleccionar todo", "Invertir selección", "Expandir selección 1 px", "Contraer selección 1 px", "Borrar selección", "Deseleccionar"},
            new Runnable[]{this::undo, this::redo, this::copySelection, this::cutSelection, this::pasteSelection, this::duplicateSelection, this::moveSelectedContent, this::configureSelectionTransform, drawing::selectAll, () -> {if(!drawing.invertSelection())message("No se pudo invertir la selección");}, () -> {if(!drawing.expandSelectionOnePixel())message("No se pudo expandir la selección");}, () -> {if(!drawing.shrinkSelectionOnePixel())message("No se pudo contraer la selección");}, () -> {if(!drawing.eraseSelection())message("No hay selección válida");}, drawing::deselect});
        menu(menus, "Ver", new String[]{"Ajustar al lienzo", "Acercar", "Alejar", "Zoom 100 %", "Zoom 7000 %", "Rotar vista 15° derecha", "Rotar vista 15° izquierda", "Restablecer rotación", "Mostrar / ocultar herramientas", "Mostrar / ocultar capas"},
            new Runnable[]{drawing::fitCanvas, () -> drawing.zoomBy(1.25f), () -> drawing.zoomBy(0.8f),
                () -> drawing.setZoomPercent(100), () -> drawing.setZoomPercent(7000),
                () -> drawing.rotateView(15), () -> drawing.rotateView(-15),
                () -> drawing.rotateView(-drawing.rotationDegrees()), () -> togglePanel(toolScroll), () -> togglePanel(layerScroll)});
        new AdjustmentDialogs(this,drawing).addMenu(menus);
        menu(menus, "Extras de color", new String[]{"Invertir colores de capa", "Escala de grises (capa)", "Sepia (capa)", "Aumentar brillo (+20)", "Reducir brillo (-20)", "Brillo personalizado…", "Aumentar contraste (+20)", "Reducir contraste (-20)", "Contraste personalizado…", "Blanco y negro (umbral 128)", "Umbral personalizado…", "Posterizar (4 niveles)", "Posterizar personalizado…", "Solarizar (umbral 128)", "Solarizar personalizado…", "Aumentar saturación (+20)", "Reducir saturación (-20)", "Saturación personalizada…", "Gamma clara (120 %)", "Gamma oscura (80 %)", "Gamma personalizada…", "Tono cálido (+15 rojo)", "Tono frío (+15 azul)", "Canales RGB personalizados…", "Intercambiar rojo y verde", "Intercambiar rojo y azul", "Intercambiar verde y azul", "Aumentar rojo (120 %)", "Aumentar verde (120 %)", "Aumentar azul (120 %)", "Balance RGB personalizado…", "Rotar tono (+30°)", "Rotar tono (-30°)", "Rotación de tono personalizada…", "Ajustar niveles (16–239)", "Niveles personalizados…", "Exposición +20 %", "Exposición -20 %", "Exposición personalizada…", "Escala de grises desde rojo", "Escala de grises desde verde", "Escala de grises desde azul", "Reducir alfa de píxeles (80 %)", "Aumentar alfa de píxeles (120 %)", "Eliminar canal rojo", "Eliminar canal verde", "Eliminar canal azul", "Normalizar colores de capa", "Cuantizar colores (paso 16)", "Cuantizar colores (paso 32)", "Cuantización personalizada…", "Limitar canales RGB a 224", "Límite de luces personalizado…", "Elevar canales RGB a 32", "Añadir rojo (+20)", "Añadir verde (+20)", "Añadir azul (+20)"}, new Runnable[]{() -> {if(!drawing.invertActiveColors())message("No hay colores visibles para invertir");}, () -> {if(!drawing.grayscaleActive())message("La capa ya está en escala de grises o está vacía");}, () -> {if(!drawing.sepiaActive())message("La capa no tiene cambios para aplicar sepia");}, () -> {if(!drawing.brightnessActive(20))message("No hay cambios de brillo");}, () -> {if(!drawing.brightnessActive(-20))message("No hay cambios de brillo");}, this::configureBrightness, () -> {if(!drawing.contrastActive(20))message("No hay cambios de contraste");}, () -> {if(!drawing.contrastActive(-20))message("No hay cambios de contraste");}, this::configureContrast, () -> {if(!drawing.thresholdActive(128))message("La capa ya es blanco y negro o está vacía");}, this::configureThreshold, () -> {if(!drawing.posterizeActive(4))message("La capa no tiene cambios para posterizar");}, this::configurePosterization, () -> {if(!drawing.solarizeActive(128))message("La capa no tiene cambios para solarizar");}, this::configureSolarization, () -> {if(!drawing.saturationActive(20))message("No hay cambios de saturación");}, () -> {if(!drawing.saturationActive(-20))message("No hay cambios de saturación");}, this::configureSaturation, () -> {if(!drawing.gammaActive(120))message("No hay cambios de gamma");}, () -> {if(!drawing.gammaActive(80))message("No hay cambios de gamma");}, this::configureGamma, () -> {if(!drawing.tintActive(15,0,0))message("No hay cambios de tono");}, () -> {if(!drawing.tintActive(0,0,15))message("No hay cambios de tono");}, this::configureRgbOffsets, () -> {if(!drawing.swapChannelsActive(0))message("No hay cambios de canales");}, () -> {if(!drawing.swapChannelsActive(1))message("No hay cambios de canales");}, () -> {if(!drawing.swapChannelsActive(2))message("No hay cambios de canales");}, () -> {if(!drawing.colorBalanceActive(120,100,100))message("No hay cambios de balance");}, () -> {if(!drawing.colorBalanceActive(100,120,100))message("No hay cambios de balance");}, () -> {if(!drawing.colorBalanceActive(100,100,120))message("No hay cambios de balance");}, this::configureRgbBalance, () -> {if(!drawing.hueRotateActive(30))message("No hay cambios de tono");}, () -> {if(!drawing.hueRotateActive(-30))message("No hay cambios de tono");}, this::configureHue, () -> {if(!drawing.levelsActive(16,239))message("No hay cambios de niveles");}, this::configureLevels, () -> {if(!drawing.exposureActive(120))message("No hay cambios de exposición");}, () -> {if(!drawing.exposureActive(80))message("No hay cambios de exposición");}, this::configureExposure, () -> {if(!drawing.grayscaleFromChannelActive(0))message("No hay cambios de escala de grises");}, () -> {if(!drawing.grayscaleFromChannelActive(1))message("No hay cambios de escala de grises");}, () -> {if(!drawing.grayscaleFromChannelActive(2))message("No hay cambios de escala de grises");}, () -> {if(!drawing.adjustAlphaActive(80))message("No hay cambios de alfa");}, () -> {if(!drawing.adjustAlphaActive(120))message("No hay cambios de alfa");}, () -> {if(!drawing.removeChannelActive(0))message("El canal rojo ya está vacío");}, () -> {if(!drawing.removeChannelActive(1))message("El canal verde ya está vacío");}, () -> {if(!drawing.removeChannelActive(2))message("El canal azul ya está vacío");}, () -> {if(!drawing.normalizeActive())message("No hay cambios para normalizar");}, () -> {if(!drawing.quantizeActive(16))message("No hay cambios al cuantizar");}, () -> {if(!drawing.quantizeActive(32))message("No hay cambios al cuantizar");}, this::configureQuantization, () -> {if(!drawing.clampHighlightsActive(224))message("No hay cambios al limitar colores");}, this::configureHighlightCeiling, () -> {if(!drawing.liftShadowsActive(32))message("No hay cambios al elevar sombras");}, () -> {if(!drawing.adjustChannelActive(0,20))message("No hay cambios en rojo");}, () -> {if(!drawing.adjustChannelActive(1,20))message("No hay cambios en verde");}, () -> {if(!drawing.adjustChannelActive(2,20))message("No hay cambios en azul");}});
        menu(menus, "Imagen", new String[]{"Nuevo lienzo…", "Cambiar tamaño de imagen…", "Cambiar tamaño de lienzo…", "Recortar documento a selección", "Voltear capa horizontalmente", "Voltear capa verticalmente", "Rotar capa 180°", "Rotar capa 90° derecha", "Rotar capa 90° izquierda", "Conservar solo selección rectangular (capa)", "Conservar solo selección elíptica (capa)", "Conservar selección libre o varita (capa)"},
            new Runnable[]{() -> configureDimensions(0), () -> configureDimensions(1), () -> configureDimensions(2), () -> {if(!drawing.cropDocument())message("Selecciona el área del documento que quieres conservar");}, () -> {if(!drawing.flipActiveHorizontal())message("La capa no tiene cambios para voltear");},
                () -> {if(!drawing.flipActiveVertical())message("La capa no tiene cambios para voltear");},
                () -> {if(!drawing.rotateActive180())message("La capa no tiene cambios para rotar");},
                () -> {if(!drawing.rotateActive90(true))message("La capa no tiene cambios para rotar");},
                () -> {if(!drawing.rotateActive90(false))message("La capa no tiene cambios para rotar");},
                () -> {if(!drawing.trimActiveToRectSelection())message("Selecciona un rectángulo válido con contenido exterior");},
                () -> {if(!drawing.trimActiveToEllipseSelection())message("Selecciona una elipse válida con contenido exterior");},
                () -> {if(!drawing.trimActiveToFreeSelection())message("Selecciona una región libre o con varita que tenga contenido exterior");}});
        menu(menus, "Capas", new String[]{"Añadir capa", "Seleccionar capa", "Eliminar capa", "Mostrar / ocultar", "Subir capa", "Bajar capa"},
            new Runnable[]{this::addLayer, this::chooseLayer, this::deleteLayer, this::toggleLayer,
                () -> moveLayer(1), () -> moveLayer(-1)});
        menu(menus, "Herramientas", new String[]{"Tolerancia de varita mágica", "Configurar pincel", "Elegir color…"},
            new Runnable[]{this::configureWandTolerance, this::configureBrush, this::configureColor});
        AdjustmentDialogs filters=new AdjustmentDialogs(this,drawing);
        menu(menus, "Efectos",new String[]{"Básicos","Desenfoques","Distorsiones","Arte y fotografía","Generadores","Más filtros","Objetos"},new Runnable[]{()->new AlertDialog.Builder(this).setTitle("Efectos básicos").setItems(new String[]{"Desenfoque de caja…","Enfocar…","Detectar bordes…","Repujado…","Pixelar…","Ruido…","Viñeta…"},(d,k)->configureEffect(k)).show(),filters::openBlurMenu,filters::openDistortionMenu,filters::openArtisticMenu,filters::openRenderMenu,filters::openUtilityMenu,filters::openObjectMenu});
        menu(menus, "Ayuda", new String[]{"Acerca de"},
            new Runnable[]{() -> message("Velyntora Core 0.1 — versión de desarrollo Android")});
        addScrollable(root, menus);

        LinearLayout commands = row();
        button(commands, "Nuevo", () -> configureDimensions(0));
        button(commands, "Abrir", this::openImage);
        button(commands, "Guardar", this::savePng);
        button(commands, "Deshacer", this::undo);
        button(commands, "Rehacer", this::redo);
        button(commands, "Herramientas", () -> togglePanel(toolScroll));
        button(commands, "Capas", () -> togglePanel(layerScroll));
        commandScroll=addScrollable(root, commands);

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(0xFFE4E4E4);
        sidebar.setPadding(dp(3), dp(4), dp(3), dp(4));
        TextView toolsTitle = text("HERRAMIENTAS");
        sidebar.addView(toolsTitle);
        android.widget.GridLayout toolGrid = new android.widget.GridLayout(this);toolGrid.setColumnCount(3);sidebar.addView(toolGrid);
        tool(toolGrid, "Pincel", DrawingView.BRUSH);
        tool(toolGrid, "Lápiz (1 píxel)", DrawingView.PENCIL);
        tool(toolGrid, "Desplazamiento", DrawingView.PAN);
        tool(toolGrid, "Zoom: toque acerca; toque largo aleja; arrastre vertical", DrawingView.ZOOM);
        tool(toolGrid, "Línea", DrawingView.LINE);
        tool(toolGrid, "Rectángulo", DrawingView.RECTANGLE);
        tool(toolGrid, "Elipse", DrawingView.ELLIPSE);
        tool(toolGrid, "Rectángulo relleno", DrawingView.FILLED_RECTANGLE);
        tool(toolGrid, "Elipse rellena", DrawingView.FILLED_ELLIPSE);
        tool(toolGrid, "Rectángulo redondeado", DrawingView.ROUNDED_RECTANGLE);
        tool(toolGrid, "Redondeado relleno", DrawingView.FILLED_ROUNDED_RECTANGLE);
        tool(toolGrid, "Triángulo", DrawingView.TRIANGLE);
        tool(toolGrid, "Triángulo relleno", DrawingView.FILLED_TRIANGLE);
        tool(toolGrid, "Texto", DrawingView.TEXT);
        tool(toolGrid, "Cubeta", DrawingView.BUCKET);
        tool(toolGrid, "Cuentagotas", DrawingView.PICKER);
        tool(toolGrid, "Borrador", DrawingView.ERASER);
        tool(toolGrid, "Selección rectangular", DrawingView.SELECT_RECTANGLE);
        tool(toolGrid, "Selección elíptica", DrawingView.SELECT_ELLIPSE);
        tool(toolGrid, "Selección libre (contorno)", DrawingView.SELECT_FREE);
        tool(toolGrid, "Varita mágica", DrawingView.MAGIC_WAND);
        tool(toolGrid, "Mover contorno", DrawingView.MOVE_SELECTION);
        tool(toolGrid, "Mover píxeles", DrawingView.MOVE_PIXELS);
        TextView brushSizeLabel = text("Radio: 4 px");
        sidebar.addView(brushSizeLabel);
        SeekBar brushSize = new SeekBar(this);
        brushSize.setContentDescription("Radio del pincel en píxeles");
        brushSize.setMax(127);
        brushSize.setProgress(3);
        brushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int size = progress + 1;
                brushSizeLabel.setText("Radio: " + size + " px");
                drawing.setBrushRadius(size);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        sidebar.addView(brushSize);
        button(sidebar, "Configurar pincel", this::configureBrush);
        selectedTool = text("Pincel");
        sidebar.addView(selectedTool);
        ScrollView toolScroll = new ScrollView(this);toolScroll.addView(sidebar);
        this.toolScroll=toolScroll;
        LinearLayout layerPanel = new LinearLayout(this);
        layerPanel.setOrientation(LinearLayout.VERTICAL);
        layerPanel.setBackgroundColor(0xFFE6E6E6);
        TextView layerTitle = text("CAPAS");
        layerTitle.setPadding(dp(6), dp(8), dp(6), dp(8));
        layerPanel.addView(layerTitle);
        LinearLayout layerCommands = row();
        button(layerCommands, "+", this::addLayer);
        button(layerCommands, "−", this::deleteLayer);
        layerPanel.addView(layerCommands);
        LinearLayout layerOrder = row();
        button(layerOrder, "↑", () -> moveLayer(1));
        button(layerOrder, "↓", () -> moveLayer(-1));
        layerPanel.addView(layerOrder);
        button(layerPanel, "Mostrar / ocultar", this::toggleLayer);
        TextView opacityLabel = text("Opacidad: 100%");
        opacityLabel.setPadding(dp(6), dp(8), dp(6), dp(2));
        layerPanel.addView(opacityLabel);
        SeekBar opacity = new SeekBar(this);
        opacity.setContentDescription("Opacidad de la capa activa");
        opacity.setMax(100);
        opacity.setProgress(100);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                opacityLabel.setText("Opacidad: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                // One history checkpoint per gesture, not one per slider tick.
                drawing.setLayerOpacity(bar.getProgress() / 100f);
            }
        });
        layerPanel.addView(opacity);
        this.layerOpacity = opacity;
        LinearLayout layerItems = new LinearLayout(this);
        layerItems.setOrientation(LinearLayout.VERTICAL);
        layerPanel.addView(layerItems);
        ScrollView layerScroll = new ScrollView(this);
        layerScroll.addView(layerPanel);

        this.layerItems = layerItems;
        this.layerScroll = layerScroll;

        refreshLayerPanel();
        workspaceFrame=new android.widget.FrameLayout(this);
        layoutWorkspace();
        root.addView(workspaceFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout colors = row();
        currentColorButton=new Button(this);currentColorButton.setText("");currentColorButton.setPadding(0,0,0,0);currentColorButton.setBackgroundTintList(null);currentColorButton.setOnClickListener(v->configureColor());
        colors.addView(currentColorButton,new LinearLayout.LayoutParams(dp(48),dp(48)));setActiveColor(activeColor);
        TextView paletteLabel = text("COLORES  ");
        colors.addView(paletteLabel);
        button(colors,"Elegir color…",this::configureColor);
        int[] palette = {Color.BLACK, Color.WHITE, Color.GRAY, Color.RED, 0xFFFF9800,
            Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA,
            0xFF795548, 0xFF9C27B0};
        for (int color : palette) {
            View swatch = new View(this);
            swatch.setBackgroundColor(color);swatch.setFocusable(true);swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            swatch.setContentDescription(String.format(java.util.Locale.US,"Color #%06X",color & 0xffffff));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            colors.addView(swatch, params);
            swatch.setOnClickListener(v -> {
                setActiveColor(color);
            });
        }
        paletteScroll=addScrollable(root, colors);updateWorkspaceChrome();
        status = text("800 × 800 px  |  Zoom: ajustar  |  No guardado");
        status.setPadding(dp(10), dp(4), dp(10), dp(4));
        root.addView(status);
        drawing.setOnViewportChangedListener(() -> status.setText(String.format(java.util.Locale.US,
            "%d × %d px  |  Zoom: %.1f %%  |  Rotación: %.1f°", drawing.documentWidth(), drawing.documentHeight(), drawing.zoomPercent(), drawing.rotationDegrees())));
        setContentView(root);
    }

    private void configureColor(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        View preview=new View(this);preview.setBackgroundColor(activeColor);preview.setContentDescription("Vista previa del color");form.addView(preview,new LinearLayout.LayoutParams(-1,dp(48)));
        int[] values={activeColor>>>24,(activeColor>>>16)&255,(activeColor>>>8)&255,activeColor&255};String[] labels={"Alfa","Rojo","Verde","Azul"};
        for(int index=0;index<4;index++){final int channel=index;TextView label=text(labels[index]+": "+values[index]);form.addView(label);SeekBar slider=new SeekBar(this);slider.setMax(255);slider.setProgress(values[index]);slider.setContentDescription(labels[index]);form.addView(slider);slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){values[channel]=n;label.setText(labels[channel]+": "+n);preview.setBackgroundColor(values[0]<<24|values[1]<<16|values[2]<<8|values[3]);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}
        new AlertDialog.Builder(this).setTitle("Color RGBA").setView(scrollForm(form)).setNegativeButton("Cancelar",null).setPositiveButton("Usar color",(d,w)->{setActiveColor(values[0]<<24|values[1]<<16|values[2]<<8|values[3]);}).show();
    }

    private void setActiveColor(int color){
        activeColor=color;drawing.setColor(color);
        if(currentColorButton!=null){currentColorButton.setBackground(new ColorSwatchDrawable(color));String label=String.format(java.util.Locale.US,"Color activo: rojo %d, verde %d, azul %d, alfa %d. Elegir color",color>>>16&255,color>>>8&255,color&255,color>>>24);currentColorButton.setContentDescription(label);if(android.os.Build.VERSION.SDK_INT>=26)currentColorButton.setTooltipText(String.format(java.util.Locale.US,"Color activo #%08X",color));}
    }
    private void updateWorkspaceChrome(){
        android.content.res.Configuration config=getResources().getConfiguration();WorkspaceLayout layout=new WorkspaceLayout(Math.max(1,config.screenWidthDp),Math.max(1,config.screenHeightDp),config.fontScale);
        if(commandScroll!=null)commandScroll.setVisibility(layout.compact?View.GONE:View.VISIBLE);if(paletteScroll!=null)paletteScroll.setVisibility(layout.compact?View.GONE:View.VISIBLE);
    }
    private void togglePanel(ScrollView panel){if(panel==null)return;boolean show=panel.getVisibility()!=View.VISIBLE;if(show&&!panelsInline){toolScroll.setVisibility(View.GONE);layerScroll.setVisibility(View.GONE);}panel.setVisibility(show?View.VISIBLE:View.GONE);}
    private void detach(View view){if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);}
    private void layoutWorkspace(){
        android.content.res.Configuration config=getResources().getConfiguration();WorkspaceLayout layout=new WorkspaceLayout(Math.max(1,config.screenWidthDp),Math.max(1,config.screenHeightDp),config.fontScale);panelsInline=layout.inline;updateWorkspaceChrome();
        detach(drawing);detach(toolScroll);detach(layerScroll);workspaceFrame.removeAllViews();
        if(panelsInline){LinearLayout row=row();row.addView(toolScroll,new LinearLayout.LayoutParams(dp(layout.toolsWidth),-1));row.addView(drawing,new LinearLayout.LayoutParams(0,-1,1));row.addView(layerScroll,new LinearLayout.LayoutParams(dp(layout.layersWidth),-1));workspaceFrame.addView(row,new android.widget.FrameLayout.LayoutParams(-1,-1));toolScroll.setVisibility(View.VISIBLE);layerScroll.setVisibility(View.VISIBLE);}
        else{workspaceFrame.addView(drawing,new android.widget.FrameLayout.LayoutParams(-1,-1));workspaceFrame.addView(toolScroll,new android.widget.FrameLayout.LayoutParams(dp(layout.toolsWidth),-1,Gravity.START));workspaceFrame.addView(layerScroll,new android.widget.FrameLayout.LayoutParams(dp(layout.layersWidth),-1,Gravity.END));toolScroll.setVisibility(View.GONE);layerScroll.setVisibility(View.GONE);}
        toolScroll.setBackgroundColor(0xFFE4E4E4);layerScroll.setBackgroundColor(0xFFE6E6E6);
        toolScroll.setElevation(dp(6));layerScroll.setElevation(dp(6));
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(workspaceFrame!=null)layoutWorkspace();}

    private View scrollForm(View form) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.DKGRAY);
        view.setTextSize(12);
        return view;
    }

    private HorizontalScrollView addScrollable(LinearLayout root, LinearLayout content) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(content);
        root.addView(scroll);
        return scroll;
    }

    private Button button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);button.setMinHeight(dp(48));button.setMinimumHeight(dp(48));
        String description=label.equals("+")?"Añadir capa":label.equals("−")?"Eliminar capa":label.equals("↑")?"Subir capa":label.equals("↓")?"Bajar capa":label;
        button.setContentDescription(description);
        button.setOnClickListener(v -> action.run());
        parent.addView(button);
        return button;
    }

    private void tool(android.widget.GridLayout parent, String label, int tool) {
        Button iconButton = new Button(this);
        iconButton.setText(label);
        iconButton.setTextSize(12);
        iconButton.setAllCaps(false);
        iconButton.setMinWidth(0);
        iconButton.setMinimumWidth(0);iconButton.setBackgroundResource(R.drawable.tool_button_background);iconButton.setBackgroundTintList(null);iconButton.setSelected(tool==drawing.currentTool());toolButtons.put(tool,iconButton);
        int icon = 0;
        switch (tool) {
            case DrawingView.PENCIL: icon = R.drawable.pinta_pencil; break;
            case DrawingView.PAN: icon = R.drawable.pinta_pan; break;
            case DrawingView.ZOOM: icon = R.drawable.pinta_zoom; break;
            case DrawingView.BRUSH: icon = R.drawable.pinta_brush; break;
            case DrawingView.LINE: icon = R.drawable.pinta_line; break;
            case DrawingView.RECTANGLE:
            case DrawingView.FILLED_RECTANGLE: icon = R.drawable.pinta_rectangle; break;
            case DrawingView.ELLIPSE:
            case DrawingView.FILLED_ELLIPSE: icon = R.drawable.pinta_ellipse; break;
            case DrawingView.BUCKET: icon = R.drawable.pinta_bucket; break;
            case DrawingView.PICKER: icon = R.drawable.pinta_picker; break;
            case DrawingView.ERASER: icon = R.drawable.pinta_eraser; break;
            case DrawingView.SELECT_RECTANGLE: icon = R.drawable.pinta_select_rectangle; break;
            case DrawingView.SELECT_ELLIPSE: icon = R.drawable.pinta_select_ellipse; break;
            case DrawingView.TEXT: icon = R.drawable.pinta_text; break;
            case DrawingView.ROUNDED_RECTANGLE:
            case DrawingView.FILLED_ROUNDED_RECTANGLE: icon = R.drawable.pinta_rounded; break;
            case DrawingView.TRIANGLE:
            case DrawingView.FILLED_TRIANGLE: icon = R.drawable.pinta_triangle; break;
            case DrawingView.SELECT_FREE: icon = R.drawable.pinta_lasso; break;
            case DrawingView.MAGIC_WAND: icon = R.drawable.pinta_wand; break;
            case DrawingView.MOVE_SELECTION: icon = R.drawable.pinta_move_selection; break;
            case DrawingView.MOVE_PIXELS: icon = R.drawable.pinta_move_pixels; break;
        }
        if (icon != 0) {
            android.graphics.drawable.Drawable graphic = getDrawable(icon);
            boolean filled = tool == DrawingView.FILLED_RECTANGLE || tool == DrawingView.FILLED_ELLIPSE || tool == DrawingView.FILLED_ROUNDED_RECTANGLE || tool == DrawingView.FILLED_TRIANGLE;
            if(filled){android.graphics.drawable.GradientDrawable marker=new android.graphics.drawable.GradientDrawable();marker.setShape(android.graphics.drawable.GradientDrawable.OVAL);marker.setColor(0xff333333);android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{graphic,marker});layers.setLayerSize(1,dp(6),dp(6));layers.setLayerGravity(1,Gravity.BOTTOM|Gravity.RIGHT);graphic=layers;}
            graphic.setBounds(0,0,dp(24),dp(24));iconButton.setCompoundDrawables(null,graphic,null,null);iconButton.setText("");iconButton.setPadding(0,dp(12),0,dp(12));
            iconButton.setContentDescription(label);
            if(android.os.Build.VERSION.SDK_INT>=30)iconButton.setStateDescription(tool==drawing.currentTool()?"Activa":"");
            if (android.os.Build.VERSION.SDK_INT >= 26) iconButton.setTooltipText(label);
        }
        iconButton.setOnClickListener(v -> {
            drawing.setTool(tool);
            drawing.setColor(activeColor);
            selectedTool.setText(label);
            for(int i=0;i<toolButtons.size();i++)toolButtons.valueAt(i).setSelected(toolButtons.keyAt(i)==tool);
            if(android.os.Build.VERSION.SDK_INT>=30)for(int i=0;i<toolButtons.size();i++)toolButtons.valueAt(i).setStateDescription(toolButtons.keyAt(i)==tool?"Activa":"");
            if(!panelsInline&&toolScroll!=null)toolScroll.setVisibility(View.GONE);
        });
        android.widget.GridLayout.LayoutParams cell = new android.widget.GridLayout.LayoutParams();cell.width=dp(48);cell.height=dp(48);parent.addView(iconButton,cell);
    }

    private void menu(LinearLayout parent, String title, String[] labels, Runnable[] actions) {
        button(parent, title, () -> new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels, (dialog, index) -> actions[index].run())
            .show());
    }

    private void undo(){drawing.undo();refreshLayerPanel();}
    private void redo(){drawing.redo();refreshLayerPanel();}

    private final Runnable layerRefreshTask = this::refreshLayerPanel;

    @Override protected void onDestroy() {
        if (projectProgress != null) { projectProgress.dismiss(); projectProgress = null; }
        layerRefreshHandler.removeCallbacks(layerRefreshTask);
        if (layerItems != null) layerItems.removeAllViews();
        for (Bitmap old : thumbnails) old.recycle();
        thumbnails.clear();
        super.onDestroy();
    }

    private void refreshLayerPanel() {
        if (layerItems == null) return;
        layerItems.removeAllViews();
        for (Bitmap old : thumbnails) old.recycle();
        thumbnails.clear();
        if (layerOpacity != null) layerOpacity.setProgress(Math.round(drawing.layerOpacity() * 100));
        for (int i = drawing.layerCount() - 1; i >= 0; --i) {
            final int index = i;
            String label = "Capa " + (index + 1) + (drawing.layerVisible(index)?"":" · Oculta");
            LinearLayout item = row();
            Bitmap preview = drawing.layerThumbnail(index);
            if (preview != null) {
                thumbnails.add(preview);
                ImageView thumbnail = new ImageView(this);
                thumbnail.setImageBitmap(preview);thumbnail.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                item.addView(thumbnail, new LinearLayout.LayoutParams(dp(48), dp(48)));
            }
            Button layerButton=button(item, label, () -> {
                drawing.selectLayer(index);
                refreshLayerPanel();
            });
            layerButton.setSelected(index==drawing.activeLayer());layerButton.setBackgroundResource(R.drawable.tool_button_background);layerButton.setBackgroundTintList(null);
            layerButton.setContentDescription("Capa "+(index+1)+(index==drawing.activeLayer()?", activa":"")+(drawing.layerVisible(index)?", visible":", oculta"));
            if(android.os.Build.VERSION.SDK_INT>=30)layerButton.setStateDescription(index==drawing.activeLayer()?"Activa":"");
            item.setOnClickListener(view -> {
                drawing.selectLayer(index);
                refreshLayerPanel();
            });
            layerItems.addView(item);
        }
    }

    private void addLayer() {
        if (!drawing.addLayer()) message("Límite de capas o memoria alcanzado");
        else message("Capa creada: " + (drawing.activeLayer() + 1));
        refreshLayerPanel();
    }

    private void chooseLayer() {
        int count = drawing.layerCount();
        String[] items = new String[count];
        for (int i = 0; i < count; ++i) {
            items[i] = (i == drawing.activeLayer() ? "● " : "  ") +
                "Capa " + (i + 1) + (drawing.layerVisible(i) ? "" : " (oculta)");
        }
        new AlertDialog.Builder(this).setTitle("Capas").setItems(items,
            (dialog, index) -> {drawing.selectLayer(index); refreshLayerPanel();}).show();
    }

    private void deleteLayer() {
        if (!drawing.deleteLayer()) message("No se puede eliminar la única capa");
        refreshLayerPanel();
    }

    private void toggleLayer() {
        if (!drawing.toggleLayer()) message("No se pudo cambiar la visibilidad");
        refreshLayerPanel();
    }

    private void moveLayer(int direction) {
        if (!drawing.moveLayer(direction)) message("La capa ya está en el extremo");
        refreshLayerPanel();
    }

    private void message(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /** Decode with a bounded memory footprint, even for very large source images. */
    private Bitmap decodeImage(Uri uri) throws java.io.IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new java.io.IOException("No se puede abrir el archivo");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
            throw new java.io.IOException("Dimensiones inválidas");
        int sample = ImageDecodePolicy.sampleSize(bounds.outWidth,bounds.outHeight);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new java.io.IOException("No se puede leer el archivo");
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) throw new java.io.IOException("Imagen no compatible");
            if(decoded.getWidth()>8192||decoded.getHeight()>8192||(long)decoded.getWidth()*decoded.getHeight()>4000000){decoded.recycle();throw new java.io.IOException("Imagen demasiado grande");}
            return decoded;
        }
    }

    private void configureSelectionTransform() {
        if (!drawing.hasSelection()) {message("Crea una selección primero");return;}
        LinearLayout form = new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        SeekBar angle = new SeekBar(this), scaleX = new SeekBar(this), scaleY = new SeekBar(this);
        angle.setMax(360);angle.setProgress(180);scaleX.setMax(390);scaleX.setProgress(90);scaleY.setMax(390);scaleY.setProgress(90);
        TextView label = text("Rotación: 0°  |  Escala X/Y: 100 % / 100 %");form.addView(label);
        form.addView(text("Rotación"));form.addView(angle);form.addView(text("Escala horizontal"));form.addView(scaleX);form.addView(text("Escala vertical"));form.addView(scaleY);
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean u){label.setText("Rotación: " + (angle.getProgress()-180) + "°  |  X/Y: " + (scaleX.getProgress()+10) + " % / " + (scaleY.getProgress()+10) + " %");}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        };angle.setOnSeekBarChangeListener(listener);scaleX.setOnSeekBarChangeListener(listener);scaleY.setOnSeekBarChangeListener(listener);
        android.widget.CheckBox flipX = new android.widget.CheckBox(this), flipY = new android.widget.CheckBox(this);
        flipX.setText("Reflejar horizontalmente");flipY.setText("Reflejar verticalmente");form.addView(flipX);form.addView(flipY);
        form.addView(text("Gira alrededor del centro de la selección. El contenido exterior al lienzo se recorta."));
        new AlertDialog.Builder(this).setTitle("Transformar selección").setView(scrollForm(form)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Aplicar",(d,w)->{if(!drawing.transformSelection(angle.getProgress()-180,(scaleX.getProgress()+10)/100f*(flipX.isChecked()?-1:1),(scaleY.getProgress()+10)/100f*(flipY.isChecked()?-1:1)))message("La transformación no cambia el contenido de la selección");}).show();
    }

    private void configureEffect(int kind) {
        String[] names = {"Desenfoque de caja", "Enfocar", "Detectar bordes", "Repujado", "Pixelar", "Ruido", "Viñeta"};
        LinearLayout form = new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        int max = kind == 0 || kind == 4 ? 64 : 100;
        SeekBar amount = new SeekBar(this);amount.setMax(max-1);amount.setProgress(kind == 0 ? 2 : kind == 4 ? 7 : kind == 5 ? 19 : 49);
        TextView label = text("Valor: " + (amount.getProgress()+1));form.addView(label);form.addView(amount);
        form.addView(text("Se aplica a toda la capa activa. Puedes deshacer el resultado."));
        amount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean u){label.setText("Valor: " + (n+1));}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        new AlertDialog.Builder(this).setTitle(names[kind]).setView(scrollForm(form)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Aplicar",(d,w) -> runEffect(kind,amount.getProgress()+1)).show();
    }

    private void runEffect(int kind,int amount) {runColorOperation(() -> drawing.applyEffect(kind,amount));}
    interface ColorOperation { boolean getAsBoolean(); }
    void runColorOperation(ColorOperation operation) {
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        if(!drawing.prepareEffectSelection()){message("No se pudo preparar la selección");return;}
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Aplicando efecto…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(() -> {
            boolean changed = operation.getAsBoolean();
            runOnUiThread(() -> {if (!isDestroyed()) {progress.dismiss();projectProgress=null;drawing.setEnabled(true);
                if (changed) drawing.effectApplied();else message("La capa no tiene cambios para este efecto");}
            });
        },"velyntora-effect").start();
    }

    private void configureText(int x,int y) {
        LinearLayout form = new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        android.widget.EditText input = new android.widget.EditText(this);input.setHint("Escribe tu texto");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setMinLines(3);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4096)});form.addView(input);
        TextView sizeLabel = text("Tamaño: 32 px");form.addView(sizeLabel);
        SeekBar size = new SeekBar(this);size.setMax(252);size.setProgress(28);form.addView(size);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean u){sizeLabel.setText("Tamaño: " + (n+4) + " px");}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        android.widget.CheckBox bold = new android.widget.CheckBox(this), italic = new android.widget.CheckBox(this);
        bold.setText("Negrita");italic.setText("Cursiva");form.addView(bold);form.addView(italic);
        android.widget.Spinner font = new android.widget.Spinner(this);
        String[] families = {"sans-serif", "serif", "monospace"};
        font.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,families));form.addView(font);
        form.addView(text("Se inserta en la capa activa. Puedes deshacer, seleccionar y mover el resultado."));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Texto en " + x + ", " + y).setView(scrollForm(form))
            .setNegativeButton("Cancelar",null).setPositiveButton("Insertar",null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (drawing.insertText(input.getText().toString(),x,y,size.getProgress()+4,bold.isChecked(),italic.isChecked(),families[font.getSelectedItemPosition()])) dialog.dismiss();
            else message("Introduce texto visible dentro del lienzo");
        }));dialog.show();
    }

    private void configureBrush() {
        LinearLayout form = new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        SeekBar opacity = new SeekBar(this), hardness = new SeekBar(this);
        opacity.setContentDescription("Opacidad de la capa activa");
        opacity.setMax(100);opacity.setProgress(Math.round(drawing.brushOpacity()*100));hardness.setMax(100);hardness.setProgress(Math.round(drawing.brushHardness()*100));
        TextView opacityLabel = text("Opacidad: " + opacity.getProgress() + " %"), hardnessLabel = text("Dureza: " + hardness.getProgress() + " %");
        form.addView(opacityLabel);form.addView(opacity);form.addView(hardnessLabel);form.addView(hardness);
        android.widget.CheckBox square = new android.widget.CheckBox(this), pressure = new android.widget.CheckBox(this);
        square.setText("Punta cuadrada");square.setChecked(drawing.squareBrush());pressure.setText("Presión del lápiz: variar radio");pressure.setChecked(drawing.pressureBrush());form.addView(square);form.addView(pressure);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int n,boolean u){opacityLabel.setText("Opacidad: " + n + " %");}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        hardness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int n,boolean u){hardnessLabel.setText("Dureza: " + n + " %");}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        new AlertDialog.Builder(this).setTitle("Pincel y borrador").setView(scrollForm(form)).setNegativeButton("Cancelar",null)
            .setPositiveButton("Aplicar",(d,w)->drawing.configureBrush(opacity.getProgress()/100f,hardness.getProgress()/100f,square.isChecked(),pressure.isChecked())).show();
    }

    private void resizeDocumentAsync(int w,int h,boolean scale,boolean bilinear,int anchor){
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage("Cambiando tamaño…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(()->{boolean ok=drawing.resizeDocumentPixels(w,h,scale,bilinear,anchor);runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();projectProgress=null;drawing.setEnabled(true);if(ok)drawing.documentResized();else message("Sin cambios o tamaño demasiado grande para estas capas");}});},"velyntora-resize").start();
    }

    private android.text.TextWatcher dimensionWatcher(Runnable change){
        return new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(android.text.Editable value){change.run();}};
    }

    private void configureDimensions(int mode) {
        LinearLayout form = new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        android.widget.EditText width = new android.widget.EditText(this), height = new android.widget.EditText(this);
        width.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);height.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        width.setText(Integer.toString(drawing.documentWidth()));height.setText(Integer.toString(drawing.documentHeight()));
        form.addView(text("Ancho (px)"));form.addView(width);form.addView(text("Alto (px)"));form.addView(height);
        final int originalWidth=drawing.documentWidth(),originalHeight=drawing.documentHeight();
        final int[] ratio={originalWidth,originalHeight},lastEdited={0};final boolean[] updating={false};
        android.widget.CheckBox lockRatio=new android.widget.CheckBox(this);lockRatio.setText("Mantener proporciones");lockRatio.setChecked(mode==1);
        android.widget.EditText percent=new android.widget.EditText(this);percent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);percent.setText("100");percent.setContentDescription("Escala uniforme en porcentaje");
        if(mode==1){form.addView(lockRatio);form.addView(text("Escala uniforme (%)"));form.addView(percent);}
        Runnable synchronize=()->{
            if(updating[0]||mode!=1)return;
            updating[0]=true;
            try{
                DocumentDimensions dimensions;
                if(lastEdited[0]==2){dimensions=DocumentDimensions.fromPercent(originalWidth,originalHeight,Double.parseDouble(percent.getText().toString().replace(',','.')));ratio[0]=originalWidth;ratio[1]=originalHeight;}
                else if(lockRatio.isChecked())dimensions=lastEdited[0]==0?DocumentDimensions.fromWidth(ratio[0],ratio[1],Integer.parseInt(width.getText().toString())):DocumentDimensions.fromHeight(ratio[0],ratio[1],Integer.parseInt(height.getText().toString()));
                else{percent.setText("");return;}
                if(lastEdited[0]!=0)width.setText(Integer.toString(dimensions.width));
                if(lastEdited[0]!=1)height.setText(Integer.toString(dimensions.height));
                width.setError(null);height.setError(null);percent.setError(null);
                if(lastEdited[0]!=2)percent.setText("");
            }catch(IllegalArgumentException ignored){/* Keep partial input editable; validate when applying. */}
            finally{updating[0]=false;}
        };
        width.addTextChangedListener(dimensionWatcher(()->{if(!updating[0]){lastEdited[0]=0;synchronize.run();}}));
        height.addTextChangedListener(dimensionWatcher(()->{if(!updating[0]){lastEdited[0]=1;synchronize.run();}}));
        percent.addTextChangedListener(dimensionWatcher(()->{if(!updating[0]){lastEdited[0]=2;synchronize.run();}}));
        lockRatio.setOnCheckedChangeListener((button,checked)->{if(checked)try{DocumentDimensions dimensions=new DocumentDimensions(Integer.parseInt(width.getText().toString()),Integer.parseInt(height.getText().toString()));ratio[0]=dimensions.width;ratio[1]=dimensions.height;}catch(IllegalArgumentException ignored){} });
        if(mode==0){
            android.widget.Spinner presets=new android.widget.Spinner(this);presets.setContentDescription("Tamaños rápidos de documento");
            presets.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Personalizado","Cuadrado · 512 × 512","Cuadrado · 1024 × 1024","Horizontal · 1920 × 1080","Vertical · 1080 × 1920","Panorama · 2400 × 1000"}));
            final int[][] sizes={{originalWidth,originalHeight},{512,512},{1024,1024},{1920,1080},{1080,1920},{2400,1000}};
            presets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> parent){}public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){if(position>0){width.setText(Integer.toString(sizes[position][0]));height.setText(Integer.toString(sizes[position][1]));}}});form.addView(text("Tamaños rápidos"));form.addView(presets);
        }
        android.widget.Spinner method=new android.widget.Spinner(this),anchor=new android.widget.Spinner(this);
        method.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Píxel cercano (pixel art)","Bilineal (suave)"}));method.setSelection(1);method.setContentDescription("Método de remuestreo");
        anchor.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Arriba izquierda","Arriba centro","Arriba derecha","Centro izquierda","Centro","Centro derecha","Abajo izquierda","Abajo centro","Abajo derecha"}));anchor.setSelection(4);anchor.setContentDescription("Anclaje del contenido");
        if(mode==1){form.addView(text("Remuestreo"));form.addView(method);}
        if(mode==2){form.addView(text("Anclaje del contenido"));form.addView(anchor);}
        width.setContentDescription("Ancho en píxeles");height.setContentDescription("Alto en píxeles");
        form.addView(text(mode == 2 ? "El anclaje determina dónde se conserva el contenido al ampliar o recortar. El área nueva es transparente."
            : mode == 1 ? "Todas las capas conservan sus propiedades. Bilineal interpola color y transparencia; píxel cercano mantiene bordes duros." : "Se crea un documento nuevo. Guarda primero el dibujo actual."));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(mode == 0 ? "Nuevo documento" : mode == 1 ? "Tamaño de imagen" : "Tamaño de lienzo")
            .setView(scrollForm(form)).setNegativeButton("Cancelar", null).setPositiveButton("Aplicar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                DocumentDimensions dimensions;
                if(mode==1&&lastEdited[0]==2)dimensions=DocumentDimensions.fromPercent(originalWidth,originalHeight,Double.parseDouble(percent.getText().toString().replace(',','.')));
                else if(mode==1&&lockRatio.isChecked())dimensions=lastEdited[0]==1?DocumentDimensions.fromHeight(ratio[0],ratio[1],Integer.parseInt(height.getText().toString())):DocumentDimensions.fromWidth(ratio[0],ratio[1],Integer.parseInt(width.getText().toString()));
                else dimensions=new DocumentDimensions(Integer.parseInt(width.getText().toString()),Integer.parseInt(height.getText().toString()));
                int w=dimensions.width,h=dimensions.height;
                if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
                if(mode==0){if(!drawing.newDocument(w,h)){message("No se pudo crear el documento");return;}dialog.dismiss();}
                else{dialog.dismiss();resizeDocumentAsync(w,h,mode==1,method.getSelectedItemPosition()==1,anchor.getSelectedItemPosition());}
            } catch (IllegalArgumentException e) { message(e instanceof NumberFormatException?"Introduce dimensiones y porcentaje válidos":e.getMessage()); }
        }));dialog.show();
    }

    private void saveImage(String mime, String name, int request) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);intent.putExtra(Intent.EXTRA_TITLE, name);startActivityForResult(intent, request);
    }

    private void projectPicker(boolean save) {
        Intent intent = new Intent(save ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        if (save) intent.putExtra(Intent.EXTRA_TITLE, "dibujo.vlycore");
        startActivityForResult(intent, save ? SAVE_PROJECT : OPEN_PROJECT);
    }

    private void openRasterPicker(boolean save){Intent intent=new Intent(save?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(save?"image/openraster":"*/*");if(save)intent.putExtra(Intent.EXTRA_TITLE,"dibujo.ora");startActivityForResult(intent,save?SAVE_ORA:OPEN_ORA);}
    private void transferOpenRaster(Uri uri,boolean save){
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage(save?"Guardando OpenRaster…":"Abriendo OpenRaster…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(()->{boolean success=false;try{OpenRasterProjects.transfer(this,drawing,uri,save);success=true;}catch(Exception e){success=false;}final boolean ok=success;runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();projectProgress=null;drawing.setEnabled(true);if(ok&&!save)drawing.projectOpened();message(ok?"Proyecto OpenRaster listo": "OpenRaster incompatible, incompleto o demasiado grande; se admiten capas normales sin grupos");}});},"velyntora-openraster").start();
    }

    private void transferProject(Uri uri, boolean save) {
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage(save ? "Guardando proyecto…" : "Abriendo proyecto…");
        progress.setCancelable(false);
        projectProgress = progress;
        progress.show();
        drawing.setEnabled(false);
        new Thread(() -> {
            boolean success = false;
            try (android.os.ParcelFileDescriptor file = getContentResolver().openFileDescriptor(uri, save ? "wt" : "r")) {
                if (file != null) success = save ? drawing.writeProject(file.getFd()) : drawing.readProject(file.getFd());
            } catch (Exception e) { success = false; }
            final boolean ok = success;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    progress.dismiss();projectProgress = null;drawing.setEnabled(true);
                    if (ok && !save) drawing.projectOpened();
                    message(ok ? (save ? "Proyecto guardado con capas" : "Proyecto abierto con capas")
                        : (save ? "No se pudo guardar el proyecto" : "Proyecto incompatible, incompleto o demasiado grande"));
                }
            });
        }, "velyntora-project-io").start();
    }

    private void exportRaster(Uri uri,int request) {
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        Bitmap image=drawing.snapshot();
        android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage("Exportando imagen…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(()->{
            boolean success=false;
            try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){
                if(output==null)throw new java.io.IOException("No output");
                java.io.BufferedOutputStream buffered=new java.io.BufferedOutputStream(output);
                RasterFileWriter.RowSource rows=(y,row)->image.getPixels(row,0,image.getWidth(),0,y,image.getWidth(),1);
                if(request==SAVE_TIFF)TiffWriter.write(buffered,image.getWidth(),image.getHeight(),rows);
                else if(request==SAVE_GIF)GifWriter.write(buffered,image.getWidth(),image.getHeight(),rows);
                else if(request==SAVE_PPM)PpmCodec.write(buffered,image.getWidth(),image.getHeight(),rows);
                else if(request==SAVE_ICO){
                    double scale=Math.min(1.0,256.0/Math.max(image.getWidth(),image.getHeight()));
                    int w=Math.max(1,(int)Math.round(image.getWidth()*scale)),h=Math.max(1,(int)Math.round(image.getHeight()*scale));
                    Bitmap icon=Bitmap.createScaledBitmap(image,w,h,true);
                    try{IcoCodec.write(buffered,w,h,(y,row)->icon.getPixels(row,0,w,0,y,w,1));}finally{if(icon!=image)icon.recycle();}
                }
                else if(request==SAVE_BMP||request==SAVE_TGA)RasterFileWriter.write(buffered,image.getWidth(),image.getHeight(),request==SAVE_TGA,rows);
                else {
                    Bitmap encoded=image;
                    try {
                        if(request==SAVE_JPEG){encoded=Bitmap.createBitmap(image.getWidth(),image.getHeight(),Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(encoded);canvas.drawColor(Color.WHITE);canvas.drawBitmap(image,0,0,null);}
                        Bitmap.CompressFormat format=request==SAVE_JPEG?Bitmap.CompressFormat.JPEG:request==SAVE_WEBP?(android.os.Build.VERSION.SDK_INT>=30?Bitmap.CompressFormat.WEBP_LOSSLESS:Bitmap.CompressFormat.WEBP):Bitmap.CompressFormat.PNG;
                        if(!encoded.compress(format,100,buffered))throw new java.io.IOException("No se pudo exportar la imagen");
                    }finally{if(encoded!=image)encoded.recycle();}
                }
                buffered.flush();success=true;
            }catch(Exception e){success=false;}finally{image.recycle();}
            final boolean ok=success;runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();projectProgress=null;drawing.setEnabled(true);message(ok?"Imagen guardada":"Error al guardar la imagen");}});
        },"velyntora-raster-export").start();
    }

    private void openTga(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,OPEN_TGA);}
    private void openTiff(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,OPEN_TIFF);}
    private void openIco(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,OPEN_ICO);}
    private void openPpm(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,OPEN_PPM);}
    private void importTga(Uri uri){importRaster(uri,OPEN_TGA);}
    private void importRaster(Uri uri,int request){
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        final String format=request==OPEN_TIFF?"TIFF":request==OPEN_ICO?"ICO":request==OPEN_PPM?"PPM":"TGA";
        android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage("Abriendo "+format+"…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(()->{int[] pixels=null;int width=0,height=0;try(InputStream input=getContentResolver().openInputStream(uri)){
                if(input==null)throw new java.io.IOException("Sin archivo");
                if(request==OPEN_TIFF){TiffReader.Image image=TiffReader.read(new java.io.BufferedInputStream(input));pixels=image.pixels;width=image.width;height=image.height;}
                else if(request==OPEN_PPM){PpmCodec.Image image=PpmCodec.read(new java.io.BufferedInputStream(input));pixels=image.pixels;width=image.width;height=image.height;}
                else if(request==OPEN_ICO){IcoCodec.Image image=IcoCodec.read(new java.io.BufferedInputStream(input),png->{
                    Bitmap bitmap=BitmapFactory.decodeByteArray(png,0,png.length);
                    if(bitmap==null)throw new java.io.IOException("PNG ICO inválido");
                    try{int w=bitmap.getWidth(),h=bitmap.getHeight();if(w>256||h>256)throw new java.io.IOException("ICO demasiado grande");int[] p=new int[w*h];bitmap.getPixels(p,0,w,0,0,w,h);return new IcoCodec.Image(w,h,p);}finally{bitmap.recycle();}
                });pixels=image.pixels;width=image.width;height=image.height;}
                else{TgaReader.Image image=TgaReader.read(new java.io.BufferedInputStream(input));pixels=image.pixels;width=image.width;height=image.height;}
            }catch(Exception e){pixels=null;}
            final int[] decoded=pixels;final int w=width,h=height;
            runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();projectProgress=null;drawing.setEnabled(true);if(decoded==null){message(format+" incompatible, incompleto o demasiado grande");return;}Bitmap bitmap=Bitmap.createBitmap(decoded,w,h,Bitmap.Config.ARGB_8888);try{if(!drawing.loadBitmap(bitmap))message("No se pudo abrir el "+format);}finally{bitmap.recycle();}}});
        },"velyntora-raster-import").start();
    }

    private void savePng() { saveImage("image/png", "dibujo.png", SAVE_PNG); }

    private void importImage(Uri uri){
        if(projectProgress!=null){message("Espera a que termine la operación actual");return;}
        android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage("Abriendo imagen…");progress.setCancelable(false);projectProgress=progress;progress.show();drawing.setEnabled(false);
        new Thread(()->{Bitmap decoded=null;try{decoded=decodeImage(uri);}catch(Exception e){decoded=null;}final Bitmap image=decoded;
            runOnUiThread(()->{try{if(!isDestroyed()){progress.dismiss();projectProgress=null;drawing.setEnabled(true);if(image==null||!drawing.loadBitmap(image))message("No se pudo abrir la imagen");}}finally{if(image!=null)image.recycle();}});
        },"velyntora-image-import").start();
    }

    private void openImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, OPEN_IMAGE);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == SAVE_ORA || request == OPEN_ORA) {
            transferOpenRaster(uri,request==SAVE_ORA);
        } else if (request == SAVE_PROJECT || request == OPEN_PROJECT) {
            transferProject(uri, request == SAVE_PROJECT);
        } else if (request == OPEN_TIFF) {
            importRaster(uri,OPEN_TIFF);
        } else if (request == OPEN_PPM) {
            importRaster(uri,OPEN_PPM);
        } else if (request == OPEN_ICO) {
            importRaster(uri,OPEN_ICO);
        } else if (request == OPEN_TGA) {
            importTga(uri);
        } else if (request == OPEN_IMAGE) {
            importImage(uri);
        } else if (request == SAVE_BMP || request == SAVE_TGA || request == SAVE_TIFF || request == SAVE_GIF || request == SAVE_ICO || request == SAVE_PPM || request == SAVE_PNG || request == SAVE_JPEG || request == SAVE_WEBP) {
            exportRaster(uri,request);
        }
    }
}
