#include "velyntora/Canvas.hpp"

#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>
namespace velyntora {
Canvas::Canvas(int w, int h) : width_(w), height_(h) {
  if (w <= 0 || h <= 0 || static_cast<std::uint64_t>(w) * h > 16000000ULL)
    throw std::invalid_argument("Invalid canvas size");
  pixels_.resize(static_cast<std::size_t>(w) * h, 0xFFFFFFFFu);
}
void Canvas::clear(std::uint32_t color) { std::fill(pixels_.begin(), pixels_.end(), color); }
void Canvas::setPixels(const std::vector<std::uint32_t>& pixels) {
  if (pixels.size() != pixels_.size()) throw std::invalid_argument("Pixel count mismatch");
  pixels_ = pixels;
}
void Canvas::pixel(int x, int y, std::uint32_t color) {
  if (x >= 0 && y >= 0 && x < width_ && y < height_)
    pixels_[static_cast<std::size_t>(y) * width_ + x] = color;
}
}  // namespace velyntora
