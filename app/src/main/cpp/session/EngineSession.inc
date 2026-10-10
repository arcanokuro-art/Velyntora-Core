#include <jni.h>
#include <unistd.h>

#include <algorithm>
#include <cerrno>
#include <cmath>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

#include "velyntora/ArtisticEffects.hpp"
#include "velyntora/BlurEffects.hpp"
#include "velyntora/Canvas.hpp"
#include "velyntora/ColorAdjustments.hpp"
#include "velyntora/DistortionEffects.hpp"
#include "velyntora/LayerDocument.hpp"
#include "velyntora/ObjectEffects.hpp"
#include "velyntora/PixelEffects.hpp"
#include "velyntora/ProjectIO.hpp"
#include "velyntora/RenderEffects.hpp"
#include "velyntora/SelectionTransform.hpp"
#include "velyntora/UtilityEffects.hpp"

namespace {
std::mutex guard;
std::unique_ptr<velyntora::Canvas> canvas;
std::unique_ptr<velyntora::LayerDocument> layers;
int curveTag = 0;
std::uint64_t revision = 0, nextRevision = 0;
struct Snapshot {
  velyntora::LayerDocument layers;
  int curveTag = 0;
  std::uint64_t revision = 0;
};
std::vector<Snapshot> undoStack, redoStack;
std::vector<std::uint8_t> brushMask, effectMask;
std::vector<std::uint32_t> sampledSource;
std::uint32_t sampledTarget;
void applyEffectSelection(std::vector<std::uint32_t>& pixels) {
  if (effectMask.empty()) return;
  if (effectMask.size() != pixels.size()) {
    pixels = canvas->pixels();
    return;
  }
  for (std::size_t i = 0; i < pixels.size(); i++)
    if (!effectMask[i]) pixels[i] = canvas->pixels()[i];
}
constexpr std::size_t limit = 15;
// Cap history memory across both stacks to avoid exhausting Android heap
// when many full-resolution layers are present.
constexpr std::size_t historyBudget = 96ULL * 1024 * 1024;
std::size_t snapshotBytes(const Snapshot& snapshot) {
  std::size_t total = 0;
  for (std::size_t i = 0; i < snapshot.layers.layerCount(); ++i)
    total += snapshot.layers.layer(i).pixels.size() * sizeof(std::uint32_t);
  return total;
}
std::size_t historyBytes() {
  std::size_t total = 0;
  for (const auto& s : undoStack) total += snapshotBytes(s);
  for (const auto& s : redoStack) total += snapshotBytes(s);
  return total;
}
void trimHistory() {
  while (undoStack.size() > limit) undoStack.erase(undoStack.begin());
  while (redoStack.size() > limit) redoStack.erase(redoStack.begin());
  while (historyBytes() > historyBudget) {
    if (!undoStack.empty())
      undoStack.erase(undoStack.begin());
    else if (!redoStack.empty())
      redoStack.erase(redoStack.begin());
    else
      break;
  }
}

void checkpoint() {
  if (!canvas) return;
  undoStack.push_back({*layers, curveTag, revision});
  redoStack.clear();
  revision = ++nextRevision;
  curveTag = 0;
  trimHistory();
}
void storeActive() {
  curveTag = 0;
  if (layers && canvas) layers->replaceActivePixels(canvas->pixels());
}
// Brush edits touch only their swept bounds, not every pixel in the active layer.
void storeStrokeRegion(float x0,float y0,float x1,float y1,float radius) {
  if (!layers || !canvas || !std::isfinite(x0) || !std::isfinite(y0)
      || !std::isfinite(x1) || !std::isfinite(y1) || !std::isfinite(radius) || radius<=0) return;
  radius=std::min(radius,2048.f)+2;
  int left=int(std::clamp(std::floor(std::min(x0,x1)-radius),0.f,float(canvas->width())));
  int top=int(std::clamp(std::floor(std::min(y0,y1)-radius),0.f,float(canvas->height())));
  int right=int(std::clamp(std::ceil(std::max(x0,x1)+radius),0.f,float(canvas->width())));
  int bottom=int(std::clamp(std::ceil(std::max(y0,y1)+radius),0.f,float(canvas->height())));
  curveTag=0;
  if(right>left && bottom>top)
    layers->replaceActiveRegion(canvas->pixels(),left,top,right-left,bottom-top);
}
void loadActive() {
  if (layers && canvas) canvas->setPixels(layers->layer(layers->activeIndex()).pixels);
}
void resetHistory() {
  revision = ++nextRevision;
  sampledSource.clear();
  curveTag = 0;
  undoStack.clear();
  redoStack.clear();
  brushMask.clear();
}
void restore(const Snapshot& snapshot) {
  auto restored = std::make_unique<velyntora::LayerDocument>(snapshot.layers);
  auto restoredCanvas = std::make_unique<velyntora::Canvas>(restored->width(), restored->height());
  restoredCanvas->setPixels(restored->layer(restored->activeIndex()).pixels);
  layers = std::move(restored);
  canvas = std::move(restoredCanvas);
  curveTag = snapshot.curveTag;
  revision = snapshot.revision;
}
}  // namespace
