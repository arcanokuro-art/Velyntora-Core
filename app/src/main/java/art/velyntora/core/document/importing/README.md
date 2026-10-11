# Original-resolution image import

RegionImageImporter decodes JPEG/PNG/WebP regions into bounded ARGB arrays, streams them into a native import transaction and handles all eight EXIF orientations within those regions. The document retains source dimensions up to 10000 per side / 100 million pixels. Oversized files are rejected explicitly, never silently reduced. Decoder and file access run on the import worker. A failed transaction preserves the previous document.
