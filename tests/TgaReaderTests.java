package art.velyntora.core;
import java.io.*;
import java.util.Arrays;
public final class TgaReaderTests {
 static void check(boolean v){if(!v)throw new AssertionError();}
 static byte[] file(int type,int descriptor,int... body)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{0,0,(byte)type,0,0,0,0,0,0,0,0,0,2,0,2,0,32,(byte)descriptor});for(int v:body)out.write(v);return out.toByteArray();}
 public static void main(String[] args)throws Exception{
  int[] pixels={0x80ff0000,0xff00ff00,0xff0000ff,0};ByteArrayOutputStream out=new ByteArrayOutputStream();RasterFileWriter.write(out,2,2,true,(y,row)->System.arraycopy(pixels,y*2,row,0,2));TgaReader.Image image=TgaReader.read(new ByteArrayInputStream(out.toByteArray()));check(image.width==2&&image.height==2&&Arrays.equals(image.pixels,pixels));
  byte[] rle=file(10,0x28,129,0,0,255,128,1,0,255,0,255,255,0,0,255);image=TgaReader.read(new ByteArrayInputStream(rle));check(Arrays.equals(image.pixels,new int[]{0x80ff0000,0x80ff0000,0xff00ff00,0xff0000ff}));
  byte[] bottom=file(2,0x18,0,0,255,255,0,255,0,255,255,0,0,255,255,255,255,255);image=TgaReader.read(new ByteArrayInputStream(bottom));check(Arrays.equals(image.pixels,new int[]{0xffffffff,0xff0000ff,0xff00ff00,0xffff0000}));
  byte[] opaque=file(10,0x20,131,0,0,255,0);image=TgaReader.read(new ByteArrayInputStream(opaque));for(int pixel:image.pixels)check(pixel==0xffff0000);
  for(byte[] invalid:new byte[][]{Arrays.copyOf(rle,rle.length-1),file(10,0x28,132,0,0,0,255),file(1,0x28),file(2,0xe8)}){boolean rejected=false;try{TgaReader.read(new ByteArrayInputStream(invalid));}catch(IOException e){rejected=true;}check(rejected);}
  System.out.println("TGA roundtrip, RLE, origins, alpha and malformed packets passed");
 }
}
