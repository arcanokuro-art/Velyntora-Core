#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Fragment, unfocus, dents, polar inversion, reduce noise, outline edges, relief, dithering.
std::vector<std::uint32_t> utilityEffect(const std::vector<std::uint32_t>& pixels, int width,
                                         int height, int kind, int amount, int size, int parameter);
}  // namespace velyntora
