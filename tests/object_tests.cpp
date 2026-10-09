#include "velyntora/ObjectEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){std::vector<std::uint32_t> point(25);point[6]=0x80ff0000;auto center=objectEffect(point,5,5,0,4,0,0,false);assert(center[12]==point[6]&&center[6]==0);auto bottom=objectEffect(point,5,5,0,8,0,0,false);assert(bottom[24]==point[6]);std::vector<std::uint8_t> mask(25);for(int y=1;y<4;y++)for(int x=1;x<4;x++)mask[y*5+x]=1;auto aligned=objectEffect(point,5,5,0,8,0,0,false,&mask);assert(aligned[18]==point[6]);
 std::vector<std::uint32_t> square(25);for(int y=1;y<4;y++)for(int x=1;x<4;x++)square[y*5+x]=0xffff0000;auto feather=objectEffect(square,5,5,1,2,0,0,false);assert(feather[12]==0xffff0000&&(feather[6]>>24)==128&&feather[0]==0);auto outline=objectEffect(point,5,5,2,1,0,0xff00ff00,false);assert(outline[1]==0xff00ff00&&outline[5]==0xff00ff00&&outline[0]==0);assert((outline[6]>>24)==255);
 std::vector<std::uint32_t> solid(25,0xff123456);assert(objectEffect(solid,5,5,1,2,0,0,false)==solid);assert((objectEffect(solid,5,5,1,2,0,0,true)[0]>>24)==0);for(int kind=0;kind<3;kind++)assert(objectEffect(std::vector<std::uint32_t>(25),5,5,kind,2,0,0xff00ff00,false)==std::vector<std::uint32_t>(25));
 bool invalid=false;try{objectEffect(point,5,5,0,9,0,0,false);}catch(const std::invalid_argument&){invalid=true;}assert(invalid);
}
