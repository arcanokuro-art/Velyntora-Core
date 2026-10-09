#pragma once
#include <array>
#include <vector>
#include <cstdint>
namespace velyntora {
struct ChannelLevels {int inputBlack=0,inputWhite=255,gamma=100,outputBlack=0,outputWhite=255;};
std::vector<std::uint32_t> applyCurve(const std::vector<std::uint32_t>& pixels,const std::array<int,256>& curve,int channel);
std::vector<std::uint32_t> applyLevels(const std::vector<std::uint32_t>& pixels,const std::array<ChannelLevels,3>& levels);
std::vector<std::uint32_t> autoLevels(const std::vector<std::uint32_t>& pixels,int clipPermille);
std::vector<std::uint32_t> posterizeRgb(const std::vector<std::uint32_t>& pixels,const std::array<int,3>& levels);
std::vector<std::uint32_t> hueSaturation(const std::vector<std::uint32_t>& pixels,int hue,int saturation,int lightness);
}
