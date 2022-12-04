package com.aniqq.tv

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aniqq.tv.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util

import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.WindowManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.session.MediaButtonReceiver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import kotlin.concurrent.thread

private const val TAG = "PlayerActivity"

class Player : AppCompatActivity() {
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private var playbackStateListener: Player.EventListener = playbackStateListener()
    private lateinit var exoplayer: ExoPlayer

    private var playWhenReady = true
    private var currentEpisodePosition = 0
    private var playbackPosition = 0L
    private lateinit var episodes: ArrayList<ListSeries.DataSetOut>

    private var mMediaSessionCompat: MediaSessionCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        currentEpisodePosition = intent.getIntExtra("episode_position", 0)
        episodes =
            intent.getParcelableArrayListExtra<ListSeries.DataSetOut>("series") as ArrayList<ListSeries.DataSetOut>

        val mediaButtonReceiver = ComponentName(
            application,
            MediaButtonReceiver::class.java
        )
        mMediaSessionCompat =
            MediaSessionCompat(
                applicationContext,
                "MediaSessionCompatLog",
                mediaButtonReceiver,
                null
            )

        mMediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()

        mMediaSessionCompat!!.setPlaybackState(state)
        mMediaSessionCompat!!.isActive = true

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isVisibleController = viewBinding.playerView.isControllerFullyVisible
        if (keyCode == 4 && isVisibleController) {
            viewBinding.playerView.hideController()
            return false
        }

        if (viewBinding.playerView.isControllerFullyVisible) {
            super.onKeyDown(keyCode, event)
            return false;
        }

        return if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showPosition()
            exoplayer.stop()
            true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            viewBinding.playerView.showController()

            true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            exoplayer.seekBack()
            true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            exoplayer.seekForward()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun showPosition() {
        Toast.makeText(
            applicationContext,
            "Episode " + episodes[currentEpisodePosition].series_id,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        exoplayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()

        viewBinding.playerView.player = exoplayer

        episodes.forEach {
            val mediaItem = MediaItem.Builder()
                .setUri(it.url_m3u8)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()

            exoplayer.addMediaItem(mediaItem)
        }

        exoplayer.playWhenReady = playWhenReady
        exoplayer.addListener(playbackStateListener)

        val mediaSessionConnector = MediaSessionConnector(mMediaSessionCompat!!)
        mediaSessionConnector.setPlayer(exoplayer)

        exoplayer!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                currentEpisodePosition = exoplayer!!.currentMediaItemIndex
                showPosition()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerView.postDelayed(getCurrentPlayerPosition(), 1000);
                }
            }
        })

        Log.i("LogTest", mediaSessionConnector!!.mediaSession.toString())

        exoplayer.seekTo(currentEpisodePosition, playbackPosition)

        if (playbackPosition.toInt() == 0) {
            setTimePosAndStart()
        } else {
            exoplayer.prepare()
        }
    }

    private fun getCurrentPlayerPosition(): Runnable? {
        val token = SharedStorage(this.application).getProperty("token_auth")
        val animeId = episodes[currentEpisodePosition].anime_id
        val vodId = episodes[currentEpisodePosition].vod_id
        val seconds = exoplayer.currentPosition / 1000

        val request = Request.Builder()
            .url(getString(R.string.api_url) + "saveTimelineEpisode/$animeId/$vodId/$seconds?token=$token")
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })

        if (exoplayer.isPlaying) {
            playerView.postDelayed({ getCurrentPlayerPosition() }, 1000)
        }
        return null
    }

    private fun setTimePosAndStart() {
        val token = SharedStorage(this.application).getProperty("token_auth")
        val animeId = episodes[currentEpisodePosition].anime_id
        val request = Request.Builder()
            .url(getString(R.string.api_url) + "getEpisodeInfo/$animeId?token=$token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
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
                        if (dataSet[0].vod_id == episodes[currentEpisodePosition].vod_id) {
                            playbackPosition = (dataSet[0].time_start.toInt() * 1000).toLong()
                            exoplayer.seekTo(currentEpisodePosition, playbackPosition)
                            Log.i("LogTest Starting with:", playbackPosition.toString())
                        }
                        exoplayer.prepare()
                    }
                }
            }
        })
    }

    data class DataSet(
        @SerializedName("vod_id")
        val vod_id: String,
        @SerializedName("time_start")
        val time_start: String
    )

    annotation class SerializedName(val value: String)

    private fun releasePlayer() {
        mMediaSessionCompat?.release();
        exoplayer.stop()
        exoplayer?.run {
            playbackPosition = this.currentPosition
            currentEpisodePosition = this.currentMediaItemIndex
            playWhenReady = this.playWhenReady
            removeListener(playbackStateListener)
            release()
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        viewBinding.playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}

private fun playbackStateListener() = object : Player.EventListener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
            else -> "UNKNOWN_STATE             -"
        }
        Log.d(TAG, "changed state to $stateString")
    }
}