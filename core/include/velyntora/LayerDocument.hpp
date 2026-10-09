#pragma once
#include "velyntora/Canvas.hpp"
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace velyntora {
struct Layer {
 std::string name;
 std::vector<std::uint32_t> pixels;
 bool visible=true;
 float opacity=1.f;
};
class LayerDocument {
public:
 LayerDocument(int width,int height);
 int width() const noexcept {return width_;}
 int height() const noexcept {return height_;}
 std::size_t layerCount() const noexcept {return layers_.size();}
 std::size_t activeIndex() const noexcept {return active_;}
 const Layer& layer(std::size_t index) const {return layers_.at(index);}
 bool renameLayer(std::size_t index,const std::string& name);
 LayerDocument resized(int width,int height,bool scalePixels) const;
 LayerDocument cropped(int left,int top,int width,int height) const;
 void addLayer(const std::string& name);
 bool removeLayer(std::size_t index);
 bool selectLayer(std::size_t index);
 bool moveLayer(std::size_t from,std::size_t to);
 bool setVisible(std::size_t index,bool visible);
 bool setOpacity(std::size_t index,float opacity);
 void replaceActivePixels(const std::vector<std::uint32_t>& pixels);
 std::vector<std::uint32_t> flatten() const;
private:
 int width_,height_;
 std::size_t active_=0;
 std::vector<Layer> layers_;
};
}
