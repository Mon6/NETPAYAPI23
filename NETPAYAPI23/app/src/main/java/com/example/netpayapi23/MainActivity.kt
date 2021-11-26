package com.example.netpayapi23

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import mx.com.netpay.sdk.api.models.transactions.Response
import mx.com.netpay.sdk.device.ConnectDiscoveryState
import mx.com.netpay.sdk.device.ConnectReaderDevice
import mx.com.netpay.sdk.listener.*
import mx.com.netpay.sdk.reports.NpReports
import mx.com.netpay.sdk.transactions.NpTransactions
import mx.com.netpay.sdk.utils.MiniPreferences
import java.io.UnsupportedEncodingException
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity(), ITransactionListener, IReportsListener {

    private val connectReader: IConnectReader by lazy { ConnectReaderDevice(this) }
    private lateinit var transaction: INpTransactions
    private lateinit var reports: INpReports
    private lateinit var miniPreferences: IMiniPreferences
    private var transID: String = "" //para el servicio del detalle de la compra
    private var orderId = "" //para el servicio del vouncher

    companion object {
        private const val BT_REQUEST_PERMISSION = 222
        private const val ENABLE_BT_REQUEST_CODE = 745
        private const val SIGNATURE = 111  // TODO <<<<<<<
        private val TAG = MainActivity::class.java.simpleName
        private const val userNameDefault = "netpay-mini-android-sdk@bylup.com"
        private const val userPassDefault = "Password123!"
        private var _ConfigSdk: ConfigSdk? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transaction = NpTransactions(this, connectReader, this)
        reports = NpReports(this, this)
        miniPreferences = MiniPreferences(this)
        _ConfigSdk = ConfigSdk.SANDBOX
        // Subscribe broadcast for finding device
        initializeDevice()
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
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        // Start finding device and show results in initializeDevice()
        findDeviceAction.setOnClickListener {
            val permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), BT_REQUEST_PERMISSION)
            }
            startConnectReaderDevice()
        }
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        // Start SDK configurations and start transaction
        startTransactionAction.setOnClickListener {
            processingTransaction()
        }

        // Start SDK configurations and start transaction
        actionImageVoucher.setOnClickListener {
            getImageVoucher()
        }
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        initializeViewState()
    }
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    private fun initializeViewState() {
        val promotions = PromotionEnum.values().map { it.text }
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, promotions)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPromotion.apply {
            adapter = aa
        }
    }
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    override fun onStart() {
        super.onStart()
        connectReader.registerReceiver()
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

    private fun processingTransaction() {
        transaction.initialize(
            config = _ConfigSdk!!,
            userName = userNameDefault,
            password = userPassDefault,
            requireSignature = false
        ) {}
    }

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

    private fun connectAndCancelFindingDevice(deviceName: String, deviceAddress: String) {
        connectReader.registerDevice(deviceName, deviceAddress)
    }

    override fun errorResult(npErrorEnum: NpErrorEnum) {
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        runOnUiThread {
            Toast.makeText(this, npErrorEnum.name, Toast.LENGTH_SHORT).show()
            Log.i("ERROR RESULT",""+npErrorEnum.name);
        }
//TODO ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun posTransactionEnumResult(result: NpTransactionEnum) {
        TODO("Not yet implemented")
    }

    override fun provideSignaturePathResult(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun refundResult(message: String, processed: Boolean) {
        runOnUiThread {
            if (processed) {
                // Se completó la cancelación
            }
        }
        TODO("Not yet implemented")
    }

    override fun reverseResult(message: String, processed: Boolean) {
        runOnUiThread {
            Toast.makeText(this, "reverse: $message", Toast.LENGTH_SHORT).show()
        }
        TODO("Not yet implemented")
    }

    override fun selectedAppAction(apps: List<String>, action: (Int) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun startingPaymentProcessing() {
        TODO("Not yet implemented")
    }

    override fun transactionsResult(message: String, processed: Boolean) {
//        val actionImageVoucher: Button = findViewById(R.id.actionImageVoucher)
//        val btnListReport: Button = findViewById(R.id.btnListReport)
//        val bReportDetails: Button = findViewById(R.id.bReportDetails)
//
//        runOnUiThread {
//            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//
//            if (processed) {
//                enableButton(actionImageVoucher)
//                enableButton(btnListReport)
//                enableButton(bReportDetails)
//                Log.d(TAG, "transactionsResult message: $message")
//                refundLastTransactionId(message.split(":")[1])
//                orderId = message.split(":")[2]
//            }
//        }

        runOnUiThread {
            if (processed) {
                Log.d(TAG, "transactionsResult message: $message")
                refundLastTransactionId ( message.split(":")[1])
                orderId = message.split(":")[2]
            }
        }
        TODO("Not yet implemented")
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

    override fun valuesToProcessing(values: (total: Double, tip: Double, reference: String, promotion: String) -> Unit) {
        val total = editTransValue.text.toString()

        val referenceText = editTextReference.text.toString()
        val referenceEncoded = encodeString(referenceText)

        val promotionIndex = spinnerPromotion.selectedItemPosition
        val promotionSelected = PromotionEnum.values().get(promotionIndex)

        values(total.toDouble(), 0.0, referenceEncoded, promotionSelected.value)
        TODO("Not yet implemented")
    }

    override fun reportSaleDetailsResult(response: Response?, success: Boolean) {
        runOnUiThread {
            // Respuesta en objeto response
        }
        TODO("Not yet implemented")
    }

    override fun reportSalesByDateAndUserResult(
        processed: Boolean,
        message: String?,
        reportSales: Response?
    ) {

        Log.i(TAG, "reportSalesByDateAndUserResult")
        reportSales?.let {
            Log.i(TAG, "reportSales.report?.valueList?.value?.size: ${reportSales.report?.valueList?.value?.size}")
            tvResponse.text = reportSales.report?.valueList.toString()
            //Gson
            //val sales = fromJson(reportSales.report?.valueList, SalesResponseValue::class.java).value
        TODO("Not yet implemented")
    }
        message?.let {
            Log.i(TAG, "message: $message")
        }
    }
    private fun enableButton(btn: Button) {
        btn.apply {
            setTextColor(Color.WHITE)
            setBackgroundResource(R.color.design_default_color_primary)
        }
    }

    private fun base64ToBitmap(b64: String): Bitmap? {
        val imageAsBytes = Base64.decode(b64.toByteArray(), Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
    }

}