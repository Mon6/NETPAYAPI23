package com.example.netpayapi23

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import mx.com.netpay.sdk.api.models.transactions.Response
import mx.com.netpay.sdk.device.ConnectDiscoveryState
import mx.com.netpay.sdk.device.ConnectReaderDevice
import mx.com.netpay.sdk.listener.*
import mx.com.netpay.sdk.reports.NpReports
import mx.com.netpay.sdk.transactions.NpTransactions
import mx.com.netpay.sdk.utils.MiniPreferences
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.MarshalHashtable.NAMESPACE
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.io.*
import java.util.*
import javax.xml.transform.OutputKeys.METHOD


class MainActivity : AppCompatActivity(), ITransactionListener, IReportsListener {

    private val connectReader: IConnectReader by lazy { ConnectReaderDevice(this) }
    private lateinit var transaction: INpTransactions
    private lateinit var reports: INpReports
    private lateinit var miniPreferences: IMiniPreferences
    private var transID: String = "" //para el servicio del detalle de la compra
    private var orderId = "" //para el servicio del vouncher
    var bd: SQLiteDatabase? = null
    var admin: SQLiteOpenHelper? = null
    protected var IdUsuario: String? = null
    protected var pContrasena : String? = null
    private var pAmbienteDB = 0

    var pos: String? = null
    var lector:kotlin.String? = null
    var URL = "" //"http://192.168.1.38/Veribox/Veribox.php";
    var tds: String? = null
    var user_sol: String? = null
    var tipocompro: String? = null
    var tipo_cliente: String? = null
    var cliente: String? = null
    var tcks: String? = null

    private var monto_venta: String? = null

    var mac: String? = null
    var mac_serial: String? = null

    var tar_digi: String? = null
    var enti_banco_f: String? = null

    val version: String? = null
    val nomad_serie: String? = null
    val intentos: String? = null
    val guardar = "0"

    private val SOAPACTION = "urn:veriboxwsdl#veribox"

    var timer2 = Timer() //Tiempo espera para lanzar a VERIBOX
    var estado_lector = false

    private val puente: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            //3.-Pago Bancario - Respuesta de Servidor
            val muestra = msg.obj as String
            if (muestra == "") {
                msj1(">Problemas COM>2>3>4>5>6>7>")
                fin_f2()
            } else {
                //3.1.-Pago Bancario - Respuesta de Servidor Correcta
                busca_cadena1(muestra)
            }
        }
    }
    companion object {
        private const val BT_REQUEST_PERMISSION = 222
        private const val ENABLE_BT_REQUEST_CODE = 745
        private const val SIGNATURE = 111
        private val TAG = MainActivity::class.java.simpleName
        //    private const val userNameDefault = "rafael.jacobo@netpay.com"
        private const val userNameDefault = "netpay-mini-android-sdk@bylup.com"
        private const val userPassDefault = "Password123!"
        private var _ConfigSdk: ConfigSdk? = null

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //PREPARA ANTES DE COBRAR

        admin = AdminSQLiteOpenHelper(this, "coba", null, 1)
        bd = admin!!.getWritableDatabase()

        // Initialize actions for transactions
        transaction = NpTransactions(this, connectReader, this)
        reports = NpReports(this, this)
        miniPreferences = MiniPreferences(this)

        // Subscribe broadcast for finding device
        initializeDevice()
        _ConfigSdk = ConfigSdk.SANDBOX
        textView001.setText("" + _ConfigSdk)
        textView002.setText(userNameDefault)
        //TODO IMPORTANTE #1 setInitializeSDK
//    miniPreferences.setInitializeSDK(userNameDefault, "adm0n2", ConfigSdk.PRODUCTION)
        miniPreferences.setInitializeSDK(userNameDefault, userPassDefault, _ConfigSdk!!)


        btnListReport.setOnClickListener {
            reports.getReportSalesByDateAndUser(
                startDate = "2020-05-18",
                endDate = null,
                userId = null
            ) {
                runOnUiThread {
                    Log.w(TAG, it)
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }

            }
        }

        bReportDetails.setOnClickListener {
            //NpTransactions
            if (transID == "") {
                Toast.makeText(this, "Se necesita realizar transacción para obtener el transID", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Proceso Iniciado", Toast.LENGTH_SHORT).show()
                reports.getReportSalesDetail(
                    transID
                ) {
                    runOnUiThread {
                        Log.w(TAG, it)
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Start finding device and show results in initializeDevice()
        findDeviceAction.setOnClickListener {
            val permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), BT_REQUEST_PERMISSION)
            }
            startConnectReaderDevice()
        }

        // Start SDK configurations and start transaction
        startTransactionAction.setOnClickListener {
            processingTransaction()
        }

        // Start SDK configurations and start transaction
        actionImageVoucher.setOnClickListener {
            getImageVoucher()
        }

        initializeViewState()

        Leedb()

    }

    private fun initializeViewState() {
        val promotions = PromotionEnum.values().map { it.text }
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, promotions)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPromotion.apply {
            adapter = aa
        }
    }

    override fun onStart() {
        super.onStart()
        connectReader.registerReceiver()
    }

    override fun onStop() {
        connectReader.unregisterReceiver()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            /**
             * Permiso requerido cuando inicia la panrtalla pata tomar la geolocalizacion
             */
            BT_REQUEST_PERMISSION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Needs permission
                } else {
                    // try to connect
                    startConnectReaderDevice()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGNATURE) {
            val signaturePath = data?.extras?.getString("np_sdk_signature_path")?:""
            transaction.provideSignaturePath(signaturePath)
        }
    }

    /**
     * START: Transactions
     */

    private fun processingTransaction() {

        transaction.initialize(
            config = _ConfigSdk!!,
            userName = userNameDefault,
//            password = "adm0n2",
            password = userPassDefault,
            requireSignature = false
        ) {
            Log.i("INITIALIZE SDK MSG", it)
            runOnUiThread {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * START: ImageVoucher
     */

    private fun getImageVoucher() {

        if (orderId.isNotEmpty()) {
            transaction.getImageVoucher(
                orderId = orderId
            ) {
                runOnUiThread {
                    Log.w(TAG, it)
                }
            }
        } else {
            Toast.makeText(this, "Realiza una transacción exitosa para obtener el orderId", Toast.LENGTH_SHORT).show()
        }
    }

    override fun valuesToProcessing(values: (total: Double, tip: Double, reference: String, promotion: String) -> Unit) {
        val total = editTransValue.text.toString()

        val referenceText = editTextReference.text.toString()
        val referenceEncoded = encodeString(referenceText)

        val promotionIndex = spinnerPromotion.selectedItemPosition
        val promotionSelected = PromotionEnum.values().get(promotionIndex)

        values(total.toDouble(), 0.0, referenceEncoded, promotionSelected.value)
    }

    override fun selectedAppAction(applications: List<String>, action: (Int) -> Unit) {
        actionFirstApp.apply {
            text = applications[0]
            setOnClickListener { action(0) }
        }
        actionSecondApp.apply {
            text = applications[1]
            setOnClickListener { action(1) }
        }
    }

    override fun provideSignaturePathResult(intent: Intent) {
        startActivityForResult(intent, SIGNATURE)
    }

    override fun transactionsResult(message: String, processed: Boolean) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            if (processed) {
                enableButton(actionImageVoucher)
                enableButton(btnListReport)
                enableButton(bReportDetails)
                Log.d(TAG, "transactionsResult message: $message")
                refundLastTransactionId(message.split(":")[1])
                orderId = message.split(":")[2]
            }
        }
    }

    override fun posTransactionEnumResult(result: NpTransactionEnum) {
        Log.d(TAG, "posTransactionEnumResult")
    }

    private fun refundLastTransactionId(transactionId: String) {
        // Process to refound last transaction
        transID = transactionId
        refundAction.apply {
            text = transactionId
            setOnClickListener {
                transaction.processRefund(
                    transactionId = transactionId
                )
            }
        }
        //enableButton(refundAction)
    }

    override fun refundResult(message: String, processed: Boolean) {
        runOnUiThread {
            transID =""
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            refundAction.text = "refund: $message"
        }
    }

    override fun reverseResult(message: String, processed: Boolean) {
        runOnUiThread {
            Toast.makeText(this, "reverse: $message", Toast.LENGTH_SHORT).show()
        }
    }

    override fun errorResult(npErrorEnum: NpErrorEnum) {
        runOnUiThread {
            Toast.makeText(this, npErrorEnum.name, Toast.LENGTH_SHORT).show()
        }
    }

    override fun reportSaleDetailsResult(response: Response?, success: Boolean) {
        runOnUiThread {
            if(!success or response.toString().contains("valor nulo")){
                tvResponse.text = "Ha ocurrido un error"
                Log.d("Error Report Mensaje", response.toString())
            }
            else{
                tvResponse.text = response.toString()
            }

            Toast.makeText(this, "Proceso terminado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun startingPaymentProcessing() {

    }

    override fun imageResult(processed: Boolean, image: String?, message: String?) {
        runOnUiThread {
            if(processed){
                image?.let {
                    tvResponse.text = image
                    image_voucher.setImageBitmap(base64ToBitmap(image))
                }
            } else {
                Toast.makeText(this, "processed: $processed message: ${message ?: "null"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /**
     * END: Transactions
     */


    /**
     * START: Device adapter connection
     */

    private fun startConnectReaderDevice() {
        connectReader.findAdapterDevices { result ->
            if (!connectReader.adapterIsEnabled()) {
                startActivityForResult(connectReader.adapterEnableRequestIntent(), ENABLE_BT_REQUEST_CODE)
            }
            Log.i("FIND ADAPTER MSG", result.message)
        }
    }

    private fun initializeDevice() {
        connectReader.initialize { result ->
            when (result) {
                is ConnectDiscoveryState.Started -> {
                    Toast.makeText(this, "find device started", Toast.LENGTH_SHORT).show()
                }
                is ConnectDiscoveryState.Found -> {
                    val device = result.deviceConnected
                    connectAndCancelFindingDevice(result.deviceConnected.deviceName, result.deviceConnected.deviceAddress)

                    startTransactionAction.apply{
                        text = "Iniciar Transacción: ${device.deviceName}"
                    }
                    enableButton(startTransactionAction)

                    Toast.makeText(this, "Device found: ${device.deviceName}", Toast.LENGTH_SHORT).show()
                }
                is ConnectDiscoveryState.Finished ->  {
                    Toast.makeText(this, "finish finding device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun connectAndCancelFindingDevice(deviceName: String, deviceAddress: String) {
        connectReader.registerDevice(deviceName, deviceAddress)
    }
/**/
    /**
     * END: Device adapter connection
     */


    enum class PromotionEnum(val value: String, val text: String) {
        _DEFAULT("000000", "Sin promoción"),
        _3MONTHS("000303", "3 Meses"),
        _6MONTHS("000603", "6 Meses"),
        _9MONTHS("000903", "9 Meses"),
        _12MONTHS("001203", "12 Meses"),
        _18MONTHS("001803", "18 Meses")
    }

    private fun encodeString(s: String): String {
        var data = ByteArray(0)
        try {
            data = s.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } finally {
            return Base64.encodeToString(data, Base64.DEFAULT)
        }
    }
    override fun reportSalesByDateAndUserResult(processed: Boolean, message: String?, reportSales: Response?) {
        Log.i(TAG, "reportSalesByDateAndUserResult")
        reportSales?.let {
            Log.i(TAG, "reportSales.report?.valueList?.value?.size: ${reportSales.report?.valueList?.value?.size}")
            tvResponse.text = reportSales.report?.valueList.toString()
            //Gson
            //val sales = fromJson(reportSales.report?.valueList, SalesResponseValue::class.java).value
        }
        message?.let {
            Log.i(TAG, "message: $message")
        }
    }
    private fun enableButton(btn: Button) {
        btn.apply {
            setTextColor(Color.WHITE)
            setBackgroundResource(R.color.purple_500)
        }
    }
    private fun base64ToBitmap(b64: String): Bitmap? {
        val imageAsBytes = Base64.decode(b64.toByteArray(), Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
    }

//TODO -----------------------------------------------------------------------------------------------------------------------

    open fun busca_cadena1(xml: String) {
        guarda_log("busca_cadena1.$xml", false)
        var busca1 = "pago-bancario"
        var busca2 = "respg"
        var dato: String = regresa_xml(xml, busca1, busca2)
        //textView_con.setText(dato);
        if (dato == "true") {
            guarda_log("busca_cadena1.dato.equals(\"true\")", false)
            if (guardar == "0") {
                busca2 = "montopg"
                dato = regresa_xml(xml, busca1, busca2)
                monto_venta = dato
                //De String a Flotante
                val f = monto_venta!!.toFloat()
                monto_venta = java.lang.Float.toString(f)

                //Formatear String a Decimal
                val sf = String.format("%.2f", f)

                //5.-Pago Bancario - Asigna Monto
                textView1.setText("MONTO")
                textView2.text = "$$sf"
                monto_venta = sf
                //Toast.makeText(this, "T1", Toast.LENGTH_SHORT).show();
                //6.-Pago Bancario - Conecta con NOMAD2
                if (mx.qpay.netpay.QPListener.qplclController == null) {
                    //Toast.makeText(this, "T2", Toast.LENGTH_SHORT).show();
                    qplclListener = mx.qpay.netpay.QPListener.myQpaySDkController()
                    //Toast.makeText(this, "T3", Toast.LENGTH_SHORT).show();
                    guarda_log("busca_cadena1.fnSolicitarTipoLector", false)
                    fnSolicitarTipoLector()
                }
            } else {
                Toast.makeText(this, "REVISANDO 3", Toast.LENGTH_SHORT).show()
                if (sig_tckfac) {
                    //Codigo que envia el mensaje de solicitar Ticker o Factura
                    //fin_ok();
                    fin_f2()
                } else {
                    fin_f2()
                }
            }
        } else {
            if (dato == "false") {
                busca1 = "display"
                busca2 = "dato-impresiond"
                dato = regresa_xml(xml, busca1, busca2)
                msj1(">$dato>2>3>4>5>6>7>")
                Toast.makeText(this, "xml false", Toast.LENGTH_SHORT).show()
                fin_f2()
            } else {
                msj1(">NO DISPONIBLE>2>3>4>5>6>7>")
                //gen_xml("", "", "", "", "", "", "", "", "", "", "", "", false, "");
                Toast.makeText(this, "xml false 2", Toast.LENGTH_SHORT).show()
                fin_f2()
            }
        }
    }



    open fun fnDesconectaDispositivo() {
        //Toast.makeText(getApplicationContext(), "Funcion Desconecta", Toast.LENGTH_LONG).show();
//        qplclController.qpDesconectaDispositivo()                                                 TODO -> PENDIENTE PO IMPLEMENTAR
    }

        fun lanzaVeri() {
            timer2.cancel()
            if (estado_lector) {
                fnDesconectaDispositivo()
            }

        }

        private var Accion2: Runnable? = Runnable { //Funcion a ejecutar
                lanzaVeri()
                Toast.makeText(applicationContext, "lanzaVeri!", Toast.LENGTH_LONG).show()
            }

        open fun hiloVeri() {
                //Aun dentro del mismo Hilo
                runOnUiThread(Accion2)
            }

        open fun fin_f2() {
            //stopConnection();
            timer2.scheduleAtFixedRate(object : TimerTask() {
                 override fun run() {
                    //Ejecuta
                    hiloVeri()
                }
            }, 2000, 4000)
        }

        open fun msj1(muestra: String?) {
            //msj1(">->0>3>4>5>6>7>");
            val j = Intent(this, msj::class.java)
            j.putExtra("msjcon", muestra)
            //j.putExtra("tiempo", tiempo);
            startActivity(j)
            //Toast.makeText(this, "msj1", Toast.LENGTH_SHORT).show();
        }

    fun Leedb(){
        val fila = bd!!.rawQuery(
            "select * from config where num=1"
                    + "", null
        )

        if (fila.moveToFirst()) {
            IdUsuario=fila.getString(4);
            pContrasena=fila.getString(5);
            pAmbienteDB=fila.getInt(6);

            textView003.setText(""+IdUsuario+" - "+pContrasena+" - "+pAmbienteDB)
            if (pAmbienteDB == 1){
                //b_bk.setVisibility(View.INVISIBLE); //Muestra que es DEMO
                //Toast.makeText(this, "PRODUCCION.", Toast.LENGTH_SHORT).show();

                //textView27.setVisibility(View.INVISIBLE);  // TODO -> MODO DEMO COMENTADO DEBIDO A FALTA DE USO
            }else{
                //Toast.makeText(this, "MODO DEMO.", Toast.LENGTH_SHORT).show();
            }

        } else{
            //Toast.makeText(this, "NO lee DB", Toast.LENGTH_SHORT).show();
        }

    }

    fun guarda_log(con_fin: String?, gra: Boolean) {
        // Creamos una carpeta "AIE" dentro del directorio "/"
        // Con el método "mkdirs()" creamos el directorio si es necesario
        val path = File(Environment.getExternalStorageDirectory(), "Tickets")
        path.mkdirs()

        if (gra){
        //Una vez creado disponemos de un archivo para guardar datos
        try {
            val ruta_sd = Environment.getExternalStorageDirectory()
            val f = File(ruta_sd.absolutePath, "Tickets/log.txt")
            val fout = OutputStreamWriter(
                FileOutputStream(f)
            )
            fout.write(con_fin)
            fout.close()
            //Toast.makeText(this, "Texto de prueba.3", Toast.LENGTH_SHORT).show();
        } catch (ex: Exception) {
            Log.e("Ficheros", "Error al escribir fichero a tarjeta SD  QP3")
        }
        }else{
            con_fin + "\n"
        }
    }

    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            true
        } else false
    }

    @Throws(IOException::class)
    fun loadFileAsString(filePath: String?): String? {
        val data = StringBuffer(1000)
        val reader = BufferedReader(FileReader(filePath))
        val buf = CharArray(1024)
        var numRead = 0
        while (reader.read(buf).also { numRead = it } != -1) {
            val readData = String(buf, 0, numRead)
            data.append(readData)
        }
        reader.close()
        return data.toString()
    }

    fun getMacAddress(): String? {
        try {
            return loadFileAsString("/sys/class/net/eth0/address")
                ?.toUpperCase()?.substring(0, 17)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun envia(salida: String?) {
        guarda_log("ENVIA", false)
        Thread {
            var respuesta = ""
            val request = SoapObject(NAMESPACE, METHOD)
            request.addProperty("d0", tds)
            request.addProperty("d1", salida)
            request.addProperty("d2", "")
            request.addProperty("d3", "")
            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
            envelope.dotNet = true
            envelope.setOutputSoapObject(request)
            val androidHttpTransport = HttpTransportSE(URL)
            try {
                androidHttpTransport.call(SOAPACTION, envelope)
                respuesta = envelope.getResponse().toString()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            val sms = Message()
            sms.obj = respuesta
            puente.sendMessage(sms)
        }.start()
    }

    fun gen_xml(
        salida: String?,
        descrip: String?,
        num_trans: String,
        codigoAprobacion: String,
        digitos_tar: String,
        h_f_sf: String,
        marcaTarjeta: String,
        bancoEmisor: String,
        arqc: String,
        aid: String,
        titular: String,
        tsi: String,
        res_pago: Boolean,
        monto_venta_s: String,
        vigencia: String,
        terminalID: String,
        numeroControl: String,
        referenciaBanco: String,
        tvr: String,
        apn: String,
        afiliacion: String,
        TipoTarjeta: String
    ) {
        var digitos_tar = digitos_tar
        var monto_venta_s = monto_venta_s
        guarda_log("GEN_XML.$res_pago", false)
        Toast.makeText(this, "CON DATOS", Toast.LENGTH_SHORT).show()
        if (digitos_tar.length > 15) {
            digitos_tar = digitos_tar.substring(15)
            //Toast.makeText(this, "CON DATOS LLEGO BIEN", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "CON DATOS LLEGO BIEN", Toast.LENGTH_SHORT).show();
            monto_venta_s = monto_venta_s.replace(" ", "")
            tar_digi = digitos_tar
            enti_banco_f = bancoEmisor

            //MSM 03/Oct/2017 Ver:1.8-0
            //Se Coloca un parametro mas al Pago Bancario para que marque los tickets cobrados
            Toast.makeText(this, "REVISANDO 2", Toast.LENGTH_SHORT).show()
            val tipo = "PB"
            val text: String = ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "<peticion>\n" +
                    "   <mensaje-tipo tipo=\"" + tipo + "\"></mensaje-tipo>\n" +
                    "	<envio tds=\"" + tds + "\" mac=\"" + mac + "\" version=\"" + version + "\" mac_serial=\"" + mac_serial + "\" nomad=\"" + nomad_serie + "\" intentos=\"" + intentos + "\"></envio>\n" +
                    "   <datos>\n" +
                    "       <posicion pos=\"" + pos + "\"></posicion>\n" +
                    "   	<usuario user_sol=\"" + user_sol + "\"></usuario>\n" +
                    "       <pago-bancario solicito=\"" + res_pago + "\" processed=\"\" tranzaccion=\"cobro\" guardar=\"" + guardar + "\">\n" +
                    "   	<comprobante tipo_compro=\"" + tipocompro + "\" tipo_cliepb=\"" + tipo_cliente + "\" clientepb=\"" + cliente + "\" >\n" +
                    "       </comprobante>\n" +
                    "       <registra bancoEmisor=\"" + bancoEmisor + "\" cardNumber=\"" + digitos_tar + "\" tipotarjeta=\"" + TipoTarjeta + "\" hora_fecha=\"" + h_f_sf + "\" serieTDS=\"" + nomad_serie + "\" aid=\"" + aid + "\" error_description=\"\" transacc=\"" + num_trans + "\" monto=\"" + monto_venta_s + "\" arqc=\"" + arqc + "\" codigoAprobacion=\"" + codigoAprobacion + "\" marcaTarjeta=\"" + marcaTarjeta + "\"" +
                    "       vigencia=\"" + vigencia + "\" titular=\"" + titular + "\" terminalID=\"" + terminalID + "\" numeroControl=\"" + numeroControl + "\" referenciaBanco=\"" + referenciaBanco + "\" tvr=\"" + tvr + "\" tsi=\"" + tsi + "\" apn=\"" + apn + "\" afiliacion=\"" + afiliacion + "\" tckspb=\"" + tcks + "\" ></registra>\n" +
                    "       </pago-bancario>\n" +
                    "   </datos>\n" +
                    "</peticion>")

            /*
        byte[] data = null;
        try {
            data = text.getBytes("UTF-8");
        }catch (UnsupportedEncodingException e1){
            System.out.println(e1.getMessage());
        }

        String base64 = "";
        try {
            base64 = Base64.encodeBytes(data);      // Receiving side
        }catch (Exception err){
            System.out.println(err.toString());
        }
        */
            //guarda_log ("GEN_XML.envia", false);
            envia(text)
        }
    }

    fun lee_doc() {
        guarda_log("LEE_DOC", false)
        val todo = ""
        if (isExternalStorageReadable()) {
            //File rFileE = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), EXTERNAL_FILENAME);
            //File rFileE = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AIE3.txt");
            val rFileE = File(Environment.getExternalStorageDirectory(), "Tickets/PBDATA.txt")
            try {
                val bReader = BufferedReader(FileReader(rFileE))
                var textRead: String
                //while((textRead = bReader.readLine()) != null)
                //todo=todo+textRead;

                pos = bReader.readLine()
                lector = bReader.readLine()
                URL = bReader.readLine()
                tds = bReader.readLine()
                user_sol = bReader.readLine()
                tipocompro = bReader.readLine()
                tipo_cliente = bReader.readLine()
                cliente = bReader.readLine()
                tcks = "--"

                //MSM 03/Oct/2017 Ver:1.8-0
                if (pos == "99") {
                    //Asigna el Monto para el Cobro Bancario
                    monto_venta = bReader.readLine()
                    textView2.text = "$$monto_venta"
                    //Tickets que se enviaran a marcar como cobrados
                    tcks = bReader.readLine()
                }


                //Cieraa archivo
                bReader.close()
                //Borra archivo.
                rFileE.delete()
                guarda_log("LEE_DOC.borra archivo", false)

                /*
                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                mac = info.getMacAddress();
                mac = mac.toUpperCase();
                */try {
                    mac = ""
                    val getMacetho: String? = getMacAddress()
                    if (getMacetho.length > 0) {
                        mac = getMacetho
                    } else {
                        try {
                            val manager =
                                applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                            val info = manager.connectionInfo
                            mac = info.macAddress.toUpperCase()
                            Toast.makeText(this, "MAC INALAMBRICA$mac", Toast.LENGTH_SHORT).show()
                        } catch (e1: java.lang.Exception) {
                            mac = "Error MAC."
                        }

                        // mac = "Error MAC.";
                    }
                } catch (e: java.lang.Exception) {
                    mac = "Error MAC2."
                    /*
					try {
						String macetho2 = loadFileAsString("/sys/class/net/eth0/address");
						if (macetho2.length() > 0 ){
							address = address + macetho2 + "=";
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						address = address + "Exception"+ "]";
						e1.printStackTrace();
					}
					*/
                }
                mac_serial = "---"
                //nomad_serie="WP02840312150094";
                textView003.setText(lector)
                URL = "http://$URL/Veribox/Veribox.php"
                //1.-Pago Bancario - Solicita monto de venta
                //Enviando=">"+num_tabled+">"+mac+">"+version+">"+mac_serial+">"+nomad+">"+intentos+">";

                //MSM 03/Oct/2017 Ver:1.8-0
                if (pos == "99") {
                    //Directamente envia a Conectar NOMAD2
                    //1-Pago Bancario - Conecta con NOMAD2

//                    if (qplclController == null) {                                        TODO -> PENDIENTE DE IMPLEMENTAR
//                        qplclListener = myQpaySDkController()                             TODO -> PENDIENTE DE IMPLEMENTAR
//                        guarda_log("busca_cadena1.fnSolicitarTipoLector", false)          TODO -> PENDIENTE DE IMPLEMENTAR
//                        fnSolicitarTipoLector()                                           TODO -> PENDIENTE DE IMPLEMENTAR
//                    }

                } else {
                    //MSJ=0;
                    //mensajes("");
                    Toast.makeText(this, "DESDE: LEE_DOC", Toast.LENGTH_SHORT).show()
                    guarda_log("LEE_DOC.gen_xml", false)
                    gen_xml("", "", "", "", "", "", "", "", "", "", "", "", true, "", "", "", "", "", "", "", "", "")

                    //String textRead = bReader.readLine();
                    //tv1.setText(todo);
                    //Toast.makeText(this, pos, Toast.LENGTH_SHORT).show();
                }
            } catch (e: IOException) {
                Toast.makeText(this, "NO Lee parametros de entrada", Toast.LENGTH_SHORT).show()
                fin_f2()
            }
        } else {
            Toast.makeText(this, "NO hay acceso al Almacenamiento.", Toast.LENGTH_SHORT).show()
            fin_f2()
        }
    }

    fun busca_doc() {
        Toast.makeText(this, "BUSCA ARCHIVO VENTA", Toast.LENGTH_SHORT).show()
        var contenido = ""
        var encontro = false
        //Defino la ruta donde busco los ficheros
        val f = File(Environment.getExternalStorageDirectory().toString() + "/Tickets/")
        //Creo el array de tipo File con el contenido de la carpeta
        val files = f.listFiles()
        //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
        var i = 0
        while (i < files.size) {
            //Sacamos del array files un fichero
            val file = files[i]
            //Si es directorio...
            if (file.isDirectory())  contenido=contenido+(file.name + "/ carpeta\n")
                //Si es fichero...
            else {
                if (file.name == "PBDATA.txt") {
                    lee_doc()
                    encontro = true
                    i = files.size
                }
            }
            i++
        }
        if (encontro) {
            esp()
            Toast.makeText(this, "ARCHIVO ENCONTRADO", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "NO ENCONTRADO", Toast.LENGTH_SHORT).show()
            fin_f2()
        }
    }

    fun lee_docConf() {
        //Toast.makeText(this, "Entra 1", Toast.LENGTH_SHORT).show();
        var ambiente = ""
        var usrPago = ""
        var contra = ""
        var salir = false
        if (isExternalStorageReadable()) {
            //Toast.makeText(this, "Entra 2", Toast.LENGTH_SHORT).show();
            //File rFileE = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), EXTERNAL_FILENAME);
            //File rFileE = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AIE3.txt");
            val rFileE = File(Environment.getExternalStorageDirectory(), "Tickets/PBCONF.txt")
            try {
                val bReader = BufferedReader(FileReader(rFileE))
                var textRead: String
                //while((textRead = bReader.readLine()) != null)
                //todo=todo+textRead;
                //Toast.makeText(this, "Entra 3", Toast.LENGTH_SHORT).show();
                ambiente = bReader.readLine()

                //Toast.makeText(this, ambiente, Toast.LENGTH_SHORT).show();
                if (ambiente == "0") {
                    //Toast.makeText(this, "Configura test", Toast.LENGTH_SHORT).show();
                    usrPago = "test@test.com"
                    contra = "demo"

                    //----------------------------------------------------
                    val registro = ContentValues()
                    registro.put("pAmbiente", ambiente)
                    registro.put("pContrasena", contra)
                    registro.put("IdUsuario", usrPago)
                    val cant = bd!!.update("config", registro, "num=1", null)
                    if (cant == 1) {
                        //Toast.makeText(this, "ACTUALIZO a PRUEBAS", Toast.LENGTH_SHORT).show();
                    }
                    //else
                    //Toast.makeText(this, "NO Graba en DB", Toast.LENGTH_SHORT).show();
                    //----------------------------------------------------
                    busca_doc()
                } else {
                    //if (ambiente.equals("1")){
                    //Detiene el contero para cerrar.
                    if (pAmbienteDB == 1) {
                        Toast.makeText(this, "Ya es PRODUCCION", Toast.LENGTH_SHORT).show()
                        busca_doc()
                        salir = true
                    } else {
                        //Toast.makeText(this, "Solicita DATOS", Toast.LENGTH_SHORT).show();
                        val intent = Intent(this, User_pass::class.java)
                        startActivityForResult(intent, 0)
                    }
                }
                bReader.close()

                //Borra archivo.
                rFileE.delete()
                //Toast.makeText(this, "Entra 4", Toast.LENGTH_SHORT).show();
            } catch (e: IOException) {
                Toast.makeText(this, "NO Lee parametros de entrada", Toast.LENGTH_SHORT).show()
                fin_f2()
            }
        }
    }

    fun buscaConf() {
        guarda_log("BUSCA_DOCCONFIG", false)
        var contenido = ""
        var encontro = false
        //Defino la ruta donde busco los ficheros
        val f = File(Environment.getExternalStorageDirectory().toString() + "/Tickets/")
        //Creo el array de tipo File con el contenido de la carpeta
        val files = f.listFiles()
        //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
        var i = 0
        while (i < files.size) {

            //Sacamos del array files un fichero
            val file = files[i]
            //Si es directorio...
            if (file.isDirectory)
                contenido = contenido +(file.name + "/ carpeta\n")
            //Si es fichero...
            else {
                    if (file.name == "PBCONF.txt") {
                        Toast.makeText(this, "configura", Toast.LENGTH_SHORT).show()
                        encontro = true
                        i = files.size
                        lee_docConf()
                    }
                }
            i++
        }
        if (encontro) {
            Toast.makeText(this, "ENCONTRO CONF", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "NO ENCONTRO CONF", Toast.LENGTH_SHORT).show()
            busca_doc()
        }
    }





}



