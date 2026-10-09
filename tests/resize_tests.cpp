#include "velyntora/LayerDocument.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 LayerDocument d(3,2);d.renameLayer(0,"Base");d.replaceActivePixels({1,2,3,4,5,6});d.addLayer("Arriba");d.replaceActivePixels({7,8,9,10,11,12});d.setVisible(1,false);d.setOpacity(1,.5f);
 auto expanded=d.resized(5,4,false);assert(expanded.layerCount()==2&&expanded.activeIndex()==1);
 assert(expanded.layer(0).pixels[0]==1&&expanded.layer(0).pixels[5]==4&&expanded.layer(0).pixels[4]==0&&expanded.layer(0).pixels[19]==0);
 assert(!expanded.layer(1).visible&&expanded.layer(1).opacity==.5f&&expanded.layer(0).name=="Base");
 auto scaled=d.resized(6,4,true);assert(scaled.layer(0).pixels[0]==1&&scaled.layer(0).pixels[1]==1&&scaled.layer(0).pixels[2]==2&&scaled.layer(0).pixels[23]==6);
 auto crop=d.cropped(1,0,2,2);assert(crop.width()==2&&crop.height()==2&&crop.activeIndex()==1);assert((crop.layer(0).pixels==std::vector<std::uint32_t>{2,3,5,6}));
 bool failed=false;try{d.cropped(2,0,2,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);assert(d.width()==3&&d.layer(0).pixels[0]==1);
}
