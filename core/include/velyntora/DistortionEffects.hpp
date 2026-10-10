#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Twist, bulge/pinch, radial ripple, tiled glass, frosted glass.
std::vector<std::uint32_t> distortionEffect(const std::vector<std::uint32_t>& source, int width,
                                            int height, int kind, int amount, int size, int angle,
                                            int centerX, int centerY);
}  // namespace velyntora
