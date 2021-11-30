package com.example.netpayapi23;

public class Coor_xy {

    public Coor_xy() {
        //Inicia variables de uso en la Clase.
        //this.p = p;
        //this.pf = pf;
    }

    public  String co_xy( float X, float Y){
        //String z;
        String co_x = "";
        String co_y = "";
        float reso_pant_x = 1850;
        float reso_pant_y = 1080;
        float ini_pant_x = 50;
        float ini_pant_y = 10;
        reso_pant_x = reso_pant_x-ini_pant_x;
        reso_pant_y = reso_pant_y-ini_pant_y;
        float no_cel = 8;
        float tam_cel_y = reso_pant_y/no_cel;
        float tam_cel_x = reso_pant_x/no_cel;
        float x1,x2,x3,x4,x5,x6,x7,x8;
        float y1,y2,y3,y4,y5,y6,y7,y8;

        y1=tam_cel_y;
        y2=y1+tam_cel_y;
        y3=y2+tam_cel_y;
        y4=y3+tam_cel_y;
        y5=y4+tam_cel_y;
        y6=y5+tam_cel_y;
        y7=y6+tam_cel_y;
        y8=y7+tam_cel_y;

        x1=tam_cel_x;
        x2=x1+tam_cel_x;
        x3=x2+tam_cel_x;
        x4=x3+tam_cel_x;
        x5=x4+tam_cel_x;
        x6=x5+tam_cel_x;
        x7=x6+tam_cel_x;
        x8=x7+tam_cel_x;

        //OBTIENE X
        if ((X>0) && (X<=x1)){
            co_x = "A";
        }
        if ((X>x1) && (X<=x2)){
            co_x = "B";
        }
        if ((X>x2) && (X<=x3)){
            co_x = "C";
        }
        if ((X>x3) && (X<=x4)){
            co_x = "D";
        }
        if ((X>x4) && (X<=x5)){
            co_x = "E";
        }
        if ((X>x5) && (X<=x6)){
            co_x = "F";
        }
        if ((X>x6) && (X<=x7)){
            co_x = "G";
        }
        if ((X>x7) && (X<=x8)){
            co_x = "H";
        }

        //OBTIENE Y
        if ((Y>0) && (Y<=y1)){
            co_y = "1";
        }
        if ((Y>y1) && (Y<=y2)){
            co_y = "2";
        }
        if ((Y>y2) && (Y<=y3)){
            co_y = "3";
        }
        if ((Y>y3) && (Y<=y4)){
            co_y = "4";
        }
        if ((Y>y4) && (Y<=y5)){
            co_y = "5";
        }
        if ((Y>y5) && (Y<=y6)){
            co_y = "6";
        }
        if ((Y>y6) && (Y<=y7)){
            co_y = "7";
        }
        if ((Y>y7) && (Y<=y8)){
            co_y = "8";
        }

        String xy = co_x + co_y;
        if (xy.length()>0)
        {
            //Toast.makeText(this, xy, Toast.LENGTH_SHORT).show();
            //textView1.setText(xy);
            return xy;
        }
        //Toast.makeText(this, "0000", Toast.LENGTH_SHORT).show();
        return xy;
    }

}
