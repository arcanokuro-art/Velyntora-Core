#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// 0 box blur, 1 sharpen, 2 edges, 3 emboss, 4 pixelate, 5 noise, 6 vignette.
std::vector<std::uint32_t> pixelEffect(const std::vector<std::uint32_t>& source,int width,int height,int kind,int amount);
}
