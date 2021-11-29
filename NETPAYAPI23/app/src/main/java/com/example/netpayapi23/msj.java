package com.example.netpayapi23;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;

public class msj extends Activity {

    private TextView tv01, tv02, tv03, tv04, tv05;
    Typeface sutil, normal, marcado;
    SQLiteDatabase bd;
    boolean es_fin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msj);

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "coba", null, 1);
        bd = admin.getWritableDatabase();

        //Tipo de Fuentes
        //sutil = Typeface.createFromAsset(getAssets(), "fonts/sutil.ttf");
        //normal = Typeface.createFromAsset(getAssets(), "fonts/normal.ttf");
        //marcado = Typeface.createFromAsset(getAssets(), "fonts/marcado.ttf");
        //Asignacion de TextView
        tv01 = (TextView)findViewById(R.id.textView1);
        tv02 = (TextView)findViewById(R.id.textView2);
        tv03 = (TextView)findViewById(R.id.textView3);
        tv04 = (TextView)findViewById(R.id.textView4);
        tv05 = (TextView)findViewById(R.id.textView5);

        //Asigna Fuentes
        //tv01.setTypeface(normal);
        //tv02.setTypeface(sutil);
        //tv03.setTypeface(marcado);
        //tv04.setTypeface(marcado);
        //tv05.setTypeface(marcado);

        //Asigna Tamaños de Texto
        tv01.setTextSize(60);
        tv02.setTextSize(60);
        tv03.setTextSize(60);
        tv04.setTextSize(60);
        tv05.setTextSize(60);

        //Cambio de fondo en los Textos
        tv01.setBackgroundColor(Color.TRANSPARENT);
        tv02.setBackgroundColor(Color.TRANSPARENT);
        tv03.setBackgroundColor(Color.TRANSPARENT);
        tv04.setBackgroundColor(Color.TRANSPARENT);
        tv05.setBackgroundColor(Color.TRANSPARENT);

        tv01.setTextColor(Color.YELLOW);
        tv02.setTextColor(Color.YELLOW);
        tv03.setTextColor(Color.YELLOW);
        tv04.setTextColor(Color.YELLOW);
        tv05.setTextColor(Color.YELLOW);

        //Oculta los TextView que no se usan
        tv02.setVisibility(View.INVISIBLE);
        tv03.setVisibility(View.INVISIBLE);
        tv04.setVisibility(View.INVISIBLE);
        tv05.setVisibility(View.INVISIBLE);


        //recive datos del accion anterior
        Bundle bundle = getIntent().getExtras();
        String msjcon = bundle.getString("msjcon");
        int index = bundle.getInt("index");

        es_fin = true;

        //Dividir una cadena en partes por |
        String[] partes = msjcon.split("\\>");
        String texto = partes[1];
        String tiempo_s = partes[2];
        String pos = partes[3];
        String tamano = partes[4];
        String tipo = partes[5];
        String estilo = partes[6];
        String fuente = partes[7];


        tv01.setText(texto);
        tv02.setText(texto);
        tv03.setText(texto);
        tv04.setText(texto);
        tv05.setText(texto);

        if (tiempo_s.equals("0")){
            mensaje_db();
        }else{
            timepo(tiempo_s, index);
        }


    }

    public void mensaje_db(){

        Cursor fila = bd.rawQuery("select * from config where num=1"
                + "", null);
        String espera_txt = "";
        if (fila.moveToFirst()) {
            espera_txt=fila.getString(2);
            if (espera_txt.length() > 0 ){
                tv01.setText(espera_txt);
            }else{
                es_fin = false;
            }
        }

        if (es_fin){
            //*****************************************************************
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    mensaje_db();
                }
            }, 100);
        }else{
            bd.close();
            finish();
        }


    }


    public void timepo(String timepo_s, final int index){
        int tiempo_num = 0;
        try {
            tiempo_num = NumberFormat.getInstance().parse(timepo_s).intValue();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (tiempo_num>0){
            tiempo_num = tiempo_num * 1000;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // acciones que se ejecutan tras los milisegundos
                    if (index==1){
                        finmain();
                    }else{
                        finish();
                    }
                }
            }, tiempo_num);
        }
    }

    public void finmain(){
        Intent i = new Intent(this, MainActivity.class );
        startActivity(i);
        finish();
    }
}
