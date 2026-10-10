#include "velyntora/ProjectIO.hpp"

#include <algorithm>
#include <array>
#include <bit>
#include <cmath>
#include <cstring>
#include <stdexcept>

namespace velyntora {
namespace {
constexpr std::size_t maxLayers = 32, maxName = 4096;
constexpr std::uint64_t maxPixels = 24000000;
constexpr std::array<unsigned char, 8> magic = {'V', 'L', 'Y', 'C', 'O', 'R', 'E', 1};
void put(const ProjectWrite& write, std::uint32_t value) {
  unsigned char b[4];
  for (int i = 0; i < 4; ++i) b[i] = static_cast<unsigned char>(value >> (8 * i));
  write(b, 4);
}
std::uint32_t get(const ProjectRead& read) {
  unsigned char b[4];
  read(b, 4);
  std::uint32_t value = 0;
  for (int i = 0; i < 4; ++i) value |= std::uint32_t(b[i]) << (8 * i);
  return value;
}
void dimensions(std::uint32_t w, std::uint32_t h, std::uint32_t count) {
  if (w == 0 || h == 0 || w > 16000000 || h > 16000000 || count == 0 || count > maxLayers ||
      std::uint64_t(w) * h > maxPixels / count)
    throw std::invalid_argument("Project exceeds document limits");
}
}  // namespace
void writeProject(const LayerDocument& doc, const ProjectWrite& write) {
  dimensions(doc.width(), doc.height(), doc.layerCount());
  for (std::size_t i = 0; i < doc.layerCount(); ++i)
    if (doc.layer(i).name.size() > maxName) throw std::invalid_argument("Layer name too long");
  write(magic.data(), magic.size());
  put(write, doc.width());
  put(write, doc.height());
  put(write, doc.layerCount());
  put(write, doc.activeIndex());
  std::array<unsigned char, 4096> block{};
  for (std::size_t i = 0; i < doc.layerCount(); ++i) {
    const auto& layer = doc.layer(i);
    put(write, layer.name.size());
    write(layer.name.data(), layer.name.size());
    put(write, layer.visible ? 1 : 0);
    put(write, std::bit_cast<std::uint32_t>(layer.opacity));
    for (std::size_t offset = 0; offset < layer.pixels.size(); offset += block.size() / 4) {
      auto count = std::min(block.size() / 4, layer.pixels.size() - offset);
      for (std::size_t p = 0; p < count; ++p)
        for (int b = 0; b < 4; ++b)
          block[p * 4 + b] = static_cast<unsigned char>(layer.pixels[offset + p] >> (8 * b));
      write(block.data(), count * 4);
    }
  }
}
LayerDocument readProject(const ProjectRead& read, int requiredWidth, int requiredHeight,
                          std::uint64_t maxDocumentPixels) {
  std::array<unsigned char, 8> signature{};
  read(signature.data(), signature.size());
  if (signature != magic) throw std::invalid_argument("Unsupported project format");
  auto w = get(read), h = get(read), count = get(read), active = get(read);
  dimensions(w, h, count);
  if (std::uint64_t(w) * h > maxDocumentPixels || active >= count ||
      (requiredWidth && w != std::uint32_t(requiredWidth)) ||
      (requiredHeight && h != std::uint32_t(requiredHeight)))
    throw std::invalid_argument("Invalid project dimensions or active layer");
  LayerDocument doc(w, h);
  std::vector<std::uint32_t> pixels(std::size_t(w) * h);
  std::array<unsigned char, 4096> block{};
  for (std::uint32_t i = 0; i < count; ++i) {
    auto size = get(read);
    if (size > maxName) throw std::invalid_argument("Layer name too long");
    std::string name(size, '\0');
    read(name.data(), size);
    auto visible = get(read);
    float opacity = std::bit_cast<float>(get(read));
    if (visible > 1 || !std::isfinite(opacity) || opacity < 0 || opacity > 1)
      throw std::invalid_argument("Invalid layer properties");
    if (i == 0)
      doc.renameLayer(0, name);
    else
      doc.addLayer(name);
    doc.selectLayer(i);
    doc.setVisible(i, visible != 0);
    doc.setOpacity(i, opacity);
    for (std::size_t offset = 0; offset < pixels.size(); offset += block.size() / 4) {
      auto n = std::min(block.size() / 4, pixels.size() - offset);
      read(block.data(), n * 4);
      for (std::size_t p = 0; p < n; ++p) {
        std::uint32_t pixel = 0;
        for (int b = 0; b < 4; ++b) pixel |= std::uint32_t(block[p * 4 + b]) << (8 * b);
        pixels[offset + p] = pixel;
      }
    }
    doc.replaceActivePixels(pixels);
  }
  doc.selectLayer(active);
  return doc;
}
}  // namespace velyntora
