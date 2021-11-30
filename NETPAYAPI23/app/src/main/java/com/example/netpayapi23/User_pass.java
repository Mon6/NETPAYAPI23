package com.example.netpayapi23;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class User_pass extends Activity {

    private EditText editText16 ,editText17, editText018, editText019;
    SQLiteDatabase bd;
    boolean permite_salir;
    private String Enviando;
    private String tds;
    private String usr, passw;
    //Captura de XY
    private TextView textView_xy ;
    StringBuilder stringBuilder = new StringBuilder();
    private Coor_xy cox_coy;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.user);


        //Oculta teclado virtual
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        editText16 = (EditText)findViewById(R.id.editText16);
        editText17 = (EditText)findViewById(R.id.editText17);
        editText018 = (EditText)findViewById(R.id.editText018);
        editText019 = (EditText)findViewById(R.id.editText019);
        //editText17.setFocusable(true);
        //editText018.setFocusable(false);
        editText17.requestFocus();
        //editText018.setOnKeyListener(this);

        //Inicia captura de Coordenadas XY
        //Coor_xy
        cox_coy= new Coor_xy();
        this.textView_xy = (TextView) findViewById( R.id.strXY );
        //this.textView_xy.setText("X: ,Y: ");//texto inicial

        //Evento Touch
        this.textView_xy.setOnTouchListener( new View.OnTouchListener()
        {
            @Override
            public boolean onTouch( View arg0, MotionEvent arg1 ) {

                stringBuilder.setLength(0);
                //si la acción que se recibe es de movimiento
                if( arg1.getAction() == MotionEvent.ACTION_UP )
                {
                    //stringBuilder.append("Moviendo, X:" + arg1.getX() + ", Y:" + arg1.getY() );
                    //stringBuilder.append( "Detenido, X:" + arg1.getX() + ", Y:" + arg1.getY() );
                    float co_X = arg1.getX();
                    float co_Y = arg1.getY();
                    //mr_co_re(co_X, co_Y);
                    String rev=cox_coy.co_xy(co_X, co_Y);
                    revisar (rev);
                    //accion( co_X, co_Y);
                }
                //Se muestra en pantalla
                //textView.setText( stringBuilder.toString() );
                return true;
            }
        });
        //FIN Cptura de Coordenadas XY

    }


    //Funcion Acciones
    public void revisar (String rev_coor){
        //Boton a HOME
        if (rev_coor.equals("H1") || rev_coor.equals("G1")){
            guardar(null);
        }
    }

    public void guardar(View view){


        String usuario = editText17.getText().toString();
        String conf_usuario = editText16.getText().toString();
        String pass_rev = editText018.getText().toString();
        String conf_pass_rev = editText019.getText().toString();

        if (conf_pass_rev.length()>0 && usuario.length()>3 && pass_rev.length()>0 && conf_pass_rev.length()>0){

            if (usuario.equals(conf_usuario) && pass_rev.equals(conf_pass_rev)){
                AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this,"coba", null, 1);
                bd = admin.getWritableDatabase();

                //----------------------------------------------------
                ContentValues registro = new ContentValues();
                registro.put("pAmbiente", "1");
                registro.put("pContrasena", pass_rev);
                registro.put("IdUsuario", usuario);

                msj_error("CONFIGURACION CORRECTA");

                int cant = bd.update("config", registro, "num=1" , null);
                if (cant == 1){
                    Toast.makeText(this, "ACTUALIZO A PRODUCCION", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("res", "ok");
                    setResult(RESULT_OK, intent);
                    permite_salir = true;
                    finish();
                }
                else
                    Toast.makeText(this, "NO Graba en DB", Toast.LENGTH_SHORT).show();
                //----------------------------------------------------
            }else{
                editText17.setText("");
                editText16.setText("");
                editText018.setText("");
                editText019.setText("");
                editText17.requestFocus();
                msj_error("Los datos no coinciden\nverifique su informacion");
            }
        }else{
            msj_error("COMPLETE TODO\nLOS CAMPOS");
            editText17.requestFocus();
        }
    }


    public void msj_error(String muestra){
        //editText17.requestFocus();
        muestra = ">"+muestra+">2>3>4>5>6>7>";
        Intent j = new Intent(this, msj.class );
        j.putExtra("msjcon", muestra);
        startActivity(j);
    }
    //Deshabilitar BOTON atras
	/*
	@Override
	public void onBackPressed() {
	}
	*/
    /*onPause(): Indica que la actividad está a punto de ser lanzada a segundo plano,
     * Es el lugar adecuado para No permir que se salga de la aplicacion y regresarla de forma forzada*/
	/*
	@Override
		protected void onPause() {
		   super.onPause();
		   if (permite_salir){
			   Toast.makeText(this, "PERMITE", Toast.LENGTH_SHORT).show();
			   permite_salir = false;
		   }else{
			   Intent i = new Intent(this, MainActivity.class );
			   i.putExtra("inicia", 1);
			   startActivity(i);
			   finish();

			   Toast.makeText(this, "DEBE REGRESAR", Toast.LENGTH_SHORT).show();
		   }
		}
		*/
}