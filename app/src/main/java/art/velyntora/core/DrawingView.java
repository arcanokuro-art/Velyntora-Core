package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public final class DrawingView extends View {
  static {
    System.loadLibrary("velyntora_jni");
  }

  static native boolean nativeTransformSelection(
      int left, int top, int width, int height, byte[] mask, float degrees, float sx, float sy);

  private static native boolean nativeEffect(int kind, int amount);

  private static native boolean nativeUtility(int kind, int amount, int size, int parameter);

  static native boolean nativeObject(
      int kind, int amount, int tolerance, int color, boolean option);

  public boolean utility(int kind, int amount, int size, int parameter) {
    return nativeUtility(kind, amount, size, parameter);
  }

  public boolean objectEffect(int kind, int amount, int tolerance, boolean option) {
    return nativeObject(kind, amount, tolerance, color, option);
  }

  private static native boolean nativeRender(
      int kind, int scale, int detail, int seed, int first, int second);

  public boolean render(int kind, int scale, int detail, int seed, int second) {
    return nativeRender(kind, scale, detail, seed, color, second);
  }

  private static native boolean nativeArtistic(int kind, int strength, int radius, int threshold);

  public boolean artistic(int kind, int strength, int radius, int threshold) {
    return nativeArtistic(kind, strength, radius, threshold);
  }

  private static native boolean nativeDistortion(
      int kind, int amount, int size, int angle, int cx, int cy);

  public boolean distortion(int kind, int amount, int size, int angle, int cx, int cy) {
    return nativeDistortion(kind, amount, size, angle, cx, cy);
  }

  private static native boolean nativeBlur(int kind, int amount, int angle, int cx, int cy);

  public boolean blur(int kind, int amount, int angle, int cx, int cy) {
    return nativeBlur(kind, amount, angle, cx, cy);
  }

  private static native boolean nativeColorAdjustment(int kind, int[] values);

  public boolean colorAdjustment(int kind, int[] values) {
    return nativeColorAdjustment(kind, values);
  }

  public boolean applyEffect(int kind, int amount) {
    return nativeEffect(kind, amount);
  }

  private static native boolean nativeSetEffectSelection(byte[] mask);

  byte[] selectionMask() {
    return SelectionSupport.selectionMask(this);
  }

  public boolean prepareEffectSelection() {
    return nativeSetEffectSelection(selectionMask());
  }

  public void effectApplied() {
    refresh();
  }

  private static native boolean nativeSaveProject(int fd);

  private static native boolean nativeOpenProject(int fd);

  private static native boolean nativeSaveRecovery(int fd, long expectedRevision);

  public boolean writeRecovery(int fd, long expectedRevision) {
    return nativeSaveRecovery(fd, expectedRevision);
  }

  public boolean writeProject(int fd) {
    return nativeSaveProject(fd);
  }

  public boolean readProject(int fd) {
    return nativeOpenProject(fd);
  }

  public void projectOpened() {
    resetDocumentTools();
    deselect();
    refresh();
    fitCanvas();
  }

  private static native boolean nativeCreate(int w, int h);

  static native void nativeBeginEdit();

  private static native boolean nativeClear();

  private static native boolean nativeFlipActiveHorizontal();

  private static native boolean nativeFlipActiveVertical();

  private static native boolean nativeRotateActive180();

  private static native boolean nativeRotateActive90(boolean clockwise);

  static native boolean nativeCropActiveSelection(int left, int top, int right, int bottom);

  static native boolean nativeCropActiveEllipse(int left, int top, int right, int bottom);

  static native boolean nativeTrimActiveMasked(
      int left, int top, int width, int height, byte[] mask);

  private static native boolean nativeInvertActiveColors();

  private static native boolean nativeGrayscaleActive();

  private static native boolean nativeSepiaActive();

  private static native boolean nativeBrightnessActive(int adjustment);

  private static native boolean nativeContrastActive(int adjustment);

  private static native boolean nativeThresholdActive(int threshold);

  private static native boolean nativePosterizeActive(int levels);

  private static native boolean nativeSolarizeActive(int threshold);

  private static native boolean nativeSaturationActive(int adjustment);

  private static native boolean nativeGammaActive(int percent);

  private static native boolean nativeTintActive(
      int redAdjustment, int greenAdjustment, int blueAdjustment);

  private static native boolean nativeSwapChannelsActive(int mode);

  private static native boolean nativeColorBalanceActive(
      int redPercent, int greenPercent, int bluePercent);

  private static native boolean nativeHueRotateActive(int degrees);

  private static native boolean nativeLevelsActive(int blackPoint, int whitePoint);

  private static native boolean nativeExposureActive(int percent);

  private static native boolean nativeDesaturateChannelActive(int channel);

  private static native boolean nativeAdjustAlphaActive(int percent);

  private static native boolean nativeRemoveChannelActive(int channel);

  private static native boolean nativeNormalizeActive();

  private static native boolean nativeQuantizeActive(int step);

  private static native boolean nativeClampHighlightsActive(int ceiling);

  private static native boolean nativeLiftShadowsActive(int floor);

  private static native boolean nativeAdjustChannelActive(int channel, int adjustment);

  static native void nativeSetBrushSelection(byte[] mask);

  static native void nativeStyledStroke(
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      int color,
      float opacity,
      float hardness,
      boolean square,
      boolean eraser);

  static native void nativeStroke(float x0, float y0, float x1, float y1, float radius, int color);

  static native void nativeShape(int kind, int x0, int y0, int x1, int y1, int color);

  static native boolean nativeFillSelection(
      int x, int y, int color, float opacity, byte[] selection);

  static native boolean nativeUndo();

  static native boolean nativeRedo();

  private static native int[] nativePixels();

  private static native boolean nativeImport(int[] pixels);

  static native int nativePickColor(int x, int y);

  static native boolean nativeEraseSelection(int kind, int x0, int y0, int x1, int y1);

  static native boolean nativeEraseMaskedSelection(int x, int y, int w, int h, byte[] mask);

  static native boolean nativeMoveMaskedSelection(
      int x, int y, int w, int h, byte[] mask, int dx, int dy);

  static native int[] nativeCopySelection(int kind, int x0, int y0, int x1, int y1);

  static native boolean nativePasteSelection(int[] data, int x, int y);

  static native boolean nativeMovePixels(int kind, int x0, int y0, int x1, int y1, int dx, int dy);

  private static native int nativeLayerCount();

  private static native int nativeActiveLayer();

  private static native boolean nativeAddLayer();

  private static native boolean nativeSelectLayer(int index);

  private static native boolean nativeDeleteLayer();

  private static native boolean nativeToggleLayer();

  private static native boolean nativeLayerVisible(int index);

  private static native boolean nativeMoveLayer(int direction);

  private static native boolean nativeSetLayerOpacity(float opacity);

  private static native float nativeLayerOpacity();

  private static native int[] nativeLayerThumbnail(int index);

  int canvasWidth = 800, canvasHeight = 800;

  private static native boolean nativeLoadBitmap(int w, int h, int[] pixels);

  private static native int nativeWidth();

  private static native int nativeHeight();

  private static native boolean nativeResizeDocument(int w, int h, boolean scalePixels);

  private static native boolean nativeResizeDocumentOptions(
      int w, int h, boolean scalePixels, boolean bilinear, int anchor);

  private static native boolean nativeCropDocument(int left, int top, int w, int h);

  private static native long nativeRevision();

  public long revision() {
    return nativeRevision();
  }

  public boolean hasPendingCurve() {
    return curve != null;
  }

  public static boolean hasDocument() {
    return nativeWidth() > 0;
  }

  public int documentWidth() {
    return canvasWidth;
  }

  public int documentHeight() {
    return canvasHeight;
  }

  public boolean resizeDocument(int w, int h, boolean scalePixels) {
    return resizeDocument(w, h, scalePixels, false, 0);
  }

  public boolean resizeDocument(int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    if (!resizeDocumentPixels(w, h, scalePixels, bilinear, anchor)) return false;
    documentResized();
    return true;
  }

  public boolean resizeDocumentPixels(
      int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    return nativeResizeDocumentOptions(w, h, scalePixels, bilinear, anchor);
  }

  public void documentResized() {
    deselect();
    refresh();
    fitCanvas();
  }

  private void resetDocumentTools() {
    cursorX = cursorY = -1;
    cancelCurve();
    savedCurves.clear();
    cloneOriginReady = false;
    drawing = false;
    strokeEditing = false;
    movingSelection = false;
    movingPixels = false;
    navigating = false;
    shapePath.reset();
    dragBackup = null;
  }

  public boolean newDocument(int w, int h) {
    if (w <= 0 || h <= 0 || w > 8192 || h > 8192 || (long) w * h > 4000000 || !nativeCreate(w, h))
      return false;
    resetDocumentTools();
    deselect();
    refresh();
    fitCanvas();
    return true;
  }

  public boolean cropDocument() {
    if (!hasSelection()) return false;
    int left = (int) Math.floor(selectionLeft), top = (int) Math.floor(selectionTop);
    if (!nativeCropDocument(
        left, top, (int) Math.ceil(selectionRight) - left, (int) Math.ceil(selectionBottom) - top))
      return false;
    deselect();
    refresh();
    fitCanvas();
    return true;
  }

  final Viewport viewport = new Viewport(canvasWidth, canvasHeight);
  private boolean navigating;
  private float gestureX, gestureY, gestureDistance, gestureAngle;
  private int cursorX = -1, cursorY = -1;

  public String cursorStatus() {
    return cursorX < 0 ? "" : "  |  X: " + cursorX + " Y: " + cursorY;
  }

  private void updateCursor(float x, float y) {
    int cx = (int) Math.floor(x), cy = (int) Math.floor(y);
    if (cx < 0 || cy < 0 || cx >= canvasWidth || cy >= canvasHeight) {
      cx = -1;
      cy = -1;
    }
    if (cx != cursorX || cy != cursorY) {
      cursorX = cx;
      cursorY = cy;
      if (viewportChangedListener != null) viewportChangedListener.run();
    }
  }

  @Override
  public boolean onHoverEvent(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) updateCursor(-1, -1);
    else
      updateCursor(
          (float) viewport.documentX(event.getX(), event.getY()),
          (float) viewport.documentY(event.getX(), event.getY()));
    return true;
  }

  private Runnable viewportChangedListener;

  public void setOnViewportChangedListener(Runnable listener) {
    viewportChangedListener = listener;
  }

  public float zoomPercent() {
    return (float) (viewport.scale * 100);
  }

  public float rotationDegrees() {
    return (float) viewport.angle;
  }

  void viewportChanged() {
    invalidate();
    if (viewportChangedListener != null) viewportChangedListener.run();
  }

  public void fitCanvas() {
    viewport.fit(getWidth(), getHeight());
    viewportChanged();
  }

  public void zoomBy(float factor) {
    viewport.zoom(viewport.scale * factor, getWidth() / 2., getHeight() / 2.);
    viewportChanged();
  }

  public void setZoomPercent(float percent) {
    viewport.zoom(percent / 100., getWidth() / 2., getHeight() / 2.);
    viewportChanged();
  }

  public void rotateView(float degrees) {
    viewport.rotate(degrees, getWidth() / 2., getHeight() / 2.);
    viewportChanged();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (oldw == 0 || oldh == 0) viewport.fit(w, h);
    else {
      viewport.centerX += (w - oldw) / 2.;
      viewport.centerY += (h - oldh) / 2.;
    }
    viewportChanged();
  }

  private void recordGesture(MotionEvent event) {
    gestureX = (event.getX(0) + event.getX(1)) / 2f;
    gestureY = (event.getY(0) + event.getY(1)) / 2f;
    float dx = event.getX(1) - event.getX(0), dy = event.getY(1) - event.getY(0);
    gestureDistance = (float) Math.hypot(dx, dy);
    gestureAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
  }

  private void cancelToolGesture() {
    Tools.controller(tool).cancel(this);
    drawing = false;
    movingSelection = false;
    movingPixels = false;
    invalidate();
  }

  public static final int BRUSH = 0,
      RECTANGLE = 1,
      ELLIPSE = 2,
      LINE = 3,
      BUCKET = 4,
      ERASER = 5,
      PICKER = 6,
      FILLED_RECTANGLE = 7,
      FILLED_ELLIPSE = 8,
      SELECT_RECTANGLE = 9,
      SELECT_ELLIPSE = 10,
      MOVE_SELECTION = 11,
      MOVE_PIXELS = 12,
      SELECT_FREE = 13,
      MAGIC_WAND = 14,
      TEXT = 15,
      ROUNDED_RECTANGLE = 16,
      FILLED_ROUNDED_RECTANGLE = 17,
      TRIANGLE = 18,
      FILLED_TRIANGLE = 19,
      PENCIL = 20,
      PAN = 21,
      ZOOM = 22,
      GRADIENT = 23,
      FREEFORM = 24,
      CIRCLE = 25,
      CLONE = 26,
      RECOLOR = 27;
  final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
  Bitmap bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
  java.util.function.BiConsumer<Integer, Integer> textPositionListener;

  public void setOnTextPositionListener(java.util.function.BiConsumer<Integer, Integer> listener) {
    textPositionListener = listener;
  }

  public boolean insertText(
      String text, int x, int y, int size, boolean bold, boolean italic, String family) {
    return TextTool.insertText(this, text, x, y, size, bold, italic, family);
  }

  boolean pasteAt(Bitmap source, int x, int y) {
    int w = source.getWidth(), h = source.getHeight();
    int[] data = new int[2 + w * h];
    data[0] = w;
    data[1] = h;
    source.getPixels(data, 2, w, 0, 0, w, h);
    if (!nativePasteSelection(data, x, y)) return false;
    deselect();
    refresh();
    return true;
  }

  void additionalShape(int kind, float x0, float y0, float x1, float y1) {
    ShapeRaster.additionalShape(this, kind, x0, y0, x1, y1);
  }

  CurveDraft curve;
  CurveDraft dragBackup, canceledCurve;
  final CurveHistory curveHistory = new CurveHistory();
  int curveHandle = -1, nextCurveTag = 1;

  static native int nativeCurveTag();

  static native void nativeMarkCurve(int tag);

  final android.util.SparseArray<CurveSnapshot> savedCurves = new android.util.SparseArray<>();

  android.graphics.Path curvePath() {
    return LineCurveTool.curvePath(this);
  }

  public boolean confirmCurve() {
    return LineCurveTool.confirmCurve(this);
  }

  public void cancelCurve() {
    LineCurveTool.cancelCurve(this);
  }

  @Override
  public boolean onKeyDown(int code, android.view.KeyEvent event) {
    if (code == android.view.KeyEvent.KEYCODE_ENTER && confirmCurve()) return true;
    if (code == android.view.KeyEvent.KEYCODE_ESCAPE && curve != null) {
      cancelCurve();
      return true;
    }
    if (event.isCtrlPressed() && code == android.view.KeyEvent.KEYCODE_Z) {
      if (event.isShiftPressed()) redo();
      else undo();
      return true;
    }
    return super.onKeyDown(code, event);
  }

  final android.graphics.Path shapePath = new android.graphics.Path();

  void commitFreeform() {
    FreeformTool.commitFreeform(this);
  }

  final NavigationTool navigationTool = new NavigationTool(4);
  int color = 0xFF202020, tool = BRUSH;
  int gradientMode, secondaryColor = 0xffffffff;
  boolean gradientTransparent = true;

  public void setSecondaryColor(int value) {
    secondaryColor = value;
  }

  public int gradientMode() {
    return gradientMode;
  }

  public boolean gradientTransparent() {
    return gradientTransparent;
  }

  public void configureGradient(int mode, boolean transparent) {
    if (mode < 0 || mode > 4) return;
    gradientMode = mode;
    gradientTransparent = transparent;
  }

  boolean cloneOriginReady;
  int cloneX, cloneY;

  static native void nativeBeginSampled(int x, int y);

  static native void nativeSampledStroke(
      boolean clone,
      int ox,
      int oy,
      int replacement,
      int tolerance,
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      float opacity,
      float hardness);

  float brushRadius = 4f, brushOpacity = 1f, brushHardness = 1f;
  boolean squareBrush, pressureBrush = true, strokeEditing;

  public float brushOpacity() {
    return brushOpacity;
  }

  public float brushHardness() {
    return brushHardness;
  }

  public boolean squareBrush() {
    return squareBrush;
  }

  public boolean pressureBrush() {
    return pressureBrush;
  }

  public void configureBrush(float opacity, float hardness, boolean square, boolean pressure) {
    brushOpacity = opacity;
    brushHardness = hardness;
    squareBrush = square;
    pressureBrush = pressure;
  }

  void paintStroke(float x0, float y0, float x1, float y1, MotionEvent event) {
    StrokeSupport.paintStroke(this, x0, y0, x1, y1, event);
  }

  float previousX, previousY, startX, startY;
  boolean drawing;
  boolean hasSelection;
  boolean movingSelection;
  boolean movingPixels;
  final android.graphics.Path freePath = new android.graphics.Path();
  boolean freeSelectionReady;
  final android.graphics.Region freeRegion = new android.graphics.Region();
  int wandTolerance = 0;
  final android.graphics.Path wandBoundary = new android.graphics.Path();

  public void setWandTolerance(int value) {
    wandTolerance = Math.max(0, Math.min(255, value));
  }

  float moveStartX,
      moveStartY,
      moveOriginalLeft,
      moveOriginalTop,
      moveOriginalRight,
      moveOriginalBottom;
  int selectionTool;
  float selectionLeft, selectionTop, selectionRight, selectionBottom;
  final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private Runnable canvasChangedListener;
  java.util.function.IntConsumer pickedColorListener;

  public void setOnColorPickedListener(java.util.function.IntConsumer listener) {
    pickedColorListener = listener;
  }

  public void setOnCanvasChangedListener(Runnable listener) {
    canvasChangedListener = listener;
  }

  public DrawingView(Context context) {
    super(context);
    setContentDescription(
        "Lienzo de dibujo; arrastra para usar la herramienta activa y utiliza dos dedos para"
            + " navegar");
    if (nativeWidth() == 0 && !nativeCreate(canvasWidth, canvasHeight))
      throw new IllegalStateException("Canvas error");
    refresh();
  }

  public boolean setLayerOpacity(float opacity) {
    boolean ok = nativeSetLayerOpacity(opacity);
    if (ok) refresh();
    return ok;
  }

  public float layerOpacity() {
    return nativeLayerOpacity();
  }

  public Bitmap layerThumbnail(int index) {
    int[] pixels = nativeLayerThumbnail(index);
    if (pixels == null || pixels.length != 48 * 48) return null;
    return Bitmap.createBitmap(pixels, 48, 48, Bitmap.Config.ARGB_8888);
  }

  private static native String nativeLayerName(int index);

  private static native boolean nativeRenameLayer(int index, String name);

  public String layerName(int index) {
    String name = nativeLayerName(index);
    return name == null ? "Capa " + (index + 1) : name;
  }

  public boolean renameLayer(int index, String name) {
    if (name == null || name.trim().isEmpty()) return false;
    boolean ok = nativeRenameLayer(index, name.trim());
    if (ok) refresh();
    return ok;
  }

  public int layerCount() {
    return nativeLayerCount();
  }

  public int activeLayer() {
    return nativeActiveLayer();
  }

  public boolean layerVisible(int index) {
    return nativeLayerVisible(index);
  }

  public boolean addLayer() {
    boolean ok = nativeAddLayer();
    if (ok) refresh();
    return ok;
  }

  public boolean selectLayer(int index) {
    confirmCurve();
    boolean ok = nativeSelectLayer(index);
    if (ok) refresh();
    return ok;
  }

  public boolean deleteLayer() {
    boolean ok = nativeDeleteLayer();
    if (ok) refresh();
    return ok;
  }

  public boolean toggleLayer() {
    boolean ok = nativeToggleLayer();
    if (ok) refresh();
    return ok;
  }

  public boolean moveLayer(int direction) {
    boolean ok = nativeMoveLayer(direction);
    if (ok) refresh();
    return ok;
  }

  public void setColor(int value) {
    color = value;
  }

  public void setBrushRadius(float radius) {
    if (Float.isFinite(radius) && radius >= 1f && radius <= 128f) brushRadius = radius;
  }

  public float brushRadius() {
    return brushRadius;
  }

  public int currentTool() {
    return tool;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (!enabled) confirmCurve();
    super.setEnabled(enabled);
  }

  public void setTool(int value) {
    if (value < BRUSH || value > RECOLOR) return;
    if (value != LINE) {
      if (curve != null) confirmCurve();
      else if (canceledCurve != null) cancelCurve();
    }
    if (value == CLONE) cloneOriginReady = false;
    tool = value;
    // Switching away from a selection tool must not leave a selection
    // permanently active as an accidental overlay on subsequent drawings.
    invalidate();
  }

  public boolean transformSelection(float degrees, float sx, float sy) {
    return MovePixelsTool.transformSelection(this, degrees, sx, sy);
  }

  public boolean hasSelection() {
    return hasSelection
        && selectionRight - selectionLeft >= 1f
        && selectionBottom - selectionTop >= 1f;
  }

  public boolean shrinkSelectionOnePixel() {
    return SelectionSupport.shrinkSelectionOnePixel(this);
  }

  public boolean expandSelectionOnePixel() {
    return SelectionSupport.expandSelectionOnePixel(this);
  }

  public boolean invertSelection() {
    return SelectionSupport.invertSelection(this);
  }

  public boolean selectionContains(int px, int py) {
    return SelectionSupport.selectionContains(this, px, py);
  }

  public boolean flipActiveHorizontal() {
    if (!nativeFlipActiveHorizontal()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean flipActiveVertical() {
    if (!nativeFlipActiveVertical()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean rotateActive180() {
    if (!nativeRotateActive180()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean rotateActive90(boolean clockwise) {
    if (!nativeRotateActive90(clockwise)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean adjustChannelActive(int channel, int adjustment) {
    if (!nativeAdjustChannelActive(channel, adjustment)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean liftShadowsActive(int floor) {
    if (!nativeLiftShadowsActive(floor)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean clampHighlightsActive(int ceiling) {
    if (!nativeClampHighlightsActive(ceiling)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean quantizeActive(int step) {
    if (!nativeQuantizeActive(step)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean normalizeActive() {
    if (!nativeNormalizeActive()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean removeChannelActive(int channel) {
    if (!nativeRemoveChannelActive(channel)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean adjustAlphaActive(int percent) {
    if (!nativeAdjustAlphaActive(percent)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean grayscaleFromChannelActive(int channel) {
    if (!nativeDesaturateChannelActive(channel)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean exposureActive(int percent) {
    if (!nativeExposureActive(percent)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean levelsActive(int blackPoint, int whitePoint) {
    if (!nativeLevelsActive(blackPoint, whitePoint)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean hueRotateActive(int degrees) {
    if (!nativeHueRotateActive(degrees)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean colorBalanceActive(int redPercent, int greenPercent, int bluePercent) {
    if (!nativeColorBalanceActive(redPercent, greenPercent, bluePercent)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean swapChannelsActive(int mode) {
    if (!nativeSwapChannelsActive(mode)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean tintActive(int red, int green, int blue) {
    if (!nativeTintActive(red, green, blue)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean gammaActive(int percent) {
    if (!nativeGammaActive(percent)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean saturationActive(int adjustment) {
    if (!nativeSaturationActive(adjustment)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean solarizeActive(int threshold) {
    if (!nativeSolarizeActive(threshold)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean posterizeActive(int levels) {
    if (!nativePosterizeActive(levels)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean thresholdActive(int threshold) {
    if (!nativeThresholdActive(threshold)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean contrastActive(int adjustment) {
    if (!nativeContrastActive(adjustment)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean brightnessActive(int adjustment) {
    if (!nativeBrightnessActive(adjustment)) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean sepiaActive() {
    if (!nativeSepiaActive()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean grayscaleActive() {
    if (!nativeGrayscaleActive()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean invertActiveColors() {
    if (!nativeInvertActiveColors()) return false;
    deselect();
    refresh();
    return true;
  }

  public boolean trimActiveToFreeSelection() {
    return SelectionSupport.trimActiveToFreeSelection(this);
  }

  public boolean trimActiveToEllipseSelection() {
    return SelectionSupport.trimActiveToEllipseSelection(this);
  }

  public boolean trimActiveToRectSelection() {
    return SelectionSupport.trimActiveToRectSelection(this);
  }

  public void clear() {
    if (nativeClear()) {
      deselect();
      refresh();
    }
  }

  public boolean pasteBitmap(Bitmap source) {
    if (source == null || source.isRecycled()) return false;
    int w = source.getWidth(), h = source.getHeight();
    if (w <= 0
        || h <= 0
        || w > canvasWidth
        || h > canvasHeight
        || ((long) w * h) > canvasWidth * canvasHeight) return false;
    int[] data = new int[2 + w * h];
    data[0] = w;
    data[1] = h;
    source.getPixels(data, 2, w, 0, 0, w, h);
    int x = hasSelection() ? (int) selectionLeft : (canvasWidth - w) / 2;
    int y = hasSelection() ? (int) selectionTop : (canvasHeight - h) / 2;
    x = Math.max(0, Math.min(canvasWidth - w, x));
    y = Math.max(0, Math.min(canvasHeight - h, y));
    boolean ok = nativePasteSelection(data, x, y);
    if (ok) {
      deselect();
      refresh();
    }
    return ok;
  }

  public Bitmap copySelection() {
    return SelectionSupport.copySelection(this);
  }

  public boolean eraseSelection() {
    return SelectionSupport.eraseSelection(this);
  }

  public void selectAll() {
    SelectionSupport.selectAll(this);
  }

  public void deselect() {
    SelectionSupport.deselect(this);
  }

  public void enableSelectionMove() {
    tool = MOVE_SELECTION;
    invalidate();
  }

  public boolean moveSelectedPixels(int dx, int dy) {
    return MovePixelsTool.moveSelectedPixels(this, dx, dy);
  }

  public void undo() {
    LineCurveTool.undo(this);
  }

  public void redo() {
    LineCurveTool.redo(this);
  }

  public boolean loadBitmap(Bitmap source) {
    if (source == null || source.isRecycled()) return false;
    int w = source.getWidth(), h = source.getHeight();
    if (w > 8192 || h > 8192 || (long) w * h > 4000000) return false;
    int[] pixels = new int[w * h];
    source.getPixels(pixels, 0, w, 0, 0, w, h);
    if (!nativeLoadBitmap(w, h, pixels)) return false;
    resetDocumentTools();
    deselect();
    refresh();
    fitCanvas();
    return true;
  }

  public Bitmap snapshot() {
    if (strokeRefreshPending) refresh();
    confirmCurve();
    return bitmap.copy(Bitmap.Config.ARGB_8888, false);
  }

  private boolean strokeRefreshPending;
  private final Runnable strokeRefresh = () -> {
    strokeRefreshPending = false;
    refreshPixels();
  };

  // Coalesce pointer events into one pixel transfer per display frame.
  void requestStrokeRefresh() {
    if (!strokeRefreshPending) {
      strokeRefreshPending = true;
      postOnAnimation(strokeRefresh);
    }
  }

  void refresh() {
    removeCallbacks(strokeRefresh);
    strokeRefreshPending = false;
    refreshPixels();
    if (canvasChangedListener != null) canvasChangedListener.run();
    viewportChanged();
  }

  private void refreshPixels() {
    int width = nativeWidth(), height = nativeHeight();
    int[] pixels = nativePixels();
    if (width <= 0 || height <= 0 || pixels == null || pixels.length != (long) width * height)
      return;
    if (width != canvasWidth || height != canvasHeight) {
      Bitmap replacement = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      bitmap.recycle();
      bitmap = replacement;
      canvasWidth = width;
      canvasHeight = height;
      viewport.documentSize(width, height);
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    invalidate();
  }

  final Paint checkerPaint = new Paint();

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    WorkspaceRenderer.draw(this, canvas);
  }

  void selectMatchingRegion(int sx, int sy) {
    MagicWandTool.selectMatchingRegion(this, sx, sy);
  }

  void translateSelectionMask(int dx, int dy) {
    MoveSelectionTool.translateSelectionMask(this, dx, dy);
  }

  private void restoreMovedSelection() {
    MoveSelectionTool.restoreMovedSelection(this);
  }

  void updateMovedSelection(float x, float y) {
    MoveSelectionTool.updateMovedSelection(this, x, y);
  }

  void updateSelection(float x, float y) {
    SelectionSupport.updateSelection(this, x, y);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isEnabled() || getWidth() <= 0 || getHeight() <= 0) return false;
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
      cancelToolGesture();
      navigating = true;
      recordGesture(event);
      getParent().requestDisallowInterceptTouchEvent(true);
      return true;
    }
    if (navigating) {
      if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 2) {
        float oldX = gestureX,
            oldY = gestureY,
            oldDistance = gestureDistance,
            oldAngle = gestureAngle;
        recordGesture(event);
        float delta = gestureAngle - oldAngle;
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        if (oldDistance > 1)
          viewport.gesture(oldX, oldY, gestureX, gestureY, gestureDistance / oldDistance, delta);
        viewportChanged();
      } else if (action == MotionEvent.ACTION_POINTER_UP && event.getPointerCount() > 2) {
        gestureDistance = 0;
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        navigating = false;
        getParent().requestDisallowInterceptTouchEvent(false);
      }
      return true;
    }
    if (tool == PAN || tool == ZOOM) return Tools.controller(tool).screen(this, event);
    float x = (float) viewport.documentX(event.getX(), event.getY());
    float y = (float) viewport.documentY(event.getX(), event.getY());
    updateCursor(x, y);
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (x < 0 || y < 0 || x >= canvasWidth || y >= canvasHeight) return true;
        getParent().requestDisallowInterceptTouchEvent(true);
        drawing = true;
        strokeEditing = false;
        startX = previousX = x;
        startY = previousY = y;
        Tools.controller(tool).down(this, x, y, event);
        return true;
      case MotionEvent.ACTION_MOVE:
        if (!drawing) return true;
        Tools.controller(tool).move(this, x, y, event);
        previousX = x;
        previousY = y;
        return true;
      case MotionEvent.ACTION_UP:
        getParent().requestDisallowInterceptTouchEvent(false);
        if (drawing) {
          Tools.controller(tool).up(this, x, y, event);
          drawing = false;
          refresh();
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
        getParent().requestDisallowInterceptTouchEvent(false);
        cancelToolGesture();
        return true;
      default:
        return true;
    }
  }
}
