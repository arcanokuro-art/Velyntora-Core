cmake_minimum_required(VERSION 3.22.1)
project(velyntora_core LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
add_library(velyntora_engine STATIC
 document/LayerDocument.cpp
 document/ProjectIO.cpp
 effects/ArtisticEffects.cpp
 effects/BlurEffects.cpp
 effects/ColorAdjustments.cpp
 effects/DistortionEffects.cpp
 effects/ObjectEffects.cpp
 effects/PixelEffects.cpp
 effects/RenderEffects.cpp
 effects/UtilityEffects.cpp
 src/Canvas.cpp
 ../tools/brush/classic/native/Raster.cpp
 ../tools/brush/antialiased/native/Raster.cpp
 ../tools/bucket/native/Raster.cpp
 ../tools/ellipse/native/Raster.cpp
 ../tools/move_pixels/native/SelectionTransform.cpp
 ../tools/rectangle/native/Raster.cpp
 ../tools/shared/native/Raster.cpp
)
set_target_properties(velyntora_engine PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_include_directories(velyntora_engine PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
if(ANDROID)
 add_library(velyntora_jni SHARED ../app/src/main/cpp/native_bridge.cpp)
 target_link_libraries(velyntora_jni PRIVATE velyntora_engine log)
endif()
