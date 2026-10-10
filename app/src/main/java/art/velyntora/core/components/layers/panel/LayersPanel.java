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


  private Button delete,duplicate,merge,up,down;
  private int lastTapIndex=-1;
  private long lastTapTime;
  void refreshLayerPanel() {
    if(host.readLayerItems()==null)return;
    host.readLayerItems().removeAllViews();for(Bitmap old:host.readThumbnails())old.recycle();host.readThumbnails().clear();
    int active=host.readDrawing().activeLayer(),count=host.readDrawing().layerCount();
    for(int i=count-1;i>=0;i--){final int index=i;LinearLayout row=host.row();row.setPadding(host.dp(6),host.dp(4),host.dp(4),host.dp(4));row.setSelected(i==active);row.setContentDescription(host.readDrawing().layerName(i)+(i==active?", activa":""));row.setBackgroundColor(i==active?0xFF397753:0xFF292929);
      ImageView thumb=new ImageView(host);Bitmap preview=host.readDrawing().layerThumbnail(i);if(preview!=null){host.readThumbnails().add(preview);thumb.setImageBitmap(preview);}thumb.setBackground(new LayerCheckerDrawable());thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(thumb,new LinearLayout.LayoutParams(host.dp(40),host.dp(40)));
      TextView label=host.text(host.readDrawing().layerName(i));label.setPadding(host.dp(8),0,host.dp(4),0);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
      android.widget.CheckBox visible=new android.widget.CheckBox(host);visible.setChecked(host.readDrawing().layerVisible(i));visible.setContentDescription("Visible: "+host.readDrawing().layerName(i));visible.setOnClickListener(v->{if(host.readProjectProgress()!=null)return;host.readDrawing().applyLayerProperties(index,host.readDrawing().layerName(index),visible.isChecked(),index==host.readDrawing().activeLayer()?host.readDrawing().layerOpacity():layerOpacity(index),host.readDrawing().layerBlendMode(index));host.refreshLayerPanel();});row.addView(visible,new LinearLayout.LayoutParams(host.dp(40),host.dp(48)));
      row.setOnClickListener(v->{if(host.readProjectProgress()==null){long now=android.os.SystemClock.uptimeMillis();boolean twice=lastTapIndex==index&&now-lastTapTime<350;lastTapTime=now;lastTapIndex=index;host.readDrawing().selectLayer(index);host.refreshLayerPanel();if(twice){lastTapIndex=-1;LayerPropertiesDialog.show(host,index);}}});row.setOnLongClickListener(v->{if(host.readProjectProgress()==null){host.readDrawing().selectLayer(index);host.refreshLayerPanel();LayerPropertiesDialog.show(host,index);}return true;});host.readLayerItems().addView(row,new LinearLayout.LayoutParams(-1,host.dp(56)));
    }
    if(delete!=null){delete.setEnabled(count>1);duplicate.setEnabled(count<32);merge.setEnabled(active>0);up.setEnabled(active<count-1);down.setEnabled(active>0);for(Button control:new Button[]{delete,duplicate,merge,up,down})control.setAlpha(control.isEnabled()?1f:.35f);}
  }
  private float layerOpacity(int index){return DrawingView.nativeIndexedLayerOpacity(index);}

  void addLayer() {
    if (!host.readDrawing().addLayer()) host.message("Límite de capas o memoria alcanzado");
    else host.message("Capa creada: " + (host.readDrawing().activeLayer() + 1));
    host.refreshLayerPanel();
  }

  void renameLayer(int index) {host.readDrawing().selectLayer(index);LayerPropertiesDialog.show(host,index);}

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
    LinearLayout panel=new LinearLayout(host);panel.setOrientation(1);panel.setBackgroundColor(0xFF202020);TextView title=host.text("CAPAS");title.setPadding(host.dp(8),host.dp(8),host.dp(8),host.dp(8));panel.addView(title);
    LinearLayout items=new LinearLayout(host);items.setOrientation(1);ScrollView list=new ScrollView(host);list.addView(items,new ScrollView.LayoutParams(-1,-2));panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));host.writeLayerItems(items);
    LinearLayout commands=host.row();panel.addView(commands,new LinearLayout.LayoutParams(-1,host.dp(48)));
    command(commands,"Añadir una capa nueva","new",host::addLayer);delete=command(commands,"Eliminar capa","delete",host::deleteLayer);duplicate=command(commands,"Duplicar la capa","duplicate",()->action(0));merge=command(commands,"Combinar con la capa inferior","merge",()->action(1));up=command(commands,"Subir capa","up",()->host.moveLayer(1));down=command(commands,"Bajar capa","down",()->host.moveLayer(-1));

    LayerPanelContainer container=new LayerPanelContainer(host);container.addView(panel,new ScrollView.LayoutParams(-1,-1));host.writeLayerScroll(container);
  }
  private Button command(LinearLayout row,String label,String glyph,Runnable action){Button b=host.iconButton(row,label,glyph,action);b.setPadding(host.dp(4),host.dp(4),host.dp(4),host.dp(4));b.setLayoutParams(new LinearLayout.LayoutParams(0,host.dp(48),1));return b;}
  private void action(int action){if(!host.readDrawing().layerAction(action))host.message("No se pudo completar la operación de capa");host.refreshLayerPanel();}
}
