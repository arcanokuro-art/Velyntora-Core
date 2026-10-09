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
    private int activeColor = Color.BLACK;

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        drawing = new DrawingView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F1F1);

        LinearLayout menus = row();
        menu(menus, "Archivo", new String[]{"Nuevo", "Abrir imagen", "Guardar PNG"},
            new Runnable[]{drawing::clear, this::openImage, this::savePng});
        menu(menus, "Editar", new String[]{"Deshacer", "Rehacer"},
            new Runnable[]{drawing::undo, drawing::redo});
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
        button(commands, "↶", drawing::undo);
        button(commands, "↷", drawing::redo);
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
        tool(sidebar, "Cubeta", DrawingView.BUCKET);
        button(sidebar, "Borrador", () -> {
            drawing.setTool(DrawingView.BRUSH);
            drawing.setColor(Color.WHITE);
            selectedTool.setText("Borrador");
        });
        selectedTool = text("Pincel");
        sidebar.addView(selectedTool);
        workspace.addView(sidebar, new LinearLayout.LayoutParams(dp(116), -1));
        workspace.addView(drawing, new LinearLayout.LayoutParams(0, -1, 1));
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
        button(parent, label, () -> {
            drawing.setTool(tool);
            drawing.setColor(activeColor);
            selectedTool.setText(label);
        });
    }

    private void menu(LinearLayout parent, String title, String[] labels, Runnable[] actions) {
        button(parent, title, () -> new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels, (dialog, index) -> actions[index].run())
            .show());
    }

    private void addLayer() {
        if (!drawing.addLayer()) message("Límite de 32 capas alcanzado");
        else message("Capa creada: " + (drawing.activeLayer() + 1));
    }

    private void chooseLayer() {
        int count = drawing.layerCount();
        String[] items = new String[count];
        for (int i = 0; i < count; ++i) {
            items[i] = (i == drawing.activeLayer() ? "● " : "  ") +
                "Capa " + (i + 1) + (drawing.layerVisible(i) ? "" : " (oculta)");
        }
        new AlertDialog.Builder(this).setTitle("Capas").setItems(items,
            (dialog, index) -> drawing.selectLayer(index)).show();
    }

    private void deleteLayer() {
        if (!drawing.deleteLayer()) message("No se puede eliminar la única capa");
    }

    private void toggleLayer() {
        if (!drawing.toggleLayer()) message("No se pudo cambiar la visibilidad");
    }

    private void moveLayer(int direction) {
        if (!drawing.moveLayer(direction)) message("La capa ya está en el extremo");
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
