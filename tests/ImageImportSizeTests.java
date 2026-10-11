package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;
public final class ImageImportSizeTests {
  static void check(boolean ok){if(!ok)throw new AssertionError();}
  public static void main(String[] args){
    ImageImportSize a=new ImageImportSize(6067,9030,1);
    check(a.reduced() && a.width==4703 && a.height==7000);
    ImageImportSize b=new ImageImportSize(9030,6067,6);
    check(b.displayWidth()==4703 && b.displayHeight()==7000);
    check(new ImageImportSize(7000,7000,1).reduced()==false);
    check(new ImageImportSize(7001,7001,1).width==7000);
    for(int w:new int[]{1,19,7000,7001,20000,Integer.MAX_VALUE})
      for(int h:new int[]{1,37,7000,9030,20000,Integer.MAX_VALUE}){
        ImageImportSize size=new ImageImportSize(w,h,1);
        check(CanvasLimits.accepts(size.width,size.height));
        check(size.width<=w && size.height<=h);
        double scale=Math.min(1.0,7000.0/Math.max(w,h));
        check(Math.abs(size.width-w*scale)<=.5 || size.width==1);
        check(Math.abs(size.height-h*scale)<=.5 || size.height==1);
      }
    boolean failed=false;try{new ImageImportSize(0,1,1);}catch(IllegalArgumentException e){failed=true;}check(failed);
    System.out.println("7000px import fitting and EXIF dimensions passed");
  }
}
