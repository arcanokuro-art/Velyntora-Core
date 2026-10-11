# Large-document selection adapter

PagedWand reads native composite rows with a bounded eight-row cache and tracks
visited pixels in a bitset. The frozen magic-wand implementation remains in use
for small documents. Long flood selections run through the document worker queue.
