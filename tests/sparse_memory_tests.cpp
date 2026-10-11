#include "velyntora/Canvas.hpp"
#include "velyntora/LayerDocument.hpp"
#include "../core/document/tiles/StrokeTiles.hpp"
#include <cassert>
#include <iostream>
int main(){
 using namespace velyntora;
 LayerDocument document(3034,4515);
 assert(document.layer(0).pixels.allocatedPixels()==0);
 for(int i=0;i<31;++i)document.addLayer("Capa");
 for(int i=0;i<32;++i)assert(document.layer(i).pixels.allocatedPixels()==0);
 auto before=document;
 tiles::LayerPixels p(std::size_t(3034)*4515,0);
 p[123456]=0xff123456;
 auto original=p;
 p[123456]=0xffaabbcc;
 assert(original[123456]==0xff123456 && p[123456]==0xffaabbcc);
 assert(p.allocatedPixels()==4096 && original.allocatedPixels()==4096);
 Canvas canvas(800,800);canvas.clear(0);
 canvas.beginOpacityStroke();canvas.strokeStyled(255,255,265,260,3,0xff123456,.5,1,false,false);
 Canvas copied=canvas;
 copied.strokeStyled(255,255,265,260,3,0xffabcdef,.8,1,false,false);
 assert(canvas.pixels()!=copied.pixels());
 Canvas assigned(1,1);assigned=copied;
 assigned.strokeStyled(260,260,280,270,3,0xffffffff,.8,1,false,false);
 assert(assigned.pixels()!=copied.pixels());
 std::vector<std::uint32_t> large(16000000,0xffffffff);
 tiles::StrokeTiles stroke;stroke.begin(large);
 assert(stroke.allocatedBlocks()==0);
 stroke.coverage(70000)=.5f;large[70000]=0;
 assert(stroke.base(70000)==0xffffffff && stroke.allocatedBlocks()==1);
 stroke.end();assert(stroke.size()==0 && stroke.allocatedBlocks()==0);
 std::cout<<"Sparse layers, COW snapshots, canvas copies and regional opacity passed\n";
}
