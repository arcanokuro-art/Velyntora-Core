#pragma once
#include <vector>
#include <cstdint>
namespace velyntora {
// kind: Gaussian, motion, radial, zoom. Centers expressed as canvas percentages.
std::vector<std::uint32_t> blurEffect(const std::vector<std::uint32_t>& pixels,int width,int height,int kind,int amount,int angle,int centerX,int centerY);
}
