package art.velyntora.core;
public final class WorkspaceLayoutTests {
 public static void main(String[] args){for(int width:new int[]{240,320,360,411,600,719,720,840,1280}){WorkspaceLayout layout=new WorkspaceLayout(width);if(layout.canvasWidth<240||layout.toolsWidth>width||layout.layersWidth>width)throw new AssertionError();if(width<720&&(layout.inline||layout.canvasWidth!=width))throw new AssertionError();if(width>=720&&(!layout.inline||layout.canvasWidth+layout.toolsWidth+layout.layersWidth!=width))throw new AssertionError();}boolean rejected=false;try{new WorkspaceLayout(0);}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError();System.out.println("Phone, tablet, split-window canvas width policy passed");}
}
