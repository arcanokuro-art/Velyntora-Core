#include "velyntora/Canvas.hpp"
#include <cassert>
#include <stdexcept>
int main(){velyntora::Canvas c(32,32);assert(c.pixels().size()==1024);assert(c.pixels()[0]==0xFFFFFFFFu);c.stroke(10,10,10,10,3,0xFF000000u);assert(c.pixels()[10*32+10]==0xFF000000u);c.clear(0xFFFFFFFFu);assert(c.pixels()[10*32+10]==0xFFFFFFFFu);bool failed=false;try{velyntora::Canvas bad(0,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);}
