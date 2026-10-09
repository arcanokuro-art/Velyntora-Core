#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
class Canvas {
public:
 Canvas(int width,int height);
 int width() const noexcept {return width_;}
 int height() const noexcept {return height_;}
 const std::vector<std::uint32_t>& pixels() const noexcept {return pixels_;}
 void clear(std::uint32_t argb);
 void stroke(float x0,float y0,float x1,float y1,float radius,std::uint32_t argb);
private:
 void dab(float x,float y,float radius,std::uint32_t argb);
 int width_,height_;
 std::vector<std::uint32_t> pixels_;
};
}
