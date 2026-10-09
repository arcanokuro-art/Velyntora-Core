#include "velyntora/ArtisticEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 std::vector<std::uint32_t> solid(25,0x804080c0);assert(artisticEffect(solid,5,5,0,8,2,80)==solid);assert(artisticEffect(solid,5,5,5,50,2,80)==solid);
 auto impulse=std::vector<std::uint32_t>(25,0xff808080);impulse[12]=0xffffffff;assert(artisticEffect(impulse,5,5,5,50,1,80)[12]==0xff808080);assert(artisticEffect(impulse,5,5,5,100,1,80)[12]==0xffffffff);
 assert(artisticEffect({0x80ff2020,0xff2040c0},2,1,6,100,1,80)[0]==0x80202020);assert(artisticEffect({0x80ff2020,0xff2040c0},2,1,6,100,1,80)[1]==0xff2040c0);
 for(int kind=0;kind<7;kind++){auto result=artisticEffect({0x80ff0000,0x00123456,0xff204080},3,1,kind,kind==0?8:100,1,30);assert(result[1]==0x00123456);assert((result[0]>>24)==128&&(result[2]>>24)==255);if(kind!=0&&kind!=5)assert(artisticEffect(solid,5,5,kind,0,1,30)==solid);}
 auto glow=artisticEffect(solid,5,5,3,100,2,80);assert((glow[12]&255)>(solid[12]&255));auto pencil=artisticEffect(solid,5,5,1,100,2,80);assert(pencil[12]==0x80ffffff);
 auto hidden=artisticEffect({0xffff0000,0x000000ff,0xffff0000},3,1,0,8,1,80);assert(hidden[0]==0xffff0000&&hidden[2]==0xffff0000);
 bool invalid=false;try{artisticEffect(solid,5,5,0,1,2,80);}catch(const std::invalid_argument&){invalid=true;}assert(invalid);
}
