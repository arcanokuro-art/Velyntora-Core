package art.velyntora.core;
/** Layout policy in density-independent pixels; overlays never consume canvas width. */
final class WorkspaceLayout {
 final boolean inline;final int toolsWidth,layersWidth,canvasWidth;
 WorkspaceLayout(int widthDp){if(widthDp<1)throw new IllegalArgumentException("Invalid window width");inline=widthDp>=720;toolsWidth=Math.min(154,Math.max(48,widthDp-48));layersWidth=Math.min(192,Math.max(48,widthDp-48));canvasWidth=inline?widthDp-toolsWidth-layersWidth:widthDp;}
}
