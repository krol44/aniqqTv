package com.aniqq.tv

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.*
import okhttp3.*
import okio.IOException
import org.json.JSONArray


class Login : AppCompatActivity() {

    private val coroutine: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        if (coroutine != null && coroutine.isActive) {
            coroutine!!.cancel()
        }

        val barcodeEncoder = BarcodeEncoder()
        val randString = getRandomString(10);
        val bitmap = barcodeEncoder.encodeBitmap(
            "https://aniqq.com/login/aniqqTv/$randString",
            BarcodeFormat.QR_CODE, 1000, 1000
        )
        val imageQRCode: ImageView = findViewById(R.id.imageQrCode)
        imageQRCode.setImageBitmap(bitmap)

        checking(randString)
    }

    override fun onStop() {
        super.onStop()
        coroutine!!.cancel()
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private val client = OkHttpClient()
    private fun checking(randString: String) {
        val request = Request.Builder()
            .url(getString(R.string.api_url) + "checkAuth/" + randString + "/" + getString(R.string.salt_key_auth))
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                client.newCall(request).execute().use { response ->
                    val stringJson = response.body!!.string()
                    Log.i("LogTest", stringJson)

                    val jsonArray = JSONArray(stringJson)

                    val dataSet: MutableList<DataSet> = mutableListOf()

                    for (i in 0 until jsonArray.length()) {
                        val out = Gson().fromJson(jsonArray[i].toString(), DataSet::class.java)
                        dataSet.add(out)
                    }

                    if (dataSet[0].status == "success") {

                        runOnUiThread {
                            val intent = Intent(this@Login, Start::class.java)
                            intent.putExtra("token_auth", dataSet[0].token)
                            startActivity(intent)
                            finishAffinity()

                            coroutine!!.cancel()
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    data class DataSet(
        @SerializedName("status")
        val status: String,
        @SerializedName("token")
        val token: String
    )

    annotation class SerializedName(val value: String)
}
