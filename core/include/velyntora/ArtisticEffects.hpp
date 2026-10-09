#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Oil paint, pencil, ink, glow, soft portrait, median, red-eye reduction.
std::vector<std::uint32_t> artisticEffect(const std::vector<std::uint32_t>& pixels,int width,int height,int kind,int strength,int radius,int threshold);
}
