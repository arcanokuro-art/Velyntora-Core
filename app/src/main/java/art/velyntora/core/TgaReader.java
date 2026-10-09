package art.velyntora.core;
import java.io.InputStream;
import java.io.IOException;
/** True-color TGA decoder with bounded allocation, RLE validation and both origins. */
final class TgaReader {
 static final class Image {final int width,height;final int[] pixels;Image(int w,int h){width=w;height=h;pixels=new int[w*h];}}
 private static int octet(InputStream input)throws IOException{int value=input.read();if(value<0)throw new IOException("TGA incompleto");return value;}
 private static int word(InputStream input)throws IOException{return octet(input)|(octet(input)<<8);}
 private static int pixel(InputStream input,int depth,boolean alpha)throws IOException{int b=octet(input),g=octet(input),r=octet(input),a=depth==32?octet(input):255;return (alpha?a:255)<<24|r<<16|g<<8|b;}
 static Image read(InputStream input)throws IOException{
  if(input==null)throw new IOException("Sin archivo");
  int id=octet(input),colorMap=octet(input),type=octet(input);for(int i=0;i<9;i++)octet(input);
  int w=word(input),h=word(input),depth=octet(input),descriptor=octet(input),attributes=descriptor&15;
  if(colorMap!=0||(type!=2&&type!=10)||(depth!=24&&depth!=32)||(descriptor&0xc0)!=0||(attributes!=0&&attributes!=8)||(depth==24&&attributes!=0)||w<1||h<1||w>8192||h>8192||(long)w*h>4000000)throw new IOException("TGA no compatible o demasiado grande");
  for(int i=0;i<id;i++)octet(input);
  Image image=new Image(w,h);int index=0;
  while(index<image.pixels.length){int packet=type==10?octet(input):0,count=type==10?(packet&127)+1:1;if(count>image.pixels.length-index)throw new IOException("Paquete TGA fuera de límites");boolean repeated=(packet&128)!=0;int value=repeated?pixel(input,depth,attributes==8):0;
   for(int i=0;i<count;i++){int p=repeated?value:pixel(input,depth,attributes==8),x=index%w,y=index/w;if((descriptor&0x20)==0)y=h-1-y;if((descriptor&0x10)!=0)x=w-1-x;image.pixels[y*w+x]=p;index++;}
  }return image;
 }
}
