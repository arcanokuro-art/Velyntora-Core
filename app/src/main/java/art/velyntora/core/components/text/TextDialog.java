package art.velyntora.core;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.*;

/** Edits retained text; applying updates the modal object, ✓ commits the layer. */
final class TextDialog {
  final MainActivity host;
  TextDialog(MainActivity host){this.host=host;}
  void configureText(int x,int y){
    if(host.readProjectProgress()!=null || host.readDrawing().placedImage().active())return;
    TextSpec initial=new TextSpec("Texto",32,host.readDrawing().readColor(),0,0,Math.max(8,Math.min(6000,host.readDrawing().documentWidth()-x)));
    show(initial,x,y,false);
  }
  static AlertDialog edit(MainActivity host){PlacedImageSession s=host.readDrawing().placedImage();if(s.active() && s.text!=null && host.readProjectProgress()==null)return new TextDialog(host).show(s.text,0,0,true);return null;}
  AlertDialog show(TextSpec initial,int x,int y,boolean editing){
    LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);
    EditText input=new EditText(host);input.setContentDescription("Contenido del texto");input.setHint("Escribe tu texto");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setMinLines(3);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4096)});input.setText(editing?initial.content:"");form.addView(input);
    EditText size=number(form,"Tamaño del texto en píxeles",initial.size);
    Spinner family=new Spinner(host);family.setContentDescription("Fuente del texto");family.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,new String[]{"Sans serif","Serif","Monoespaciada"}));family.setSelection(initial.family);form.addView(family);
    CheckBox bold=new CheckBox(host),italic=new CheckBox(host);bold.setText("Negrita");bold.setChecked((initial.style&1)!=0);italic.setText("Cursiva");italic.setChecked((initial.style&2)!=0);form.addView(bold);form.addView(italic);
    EditText color=new EditText(host);color.setSingleLine(true);color.setContentDescription("Color del texto hexadecimal ARGB");color.setText(String.format(java.util.Locale.ROOT,"%08X",initial.color));form.addView(host.text("Color ARGB (AARRGGBB):"));form.addView(color);
    EditText wrap=number(form,"Ancho máximo del texto en píxeles",initial.wrapWidth);
    form.addView(host.text("Cada texto tiene su propia capa. Después de ✓ puedes seleccionarla y editar lo escrito."));
    AlertDialog dialog=new AlertDialog.Builder(host).setTitle(editing?"Editar texto":"Añadir texto").setView(host.scrollForm(form)).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();
    dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
      int px,width,argb;
      try{px=Integer.parseInt(size.getText().toString());if(px<4 || px>256)throw new NumberFormatException();}catch(NumberFormatException error){size.setError("Usa de 4 a 256 px");return;}
      try{width=Integer.parseInt(wrap.getText().toString());if(width<8 || width>6000)throw new NumberFormatException();}catch(NumberFormatException error){wrap.setError("Usa de 8 a 6000 px");return;}
      try{String hex=color.getText().toString().trim().replace("#","");if(hex.length()!=6 && hex.length()!=8)throw new IllegalArgumentException();argb=Color.parseColor("#"+hex);}catch(IllegalArgumentException error){color.setError("Usa RRGGBB o AARRGGBB");return;}
      TextSpec next;try{next=new TextSpec(input.getText().toString(),px,argb,family.getSelectedItemPosition(),(bold.isChecked()?1:0)|(italic.isChecked()?2:0),width);}catch(IllegalArgumentException error){input.setError("Escribe de 1 a 4096 caracteres visibles");return;}
      dialog.dismiss();host.readDrawing().placedImage().applyText(next,x,y,editing);
    }));dialog.show();return dialog;
  }
  EditText number(LinearLayout form,String label,int value){form.addView(host.text(label+":"));EditText input=new EditText(host);input.setContentDescription(label);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(""+value);form.addView(input);return input;}
}
