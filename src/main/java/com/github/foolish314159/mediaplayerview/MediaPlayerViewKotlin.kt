package com.github.foolish314159.mediaplayerview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.mediaplayerview.view.*

class MediaPlayerViewKotlin : LinearLayout {

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

    private fun init(context: Context?) {
        (context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.let { inflater ->
            inflater.inflate(R.layout.mediaplayerview, this, true)

            val adapter = ArrayAdapter.createFromResource(context, R.array.playback_rates, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPlaybackRate.adapter = adapter
        }

    }

}