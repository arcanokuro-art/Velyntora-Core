package art.velyntora.core;
/** Linear or radial foreground-to-transparent gradient. */
final class GradientRaster {
 static int[] create(int width,int height,double x0,double y0,double x1,double y1,int color){
  return create(width,height,x0,y0,x1,y1,color,false);
 }
 static int[] create(int width,int height,double x0,double y0,double x1,double y1,int color,boolean radial){
  if(width<=0||height<=0||(long)width*height>4000000||!Double.isFinite(x0)||!Double.isFinite(y0)||!Double.isFinite(x1)||!Double.isFinite(y1))throw new IllegalArgumentException("Invalid gradient");
  int[] data=new int[2+width*height];data[0]=width;data[1]=height;
  double dx=x1-x0,dy=y1-y0,length=dx*dx+dy*dy;
  if(!Double.isFinite(length))throw new IllegalArgumentException("Invalid gradient extent");
  if(length==0)return data;
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   double t=Math.max(0,Math.min(1,(radial?Math.hypot(x+.5-x0,y+.5-y0)/Math.sqrt(length):((x+.5-x0)*dx+(y+.5-y0)*dy)/length)));
   int alpha=(int)Math.round((color>>>24)*(1-t));
   data[2+y*width+x]=alpha==0?0:(color&0xffffff)|(alpha<<24);
  }
  return data;
 }
}
