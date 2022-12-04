package com.aniqq.tv

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize
import okhttp3.*
import okio.IOException
import org.json.JSONArray


class ListSeries : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_series)

        fill()
    }

    private val client = OkHttpClient()
    private fun fill() {
        val token = SharedStorage(this.application).getProperty("token_auth")
        val animeId = intent.getStringExtra("anime_id")
        val request = Request.Builder()
            .url(getString(R.string.api_url) + "getAnimeSeries/$animeId?token=$token")
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

                    val jsonArray = JSONArray(stringJson)

                    val dataSet: MutableList<DataSet> = mutableListOf()
                    val dataSetOut: MutableList<DataSetOut> = arrayListOf()

                    for (i in 0 until jsonArray.length()) {
                        val out = Gson().fromJson(jsonArray[i].toString(), DataSet::class.java)
                        dataSet.add(out)
                        dataSetOut.add(
                            DataSetOut(
                                out.url_m3u8,
                                out.series_id,
                                out.anime_id,
                                out.vod_id
                            )
                        )
                    }

                    runOnUiThread {
                        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewListSeries)
                        recyclerView.layoutManager =
                            LinearLayoutManager(this@ListSeries.applicationContext)

                        val adapterVar = AdapterListSeries(dataSet)
                        recyclerView.adapter = adapterVar

                        // itemClick
                        adapterVar!!.itemClickListener(object :
                            AdapterListSeries.ItemClickListener {
                            override fun onItemClickListener(position: Int) {
                                val intent = Intent(this@ListSeries, Player::class.java)

                                intent.putExtra("series", ArrayList(dataSetOut))
                                intent.putExtra("episode_position", position)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                                startActivity(intent)
                            }
                        })
                    }
                }
            }
        })
    }

    @Parcelize
    data class DataSetOut(
        val url_m3u8: String,
        val series_id: String,
        val anime_id: String,
        val vod_id: String
    ) :
        Parcelable

    data class DataSet(
        @SerializedName("url_m3u8")
        val url_m3u8: String,
        @SerializedName("series_id")
        val series_id: String,
        @SerializedName("anime_id")
        val anime_id: String,
        @SerializedName("vod_id")
        val vod_id: String
    )

    annotation class SerializedName(val value: String)
}