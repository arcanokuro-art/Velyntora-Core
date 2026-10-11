package art.velyntora.core;

/** Owns the selected variant; tools keep the stable BRUSH ID. */
final class BrushModule {
  static final int CLASSIC=0, ANTIALIASED=1;
  private final DrawingView view;
  private int selected;
  BrushModule(DrawingView view) {
    this.view=view;
    selected=view.getContext().getSharedPreferences("brush-variants",0).getInt("selected",CLASSIC);
    if(selected!=ANTIALIASED) selected=CLASSIC;
  }
  int selected() { return selected; }
  boolean isAntialiased() { return selected==ANTIALIASED; }
  String label() { return isAntialiased()?"Pincel suavizado":"Pincel clásico"; }
  void select(int id) {
    if(id!=CLASSIC && id!=ANTIALIASED) return;
    view.finishOpacityStroke();
    selected=id;
    view.getContext().getSharedPreferences("brush-variants",0).edit().putInt("selected",id).apply();
    view.setTool(DrawingView.BRUSH);
    view.invalidate();
  }
}
