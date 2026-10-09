package art.velyntora.core;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.OutputStream;
public final class MainActivity extends Activity {
 private static final int SAVE_PNG=41;
 private DrawingView drawing;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);drawing=new DrawingView(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  LinearLayout tools=new LinearLayout(this);
  button(tools,"Pincel",()->drawing.setTool(DrawingView.BRUSH));
  button(tools,"Línea",()->drawing.setTool(DrawingView.LINE));
  button(tools,"Rectángulo",()->drawing.setTool(DrawingView.RECTANGLE));
  button(tools,"Elipse",()->drawing.setTool(DrawingView.ELLIPSE));
  button(tools,"Cubeta",()->drawing.setTool(DrawingView.BUCKET));
  HorizontalScrollView toolScroll=new HorizontalScrollView(this);toolScroll.addView(tools);root.addView(toolScroll);
  LinearLayout actions=new LinearLayout(this);
  button(actions,"Negro",()->drawing.setColor(Color.BLACK));
  button(actions,"Rojo",()->drawing.setColor(Color.RED));
  button(actions,"Borrador",()->{drawing.setColor(Color.WHITE);drawing.setTool(DrawingView.BRUSH);});
  button(actions,"Deshacer",()->drawing.undo());
  button(actions,"Rehacer",()->drawing.redo());
  button(actions,"Nuevo",()->drawing.clear());
  button(actions,"PNG",this::savePng);
  HorizontalScrollView actionScroll=new HorizontalScrollView(this);actionScroll.addView(actions);root.addView(actionScroll);
  root.addView(drawing,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
 }
 private void button(LinearLayout bar,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->action.run());bar.addView(b);}
 private void savePng(){Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/png");intent.putExtra(Intent.EXTRA_TITLE,"dibujo.png");startActivityForResult(intent,SAVE_PNG);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=SAVE_PNG||result!=RESULT_OK||data==null)return;
  Uri uri=data.getData();if(uri==null)return;
  try{
   Bitmap image=drawing.snapshot();
   if(image==null)throw new IllegalStateException("Snapshot failed");
   try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
    if(out==null||!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IllegalStateException("PNG failed");
    out.flush();
   }finally{image.recycle();}
   Toast.makeText(this,"PNG guardado",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"Error al guardar PNG",Toast.LENGTH_LONG).show();}
 }
}
