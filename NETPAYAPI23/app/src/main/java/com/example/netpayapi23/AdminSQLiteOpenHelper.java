package com.example.netpayapi23;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

	public AdminSQLiteOpenHelper(Context context, String nombre, CursorFactory factory, int version) {
		super(context, nombre, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table config (num integer primary key, "+
				"pend text, "+			//1
				"pResultado text, "+	//2
				"max_tck text, "+		//3
				"IdUsuario text, "+		//4
				"pContrasena text, "+	//5
				"pAmbiente text)");		//6
				
		db.execSQL("INSERT INTO config (num, pend, IdUsuario, pContrasena, pAmbiente) VALUES ('1', '0', 'test@test.com', 'demo', '0') ");

		db.execSQL("create table pend (num integer primary key, "+
				"tipo text, "+		//
				"intentos text, "+	//Numero de intetos
				"confi, "+			//Confimacion de pendiente
				"secu text, "+		//secuendia de mensajes
				"servidor text, "+
				"imp_ext text, "+
				"mac_serial text, "+
				"nomad text, "+
				"pend text)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int versionAnte, int versionNue) {
		db.execSQL("drop table if exists config");
		db.execSQL("create table config (num integer primary key, "+
				"clv text, "+
				"num_tablet text, "+
				"mac text, "+
				"version text, "+
				"servidor text, "+
				"imp_ext text, "+
				"mac_serial text, "+
				"nomad text)"+
				"pend text)");
		/*db.execSQL("INSERT INTO idw (uso) VALUES ('0') ");*/						
	}	
}
