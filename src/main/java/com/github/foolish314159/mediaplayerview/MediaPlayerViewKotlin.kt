package com.github.foolish314159.mediaplayerview

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlinx.android.synthetic.main.mediaplayerview.view.*;
import java.io.IOException
import java.util.concurrent.TimeUnit

open class MediaPlayerViewKotlin : LinearLayout {

    var mediaPlayer: MediaPlayer? = null
        private set
    var url: String = ""


    var seekByMilliseconds = 10000
    private var shouldPlayAfterSeek = false

    private var progressThread: Thread? = null
    private var isProgressTrackingCancelled = false

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    protected fun init(context: Context?) {
        (context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.let { inflater ->
            inflater.inflate(R.layout.mediaplayerview, this, true)

            // Playback spinner (MediaPlayer playback param changing requires API >= M(23))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val adapter = ArrayAdapter.createFromResource(context, R.array.playback_rates, android.R.layout.simple_spinner_item)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPlaybackRate.adapter = adapter
                // Default to 100%
                spinnerPlaybackRate.setSelection(5)
                spinnerPlaybackRate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(adapter: AdapterView<*>?) {
                    }

                    @RequiresApi(Build.VERSION_CODES.M)
                    override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        changePlaybackRate()
                    }
                }
            }

            // Playback control buttons
            buttonRewind.setOnClickListener { rewind() }
            buttonPlay.setOnClickListener { startPause() }
            buttonFastForward.setOnClickListener { fastForward() }

            // Progress TextViews and SeekBar
            seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            })
        }

    }

    // ACTUAL MEDIA PLAYER

    @Throws(IllegalStateException::class, IOException::class, IllegalArgumentException::class, SecurityException::class)
    fun setupPlayer(url: String) {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.let { mp ->
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            this.url = url
            mp.setDataSource(this.url)
            mp.setOnCompletionListener { onCompletion(it) }
            mp.setOnPreparedListener { onPrepared(it) }
            mp.setOnSeekCompleteListener { onSeekComplete(it) }
            mp.prepareAsync()
            mp.setVolume(1.0f, 1.0f)
        }

    }

    fun releasePlayer() {
        mediaPlayer?.let { mp ->
            mp.release()
        }
    }

    protected fun onCompletion(mp: MediaPlayer) {
        buttonPlay?.setImageResource(android.R.drawable.ic_media_play)
        mp.pause()
        trackProgress(false)

        val durationString = formatMillis(0)
        textViewProgress?.text = durationString
        seekBarProgress?.progress = 0
    }

    protected fun onPrepared(mp: MediaPlayer) {
        val duration = mp.duration
        val durationString = formatMillis(duration)
        textViewTotal?.text = durationString
    }

    protected fun onSeekComplete(mp: MediaPlayer) {
        if (shouldPlayAfterSeek) {
            mp.start()
        }
    }

    // PLAYBACK RATE

    @RequiresApi(Build.VERSION_CODES.M)
    private fun changePlaybackRate() {
        mediaPlayer?.let { mp ->
            try {
                val wasPlaying = mp.isPlaying
                val pos = mp.currentPosition
                mp.reset()
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mp.setDataSource(url)
                mp.prepare()
                val selectedSpeed = spinnerPlaybackRate.selectedItem.toString()
                val speed = selectedSpeed.replace("%", "").toFloat() / 100.0f
                val params = mp.playbackParams
                params.speed = speed
                mp.playbackParams = params
                mp.seekTo(pos)
                if (wasPlaying) {
                    mp.start()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // BUTTON CONTROLS

    protected fun seekBy(milliseconds: Int) {
        mediaPlayer?.let { mp ->
            try {
                shouldPlayAfterSeek = mp.isPlaying
                val newPos = mp.currentPosition - milliseconds
                mp.pause()
                mp.seekTo(newPos)
            } catch (ise: IllegalStateException) {
                // ignore
            }
        }
    }

    protected fun rewind() {
        seekBy(-seekByMilliseconds)
    }

    protected fun startPause() {
        mediaPlayer?.let { mp ->
            try {
                // throws IllegalStateException on first click
                if (mp.isPlaying) {
                    buttonPlay?.setImageResource(android.R.drawable.ic_media_play)
                    mp.pause()
                    trackProgress(false)
                } else {
                    buttonPlay?.setImageResource(android.R.drawable.ic_media_pause)
                    mp.start()
                    trackProgress(true)
                }
            } catch (ise: IllegalStateException) {
                try {
                    buttonPlay?.setImageResource(android.R.drawable.ic_media_pause)
                    mp.start()
                } catch (ise2: IllegalStateException) {
                    // ignore
                }
            }
        }
    }

    protected fun fastForward() {
        seekBy(seekByMilliseconds)
    }

    // PROGRESS TRACKING/CONTROLS

    protected fun formatMillis(millis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
        val durationString = String.format("%02d:%02d", minutes, seconds)
        return durationString
    }

    /**
     * @param shouldTrack true to track, false to stop tracking
     */
    protected fun trackProgress(shouldTrack: Boolean) {
        if (!shouldTrack) {
            isProgressTrackingCancelled = true
            progressThread?.interrupt()
            progressThread = null
        } else {
            isProgressTrackingCancelled = false
            progressThread = Thread(progressTrackingRunnable)
            progressThread?.start()
        }
    }

    private val progressTrackingRunnable = Runnable {
        while (!isProgressTrackingCancelled) {
            try {
                val mp = mediaPlayer
                val sbProgress = seekBarProgress
                if (mp != null && sbProgress != null) {
                    if (mp.isPlaying) {
                        val duration = mp.duration
                        val position = mp.currentPosition
                        val progress: Int = (position.toFloat() / duration * sbProgress.max).toInt()
                        val durationString = formatMillis(position)

                        val context = context
                        (context as? Activity)?.runOnUiThread {
                            sbProgress.progress = progress
                            textViewProgress?.text = durationString
                        }
                    }

                    // Sleep for less or more than a second if playback speed has been adjusted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val sleepDuration = (1000 / mp.playbackParams.speed).toLong()
                            Thread.sleep(sleepDuration)
                        } catch (ie: InterruptedException) {
                        }

                    } else {
                        try {
                            Thread.sleep(1000)
                        } catch (ie: InterruptedException) {
                        }
                    }
                }
            } catch (ise: IllegalStateException) {
                // ignore
            }
        }
    }

    protected fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {}

    protected fun onStartTrackingTouch(seekBar: SeekBar) {}

    protected fun onStopTrackingTouch(seekBar: SeekBar) {
        mediaPlayer?.let { mp ->
            try {
                val progress = seekBar.progress
                val duration = mp.getDuration()
                val percentage = progress.toFloat() / seekBar.max
                val newPosition = (duration * percentage).toInt()
                mp.seekTo(newPosition)
            } catch (ise: IllegalStateException) {
                // ignore
            }
        }
    }

}