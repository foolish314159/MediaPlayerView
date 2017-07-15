package com.github.foolish314159.mediaplayerviewexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val thread = Thread({
            val conn = URL("http://www3.nhk.or.jp/news/easy/k10011056071000/k10011056071000.mp3").openConnection()
            val input = conn.getInputStream()
            val output = this.openFileOutput("k10011056071000.mp3", android.content.Context.MODE_PRIVATE)
            val buffer = ByteArray(4096)
            while (true) {
                val len = input.read(buffer)
                if (len <= 0) {
                    break
                }
                output.write(buffer, 0, len)
                output.flush()
            }
            output.close()
            input.close()

            this.filesDir.listFiles().forEach {
                if (it.name.contains("k10011056071000")) {
                    mediaPlayerView.setupPlayer(it.absolutePath)
                }
            }
        })
        thread.start()
    }

    override fun onStop() {
        super.onStop()

        // Save current position and restore in onResume to continue playback at same position
        mediaPlayerView.releasePlayer()
    }
}
