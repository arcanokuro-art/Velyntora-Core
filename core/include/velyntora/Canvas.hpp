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
 void strokeStyled(float x0,float y0,float x1,float y1,float radius,std::uint32_t argb,float opacity,float hardness,bool square,bool eraser);
 void rectangle(int x0,int y0,int x1,int y1,std::uint32_t argb,bool filled);
 void ellipse(int x0,int y0,int x1,int y1,std::uint32_t argb,bool filled);
 void fill(int x,int y,std::uint32_t argb);
 void setPixels(const std::vector<std::uint32_t>& pixels);
private:
 void dab(float x,float y,float radius,std::uint32_t argb);
 void pixel(int x,int y,std::uint32_t argb);
 int width_,height_;
 std::vector<std::uint32_t> pixels_;
};
}
