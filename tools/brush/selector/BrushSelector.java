package art.velyntora.core;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/** Anchored, accessible dropdown. Selection survives workspace reconstruction. */
final class BrushSelector {
  static void show(View anchor,DrawingView drawing,Runnable changed) {
    PopupMenu popup=new PopupMenu(anchor.getContext(),anchor);
    Menu menu=popup.getMenu();
    menu.add(1,BrushModule.CLASSIC,0,"Pincel clásico");
    menu.add(1,BrushModule.ANTIALIASED,1,"Pincel con bordes suavizados");
    menu.setGroupCheckable(1,true,true);
    menu.findItem(drawing.brushes().selected()).setChecked(true);
    popup.setOnMenuItemClickListener(item->{
      drawing.brushes().select(item.getItemId());changed.run();return true;
    });
    popup.show();
  }
}
