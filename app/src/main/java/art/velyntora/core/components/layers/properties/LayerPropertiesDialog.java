package art.velyntora.core;
import android.app.AlertDialog;
import android.widget.*;
import android.text.*;
import android.text.InputType;
final class LayerPropertiesDialog {
 static final String[] MODES={"Normal","Multiplicar","Subexponer el color","Sobreexponer el color","Superponer","Diferencia","Aclarar","Oscurecer","Trama","O exclusivo","Luz fuerte","Luz suave","Color","Luminosidad","Tono","Saturación"};
 static AlertDialog show(MainActivity host,int index){
 LinearLayout form=new LinearLayout(host);form.setOrientation(1);form.setPadding(host.dp(20),host.dp(12),host.dp(20),host.dp(12));
 form.addView(host.text("Nombre:"));EditText name=new EditText(host);name.setSingleLine(true);name.setText(host.readDrawing().layerName(index));form.addView(name,new LinearLayout.LayoutParams(-1,-2));
 CheckBox visible=new CheckBox(host);visible.setText("Visible");visible.setChecked(host.readDrawing().layerVisible(index));form.addView(visible);
 form.addView(host.text("Modo de mezcla:"));Spinner mode=new Spinner(host);ArrayAdapter<String> adapter=new ArrayAdapter<>(host,android.R.layout.simple_spinner_item,MODES);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);mode.setAdapter(adapter);mode.setSelection(host.readDrawing().layerBlendMode(index));form.addView(mode,new LinearLayout.LayoutParams(-1,host.dp(48)));
 form.addView(host.text("Opacidad:"));LinearLayout row=host.row();EditText number=new EditText(host);number.setSingleLine(true);number.setInputType(InputType.TYPE_CLASS_NUMBER);number.setText(""+Math.round(host.readDrawing().layerOpacity()*100));row.addView(number,new LinearLayout.LayoutParams(host.dp(64),-2));SeekBar slider=new SeekBar(host);slider.setMax(100);slider.setProgress(Math.round(host.readDrawing().layerOpacity()*100));slider.setContentDescription("Opacidad");row.addView(slider,new LinearLayout.LayoutParams(0,host.dp(48),1));form.addView(row);
 slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){if(user)number.setText(""+v);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
 number.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){try{int v=Integer.parseInt(s.toString());if(v>=0&&v<=100)slider.setProgress(v);}catch(NumberFormatException e){}}public void afterTextChanged(Editable e){}});
 AlertDialog dialog=new AlertDialog.Builder(host).setTitle("Propiedades de capa").setView(host.scrollForm(form)).setNegativeButton("Cancelar",null).setPositiveButton("Aceptar",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{int opacity;try{opacity=Integer.parseInt(number.getText().toString());}catch(NumberFormatException e){number.setError("Usa un valor entre 0 y 100");return;}if(opacity<0||opacity>100){number.setError("Usa un valor entre 0 y 100");return;}if(host.readProjectProgress()!=null)return;if(!host.readDrawing().applyLayerProperties(index,name.getText().toString().trim(),visible.isChecked(),opacity/100f,mode.getSelectedItemPosition())){name.setError("Nombre inválido (máximo 4096 bytes UTF-8)");return;}dialog.dismiss();host.refreshLayerPanel();}));dialog.show();return dialog;
 }
}
