extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeLayerCount(JNIEnv*,
                                                                                       jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return layers ? static_cast<jint>(layers->layerCount()) : 0;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeActiveLayer(JNIEnv*,
                                                                                        jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return layers ? static_cast<jint>(layers->activeIndex()) : -1;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeAddLayer(JNIEnv*,
                                                                                         jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || layers->layerCount() >= 32 ||
      std::uint64_t(layers->width()) * layers->height() * (layers->layerCount() + 1) > 24000000)
    return JNI_FALSE;
  try {
    checkpoint();
    layers->addLayer("Capa " + std::to_string(layers->layerCount() + 1));
    loadActive();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSelectLayer(JNIEnv*, jclass, jint index) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || index < 0 || !layers->selectLayer(static_cast<std::size_t>(index)))
    return JNI_FALSE;
  loadActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeDeleteLayer(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || layers->layerCount() == 1) return JNI_FALSE;
  checkpoint();
  if (!layers->removeLayer(layers->activeIndex())) return JNI_FALSE;
  loadActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeToggleLayer(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return JNI_FALSE;
  const auto i = layers->activeIndex();
  checkpoint();
  return layers->setVisible(i, !layers->layer(i).visible) ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeLayerVisible(JNIEnv*, jclass, jint index) {
  std::lock_guard<std::mutex> lock(guard);
  return layers && index >= 0 && static_cast<std::size_t>(index) < layers->layerCount() &&
                 layers->layer(index).visible
             ? JNI_TRUE
             : JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeMoveLayer(JNIEnv*, jclass, jint direction) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return JNI_FALSE;
  int from = static_cast<int>(layers->activeIndex()), to = from + direction;
  if (to < 0 || to >= static_cast<int>(layers->layerCount())) return JNI_FALSE;
  checkpoint();
  if (!layers->moveLayer(static_cast<std::size_t>(from), static_cast<std::size_t>(to)))
    return JNI_FALSE;
  loadActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSetLayerOpacity(JNIEnv*, jclass, jfloat opacity) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return JNI_FALSE;
  const auto i = layers->activeIndex();
  if (!std::isfinite(opacity) || opacity < 0.f || opacity > 1.f) return JNI_FALSE;
  if (layers->layer(i).opacity == opacity) return JNI_TRUE;
  checkpoint();
  return layers->setOpacity(i, opacity) ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT jfloat JNICALL Java_art_velyntora_core_DrawingView_nativeLayerOpacity(JNIEnv*,
                                                                                           jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return layers ? layers->layer(layers->activeIndex()).opacity : 1.f;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_art_velyntora_core_DrawingView_nativeLayerThumbnail(JNIEnv* env, jclass, jint index) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || index < 0 || static_cast<std::size_t>(index) >= layers->layerCount())
    return nullptr;
  constexpr int side = 48;
  const auto& layer = layers->layer(static_cast<std::size_t>(index));
  const int w = layers->width(), h = layers->height();
  if (w <= 0 || h <= 0) return nullptr;
  std::vector<jint> preview(side * side);
  for (int y = 0; y < side; ++y) {
    const int sy = static_cast<int>((static_cast<std::int64_t>(y) * h) / side);
    for (int x = 0; x < side; ++x) {
      const int sx = static_cast<int>((static_cast<std::int64_t>(x) * w) / side);
      preview[y * side + x] =
          static_cast<jint>(layer.pixels[static_cast<std::size_t>(sy) * w + sx]);
    }
  }
  jintArray result = env->NewIntArray(side * side);
  if (result) env->SetIntArrayRegion(result, 0, side * side, preview.data());
  return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_art_velyntora_core_DrawingView_nativeLayerName(JNIEnv* env, jclass, jint index) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || index < 0 || static_cast<std::size_t>(index) >= layers->layerCount())
    return nullptr;
  const auto& name = layers->layer(static_cast<std::size_t>(index)).name;
  jbyteArray bytes = env->NewByteArray(static_cast<jsize>(name.size()));
  if (!bytes) return nullptr;
  env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(name.size()),
                          reinterpret_cast<const jbyte*>(name.data()));
  jclass strings = env->FindClass("java/lang/String");
  jmethodID constructor = env->GetMethodID(strings, "<init>", "([BLjava/lang/String;)V");
  jstring charset = env->NewStringUTF("UTF-8");
  return static_cast<jstring>(env->NewObject(strings, constructor, bytes, charset));
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRenameLayer(
    JNIEnv* env, jclass, jint index, jstring name) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || !name || index < 0 || static_cast<std::size_t>(index) >= layers->layerCount() ||
      env->GetStringLength(name) < 1 || env->GetStringLength(name) > 4096)
    return JNI_FALSE;
  try {
    jclass strings = env->FindClass("java/lang/String");
    jmethodID getBytes = env->GetMethodID(strings, "getBytes", "(Ljava/lang/String;)[B");
    jstring charset = env->NewStringUTF("UTF-8");
    auto bytes = static_cast<jbyteArray>(env->CallObjectMethod(name, getBytes, charset));
    if (env->ExceptionCheck() || !bytes) return JNI_FALSE;
    jsize length = env->GetArrayLength(bytes);
    if (length < 1 || length > 4096) return JNI_FALSE;
    std::string utf8(static_cast<std::size_t>(length), '\0');
    env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(utf8.data()));
    if (env->ExceptionCheck() || utf8.find('\0') != std::string::npos) return JNI_FALSE;
    if (layers->layer(static_cast<std::size_t>(index)).name == utf8) return JNI_TRUE;
    checkpoint();
    return layers->renameLayer(static_cast<std::size_t>(index), utf8) ? JNI_TRUE : JNI_FALSE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}
