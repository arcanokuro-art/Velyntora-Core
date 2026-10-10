package art.velyntora.core;
public final class RasterFormatTests {
 static void check(byte[] b,String name,int want){if(RasterFormat.detect(b,name)!=want)throw new AssertionError();}
 public static void main(String[] args){check(new byte[]{'B','M'},"a.png",1);check(new byte[]{'I','I',42,0},null,2);check(new byte[]{'M','M',0,42},null,2);check(new byte[]{0,0,1,0},null,3);for(int c='1';c<='6';c++)check(new byte[]{'P',(byte)c,'\n'},null,4);check(new byte[]{'P','6',1},null,0);check(new byte[0],"A.TGA",5);check(new byte[]{(byte)255,(byte)216,(byte)255},"a.tga",0);check(new byte[]{(byte)137,'P','N','G'},"a.tga",0);check(new byte[]{'P','K',3,4},null,7);check(new byte[]{'V','L','Y','C','O','R','E',1},null,6);System.out.println("Signature routing and TGA name fallback passed");}
}
