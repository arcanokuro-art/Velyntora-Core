#pragma once
#include <vector>
#include <cstdint>
namespace velyntora {
// Clouds, Voronoi, cells, Mandelbrot, Julia. Generated pixels replace selected layer pixels.
std::vector<std::uint32_t> renderEffect(int width,int height,int kind,int scale,int detail,std::uint32_t seed,std::uint32_t first,std::uint32_t second);
}
