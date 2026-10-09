// Exercise the exact JNI bridge used by the APK, without an Android runtime.
#include "../app/src/main/cpp/native_bridge.cpp"
#include <cassert>
#include <fcntl.h>
#include <cstdio>
int main(){
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,800,800));
 assert(Java_art_velyntora_core_DrawingView_nativeAddLayer(nullptr,nullptr));
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,20,20,20,20,4,0x80123456);
 Java_art_velyntora_core_DrawingView_nativeSetLayerOpacity(nullptr,nullptr,.375f);
 auto expected=*layers;
 char path[]="/tmp/velyntora-project-XXXXXX";int fd=mkstemp(path);assert(fd>=0);unlink(path);
 assert(Java_art_velyntora_core_DrawingView_nativeSaveProject(nullptr,nullptr,fd));
 Java_art_velyntora_core_DrawingView_nativeClear(nullptr,nullptr);
 assert(layers->layerCount()==1);assert(lseek(fd,0,SEEK_SET)==0);
 assert(Java_art_velyntora_core_DrawingView_nativeOpenProject(nullptr,nullptr,fd));
 assert(layers->layerCount()==2&&layers->activeIndex()==1);assert(layers->flatten()==expected.flatten());
 assert(undoStack.empty()&&redoStack.empty());
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,30,30,30,30,4,0xffabcdef);
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));assert(layers->flatten()==expected.flatten());
 assert(Java_art_velyntora_core_DrawingView_nativeRedo(nullptr,nullptr));
 auto edited=layers->flatten();auto history=undoStack.size();
 assert(ftruncate(fd,10)==0);assert(lseek(fd,0,SEEK_SET)==0);
 assert(!Java_art_velyntora_core_DrawingView_nativeOpenProject(nullptr,nullptr,fd));
 assert(layers->flatten()==edited&&undoStack.size()==history);
 assert(!Java_art_velyntora_core_DrawingView_nativeSaveProject(nullptr,nullptr,-1));close(fd);
}
