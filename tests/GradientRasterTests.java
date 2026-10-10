package art.velyntora.core;
public final class GradientRasterTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  int[] a=GradientRaster.create(3,1,.5,.5,2.5,.5,0xff804020);check(a[2]==0xff804020&&a[3]==0x80804020&&a[4]==0);
  int[] b=GradientRaster.create(3,1,2.5,.5,.5,.5,0x80804020);check(b[2]==0&&b[3]==0x40804020&&b[4]==0x80804020);
  int[] c=GradientRaster.create(1,3,.5,.5,.5,2.5,0xff112233);check(c[2]==0xff112233&&c[4]==0);
  int[] d=GradientRaster.create(2,2,1,1,1,1,0xff000000);for(int i=2;i<d.length;i++)check(d[i]==0);
  try{GradientRaster.create(2,2,Double.NaN,0,1,1,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  int[] radial=GradientRaster.create(3,3,1.5,1.5,2.5,1.5,0xff112233,true);
  check(radial[6]==0xff112233&&radial[3]==0&&radial[5]==0&&radial[7]==0&&radial[9]==0);
  try{GradientRaster.create(1,1,-Double.MAX_VALUE,0,Double.MAX_VALUE,0,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Gradient endpoints, alpha, direction, zero-length and invalid input passed");
 }
}
