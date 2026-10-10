extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeBeginEdit(JNIEnv*,
                                                                                      jclass) {
  std::lock_guard<std::mutex> lock(guard);
  checkpoint();
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUndo(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || undoStack.empty()) return JNI_FALSE;
  if (redoStack.size() == limit) redoStack.erase(redoStack.begin());
  redoStack.push_back({*layers, curveTag, revision});
  restore(undoStack.back());
  undoStack.pop_back();
  trimHistory();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRedo(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || redoStack.empty()) return JNI_FALSE;
  if (undoStack.size() == limit) undoStack.erase(undoStack.begin());
  undoStack.push_back({*layers, curveTag, revision});
  restore(redoStack.back());
  redoStack.pop_back();
  trimHistory();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeCurveTag(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return curveTag;
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeMarkCurve(JNIEnv*,
                                                                                      jclass,
                                                                                      jint tag) {
  std::lock_guard<std::mutex> lock(guard);
  curveTag = tag;
}

extern "C" JNIEXPORT jlong JNICALL Java_art_velyntora_core_DrawingView_nativeRevision(JNIEnv*,
                                                                                      jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return static_cast<jlong>(revision);
}

