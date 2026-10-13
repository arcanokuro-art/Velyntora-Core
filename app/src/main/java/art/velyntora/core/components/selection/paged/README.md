# Large-document selection adapter

PagedWand reads native composite rows with a bounded eight-row cache and tracks
visited pixels in a bitset. The frozen magic-wand implementation remains in use
for small documents. Long flood selections run through the document worker queue.

Large selection contours are prepared on the worker and displayed at a bounded
resolution. The full Region stays exact for edits. PagedSelectionMove builds the
mask from RegionIterator spans and executes movement/frame preparation on the
worker; the UI receives the completed frame and selection together.
