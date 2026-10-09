#include "velyntora/Canvas.hpp"
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>
namespace velyntora {
Canvas::Canvas(int w,int h):width_(w),height_(h){
 if(w<=0||h<=0||static_cast<std::uint64_t>(w)*h>16000000ULL) throw std::invalid_argument("Invalid canvas size");
 pixels_.resize(static_cast<std::size_t>(w)*h,0xFFFFFFFFu);
}
void Canvas::clear(std::uint32_t color){std::fill(pixels_.begin(),pixels_.end(),color);}
void Canvas::setPixels(const std::vector<std::uint32_t>& pixels){
 if(pixels.size()!=pixels_.size())throw std::invalid_argument("Pixel count mismatch");
 pixels_=pixels;
}
void Canvas::pixel(int x,int y,std::uint32_t color){
 if(x>=0&&y>=0&&x<width_&&y<height_)pixels_[static_cast<std::size_t>(y)*width_+x]=color;
}
void Canvas::dab(float x,float y,float radius,std::uint32_t color){
 if(!std::isfinite(x)||!std::isfinite(y)||!std::isfinite(radius)||radius<=0)return;
 radius=std::min(radius,2048.f);
 if(x+radius<0.f||y+radius<0.f||x-radius>=width_||y-radius>=height_)return;
 int left=static_cast<int>(std::max(0.f,std::floor(x-radius)));
 int top=static_cast<int>(std::max(0.f,std::floor(y-radius)));
 int right=static_cast<int>(std::min(static_cast<float>(width_-1),std::ceil(x+radius)));
 int bottom=static_cast<int>(std::min(static_cast<float>(height_-1),std::ceil(y+radius)));
 for(int py=top;py<=bottom;++py)for(int px=left;px<=right;++px){
  float dx=(px+0.5f)-x,dy=(py+0.5f)-y;
  if(dx*dx+dy*dy<=radius*radius)pixel(px,py,color);
 }
}
void Canvas::stroke(float x0,float y0,float x1,float y1,float radius,std::uint32_t color){
 if(!std::isfinite(x0)||!std::isfinite(y0)||!std::isfinite(x1)||!std::isfinite(y1)||!std::isfinite(radius)||radius<=0)return;
 float dx=x1-x0,dy=y1-y0,dist=std::hypot(dx,dy),step=std::max(0.5f,radius*0.5f);
 if(!std::isfinite(dist)||dist/step>100000.f)return;
 int count=static_cast<int>(std::ceil(dist/step));
 for(int i=0;i<=count;++i){float t=count==0?0.f:static_cast<float>(i)/count;dab(x0+dx*t,y0+dy*t,radius,color);}
}
void Canvas::rectangle(int x0,int y0,int x1,int y1,std::uint32_t color,bool filled){
 int left=std::max(0,std::min(x0,x1)),right=std::min(width_-1,std::max(x0,x1));
 int top=std::max(0,std::min(y0,y1)),bottom=std::min(height_-1,std::max(y0,y1));
 if(left>right||top>bottom)return;
 for(int y=top;y<=bottom;++y)for(int x=left;x<=right;++x)
  if(filled||y==top||y==bottom||x==left||x==right)pixel(x,y,color);
}
void Canvas::ellipse(int x0,int y0,int x1,int y1,std::uint32_t color,bool filled){
 int left=std::max(0,std::min(x0,x1)),right=std::min(width_-1,std::max(x0,x1));
 int top=std::max(0,std::min(y0,y1)),bottom=std::min(height_-1,std::max(y0,y1));
 double cx=(static_cast<double>(x0)+x1)/2.,cy=(static_cast<double>(y0)+y1)/2.;
 double rx=std::abs(static_cast<double>(x1)-x0)/2.,ry=std::abs(static_cast<double>(y1)-y0)/2.;
 if(rx<0.5||ry<0.5)return;
 for(int y=top;y<=bottom;++y)for(int x=left;x<=right;++x){
  double dx=(x-cx)/rx,dy=(y-cy)/ry,v=dx*dx+dy*dy;
  if(v<=1. && (filled||v>=std::pow(std::max(0.,1.-1./std::max(1.,std::min(rx,ry))),2)))pixel(x,y,color);
 }
}
void Canvas::fill(int x,int y,std::uint32_t color){
 if(x<0||y<0||x>=width_||y>=height_)return;
 const std::uint32_t target=pixels_[static_cast<std::size_t>(y)*width_+x];
 if(target==color)return;
 // Scanline flood fill: each seed expands to a horizontal run, avoiding
 // the four-neighbor push explosion of a pixel-by-pixel DFS.
 std::vector<std::pair<int,int>> pending{{x,y}};
 while(!pending.empty()){
  auto [sx,sy]=pending.back();pending.pop_back();
  if(sx<0||sx>=width_||sy<0||sy>=height_)continue;
  const std::size_t row=static_cast<std::size_t>(sy)*width_;
  if(pixels_[row+sx]!=target)continue;
  int left=sx,right=sx;
  while(left>0&&pixels_[row+left-1]==target)--left;
  while(right+1<width_&&pixels_[row+right+1]==target)++right;
  for(int px=left;px<=right;++px)pixels_[row+px]=color;
  for(int ny : {sy-1,sy+1}){
   if(ny<0||ny>=height_)continue;
   const std::size_t adjacent=static_cast<std::size_t>(ny)*width_;
   bool inRun=false;
   for(int px=left;px<=right;++px){
    const bool matches=pixels_[adjacent+px]==target;
    if(matches&&!inRun){pending.emplace_back(px,ny);inRun=true;}
    else if(!matches)inRun=false;
   }
  }
 }
}
}
