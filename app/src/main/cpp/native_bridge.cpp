#include <jni.h>
#include <memory>
#include <mutex>
#include "velyntora/Canvas.hpp"
namespace {std::mutex mutex;std::unique_ptr<velyntora::Canvas> document;}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCreate(JNIEnv*,jclass,jint w,jint h){
 std::lock_guard<std::mutex> lock(mutex);
 try{document=std::make_unique<velyntora::Canvas>(w,h);return JNI_TRUE;}catch(...){document.reset();return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeStroke(JNIEnv*,jclass,jfloat x0,jfloat y0,jfloat x1,jfloat y1,jfloat r,jint color){
 std::lock_guard<std::mutex> lock(mutex);if(document)document->stroke(x0,y0,x1,y1,r,static_cast<std::uint32_t>(color));
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> lock(mutex);if(!document)return nullptr;
 const auto& pixels=document->pixels();jintArray result=env->NewIntArray(static_cast<jsize>(pixels.size()));
 if(result)env->SetIntArrayRegion(result,0,static_cast<jsize>(pixels.size()),reinterpret_cast<const jint*>(pixels.data()));return result;
}
