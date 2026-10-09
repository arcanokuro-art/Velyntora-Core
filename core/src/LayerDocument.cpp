#include "velyntora/LayerDocument.hpp"
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

namespace velyntora {
LayerDocument::LayerDocument(int w,int h):width_(w),height_(h){
 if(w<=0||h<=0||static_cast<std::uint64_t>(w)*h>16000000ULL)throw std::invalid_argument("Invalid dimensions");
 layers_.push_back({"Fondo",std::vector<std::uint32_t>(static_cast<std::size_t>(w)*h,0xFFFFFFFFu),true,1.f});
}
void LayerDocument::addLayer(const std::string& name){
 layers_.insert(layers_.begin()+static_cast<std::ptrdiff_t>(active_+1),
  Layer{name,std::vector<std::uint32_t>(static_cast<std::size_t>(width_)*height_,0u),true,1.f});
 ++active_;
}
bool LayerDocument::removeLayer(std::size_t index){
 if(index>=layers_.size()||layers_.size()==1)return false;
 layers_.erase(layers_.begin()+static_cast<std::ptrdiff_t>(index));
 if(active_>=layers_.size())active_=layers_.size()-1;
 else if(index<active_)--active_;
 return true;
}
bool LayerDocument::selectLayer(std::size_t index){
 if(index>=layers_.size())return false;active_=index;return true;
}
bool LayerDocument::moveLayer(std::size_t from,std::size_t to){
 if(from>=layers_.size()||to>=layers_.size())return false;
 if(from==to)return true;
 Layer moved=std::move(layers_[from]);
 layers_.erase(layers_.begin()+static_cast<std::ptrdiff_t>(from));
 layers_.insert(layers_.begin()+static_cast<std::ptrdiff_t>(to),std::move(moved));
 if(active_==from)active_=to;
 else if(from<active_&&to>=active_)--active_;
 else if(from>active_&&to<=active_)++active_;
 return true;
}
bool LayerDocument::setVisible(std::size_t index,bool visible){
 if(index>=layers_.size())return false;layers_[index].visible=visible;return true;
}
bool LayerDocument::setOpacity(std::size_t index,float opacity){
 if(index>=layers_.size()||!std::isfinite(opacity)||opacity<0.f||opacity>1.f)return false;
 layers_[index].opacity=opacity;return true;
}
void LayerDocument::replaceActivePixels(const std::vector<std::uint32_t>& pixels){
 if(pixels.size()!=layers_[active_].pixels.size())throw std::invalid_argument("Pixel count mismatch");
 layers_[active_].pixels=pixels;
}
std::vector<std::uint32_t> LayerDocument::flatten() const{
 std::vector<std::uint32_t> result(static_cast<std::size_t>(width_)*height_,0u);
 for(const Layer& layer:layers_){
  if(!layer.visible||layer.opacity<=0.f)continue;
  for(std::size_t i=0;i<result.size();++i){
   const std::uint32_t src=layer.pixels[i],dst=result[i];
   const float sa=((src>>24)&255)/255.f*layer.opacity;
   const float da=((dst>>24)&255)/255.f;
   const float oa=sa+da*(1.f-sa);
   if(oa<=0.f){result[i]=0;continue;}
   std::uint32_t channels=0;
   for(int shift: {0,8,16}){
    float s=static_cast<float>((src>>shift)&255),d=static_cast<float>((dst>>shift)&255);
    int v=static_cast<int>(std::lround((s*sa+d*da*(1.f-sa))/oa));
    channels|=static_cast<std::uint32_t>(std::clamp(v,0,255))<<shift;
   }
   result[i]=(static_cast<std::uint32_t>(std::clamp(static_cast<int>(std::lround(oa*255)),0,255))<<24)|channels;
  }
 }
 return result;
}
}
