#include <jni.h>
#include <memory>
#include <mutex>
#include <vector>
#include "velyntora/Canvas.hpp"
namespace {
std::mutex mutex;
std::unique_ptr<velyntora::Canvas> document;
std::vector<std::vector<std::uint32_t>> undoStack,redoStack;
constexpr std::size_t historyLimit=15;
void pushUndo(){
 if(!document)return;
 if(undoStack.size()>=historyLimit)undoStack.erase(undoStack.begin());
 undoStack.push_back(document->pixels());redoStack.clear();
}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCreate(JNIEnv*,jclass,jint w,jint h){
 std::lock_guard<std::mutex> lock(mutex);
 try{document=std::make_unique<velyntora::Canvas>(w,h);undoStack.clear();redoStack.clear();return JNI_TRUE;}catch(...){document.reset();return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeBeginEdit(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(mutex);pushUndo();
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeStroke(JNIEnv*,jclass,jfloat x0,jfloat y0,jfloat x1,jfloat y1,jfloat r,jint color){
 std::lock_guard<std::mutex> lock(mutex);if(document)document->stroke(x0,y0,x1,y1,r,static_cast<std::uint32_t>(color));
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeShape(JNIEnv*,jclass,jint kind,jint x0,jint y0,jint x1,jint y1,jint color){
 std::lock_guard<std::mutex> lock(mutex);if(!document)return;
 if(kind==1)document->rectangle(x0,y0,x1,y1,static_cast<std::uint32_t>(color),false);
 if(kind==2)document->ellipse(x0,y0,x1,y1,static_cast<std::uint32_t>(color),false);
 if(kind==3)document->stroke(x0,y0,x1,y1,1.f,static_cast<std::uint32_t>(color));
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeFill(JNIEnv*,jclass,jint x,jint y,jint color){
 std::lock_guard<std::mutex> lock(mutex);if(document)document->fill(x,y,static_cast<std::uint32_t>(color));
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUndo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(mutex);if(!document||undoStack.empty())return JNI_FALSE;
 redoStack.push_back(document->pixels());document->setPixels(undoStack.back());undoStack.pop_back();return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRedo(JNIEnv*,jclass){
 std::lock_guard<std::mutex> lock(mutex);if(!document||redoStack.empty())return JNI_FALSE;
 undoStack.push_back(document->pixels());document->setPixels(redoStack.back());redoStack.pop_back();return JNI_TRUE;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> lock(mutex);if(!document)return nullptr;
 const auto& pixels=document->pixels();jintArray result=env->NewIntArray(static_cast<jsize>(pixels.size()));
 if(result)env->SetIntArrayRegion(result,0,static_cast<jsize>(pixels.size()),reinterpret_cast<const jint*>(pixels.data()));return result;
}
