#pragma once
#include <cstdio>
#include <cstdint>
#include <stdexcept>
#include <string>
#include <unistd.h>
#include <vector>
namespace velyntora::history {
// Unlinked session file: storage is reclaimed even after an abnormal process exit.
class HistoryFile {
  FILE* file_;
 public:
  explicit HistoryFile(const std::string& directory) {
    auto path=directory+"/velyntora-history-XXXXXX";
    std::vector<char> name(path.begin(),path.end()); name.push_back(0);
    int fd=mkstemp(name.data());
    if(fd<0)throw std::runtime_error("Cannot create history storage");
    unlink(name.data()); file_=fdopen(fd,"w+b");
    if(!file_){close(fd);throw std::runtime_error("Cannot open history storage");}
  }
  ~HistoryFile(){fclose(file_);}
  HistoryFile(const HistoryFile&)=delete;
  std::uint64_t append(const void* data,std::size_t bytes) {
    if(fseeko64(file_,0,SEEK_END))throw std::runtime_error("History seek");
    auto offset=ftello64(file_);
    if(offset<0 || fwrite(data,1,bytes,file_)!=bytes)throw std::runtime_error("History write");
    return offset;
  }
  void read(std::uint64_t offset,void* data,std::size_t bytes) {
    if(fseeko64(file_,offset,SEEK_SET)||fread(data,1,bytes,file_)!=bytes)
      throw std::runtime_error("History read");
  }
  void flush(){if(fflush(file_))throw std::runtime_error("History flush");}
};
}
