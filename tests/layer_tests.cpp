#include "velyntora/LayerDocument.hpp"
#include <cassert>
#include <vector>
int main(){
 velyntora::LayerDocument doc(4,4);
 assert(doc.layerCount()==1);
 assert(doc.flatten()[0]==0xFFFFFFFFu);
 doc.addLayer("Dibujo");
 assert(doc.layerCount()==2&&doc.activeIndex()==1);
 std::vector<std::uint32_t> pixels(16,0);
 pixels[0]=0xFFFF0000u;
 doc.replaceActivePixels(pixels);
 assert(doc.flatten()[0]==0xFFFF0000u);
 assert(doc.flatten()[1]==0xFFFFFFFFu);
 assert(doc.setOpacity(1,0.5f));
 assert(doc.flatten()[0]==0xFFFF7F7Fu||doc.flatten()[0]==0xFFFF8080u);
 assert(doc.setVisible(1,false));
 assert(doc.flatten()[0]==0xFFFFFFFFu);
 assert(doc.setVisible(1,true));
 assert(doc.moveLayer(1,0));
 assert(doc.activeIndex()==0);
 assert(doc.removeLayer(0));
 assert(doc.layerCount()==1);
 assert(!doc.removeLayer(0));
}
