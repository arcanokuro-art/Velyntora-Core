extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,
                                                                                        jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return nullptr;
  const auto pixels = layers->flatten();
  jintArray result = env->NewIntArray(static_cast<jsize>(pixels.size()));
  if (result)
    env->SetIntArrayRegion(result, 0, static_cast<jsize>(pixels.size()),
                           reinterpret_cast<const jint*>(pixels.data()));
  return result;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeRegionPixels(
    JNIEnv* env,jclass,jint x,jint y,jint w,jint h) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || x<0 || y<0 || w<=0 || h<=0 || x>layers->width()-w || y>layers->height()-h)
    return nullptr;
  try {
    auto pixels=layers->flattenRegion(x,y,w,h);
    auto result=env->NewIntArray(jsize(pixels.size()));
    if(result) env->SetIntArrayRegion(result,0,jsize(pixels.size()),
        reinterpret_cast<const jint*>(pixels.data()));
    return result;
  } catch (...) { return nullptr; }
}
