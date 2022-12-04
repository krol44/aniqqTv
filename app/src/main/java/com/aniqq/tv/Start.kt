package com.aniqq.tv

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import java.io.FileInputStream


class Start : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        val shared = SharedStorage(this.application)
        val token = intent.getStringExtra("token_auth")
        if (!token.isNullOrEmpty()) {

            shared.addProperty("token_auth", token)
        }

        val innerToken = shared.getProperty("token_auth")
        if (innerToken.isNullOrEmpty()) {
            val intent = Intent(this@Start, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent)
            finishAffinity()
        } else {
            setContentView(R.layout.activity_main)

            fill()
        }
    }

    override fun onRestart() {
        super.onRestart()
        fill()
    }

    private val client = OkHttpClient()
    private fun fill() {
        val shared = SharedStorage(this.application)
        val token = shared.getProperty("token_auth")
        val request = Request.Builder()
            .url(getString(R.string.api_url) + "getListMenu?token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("LogTest", e.message.toString())
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.code.toString() == "403") {
                        Log.i("LogTest", "exit")
                        shared.addProperty("token_auth", "")
                        val intent = Intent(this@Start, Login::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent)
                        finishAffinity()
                    }

                    for ((name, value) in response.headers) {
                        Log.i("LogHeader", "$name: $value")
                    }

                    val stringJson = response.body!!.string()
                    Log.i("LogTest", stringJson)

                    val jsonArray = JSONArray(stringJson)

                    val dataSet: MutableList<DataSet> = mutableListOf()

                    for (i in 0 until jsonArray.length()) {
                        val out = Gson().fromJson(jsonArray[i].toString(), DataSet::class.java)
                        dataSet.add(out)
                    }

                    runOnUiThread {
                        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewMain)
                        recyclerView.layoutManager =
                            LinearLayoutManager(this@Start.applicationContext)

                        val adapterVar = AdapterMainMenu(dataSet)
                        recyclerView.adapter = adapterVar

//                        recyclerView?.setOnFocusChangeListener { view, isFocused ->
//                            Log.i("test", ".")
//                        }

                        adapterVar!!.itemClickListener(object : AdapterMainMenu.ItemClickListener {
                            override fun onItemClickListener(position: Int) {
                                Toast.makeText(
                                    applicationContext,
                                    dataSet[position].name,
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this@Start, ListAnime::class.java)
                                intent.putExtra("url", dataSet[position].url)

                                startActivity(intent)
                            }
                        })
                    }
                }
            }
        })
    }

    data class DataSet(
        @SerializedName("name")
        val name: String,

        @SerializedName("url")
        val url: String
    )

    annotation class SerializedName(val value: String)

//    fun webViewTest() {
//        val myWebView: WebView = findViewById(R.id.webview)
//        myWebView.loadUrl("http://aniqq.com")
//        myWebView.settings.setJavaScriptEnabled(true)
//        myWebView.webViewClient = object : WebViewClient() {
//            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
//                val url = request?.url.toString()
//                view?.loadUrl(url)
//                return true
//            }
//        }
//        myWebView.loadUrl("https://aniqq.com")
//    }

//    fun thread() {
//        CoroutineScope(Dispatchers.Default).launch {
//            val retVal = downloadTask()
//
//            Log.i("LogTest", retVal)
//        }
//    }
//      private suspend fun downloadTask1(): String {
//            kotlinx.coroutines.delay(3000);
//            return "Complete";
//      }
}
