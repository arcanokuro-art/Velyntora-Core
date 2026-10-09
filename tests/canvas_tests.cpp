#include "velyntora/Canvas.hpp"
#include <cassert>
#include <stdexcept>
int main(){
 velyntora::Canvas c(32,32);
 assert(c.pixels().size()==1024);
 assert(c.pixels()[0]==0xFFFFFFFFu);
 c.stroke(10,10,10,10,3,0xFF000000u);
 assert(c.pixels()[10*32+10]==0xFF000000u);
 c.clear(0xFFFFFFFFu);
 assert(c.pixels()[10*32+10]==0xFFFFFFFFu);
 c.rectangle(2,2,12,12,0xFFFF0000u,false);
 assert(c.pixels()[2*32+2]==0xFFFF0000u);
 assert(c.pixels()[5*32+5]==0xFFFFFFFFu);
 c.fill(5,5,0xFF00FF00u);
 assert(c.pixels()[5*32+5]==0xFF00FF00u);
 assert(c.pixels()[0]==0xFFFFFFFFu);
 c.ellipse(15,15,25,25,0xFF0000FFu,true);
 assert(c.pixels()[20*32+20]==0xFF0000FFu);
 std::vector<std::uint32_t> snapshot=c.pixels();
 c.clear(0xFFFFFFFFu);c.setPixels(snapshot);
 assert(c.pixels()[20*32+20]==0xFF0000FFu);
 bool failed=false;try{velyntora::Canvas bad(0,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);
}
