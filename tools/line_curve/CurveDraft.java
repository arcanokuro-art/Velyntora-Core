package art.velyntora.core;

/** Cubic curve geometry; coordinates are in document pixels. */
final class CurveDraft {
  final float[] x = new float[4], y = new float[4];

  CurveDraft(float x0, float y0, float x1, float y1) {
    for (int i = 0; i < 4; i++) {
      x[i] = x0 + (x1 - x0) * i / 3f;
      y[i] = y0 + (y1 - y0) * i / 3f;
    }
  }

  CurveDraft copy() {
    CurveDraft c = new CurveDraft(0, 0, 0, 0);
    System.arraycopy(x, 0, c.x, 0, 4);
    System.arraycopy(y, 0, c.y, 0, 4);
    return c;
  }

  void move(int point, float px, float py) {
    if (point < 0 || point > 3 || !Float.isFinite(px) || !Float.isFinite(py))
      throw new IllegalArgumentException("Invalid curve handle");
    x[point] = px;
    y[point] = py;
  }

  int hit(float px, float py, float radius) {
    int point = -1;
    double best = radius;
    for (int i = 0; i < 4; i++) {
      double d = Math.hypot(x[i] - px, y[i] - py);
      if (d <= best) {
        best = d;
        point = i;
      }
    }
    return point;
  }

  float evaluate(float t, boolean horizontal) {
    if (!Float.isFinite(t) || t < 0 || t > 1)
      throw new IllegalArgumentException("Invalid curve parameter");
    float[] p = horizontal ? x : y;
    float u = 1 - t;
    return u * u * u * p[0] + 3 * u * u * t * p[1] + 3 * u * t * t * p[2] + t * t * t * p[3];
  }
}
