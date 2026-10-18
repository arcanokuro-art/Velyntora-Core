# Editable text layers

TextDialog creates or edits Unicode text and typography. TextSpec owns the immutable model and TextSourceRenderer builds a bounded glyph cache off the UI thread. The shared object session provides transform handles, opacity, local undo/redo, confirm and cancel. Applying the dialog changes only the pending object; ✓ commits one independent text layer or replaces the same existing layer. X discards the pending changes.

Native text metadata belongs to core/components/text and TextElementBridge.inc. VLYCORE v4 preserves text, its style, source glyph cache and placement. v1/v2/v3 remain readable. Raster exports preserve the visible result. Pixel effects, merge and document resize/crop rasterize the affected element; undo restores editable metadata. Existing raster text insertion remains available for compatibility but the user-facing text tool uses retained layers.
