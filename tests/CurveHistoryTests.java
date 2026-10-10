package art.velyntora.core;
public final class CurveHistoryTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  CurveHistory h=new CurveHistory();CurveDraft a=new CurveDraft(0,0,90,0),b=a.copy();b.move(1,30,50);h.record(a,b);a.move(0,100,100);
  CurveDraft original=h.undo(b);check(original.x[0]==0&&original.y[1]==0);CurveDraft restored=h.redo(original);check(restored.y[1]==50);
  original=h.undo(restored);CurveDraft different=original.copy();different.move(2,60,70);h.record(original,different);check(h.redo(different)==null);
  h.clear();h.record(different,different);check(h.undo(different)==null);
  CurveDraft c=new CurveDraft(0,0,10,10);for(int i=0;i<120;i++){CurveDraft next=c.copy();next.move(1,i,0);h.record(c,next);c=next;}int count=0;while((c=h.undo(c))!=null)count++;check(count==100);
  System.out.println("Curve drag undo/redo, isolation, branching, no-op and 100-step limit passed");
 }
}
