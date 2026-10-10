#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Align, feather alpha inward, outline behind transparent objects.
std::vector<std::uint32_t> objectEffect(const std::vector<std::uint32_t>& pixels, int width,
                                        int height, int kind, int amount, int tolerance,
                                        std::uint32_t color, bool edgeOrSoft,
                                        const std::vector<std::uint8_t>* selection = nullptr);
}  // namespace velyntora
