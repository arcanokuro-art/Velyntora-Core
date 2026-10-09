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
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMovePixels(JNIEnv*,jclass,jint kind,jint x0,jint y0,jint x1,jint y1,jint dx,jint dy){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||(kind!=9&&kind!=10))return JNI_FALSE;
 const int left=std::max(0,std::min(x0,x1)),right=std::min(canvas->width(),std::max(x0,x1));
 const int top=std::max(0,std::min(y0,y1)),bottom=std::min(canvas->height(),std::max(y0,y1));
 if(left>=right||top>=bottom||(!dx&&!dy))return JNI_FALSE;
 const int w=right-left,h=bottom-top;
 const double cx=(left+right)/2.0,cy=(top+bottom)/2.0,rx=w/2.0,ry=h/2.0;
 const auto before=canvas->pixels();
 auto after=before;
 for(int py=top;py<bottom;++py)for(int px=left;px<right;++px){
  if(kind==10&&((px+0.5-cx)*(px+0.5-cx)/(rx*rx)+(py+0.5-cy)*(py+0.5-cy)/(ry*ry)>1.0))continue;
  after[static_cast<std::size_t>(py)*canvas->width()+px]=0u;
 }
 for(int py=top;py<bottom;++py)for(int px=left;px<right;++px){
  if(kind==10&&((px+0.5-cx)*(px+0.5-cx)/(rx*rx)+(py+0.5-cy)*(py+0.5-cy)/(ry*ry)>1.0))continue;
  const std::int64_t tx=static_cast<std::int64_t>(px)+dx,ty=static_cast<std::int64_t>(py)+dy;
  if(tx<0||ty<0||tx>=canvas->width()||ty>=canvas->height())continue;
  after[static_cast<std::size_t>(ty)*canvas->width()+tx]=before[static_cast<std::size_t>(py)*canvas->width()+px];
 }
 checkpoint();
 canvas->setPixels(after);
 storeActive();
 return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePasteSelection(JNIEnv* env,jclass,jintArray source,jint x,jint y){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!source)return JNI_FALSE;
 const jsize count=env->GetArrayLength(source);
 if(count<3)return JNI_FALSE;
 jint dims[2]={0,0};
 env->GetIntArrayRegion(source,0,2,dims);
 if(env->ExceptionCheck())return JNI_FALSE;
 const int w=dims[0],h=dims[1];
 if(w<=0||h<=0||static_cast<std::int64_t>(w)*h!=static_cast<std::int64_t>(count)-2)return JNI_FALSE;
 if(x>=canvas->width()||y>=canvas->height()||static_cast<std::int64_t>(x)+w<=0||static_cast<std::int64_t>(y)+h<=0)return JNI_FALSE;
 std::vector<jint> data(static_cast<std::size_t>(count));
 env->GetIntArrayRegion(source,0,count,data.data());
 if(env->ExceptionCheck())return JNI_FALSE;
 auto pixels=canvas->pixels();
 const auto original=pixels;
 for(int py=std::max(0,-y);py<h&&static_cast<std::int64_t>(y)+py<canvas->height();++py){
  for(int px=std::max(0,-x);px<w&&static_cast<std::int64_t>(x)+px<canvas->width();++px){
   const std::uint32_t src=static_cast<std::uint32_t>(data[2+static_cast<std::size_t>(py)*w+px]);
   const std::uint32_t sa=src>>24;
   if(sa==0)continue;
   const std::size_t pos=static_cast<std::size_t>(y+py)*canvas->width()+(x+px);
   if(sa==255){pixels[pos]=src;continue;}
   const std::uint32_t dst=pixels[pos],da=dst>>24;
   const std::uint32_t outA=sa+(da*(255-sa)+127)/255;
   if(outA==0){pixels[pos]=0;continue;}
   std::uint32_t rgb=0;
   for(int shift: {16,8,0}){
    const std::uint32_t sc=(src>>shift)&255,dc=(dst>>shift)&255;
    const std::uint32_t numerator=sc*sa*255+dc*da*(255-sa);
    const std::uint32_t denominator=outA*255;
    const std::uint32_t value=std::min(255u,(numerator+denominator/2)/denominator);
    rgb|=value<<shift;
   }
   pixels[pos]=(outA<<24)|rgb;
  }
 }
 if(pixels==original)return JNI_FALSE;
 checkpoint();
 canvas->setPixels(pixels);
 storeActive();
 return JNI_TRUE;
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
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeEraseMaskedSelection(JNIEnv* env,jclass,jint x,jint y,jint w,jint h,jbyteArray mask){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!mask||w<=0||h<=0||x<0||y<0)return JNI_FALSE;
 if(static_cast<std::int64_t>(x)+w>canvas->width()||static_cast<std::int64_t>(y)+h>canvas->height())return JNI_FALSE;
 if(static_cast<std::int64_t>(w)*h!=env->GetArrayLength(mask))return JNI_FALSE;
 std::vector<jbyte> bits(static_cast<std::size_t>(w)*h);
 env->GetByteArrayRegion(mask,0,static_cast<jsize>(bits.size()),bits.data());
 if(env->ExceptionCheck())return JNI_FALSE;
 if(std::none_of(bits.begin(),bits.end(),[](jbyte bit){return bit!=0;}))return JNI_FALSE;
 auto pixels=canvas->pixels();
 for(int row=0;row<h;++row)for(int col=0;col<w;++col)
  if(bits[static_cast<std::size_t>(row)*w+col])
   pixels[static_cast<std::size_t>(y+row)*canvas->width()+x+col]=0u;
 checkpoint();
 canvas->setPixels(pixels);
 storeActive();
 return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMoveMaskedSelection(JNIEnv* env,jclass,jint x,jint y,jint w,jint h,jbyteArray mask,jint dx,jint dy){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!mask||w<=0||h<=0||x<0||y<0||(!dx&&!dy))return JNI_FALSE;
 if(static_cast<std::int64_t>(x)+w>canvas->width()||static_cast<std::int64_t>(y)+h>canvas->height())return JNI_FALSE;
 if(static_cast<std::int64_t>(x)+dx<0||static_cast<std::int64_t>(y)+dy<0||
    static_cast<std::int64_t>(x)+w+dx>canvas->width()||static_cast<std::int64_t>(y)+h+dy>canvas->height())return JNI_FALSE;
 if(static_cast<std::int64_t>(w)*h!=env->GetArrayLength(mask))return JNI_FALSE;
 std::vector<jbyte> bits(static_cast<std::size_t>(w)*h);
 env->GetByteArrayRegion(mask,0,static_cast<jsize>(bits.size()),bits.data());
 if(env->ExceptionCheck())return JNI_FALSE;
 if(std::none_of(bits.begin(),bits.end(),[](jbyte bit){return bit!=0;}))return JNI_FALSE;
 const auto before=canvas->pixels();
 auto after=before;
 for(int row=0;row<h;++row)for(int col=0;col<w;++col)
  if(bits[static_cast<std::size_t>(row)*w+col])
   after[static_cast<std::size_t>(y+row)*canvas->width()+x+col]=0u;
 for(int row=0;row<h;++row)for(int col=0;col<w;++col)
  if(bits[static_cast<std::size_t>(row)*w+col])
   after[static_cast<std::size_t>(y+row+dy)*canvas->width()+x+col+dx]=before[static_cast<std::size_t>(y+row)*canvas->width()+x+col];
 checkpoint();
 canvas->setPixels(after);
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

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeFlipActiveHorizontal(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(w<=1||h<=0)return JNI_FALSE;
 auto pixels=canvas->pixels();
 bool changed=false;
 for(int y=0;y<h;++y)for(int x=0;x<w/2;++x){
  const std::size_t a=static_cast<std::size_t>(y)*w+x;
  const std::size_t b=static_cast<std::size_t>(y)*w+(w-1-x);
  if(pixels[a]!=pixels[b])changed=true;
  std::swap(pixels[a],pixels[b]);
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeFlipActiveVertical(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(w<=0||h<=1)return JNI_FALSE;
 auto pixels=canvas->pixels();
 bool changed=false;
 for(int y=0;y<h/2;++y)for(int x=0;x<w;++x){
  const std::size_t a=static_cast<std::size_t>(y)*w+x;
  const std::size_t b=static_cast<std::size_t>(h-1-y)*w+x;
  if(pixels[a]!=pixels[b])changed=true;
  std::swap(pixels[a],pixels[b]);
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRotateActive180(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(w<=0||h<=0)return JNI_FALSE;
 auto pixels=canvas->pixels();
 bool changed=false;
 for(std::size_t a=0,b=pixels.size()-1;a<b;++a,--b){
  if(pixels[a]!=pixels[b])changed=true;
  std::swap(pixels[a],pixels[b]);
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRotateActive90(JNIEnv*,jclass,jboolean clockwise){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(w<=0||h<=0||w!=h)return JNI_FALSE;
 const auto original=canvas->pixels();
 auto rotated=original;
 for(int y=0;y<h;++y)for(int x=0;x<w;++x){
  const int nx=clockwise?(w-1-y):y;
  const int ny=clockwise?x:(h-1-x);
  rotated[static_cast<std::size_t>(ny)*w+nx]=original[static_cast<std::size_t>(y)*w+x];
 }
 if(rotated==original)return JNI_FALSE;
 checkpoint();canvas->setPixels(rotated);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCropActiveSelection(JNIEnv*,jclass,jint left,jint top,jint right,jint bottom){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(left<0||top<0||right>w||bottom>h||left>=right||top>=bottom)return JNI_FALSE;
 if(left==0&&top==0&&right==w&&bottom==h)return JNI_FALSE;
 auto pixels=canvas->pixels();
 bool changed=false;
 for(int y=0;y<h;++y)for(int x=0;x<w;++x){
  if(x>=left&&x<right&&y>=top&&y<bottom)continue;
  auto& pixel=pixels[static_cast<std::size_t>(y)*w+x];
  if(pixel!=0u){pixel=0u;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCropActiveEllipse(JNIEnv*,jclass,jint left,jint top,jint right,jint bottom){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(left<0||top<0||right>w||bottom>h||left>=right||top>=bottom)return JNI_FALSE;
 auto pixels=canvas->pixels();
 bool changed=false;
 const double rx=(static_cast<double>(right)-left)/2.0,ry=(static_cast<double>(bottom)-top)/2.0;
 const double cx=(static_cast<double>(left)+right)/2.0,cy=(static_cast<double>(top)+bottom)/2.0;
 for(int y=0;y<h;++y)for(int x=0;x<w;++x){
  const double dx=(x+0.5-cx)/rx,dy=(y+0.5-cy)/ry;
  if(dx*dx+dy*dy<=1.0)continue;
  auto& pixel=pixels[static_cast<std::size_t>(y)*w+x];
  if(pixel!=0u){pixel=0u;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeTrimActiveMasked(JNIEnv* env,jclass,jint left,jint top,jint width,jint height,jbyteArray mask){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!mask||width<=0||height<=0||left<0||top<0)return JNI_FALSE;
 const int w=canvas->width(),h=canvas->height();
 if(static_cast<std::int64_t>(left)+width>w||static_cast<std::int64_t>(top)+height>h)return JNI_FALSE;
 if(static_cast<std::int64_t>(width)*height!=env->GetArrayLength(mask))return JNI_FALSE;
 std::vector<jbyte> bits(static_cast<std::size_t>(width)*height);
 env->GetByteArrayRegion(mask,0,static_cast<jsize>(bits.size()),bits.data());
 if(env->ExceptionCheck())return JNI_FALSE;
 if(std::none_of(bits.begin(),bits.end(),[](jbyte bit){return bit!=0;}))return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(int y=0;y<h;++y)for(int x=0;x<w;++x){
  const bool inside=x>=left&&y>=top&&x<left+width&&y<top+height;
  if(inside&&bits[static_cast<std::size_t>(y-top)*width+x-left])continue;
  auto& pixel=pixels[static_cast<std::size_t>(y)*w+x];
  if(pixel!=0u){pixel=0u;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeInvertActiveColors(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  if((pixel>>24)==0u)continue;
  const std::uint32_t inverted=(pixel&0xFF000000u)|((~pixel)&0x00FFFFFFu);
  if(inverted!=pixel){pixel=inverted;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeGrayscaleActive(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t a=pixel&0xFF000000u;
  if(!a)continue;
  const std::uint32_t r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const std::uint32_t gray=(299u*r+587u*g+114u*b+500u)/1000u;
  const std::uint32_t converted=a|(gray<<16)|(gray<<8)|gray;
  if(converted!=pixel){pixel=converted;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSepiaActive(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const int r=static_cast<int>((pixel>>16)&255u),g=static_cast<int>((pixel>>8)&255u),b=static_cast<int>(pixel&255u);
  const auto clamp=[](int v)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255,std::max(0,v)));};
  const std::uint32_t nr=clamp((393*r+769*g+189*b+500)/1000);
  const std::uint32_t ng=clamp((349*r+686*g+168*b+500)/1000);
  const std::uint32_t nb=clamp((272*r+534*g+131*b+500)/1000);
  const std::uint32_t result=alpha|(nr<<16)|(ng<<8)|nb;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBrightnessActive(JNIEnv*,jclass,jint adjustment){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||adjustment==0||adjustment< -255||adjustment>255)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto clamp=[](int v)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255,std::max(0,v)));};
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=clamp(static_cast<int>((pixel>>16)&255u)+adjustment);
  const std::uint32_t g=clamp(static_cast<int>((pixel>>8)&255u)+adjustment);
  const std::uint32_t b=clamp(static_cast<int>(pixel&255u)+adjustment);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeContrastActive(JNIEnv*,jclass,jint adjustment){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||adjustment==0||adjustment< -100||adjustment>100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const double factor=1.0+adjustment/100.0;
 const auto clamp=[](double v)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255.0,std::max(0.0,std::floor(v+0.5))));};
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const auto transform=[&](std::uint32_t channel){return clamp((static_cast<double>(channel)-127.5)*factor+127.5);};
  const std::uint32_t r=transform((pixel>>16)&255u),g=transform((pixel>>8)&255u),b=transform(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeThresholdActive(JNIEnv*,jclass,jint threshold){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||threshold<0||threshold>255)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const std::uint32_t luminance=(299u*r+587u*g+114u*b+500u)/1000u;
  const std::uint32_t monochrome=luminance>=static_cast<std::uint32_t>(threshold)?0x00FFFFFFu:0u;
  const std::uint32_t result=alpha|monochrome;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePosterizeActive(JNIEnv*,jclass,jint levels){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||levels<2||levels>256)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const auto quantize=[levels](std::uint32_t channel)->std::uint32_t{
   const int step=static_cast<int>((channel*static_cast<std::uint32_t>(levels-1)+127u)/255u);
   return static_cast<std::uint32_t>((step*255+(levels-1)/2)/(levels-1));
  };
  const std::uint32_t r=quantize((pixel>>16)&255u),g=quantize((pixel>>8)&255u),b=quantize(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSolarizeActive(JNIEnv*,jclass,jint threshold){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||threshold<0||threshold>255)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const auto solarize=[threshold](std::uint32_t channel)->std::uint32_t{
   return channel>=static_cast<std::uint32_t>(threshold)?255u-channel:channel;
  };
  const std::uint32_t r=solarize((pixel>>16)&255u),g=solarize((pixel>>8)&255u),b=solarize(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSaturationActive(JNIEnv*,jclass,jint adjustment){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||adjustment==0||adjustment< -100||adjustment>100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const double factor=1.0+adjustment/100.0;
 const auto clamp=[](double v)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255.0,std::max(0.0,std::floor(v+0.5))));};
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const double r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const double gray=0.299*r+0.587*g+0.114*b;
  const std::uint32_t nr=clamp(gray+(r-gray)*factor),ng=clamp(gray+(g-gray)*factor),nb=clamp(gray+(b-gray)*factor);
  const std::uint32_t result=alpha|(nr<<16)|(ng<<8)|nb;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeGammaActive(JNIEnv*,jclass,jint percent){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||percent<10||percent>300||percent==100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const double gamma=static_cast<double>(percent)/100.0;
 const auto correct=[gamma](std::uint32_t channel)->std::uint32_t{
  const double value=255.0*std::pow(static_cast<double>(channel)/255.0,1.0/gamma);
  return static_cast<std::uint32_t>(std::min(255.0,std::max(0.0,std::floor(value+0.5))));
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=correct((pixel>>16)&255u),g=correct((pixel>>8)&255u),b=correct(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeTintActive(JNIEnv*,jclass,jint redAdjustment,jint greenAdjustment,jint blueAdjustment){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||redAdjustment< -255||redAdjustment>255||greenAdjustment< -255||greenAdjustment>255||blueAdjustment< -255||blueAdjustment>255)return JNI_FALSE;
 if(redAdjustment==0&&greenAdjustment==0&&blueAdjustment==0)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto clamp=[](int value)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255,std::max(0,value)));};
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=clamp(static_cast<int>((pixel>>16)&255u)+redAdjustment);
  const std::uint32_t g=clamp(static_cast<int>((pixel>>8)&255u)+greenAdjustment);
  const std::uint32_t b=clamp(static_cast<int>(pixel&255u)+blueAdjustment);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSwapChannelsActive(JNIEnv*,jclass,jint mode){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||(mode!=0&&mode!=1&&mode!=2))return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const std::uint32_t nr=mode==0?g:(mode==1?b:r);
  const std::uint32_t ng=mode==0?r:(mode==1?g:b);
  const std::uint32_t nb=mode==0?b:(mode==1?r:g);
  const std::uint32_t result=alpha|(nr<<16)|(ng<<8)|nb;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeColorBalanceActive(JNIEnv*,jclass,jint redPercent,jint greenPercent,jint bluePercent){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||redPercent<0||redPercent>200||greenPercent<0||greenPercent>200||bluePercent<0||bluePercent>200)return JNI_FALSE;
 if(redPercent==100&&greenPercent==100&&bluePercent==100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto scale=[](std::uint32_t channel,int percent)->std::uint32_t{
  return std::min(255u,(channel*static_cast<std::uint32_t>(percent)+50u)/100u);
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=scale((pixel>>16)&255u,redPercent);
  const std::uint32_t g=scale((pixel>>8)&255u,greenPercent);
  const std::uint32_t b=scale(pixel&255u,bluePercent);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeHueRotateActive(JNIEnv*,jclass,jint degrees){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||degrees< -180||degrees>180||degrees==0)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const double angle=static_cast<double>(degrees)*3.14159265358979323846/180.0;
 const double cosine=std::cos(angle),sine=std::sin(angle);
 const auto clamp=[](double value)->std::uint32_t{return static_cast<std::uint32_t>(std::min(255.0,std::max(0.0,std::floor(value+0.5))));};
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const double r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const double y=0.299*r+0.587*g+0.114*b;
  const double u=b-y,v=r-y;
  const double rotatedU=u*cosine-v*sine,rotatedV=u*sine+v*cosine;
  const std::uint32_t nr=clamp(y+rotatedV),nb=clamp(y+rotatedU),ng=clamp((y-0.299*(y+rotatedV)-0.114*(y+rotatedU))/0.587);
  const std::uint32_t result=alpha|(nr<<16)|(ng<<8)|nb;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLevelsActive(JNIEnv*,jclass,jint blackPoint,jint whitePoint){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||blackPoint<0||whitePoint>255||blackPoint>=whitePoint||(blackPoint==0&&whitePoint==255))return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto remap=[blackPoint,whitePoint](std::uint32_t channel)->std::uint32_t{
  const int value=static_cast<int>(channel);
  if(value<=blackPoint)return 0u;
  if(value>=whitePoint)return 255u;
  const int range=whitePoint-blackPoint;
  return static_cast<std::uint32_t>(((value-blackPoint)*255+range/2)/range);
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=remap((pixel>>16)&255u),g=remap((pixel>>8)&255u),b=remap(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeExposureActive(JNIEnv*,jclass,jint percent){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||percent<10||percent>300||percent==100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto scale=[percent](std::uint32_t channel)->std::uint32_t{
  return std::min(255u,(channel*static_cast<std::uint32_t>(percent)+50u)/100u);
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=scale((pixel>>16)&255u),g=scale((pixel>>8)&255u),b=scale(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeDesaturateChannelActive(JNIEnv*,jclass,jint channel){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||channel<0||channel>2)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  const std::uint32_t gray=channel==0?r:(channel==1?g:b);
  const std::uint32_t result=alpha|(gray<<16)|(gray<<8)|gray;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeAdjustAlphaActive(JNIEnv*,jclass,jint percent){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||percent<0||percent>200||percent==100)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 for(auto& pixel:pixels){
  const std::uint32_t oldAlpha=(pixel>>24)&255u;
  const std::uint32_t newAlpha=std::min(255u,(oldAlpha*static_cast<std::uint32_t>(percent)+50u)/100u);
  const std::uint32_t result=(pixel&0x00FFFFFFu)|(newAlpha<<24);
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRemoveChannelActive(JNIEnv*,jclass,jint channel){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||channel<0||channel>2)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const std::uint32_t mask=channel==0?0xFF00FFFFu:(channel==1?0xFFFF00FFu:0xFFFFFF00u);
 for(auto& pixel:pixels){
  const std::uint32_t result=pixel&mask;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeNormalizeActive(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return JNI_FALSE;
 auto pixels=canvas->pixels();
 std::uint32_t minimum=255u,maximum=0u;
 bool hasVisible=false;
 for(const auto pixel:pixels){
  if((pixel&0xFF000000u)==0u)continue;
  const std::uint32_t r=(pixel>>16)&255u,g=(pixel>>8)&255u,b=pixel&255u;
  minimum=std::min(minimum,std::min(r,std::min(g,b)));
  maximum=std::max(maximum,std::max(r,std::max(g,b)));
  hasVisible=true;
 }
 if(!hasVisible||minimum>=maximum||(minimum==0u&&maximum==255u))return JNI_FALSE;
 bool changed=false;
 const std::uint32_t range=maximum-minimum;
 const auto normalize=[minimum,range](std::uint32_t channel)->std::uint32_t{
  return ((channel-minimum)*255u+range/2u)/range;
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=normalize((pixel>>16)&255u),g=normalize((pixel>>8)&255u),b=normalize(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeQuantizeActive(JNIEnv*,jclass,jint step){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||step<2||step>128)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto quantize=[step](std::uint32_t channel)->std::uint32_t{
  const std::uint32_t s=static_cast<std::uint32_t>(step);
  return std::min(255u,((channel+s/2u)/s)*s);
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=quantize((pixel>>16)&255u),g=quantize((pixel>>8)&255u),b=quantize(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeClampHighlightsActive(JNIEnv*,jclass,jint ceiling){
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||ceiling<0||ceiling>=255)return JNI_FALSE;
 auto pixels=canvas->pixels();bool changed=false;
 const auto clamp=[ceiling](std::uint32_t channel)->std::uint32_t{
  return std::min(channel,static_cast<std::uint32_t>(ceiling));
 };
 for(auto& pixel:pixels){
  const std::uint32_t alpha=pixel&0xFF000000u;
  if(!alpha)continue;
  const std::uint32_t r=clamp((pixel>>16)&255u),g=clamp((pixel>>8)&255u),b=clamp(pixel&255u);
  const std::uint32_t result=alpha|(r<<16)|(g<<8)|b;
  if(result!=pixel){pixel=result;changed=true;}
 }
 if(!changed)return JNI_FALSE;
 checkpoint();canvas->setPixels(pixels);storeActive();return JNI_TRUE;
}
