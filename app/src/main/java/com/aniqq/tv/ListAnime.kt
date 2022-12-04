package com.aniqq.tv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.*
import okio.IOException
import org.json.JSONArray


class ListAnime : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_anime)

        fill()
    }

    private val client = OkHttpClient()
    private fun fill() {
        val token = SharedStorage(this.application).getProperty("token_auth")
        val url = intent.getStringExtra("url")
        val request = Request.Builder()
            .url(getString(R.string.api_url) + "getAnimeList/$url?token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("LogTest", e.message.toString())
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val stringJson = response.body!!.string()
                    Log.i("LogTest", stringJson)
                    val jsonArray = JSONArray(stringJson)

                    val dataSet: MutableList<DataSet> = mutableListOf()

                    for (i in 0 until jsonArray.length()) {
                        val out = Gson().fromJson(jsonArray[i].toString(), DataSet::class.java)
                        dataSet.add(out)
                    }

                    runOnUiThread {
                        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewListAnime)
                        recyclerView.layoutManager =
                            LinearLayoutManager(this@ListAnime.applicationContext)

                        val adapterVar = AdapterListAnime(dataSet)
                        recyclerView.adapter = adapterVar

                        // itemClick
                        adapterVar!!.itemClickListener(object : AdapterListAnime.ItemClickListener {
                            override fun onItemClickListener(position: Int) {
                                Toast.makeText(
                                    applicationContext,
                                    dataSet[position].name,
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent = Intent(this@ListAnime, ListSeries::class.java)
                                intent.putExtra("anime_id", dataSet[position].anime_id)

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
        @SerializedName("sub_name")
        val sub_name: String,
        @SerializedName("anime_id")
        val anime_id: String,
        @SerializedName("image")
        val image: String
    )

    annotation class SerializedName(val value: String)
}