package art.velyntora.core;
public final class CurveDraftTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  CurveDraft c=new CurveDraft(0,0,90,30);check(c.evaluate(0,true)==0&&c.evaluate(1,true)==90);check(Math.abs(c.evaluate(.5f,true)-45)<.001);
  c.move(1,30,60);c.move(2,60,60);check(c.evaluate(.5f,false)>30);check(c.hit(30,60,2)==1&&c.hit(-10,-10,2)==-1);
  CurveDraft saved=c.copy();c.move(1,0,0);check(saved.x[1]==30&&saved.y[1]==60);
  try{c.move(1,Float.NaN,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{c.evaluate(2,true);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Curve endpoints, interpolation, handles, copy and invalid input passed");
 }
}
