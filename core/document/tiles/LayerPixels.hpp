#pragma once
#include <algorithm>
#include <cstdint>
#include <iterator>
#include <map>
#include <memory>
#include <vector>
#include <stdexcept>

namespace velyntora::tiles {
// Sparse copy-on-write blocks keep blank layers and unchanged history cheap.
class LayerPixels {
  static constexpr std::size_t blockSize=4096;
  using Block=std::vector<std::uint32_t>;
  std::size_t size_=0;
  std::uint32_t background_=0;
  std::map<std::size_t,std::shared_ptr<Block>> blocks_;
  std::uint32_t get(std::size_t i) const {
    auto found=blocks_.find(i/blockSize);
    return found==blocks_.end()?background_:(*found->second)[i%blockSize];
  }
  void put(std::size_t i,std::uint32_t value) {
    if(get(i)==value) return;
    auto key=i/blockSize;
    auto found=blocks_.find(key);
    if(found==blocks_.end()) {
      auto block=std::make_shared<Block>(std::min(blockSize,size_-key*blockSize),background_);
      (*block)[i%blockSize]=value;blocks_.emplace(key,std::move(block));
    } else {
      if(!found->second.unique()) found->second=std::make_shared<Block>(*found->second);
      (*found->second)[i%blockSize]=value;
    }
  }
 public:
  LayerPixels()=default;
  LayerPixels(const std::vector<std::uint32_t>& pixels):size_(pixels.size()),
      background_(pixels.empty()?0:pixels[0]) {
    for(std::size_t offset=0;offset<size_;offset+=blockSize) {
      auto first=pixels.begin()+offset,last=first+std::min(blockSize,size_-offset);
      if(std::all_of(first,last,[this](std::uint32_t value){return value==background_;}))continue;
      blocks_.emplace(offset/blockSize,std::make_shared<Block>(first,last));
    }
  }
  LayerPixels(std::size_t size,std::uint32_t background):size_(size),background_(background) {}
  std::size_t size() const noexcept {return size_;}
  bool isUniform() const noexcept {return blocks_.empty();}
  std::uint32_t background() const noexcept {return background_;}
  std::size_t allocatedPixels() const noexcept {
    std::size_t count=0;for(const auto& [key,block]:blocks_) if(block)count+=block->size();return count;
  }
  struct Reference {
    LayerPixels* owner;std::size_t index;
    operator std::uint32_t() const {return owner->get(index);}
    Reference& operator=(std::uint32_t value) {owner->put(index,value);return *this;}
    Reference& operator=(const Reference& value) {return *this=std::uint32_t(value);}
  };
  Reference operator[](std::size_t i) {return {this,i};}
  std::uint32_t operator[](std::size_t i) const {return get(i);}
  template<bool Constant> struct Iterator {
    using Owner=std::conditional_t<Constant,const LayerPixels,LayerPixels>;
    using iterator_category=std::random_access_iterator_tag;
    using value_type=std::uint32_t;
    using difference_type=std::ptrdiff_t;
    using reference=std::conditional_t<Constant,std::uint32_t,Reference>;
    using pointer=void;
    Owner* owner;difference_type index;
    reference operator*() const {return (*owner)[std::size_t(index)];}
    reference operator[](difference_type n) const {return (*owner)[std::size_t(index+n)];}
    Iterator& operator++(){++index;return *this;}
    Iterator operator++(int){auto old=*this;++*this;return old;}
    Iterator& operator--(){--index;return *this;}
    Iterator operator--(int){auto old=*this;--*this;return old;}
    Iterator& operator+=(difference_type n){index+=n;return *this;}
    Iterator& operator-=(difference_type n){index-=n;return *this;}
    Iterator operator+(difference_type n)const {return {owner,index+n};}
    Iterator operator-(difference_type n)const {return {owner,index-n};}
    difference_type operator-(Iterator other)const{return index-other.index;}
    bool operator==(Iterator other)const{return owner==other.owner&&index==other.index;}
    bool operator!=(Iterator other)const{return !(*this==other);}
    bool operator<(Iterator other)const{return index<other.index;}
    bool operator>(Iterator other)const{return index>other.index;}
    bool operator<=(Iterator other)const{return index<=other.index;}
    bool operator>=(Iterator other)const{return index>=other.index;}
    friend Iterator operator+(difference_type n,Iterator value){return value+n;}
  };
  Iterator<false> begin(){return {this,0};}
  Iterator<false> end(){return {this,std::ptrdiff_t(size_)};}
  Iterator<true> begin()const{return {this,0};}
  Iterator<true> end()const{return {this,std::ptrdiff_t(size_)};}
  operator std::vector<std::uint32_t>() const {
    std::vector<std::uint32_t> result;
    copyTo(result);
    return result;
  }
  void copyTo(std::vector<std::uint32_t>& result) const {
    result.assign(size_,background_);
    for(const auto& [key,block]:blocks_)if(block)
      std::copy(block->begin(),block->end(),result.begin()+key*blockSize);
  }
  // Bulk reads perform one map lookup per block, not one per pixel.
  void readSpan(std::size_t offset,std::size_t count,std::uint32_t* output) const {
    if(offset>size_ || count>size_-offset)throw std::out_of_range("Pixel span");
    while(count) {
      std::size_t key=offset/blockSize,inBlock=offset%blockSize;
      std::size_t length=std::min(count,blockSize-inBlock);
      auto found=blocks_.find(key);
      if(found==blocks_.end())std::fill_n(output,length,background_);
      else std::copy_n(found->second->begin()+inBlock,length,output);
      output+=length;offset+=length;count-=length;
    }
  }
  LayerPixels& operator=(const std::vector<std::uint32_t>& pixels) {
    if(pixels.size()!=size_) {
      LayerPixels replacement(pixels);*this=std::move(replacement);return *this;
    }
    if(!pixels.empty() && std::all_of(pixels.begin(),pixels.end(),
        [color=pixels[0]](std::uint32_t value){return value==color;})) {
      LayerPixels replacement(pixels.size(),pixels[0]);*this=std::move(replacement);return *this;
    }
    // Preserve unchanged shared blocks; publish only after all allocations succeed.
    auto replacement=blocks_;
    for(std::size_t offset=0;offset<size_;offset+=blockSize) {
      auto first=pixels.begin()+offset,last=first+std::min(blockSize,size_-offset);
      auto found=blocks_.find(offset/blockSize);
      bool uniform=std::all_of(first,last,[this](std::uint32_t value){return value==background_;});
      if(uniform){replacement.erase(offset/blockSize);continue;}
      if(found!=blocks_.end() && std::equal(first,last,found->second->begin()))continue;
      replacement[offset/blockSize]=std::make_shared<Block>(first,last);
    }
    blocks_=std::move(replacement);return *this;
  }
  friend bool operator==(const LayerPixels& a,const LayerPixels& b) {
    if(a.size_!=b.size_)return false;
    for(std::size_t i=0;i<a.size_;++i)if(a.get(i)!=b.get(i))return false;
    return true;
  }
  friend bool operator==(const LayerPixels& a,const std::vector<std::uint32_t>& b) {
    if(a.size_!=b.size())return false;
    for(std::size_t i=0;i<a.size_;++i)if(a.get(i)!=b[i])return false;
    return true;
  }
};
}
