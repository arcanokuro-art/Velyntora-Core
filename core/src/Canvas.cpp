#include "velyntora/Canvas.hpp"
#include <algorithm>
#include <cmath>
#include <stdexcept>
namespace velyntora {
Canvas::Canvas(int w,int h):width_(w),height_(h){
 if(w<=0||h<=0||static_cast<std::uint64_t>(w)*h>16000000ULL) throw std::invalid_argument("Invalid canvas size");
 pixels_.resize(static_cast<std::size_t>(w)*h,0xFFFFFFFFu);
}
void Canvas::clear(std::uint32_t color){std::fill(pixels_.begin(),pixels_.end(),color);}
void Canvas::dab(float x,float y,float radius,std::uint32_t color){
 if(!std::isfinite(x)||!std::isfinite(y)||!std::isfinite(radius)||radius<=0) return;
 radius=std::min(radius,2048.f);
 int left=std::max(0,static_cast<int>(std::floor(x-radius)));
 int top=std::max(0,static_cast<int>(std::floor(y-radius)));
 int right=std::min(width_-1,static_cast<int>(std::ceil(x+radius)));
 int bottom=std::min(height_-1,static_cast<int>(std::ceil(y+radius)));
 for(int py=top;py<=bottom;++py) for(int px=left;px<=right;++px){
  float dx=(px+0.5f)-x,dy=(py+0.5f)-y;
  if(dx*dx+dy*dy<=radius*radius) pixels_[static_cast<std::size_t>(py)*width_+px]=color;
 }
}
void Canvas::stroke(float x0,float y0,float x1,float y1,float radius,std::uint32_t color){
 if(!std::isfinite(x0)||!std::isfinite(y0)||!std::isfinite(x1)||!std::isfinite(y1)||!std::isfinite(radius)||radius<=0)return;
 float dx=x1-x0,dy=y1-y0,dist=std::hypot(dx,dy),step=std::max(0.5f,radius*0.5f);
 int count=static_cast<int>(std::ceil(dist/step));
 if(count>100000)return;
 for(int i=0;i<=count;++i){float t=count==0?0.f:static_cast<float>(i)/count;dab(x0+dx*t,y0+dy*t,radius,color);}
}
}
