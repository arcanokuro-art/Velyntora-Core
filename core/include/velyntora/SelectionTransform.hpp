#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
std::vector<std::uint32_t> transformSelection(const std::vector<std::uint32_t>& source, int width,
                                              int height, int left, int top, int selectionWidth,
                                              int selectionHeight,
                                              const std::vector<std::uint8_t>& mask, double degrees,
                                              double scaleX, double scaleY);
}
