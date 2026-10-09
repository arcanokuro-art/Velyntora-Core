#include "velyntora/ArtisticEffects.hpp"
#include "velyntora/BlurEffects.hpp"
#include <algorithm>
#include <array>
#include <cmath>
#include <stdexcept>
namespace velyntora {
namespace {
int channel(std::uint32_t p,int shift){return (p>>shift)&255;}
int luminance(std::uint32_t p){return (299*channel(p,16)+587*channel(p,8)+114*channel(p,0)+500)/1000;}
int mix(int old,int value,int strength){return std::clamp((old*(100-strength)+value*strength+50)/100,0,255);}
}
std::vector<std::uint32_t> artisticEffect(const std::vector<std::uint32_t>& src,int w,int h,int kind,int strength,int radius,int threshold){
 if(w<1||h<1||std::int64_t(w)*h!=std::int64_t(src.size())||kind<0||kind>6||strength<0||strength>100||radius<1||radius>16||threshold<0||threshold>255||(kind==0&&(radius>8||strength<2||strength>32))||(kind==5&&radius>4))throw std::invalid_argument("Invalid artistic parameters");
 if(strength==0&&kind!=5)return src;
 auto out=src;
 std::vector<std::uint32_t> blurred;
 if(kind==1||kind==3||kind==4)blurred=blurEffect(src,w,h,0,radius,0,50,50);
 for(int y=0;y<h;y++)for(int x=0;x<w;x++){
  auto pos=std::size_t(y)*w+x;auto p=src[pos];if(!(p>>24))continue;std::uint32_t result=p&0xff000000;
  if(kind==0){
   std::array<std::uint64_t,32> weights{};std::array<std::array<std::uint64_t,3>,32> colors{};
   for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){auto neighbor=src[std::size_t(std::clamp(y+dy,0,h-1))*w+std::clamp(x+dx,0,w-1)];auto alpha=neighbor>>24;if(!alpha)continue;int bin=luminance(neighbor)*(strength-1)/255;weights[bin]+=alpha;for(int c=0;c<3;c++)colors[bin][c]+=channel(neighbor,16-c*8)*alpha;}
   int best=int(std::max_element(weights.begin(),weights.end())-weights.begin());for(int c=0;c<3;c++)result|=std::uint32_t((colors[best][c]+weights[best]/2)/weights[best])<<(16-c*8);
  }else if(kind==5){
   for(int shift:{0,8,16}){std::array<int,256> histogram{};int total=0;for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){auto neighbor=src[std::size_t(std::clamp(y+dy,0,h-1))*w+std::clamp(x+dx,0,w-1)];if(neighbor>>24){++histogram[channel(neighbor,shift)];++total;}}int target=(total-1)*strength/100,acc=0,value=0;for(;value<255;value++){acc+=histogram[value];if(acc>target)break;}result|=std::uint32_t(value)<<shift;}
  }else if(kind==1){
   int base=luminance(p),smooth=luminance(blurred[pos]);int sketch=std::min(255,(base*255+std::max(1,smooth)/2)/std::max(1,smooth));for(int shift:{0,8,16})result|=std::uint32_t(mix(channel(p,shift),sketch,strength))<<shift;
  }else if(kind==2){
   int gx=0,gy=0;constexpr int kx[]={-1,0,1,-2,0,2,-1,0,1},ky[]={-1,-2,-1,0,0,0,1,2,1};int k=0;
   for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){auto neighbor=src[std::size_t(std::clamp(y+dy,0,h-1))*w+std::clamp(x+dx,0,w-1)];int value=(neighbor>>24)?luminance(neighbor):luminance(p);gx+=value*kx[k];gy+=value*ky[k++];}
   bool edge=std::hypot(gx,gy)/4>threshold;for(int shift:{0,8,16}){int old=channel(p,shift),ink=edge?0:((old*5+127)/255)*51;result|=std::uint32_t(mix(old,ink,strength))<<shift;}
  }else if(kind==6){
   int r=channel(p,16),g=channel(p,8),b=channel(p,0);if(r>=threshold&&r*2>3*std::max(g,b))r=mix(r,(g+b)/2,strength);result|=std::uint32_t(r)<<16|std::uint32_t(g)<<8|std::uint32_t(b);
  }else{
   for(int shift:{0,8,16}){int old=channel(p,shift),soft=channel(blurred[pos],shift),value=255-((255-old)*(255-soft)+127)/255;
    if(kind==4){int warm=std::clamp(soft+(shift==16?8:shift==0?-8:0),0,255);value=(old+warm)/2;}
    result|=std::uint32_t(mix(old,value,strength))<<shift;
   }
  }
  out[pos]=result;
 }return out;
}
}
