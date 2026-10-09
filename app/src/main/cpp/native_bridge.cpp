#include <jni.h>
#include <cmath>
#include <algorithm>
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
struct Snapshot { velyntora::LayerDocument layers; };
std::vector<Snapshot> undoStack, redoStack;
constexpr std::size_t limit=15;
// Cap history memory across both stacks to avoid exhausting Android heap
// when many full-resolution layers are present.
constexpr std::size_t historyBudget=96ULL*1024*1024;
std::size_t snapshotBytes(const Snapshot& snapshot){
 std::size_t total=0;
 for(std::size_t i=0;i<snapshot.layers.layerCount();++i)
  total+=snapshot.layers.layer(i).pixels.size()*sizeof(std::uint32_t);
 return total;
}
std::size_t historyBytes(){
 std::size_t total=0;
 for(const auto& s:undoStack)total+=snapshotBytes(s);
 for(const auto& s:redoStack)total+=snapshotBytes(s);
 return total;
}
void trimHistory(){
 while(undoStack.size()>limit)undoStack.erase(undoStack.begin());
 while(redoStack.size()>limit)redoStack.erase(redoStack.begin());
 while(historyBytes()>historyBudget){
  if(!undoStack.empty())undoStack.erase(undoStack.begin());
  else if(!redoStack.empty())redoStack.erase(redoStack.begin());
  else break;
 }
}

void checkpoint(){
 if(!canvas)return;
 undoStack.push_back({*layers});
 redoStack.clear();
 trimHistory();
}
void storeActive(){
 if(layers&&canvas)layers->replaceActivePixels(canvas->pixels());
}
void loadActive(){
 if(layers&&canvas)canvas->setPixels(layers->layer(layers->activeIndex()).pixels);
}
void resetHistory(){undoStack.clear();redoStack.clear();}
void restore(const Snapshot& snapshot){*layers=snapshot.layers;loadActive();}
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
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeClear(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 checkpoint();
 // Preserve the document's dimensions while resetting its layers and canvas.
 auto freshCanvas=std::make_unique<velyntora::Canvas>(canvas->width(),canvas->height());
 auto freshLayers=std::make_unique<velyntora::LayerDocument>(canvas->width(),canvas->height());
 canvas=std::move(freshCanvas);
 layers=std::move(freshLayers);
 return JNI_TRUE;
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
 if(kind==7)canvas->rectangle(x0,y0,x1,y1,static_cast<std::uint32_t>(color),true);
 if(kind==8)canvas->ellipse(x0,y0,x1,y1,static_cast<std::uint32_t>(color),true);
 storeActive();
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeFill(JNIEnv*,jclass,jint x,jint y,jint color){
 std::lock_guard<std::mutex> lock(guard);
 if(canvas){canvas->fill(x,y,static_cast<std::uint32_t>(color));storeActive();}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUndo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||undoStack.empty())return JNI_FALSE;
 if(redoStack.size()==limit)redoStack.erase(redoStack.begin());
 redoStack.push_back({*layers});restore(undoStack.back());undoStack.pop_back();trimHistory();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRedo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||redoStack.empty())return JNI_FALSE;
 if(undoStack.size()==limit)undoStack.erase(undoStack.begin());
 undoStack.push_back({*layers});restore(redoStack.back());redoStack.pop_back();trimHistory();return JNI_TRUE;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return nullptr;
 const auto pixels=layers->flatten();
 jintArray result=env->NewIntArray(static_cast<jsize>(pixels.size()));
 if(result)env->SetIntArrayRegion(result,0,static_cast<jsize>(pixels.size()),reinterpret_cast<const jint*>(pixels.data()));
 return result;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeCopySelection(JNIEnv* env,jclass,jint kind,jint x0,jint y0,jint x1,jint y1){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!(kind==9||kind==10))return nullptr;
 const int left=std::max(0,std::min(x0,x1)),right=std::min(canvas->width(),std::max(x0,x1));
 const int top=std::max(0,std::min(y0,y1)),bottom=std::min(canvas->height(),std::max(y0,y1));
 if(left>=right||top>=bottom)return nullptr;
 const int width=right-left,height=bottom-top;
 if(width>8000||height>8000)return nullptr;
 std::vector<jint> data(static_cast<std::size_t>(width)*height+2,0);
 data[0]=width;data[1]=height;
 const auto& pixels=canvas->pixels();
 const double cx=(left+right)/2.0,cy=(top+bottom)/2.0;
 const double rx=width/2.0,ry=height/2.0;
 for(int py=top;py<bottom;++py)for(int px=left;px<right;++px){
  if(kind==9||((px+0.5-cx)*(px+0.5-cx)/(rx*rx)+(py+0.5-cy)*(py+0.5-cy)/(ry*ry)<=1.0))
   data[2+static_cast<std::size_t>(py-top)*width+(px-left)]=static_cast<jint>(pixels[static_cast<std::size_t>(py)*canvas->width()+px]);
 }
 jintArray result=env->NewIntArray(static_cast<jsize>(data.size()));
 if(result)env->SetIntArrayRegion(result,0,static_cast<jsize>(data.size()),data.data());
 return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeEraseSelection(JNIEnv*,jclass,jint kind,jint x0,jint y0,jint x1,jint y1){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!(kind==9||kind==10))return JNI_FALSE;
 const int left=std::max(0,std::min(x0,x1)),right=std::min(canvas->width(),std::max(x0,x1));
 const int top=std::max(0,std::min(y0,y1)),bottom=std::min(canvas->height(),std::max(y0,y1));
 if(left>=right||top>=bottom)return JNI_FALSE;
 checkpoint();
 auto pixels=canvas->pixels();
 const double cx=(left+right)/2.0,cy=(top+bottom)/2.0;
 const double rx=(right-left)/2.0,ry=(bottom-top)/2.0;
 for(int py=top;py<bottom;++py)for(int px=left;px<right;++px){
  if(kind==9||((px+0.5-cx)*(px+0.5-cx)/(rx*rx)+(py+0.5-cy)*(py+0.5-cy)/(ry*ry)<=1.0))
   pixels[static_cast<std::size_t>(py)*canvas->width()+px]=0u;
 }
 canvas->setPixels(pixels);
 storeActive();
 return JNI_TRUE;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativePickColor(JNIEnv*,jclass,jint x,jint y){
 std::lock_guard<std::mutex> lock(guard);
 if(!layers||x<0||y<0||x>=layers->width()||y>=layers->height())return 0;
 const auto pixels=layers->flatten();
 return static_cast<jint>(pixels[static_cast<std::size_t>(y)*layers->width()+x]);
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
 checkpoint();layers->addLayer("Capa "+std::to_string(layers->layerCount()+1));loadActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSelectLayer(JNIEnv*,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);if(!layers||index<0||!layers->selectLayer(static_cast<std::size_t>(index)))return JNI_FALSE;
 loadActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeDeleteLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers||layers->layerCount()==1)return JNI_FALSE;
 checkpoint();if(!layers->removeLayer(layers->activeIndex()))return JNI_FALSE;
 loadActive();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeToggleLayer(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return JNI_FALSE;
 const auto i=layers->activeIndex();checkpoint();return layers->setVisible(i,!layers->layer(i).visible)?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLayerVisible(JNIEnv*,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);return layers&&index>=0&&static_cast<std::size_t>(index)<layers->layerCount()&&layers->layer(index).visible?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMoveLayer(JNIEnv*,jclass,jint direction){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return JNI_FALSE;
 int from=static_cast<int>(layers->activeIndex()),to=from+direction;
 if(to<0||to>=static_cast<int>(layers->layerCount()))return JNI_FALSE;
 checkpoint();if(!layers->moveLayer(static_cast<std::size_t>(from),static_cast<std::size_t>(to)))return JNI_FALSE;
 loadActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSetLayerOpacity(JNIEnv*,jclass,jfloat opacity){
 std::lock_guard<std::mutex> lock(guard);if(!layers)return JNI_FALSE;
 const auto i=layers->activeIndex();
 if(!std::isfinite(opacity)||opacity<0.f||opacity>1.f)return JNI_FALSE;
 if(layers->layer(i).opacity==opacity)return JNI_TRUE;
 checkpoint();return layers->setOpacity(i,opacity)?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jfloat JNICALL Java_art_velyntora_core_DrawingView_nativeLayerOpacity(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);return layers?layers->layer(layers->activeIndex()).opacity:1.f;
}

extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeLayerThumbnail(JNIEnv* env,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);
 if(!layers||index<0||static_cast<std::size_t>(index)>=layers->layerCount())return nullptr;
 constexpr int side=48;
 const auto& layer=layers->layer(static_cast<std::size_t>(index));
 const int w=layers->width(),h=layers->height();
 if(w<=0||h<=0)return nullptr;
 std::vector<jint> preview(side*side);
 for(int y=0;y<side;++y){
  const int sy=static_cast<int>((static_cast<std::int64_t>(y)*h)/side);
  for(int x=0;x<side;++x){
   const int sx=static_cast<int>((static_cast<std::int64_t>(x)*w)/side);
   preview[y*side+x]=static_cast<jint>(layer.pixels[static_cast<std::size_t>(sy)*w+sx]);
  }
 }
 jintArray result=env->NewIntArray(side*side);
 if(result)env->SetIntArrayRegion(result,0,side*side,preview.data());
 return result;
}
