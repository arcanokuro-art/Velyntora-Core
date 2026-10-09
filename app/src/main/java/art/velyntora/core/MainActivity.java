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
    private static final int SAVE_PNG = 41;
    private static final int OPEN_IMAGE = 42;
    private DrawingView drawing;
    private TextView status;
    private TextView selectedTool;
    private LinearLayout layerItems;
    private final java.util.ArrayList<Bitmap> thumbnails = new java.util.ArrayList<>();
    private android.os.Handler layerRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private SeekBar layerOpacity;
    private int activeColor = Color.BLACK;
    private Bitmap selectionClipboard;

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
            .setView(form).setNegativeButton("Cancelar", null)
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
        drawing.setOnColorPickedListener(color -> { activeColor = color; message("Color seleccionado"); });
        drawing.setOnCanvasChangedListener(() -> {
            layerRefreshHandler.removeCallbacks(layerRefreshTask);
            layerRefreshHandler.postDelayed(layerRefreshTask, 120);
        });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F1F1);

        LinearLayout menus = row();
        menu(menus, "Archivo", new String[]{"Nuevo", "Abrir imagen", "Guardar PNG"},
            new Runnable[]{drawing::clear, this::openImage, this::savePng});
        menu(menus, "Editar", new String[]{"Deshacer", "Rehacer", "Copiar selección", "Cortar selección", "Pegar selección", "Mover contenido…", "Borrar selección", "Deseleccionar"},
            new Runnable[]{this::undo, this::redo, this::copySelection, this::cutSelection, this::pasteSelection, this::moveSelectedContent, () -> {if(!drawing.eraseSelection())message("No hay selección válida");}, drawing::deselect});
        menu(menus, "Ver", new String[]{"Ajustar al lienzo"},
            new Runnable[]{drawing::invalidate});
        menu(menus, "Imagen", new String[]{"Nuevo lienzo"},
            new Runnable[]{drawing::clear});
        menu(menus, "Capas", new String[]{"Añadir capa", "Seleccionar capa", "Eliminar capa", "Mostrar / ocultar", "Subir capa", "Bajar capa"},
            new Runnable[]{this::addLayer, this::chooseLayer, this::deleteLayer, this::toggleLayer,
                () -> moveLayer(1), () -> moveLayer(-1)});
        menu(menus, "Ajustes", new String[]{"Información"},
            new Runnable[]{() -> message("Los ajustes avanzados están en desarrollo.")});
        menu(menus, "Efectos", new String[]{"Información"},
            new Runnable[]{() -> message("Los efectos están en desarrollo.")});
        menu(menus, "Ayuda", new String[]{"Acerca de"},
            new Runnable[]{() -> message("Velyntora Core 0.1 — versión de desarrollo Android")});
        addScrollable(root, menus);

        LinearLayout commands = row();
        button(commands, "Nuevo", drawing::clear);
        button(commands, "Abrir", this::openImage);
        button(commands, "Guardar", this::savePng);
        button(commands, "↶", this::undo);
        button(commands, "↷", this::redo);
        addScrollable(root, commands);

        LinearLayout workspace = row();
        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(0xFFE4E4E4);
        sidebar.setPadding(dp(3), dp(4), dp(3), dp(4));
        TextView toolsTitle = text("HERRAMIENTAS");
        sidebar.addView(toolsTitle);
        tool(sidebar, "Pincel", DrawingView.BRUSH);
        tool(sidebar, "Línea", DrawingView.LINE);
        tool(sidebar, "Rectángulo", DrawingView.RECTANGLE);
        tool(sidebar, "Elipse", DrawingView.ELLIPSE);
        tool(sidebar, "Rectángulo relleno", DrawingView.FILLED_RECTANGLE);
        tool(sidebar, "Elipse rellena", DrawingView.FILLED_ELLIPSE);
        tool(sidebar, "Cubeta", DrawingView.BUCKET);
        tool(sidebar, "Cuentagotas", DrawingView.PICKER);
        tool(sidebar, "Borrador", DrawingView.ERASER);
        tool(sidebar, "Selección rectangular", DrawingView.SELECT_RECTANGLE);
        tool(sidebar, "Selección elíptica", DrawingView.SELECT_ELLIPSE);
        tool(sidebar, "Mover contorno", DrawingView.MOVE_SELECTION);
        TextView brushSizeLabel = text("Tamaño: 4 px");
        sidebar.addView(brushSizeLabel);
        SeekBar brushSize = new SeekBar(this);
        brushSize.setMax(127);
        brushSize.setProgress(3);
        brushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int size = progress + 1;
                brushSizeLabel.setText("Tamaño: " + size + " px");
                drawing.setBrushRadius(size);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        sidebar.addView(brushSize);
        selectedTool = text("Pincel");
        sidebar.addView(selectedTool);
        workspace.addView(sidebar, new LinearLayout.LayoutParams(dp(116), -1));
        workspace.addView(drawing, new LinearLayout.LayoutParams(0, -1, 1));
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
        workspace.addView(layerScroll, new LinearLayout.LayoutParams(dp(152), -1));
        this.layerItems = layerItems;
        refreshLayerPanel();
        root.addView(workspace, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout colors = row();
        TextView paletteLabel = text("COLORES  ");
        colors.addView(paletteLabel);
        int[] palette = {Color.BLACK, Color.WHITE, Color.GRAY, Color.RED, 0xFFFF9800,
            Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA,
            0xFF795548, 0xFF9C27B0};
        for (int color : palette) {
            View swatch = new View(this);
            swatch.setBackgroundColor(color);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(30), dp(30));
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            colors.addView(swatch, params);
            swatch.setOnClickListener(v -> {
                activeColor = color;
                drawing.setColor(color);
            });
        }
        addScrollable(root, colors);
        status = text("800 × 800 px  |  Zoom: ajustar  |  No guardado");
        status.setPadding(dp(10), dp(4), dp(10), dp(4));
        root.addView(status);
        setContentView(root);
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

    private void addScrollable(LinearLayout root, LinearLayout content) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(content);
        root.addView(scroll);
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setOnClickListener(v -> action.run());
        parent.addView(button);
    }

    private void tool(LinearLayout parent, String label, int tool) {
        Button iconButton = new Button(this);
        iconButton.setText(label);
        iconButton.setTextSize(12);
        iconButton.setAllCaps(false);
        iconButton.setMinWidth(0);
        iconButton.setMinimumWidth(0);
        int icon = 0;
        switch (tool) {
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
        }
        if (icon != 0) {
            iconButton.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
            iconButton.setCompoundDrawablePadding(dp(4));
        }
        iconButton.setOnClickListener(v -> {
            drawing.setTool(tool);
            drawing.setColor(activeColor);
            selectedTool.setText(label);
        });
        parent.addView(iconButton);
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
            String label = (index == drawing.activeLayer() ? "● " : "○ ") +
                (drawing.layerVisible(index) ? "▣ " : "□ ") + "Capa " + (index + 1);
            LinearLayout item = row();
            Bitmap preview = drawing.layerThumbnail(index);
            if (preview != null) {
                thumbnails.add(preview);
                ImageView thumbnail = new ImageView(this);
                thumbnail.setImageBitmap(preview);
                thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                item.addView(thumbnail, new LinearLayout.LayoutParams(dp(48), dp(48)));
            }
            button(item, label, () -> {
                drawing.selectLayer(index);
                refreshLayerPanel();
            });
            item.setOnClickListener(view -> {
                drawing.selectLayer(index);
                refreshLayerPanel();
            });
            layerItems.addView(item);
        }
    }

    private void addLayer() {
        if (!drawing.addLayer()) message("Límite de 32 capas alcanzado");
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
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 2048
                && sample < 1024) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new java.io.IOException("No se puede leer el archivo");
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) throw new java.io.IOException("Imagen no compatible");
            return decoded;
        }
    }

    private void savePng() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, "dibujo.png");
        startActivityForResult(intent, SAVE_PNG);
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
        if (request == OPEN_IMAGE) {
            try {
                Bitmap bitmap = decodeImage(uri);
                try {
                    drawing.loadBitmap(bitmap);
                    status.setText("800 × 800 px  |  Imagen importada");
                } finally {
                    bitmap.recycle();
                }
            } catch (Exception e) {
                message("No se pudo abrir la imagen");
            }
        } else if (request == SAVE_PNG) {
            Bitmap image = drawing.snapshot();
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null || !image.compress(Bitmap.CompressFormat.PNG, 100, out))
                    throw new IllegalStateException("No se pudo guardar PNG");
                out.flush();
                status.setText("800 × 800 px  |  PNG guardado");
                message("PNG guardado");
            } catch (Exception e) {
                message("Error al guardar PNG");
            } finally {
                image.recycle();
            }
        }
    }
}
