// Isolated staging: no live document/history mutation before an accepted commit.
namespace {
std::unique_ptr<velyntora::LayerDocument> placedImage;
std::uint64_t placedImageRevision=0;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBeginPlacedImage(JNIEnv* env,jclass,jstring name,jfloat opacity){
  std::lock_guard<std::mutex> lock(guard);
  if(!layers || !canvas || placedImage || layers->layerCount()>=32 || !name || !std::isfinite(opacity) || opacity<0 || opacity>1 || env->GetStringUTFLength(name)>4096)return JNI_FALSE;
  const char* text=env->GetStringUTFChars(name,nullptr);if(!text)return JNI_FALSE;
  try{
    auto next=std::make_unique<velyntora::LayerDocument>(*layers);next->selectLayer(next->layerCount()-1);next->addLayer(text);next->setOpacity(next->activeIndex(),opacity);
    placedImage=std::move(next);placedImageRevision=revision;env->ReleaseStringUTFChars(name,text);return JNI_TRUE;
  }catch(...){env->ReleaseStringUTFChars(name,text);return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePlacedImageTile(JNIEnv* env,jclass,jint x,jint y,jint w,jint h,jintArray data){
  std::lock_guard<std::mutex> lock(guard);
  if(!placedImage || !data || w<1 || h<1 || std::int64_t(w)*h>1048576 || x<0 || y<0 || std::int64_t(x)+w>placedImage->width() || std::int64_t(y)+h>placedImage->height() || env->GetArrayLength(data)!=std::int64_t(w)*h)return JNI_FALSE;
  try{std::vector<std::uint32_t> pixels(std::size_t(w)*h);env->GetIntArrayRegion(data,0,pixels.size(),reinterpret_cast<jint*>(pixels.data()));if(env->ExceptionCheck())return JNI_FALSE;placedImage->replaceActiveRegion(pixels,x,y,w,h);return JNI_TRUE;}catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCommitPlacedImage(JNIEnv*,jclass){
  std::lock_guard<std::mutex> lock(guard);
  if(!placedImage || !layers || revision!=placedImageRevision)return JNI_FALSE;
  try{
    auto next=std::make_unique<velyntora::Canvas>(placedImage->width(),placedImage->height());next->setPixels(placedImage->layer(placedImage->activeIndex()).pixels);
    checkpoint();layers.swap(placedImage);canvas.swap(next);placedImage.reset();sampledSource.clear();return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeCancelPlacedImage(JNIEnv*,jclass){std::lock_guard<std::mutex> lock(guard);placedImage.reset();}
