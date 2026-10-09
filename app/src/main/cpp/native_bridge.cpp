#include <jni.h>
#include <memory>
#include <mutex>
#include <vector>
#include <string>
#include "velyntora/Canvas.hpp"
#include "velyntora/LayerDocument.hpp"

namespace {
std::mutex guard;
std::unique_ptr<velyntora::Canvas> canvas;
std::unique_ptr<velyntora::LayerDocument> layers;
std::vector<std::vector<std::uint32_t>> undoStack, redoStack;
constexpr std::size_t limit=15;

void checkpoint(){
 if(!canvas)return;
 if(undoStack.size()==limit)undoStack.erase(undoStack.begin());
 undoStack.push_back(canvas->pixels());
 redoStack.clear();
}
void storeActive(){
 if(layers&&canvas)layers->replaceActivePixels(canvas->pixels());
}
void loadActive(){
 if(layers&&canvas)canvas->setPixels(layers->layer(layers->activeIndex()).pixels);
}
void resetHistory(){undoStack.clear();redoStack.clear();}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCreate(JNIEnv*,jclass,jint w,jint h){
 std::lock_guard<std::mutex> lock(guard);
 try{
  auto newCanvas=std::make_unique<velyntora::Canvas>(w,h);
  auto newLayers=std::make_unique<velyntora::LayerDocument>(w,h);
  canvas=std::move(newCanvas);layers=std::move(newLayers);
  resetHistory();return JNI_TRUE;
 }catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeBeginEdit(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);checkpoint();
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeStroke(JNIEnv*,jclass,jfloat x0,jfloat y0,jfloat x1,jfloat y1,jfloat r,jint color){
 std::lock_guard<std::mutex> lock(guard);
 if(canvas){canvas->stroke(x0,y0,x1,y1,r,static_cast<std::uint32_t>(color));storeActive();}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeShape(JNIEnv*,jclass,jint kind,jint x0,jint y0,jint x1,jint y1,jint color){
 std::lock_guard<std::mutex> lock(guard);if(!canvas)return;
 if(kind==1)canvas->rectangle(x0,y0,x1,y1,static_cast<std::uint32_t>(color),false);
 if(kind==2)canvas->ellipse(x0,y0,x1,y1,static_cast<std::uint32_t>(color),false);
 if(kind==3)canvas->stroke(x0,y0,x1,y1,1.f,static_cast<std::uint32_t>(color));
 storeActive();
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeFill(JNIEnv*,jclass,jint x,jint y,jint color){
 std::lock_guard<std::mutex> lock(guard);
 if(canvas){canvas->fill(x,y,static_cast<std::uint32_t>(color));storeActive();}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUndo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||undoStack.empty())return JNI_FALSE;
 redoStack.push_back(canvas->pixels());canvas->setPixels(undoStack.back());undoStack.pop_back();storeActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRedo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||redoStack.empty())return JNI_FALSE;
 undoStack.push_back(canvas->pixels());canvas->setPixels(redoStack.back());redoStack.pop_back();storeActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return nullptr;
 const auto pixels=layers->flatten();
 jintArray result=env->NewIntArray(static_cast<jsize>(pixels.size()));
 if(result)env->SetIntArrayRegion(result,0,static_cast<jsize>(pixels.size()),reinterpret_cast<const jint*>(pixels.data()));
 return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeImport(JNIEnv* env,jclass,jintArray source){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||!source)return JNI_FALSE;
 jsize count=env->GetArrayLength(source);
 if(count!=static_cast<jsize>(canvas->pixels().size()))return JNI_FALSE;
 std::vector<jint> data(static_cast<std::size_t>(count));
 env->GetIntArrayRegion(source,0,count,data.data());if(env->ExceptionCheck())return JNI_FALSE;
 checkpoint();canvas->setPixels(std::vector<std::uint32_t>(data.begin(),data.end()));storeActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeLayerCount(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);return layers?static_cast<jint>(layers->layerCount()):0;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeActiveLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);return layers?static_cast<jint>(layers->activeIndex()):-1;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeAddLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers||layers->layerCount()>=32)return JNI_FALSE;
 layers->addLayer("Capa "+std::to_string(layers->layerCount()+1));loadActive();resetHistory();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSelectLayer(JNIEnv*,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);if(!layers||index<0||!layers->selectLayer(static_cast<std::size_t>(index)))return JNI_FALSE;
 loadActive();resetHistory();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeDeleteLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers||!layers->removeLayer(layers->activeIndex()))return JNI_FALSE;
 loadActive();resetHistory();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeToggleLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return JNI_FALSE;
 const auto i=layers->activeIndex();return layers->setVisible(i,!layers->layer(i).visible)?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLayerVisible(JNIEnv*,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);return layers&&index>=0&&static_cast<std::size_t>(index)<layers->layerCount()&&layers->layer(index).visible?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMoveLayer(JNIEnv*,jclass,jint direction){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return JNI_FALSE;
 int from=static_cast<int>(layers->activeIndex()),to=from+direction;
 if(to<0||to>=static_cast<int>(layers->layerCount()))return JNI_FALSE;
 if(!layers->moveLayer(static_cast<std::size_t>(from),static_cast<std::size_t>(to)))return JNI_FALSE;
 loadActive();resetHistory();return JNI_TRUE;
}
