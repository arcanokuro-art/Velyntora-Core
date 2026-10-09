package art.velyntora.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Portable streamed adapter for the native VLYCORE v1 project format. */
final class VlyProjectStream {
 static final byte[] MAGIC={'V','L','Y','C','O','R','E',1};
 static int integer(DataInputStream in)throws IOException{return Integer.reverseBytes(in.readInt());}
 static void integer(DataOutputStream out,int value)throws IOException{out.writeInt(Integer.reverseBytes(value));}
 static void limits(int w,int h,int count,int active)throws IOException{if(w<1||h<1||w>8192||h>8192||(long)w*h>4000000||count<1||count>32||(long)w*h*count>24000000||active<0||active>=count)throw new IOException("Proyecto demasiado grande o inválido");}
 static final class Layer {String name;boolean visible;float opacity;int[] pixels;Layer(String n,boolean v,float o,int[] p){name=n;visible=v;opacity=o;pixels=p;}}
 static final class Reader {
  final DataInputStream in;final int width,height,count,active;int index;
  Reader(InputStream input)throws IOException{in=new DataInputStream(new BufferedInputStream(input));byte[] magic=new byte[8];in.readFully(magic);if(!java.util.Arrays.equals(magic,MAGIC))throw new IOException("Proyecto incompatible");width=integer(in);height=integer(in);count=integer(in);active=integer(in);limits(width,height,count,active);}
  Layer next()throws IOException{if(index>=count)throw new EOFException();int size=integer(in);if(size<0||size>4096)throw new IOException("Nombre inválido");byte[] name=new byte[size];in.readFully(name);int visibility=integer(in);float opacity=Float.intBitsToFloat(integer(in));if((visibility!=0&&visibility!=1)||(Float.isNaN(opacity)||Float.isInfinite(opacity))||opacity<0||opacity>1)throw new IOException("Propiedades inválidas");int[] pixels=new int[width*height];for(int i=0;i<pixels.length;i++)pixels[i]=integer(in);index++;return new Layer(new String(name,StandardCharsets.UTF_8),visibility==1,opacity,pixels);}
  void finish()throws IOException{if(index!=count||in.read()!=-1)throw new IOException("Proyecto incompleto o con datos sobrantes");}
 }
 static void header(DataOutputStream out,int w,int h,int count,int active)throws IOException{limits(w,h,count,active);out.write(MAGIC);integer(out,w);integer(out,h);integer(out,count);integer(out,active);}
 static void layer(DataOutputStream out,Layer layer,int count)throws IOException{byte[] name=layer.name.getBytes(StandardCharsets.UTF_8);if(name.length>4096||layer.pixels.length!=count||(Float.isNaN(layer.opacity)||Float.isInfinite(layer.opacity))||layer.opacity<0||layer.opacity>1)throw new IOException("Capa inválida");integer(out,name.length);out.write(name);integer(out,layer.visible?1:0);integer(out,Float.floatToIntBits(layer.opacity));for(int pixel:layer.pixels)integer(out,pixel);}
}
