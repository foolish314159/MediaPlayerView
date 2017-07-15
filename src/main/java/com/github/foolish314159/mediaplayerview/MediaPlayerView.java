package com.github.foolish314159.mediaplayerview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MediaPlayerView extends LinearLayout implements AdapterView.OnItemSelectedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayer.OnSeekCompleteListener {

    private LinearLayout rowPlayback = null;

    private LinearLayout columnPlaybackRate = null;
    private Spinner spinnerPlaybackRate = null;

    private LinearLayout columnPlaybackControls = null;
    private ImageButton buttonRewind = null;
    private ImageButton buttonStartPause = null;
    private ImageButton buttonFastForward = null;

    /**
     * Top row right side. Free for additional controls if needed.
     **/
    private LinearLayout columnUnused = null;

    private LinearLayout rowProgress = null;

    private SeekBar seekBarProgress = null;
    private TextView textViewProgress = null;
    private TextView textViewTotal = null;

    private MediaPlayer mediaPlayer = null;
    private String url = null;
    private Thread progressThread = null;
    private boolean isProgressTrackingCancelled = false;

    // Expose views and mediaplayer to enable styling and custom functionality

    public Spinner getSpinnerPlaybackRate() {
        return spinnerPlaybackRate;
    }

    public ImageButton getButtonRewind() {
        return buttonRewind;
    }

    public ImageButton getButtonStartPause() {
        return buttonStartPause;
    }

    public ImageButton getButtonFastForward() {
        return buttonFastForward;
    }

    public LinearLayout getColumnUnused() {
        return columnUnused;
    }

    public SeekBar getSeekBarProgress() {
        return seekBarProgress;
    }

    public TextView getTextViewProgress() {
        return textViewProgress;
    }

    public TextView getTextViewTotal() {
        return textViewTotal;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public String getUrl() {
        return url;
    }

    public MediaPlayerView(Context context) {
        super(context);
        init(context);
    }

    public MediaPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MediaPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context) {
        setOrientation(VERTICAL);

        // Playback controls
        rowPlayback = new LinearLayout(context);
        rowPlayback.setOrientation(HORIZONTAL);
        rowPlayback.setGravity(Gravity.CENTER);

        // Playback rate
        columnPlaybackRate = new LinearLayout(context);
        columnPlaybackRate.setOrientation(HORIZONTAL);
        LayoutParams columnPlaybackRateParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // Take 30% of row width
        columnPlaybackRateParams.weight = 1.0f;
        columnPlaybackRate.setLayoutParams(columnPlaybackRateParams);

        // MediaPlayer playback rate is only available on API >23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            spinnerPlaybackRate = new Spinner(context);
            String[] playbackRates = new String[]{"50%", "60%", "70%", "80%", "90%", "100%", "110%", "120%", "130%", "140%", "150%", "160%", "170%", "180%", "190%", "200%"};
            ArrayAdapter<String> spinnerPlaybackRateAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, playbackRates);
            spinnerPlaybackRate.setAdapter(spinnerPlaybackRateAdapter);
            spinnerPlaybackRate.setSelection(5);
            LayoutParams spinnerPlaybackRateParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            spinnerPlaybackRate.setLayoutParams(spinnerPlaybackRateParams);
            spinnerPlaybackRate.setOnItemSelectedListener(this);
            columnPlaybackRate.addView(spinnerPlaybackRate);
        }

        rowPlayback.addView(columnPlaybackRate);

        // Playback controls
        columnPlaybackControls = new LinearLayout(context);
        columnPlaybackControls.setOrientation(HORIZONTAL);
        columnPlaybackControls.setGravity(Gravity.CENTER);
        LayoutParams columnPlaybackControlsParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        columnPlaybackControlsParams.weight = 1.0f;
        columnPlaybackControls.setLayoutParams(columnPlaybackControlsParams);

        buttonRewind = new ImageButton(context);
        LayoutParams buttonRewindParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonRewind.setLayoutParams(buttonRewindParams);
        buttonRewind.setBackgroundColor(Color.TRANSPARENT);
        buttonRewind.setImageResource(android.R.drawable.ic_media_rew);
        buttonRewind.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                rewind();
            }
        });
        setImageButtonBackgroundAttribute(buttonRewind);
        columnPlaybackControls.addView(buttonRewind);

        buttonStartPause = new ImageButton(context);
        LayoutParams buttonStartPauseParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonStartPause.setLayoutParams(buttonStartPauseParams);
        buttonStartPause.setBackgroundColor(Color.TRANSPARENT);
        buttonStartPause.setImageResource(android.R.drawable.ic_media_play);
        buttonStartPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startPause();
            }
        });
        setImageButtonBackgroundAttribute(buttonStartPause);
        columnPlaybackControls.addView(buttonStartPause);

        buttonFastForward = new ImageButton(context);
        LayoutParams buttonFastForwardParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonFastForward.setLayoutParams(buttonFastForwardParams);
        buttonFastForward.setBackgroundColor(Color.TRANSPARENT);
        buttonFastForward.setImageResource(android.R.drawable.ic_media_ff);
        buttonFastForward.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                fastForward();
            }
        });
        setImageButtonBackgroundAttribute(buttonFastForward);
        columnPlaybackControls.addView(buttonFastForward);

        rowPlayback.addView(columnPlaybackControls);

        // Unused right column
        columnUnused = new LinearLayout(context);
        columnUnused.setOrientation(HORIZONTAL);
        LayoutParams columnUnusedParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        columnUnusedParams.weight = 1.0f;
        columnUnusedParams.gravity = Gravity.CENTER;
        columnUnused.setLayoutParams(columnUnusedParams);

        rowPlayback.addView(columnUnused);

        addView(rowPlayback);

        // Progress bar and current/total time text views
        rowProgress = new LinearLayout(context);
        rowProgress.setOrientation(HORIZONTAL);

        textViewProgress = new TextView(context);
        LayoutParams textViewProgressParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewProgressParams.weight = 0.0f;
        textViewProgress.setLayoutParams(textViewProgressParams);
        textViewProgress.setText("00:00");
        rowProgress.addView(textViewProgress);

        seekBarProgress = new SeekBar(context);
        LayoutParams seekBarProgressParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        seekBarProgressParams.weight = 1.0f;
        seekBarProgress.setLayoutParams(seekBarProgressParams);
        seekBarProgress.setOnSeekBarChangeListener(this);
        rowProgress.addView(seekBarProgress);

        textViewTotal = new TextView(context);
        LayoutParams textViewTotalParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewTotalParams.weight = 0.0f;
        textViewTotal.setLayoutParams(textViewTotalParams);
        rowProgress.addView(textViewTotal);

        addView(rowProgress);
    }

    private void setImageButtonBackgroundAttribute(ImageButton button) {
        TypedValue outValue = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        } else {
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        }
        button.setBackgroundResource(outValue.resourceId);
    }

    // Actual media player

    public void setupPlayer(String url) throws IllegalStateException, IOException, IllegalArgumentException, SecurityException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.url = url;
        mediaPlayer.setDataSource(this.url);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.prepareAsync();
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    // Playback rate handling

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (mediaPlayer != null) {
            try {
                boolean wasPlaying = mediaPlayer.isPlaying();
                int pos = mediaPlayer.getCurrentPosition();
                mediaPlayer.reset();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(this.url);
                mediaPlayer.prepare();
                mediaPlayer.setVolume(1.0f, 1.0f);
                String selectedSpeed = spinnerPlaybackRate.getSelectedItem().toString();
                float speed = Float.valueOf(selectedSpeed.replace("%", "")) / 100.0f;
                PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(speed);
                mediaPlayer.setPlaybackParams(params);
                mediaPlayer.seekTo(pos);
                if (wasPlaying) {
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                System.err.println(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    // Playback controls

    private boolean shouldPlayAfterSeek = false;

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && shouldPlayAfterSeek) {
            mediaPlayer.start();
        }
    }

    private void seekBy(int milliseconds) {
        if (mediaPlayer != null) {
            try {
                shouldPlayAfterSeek = mediaPlayer.isPlaying();
                int newPos = mediaPlayer.getCurrentPosition() - milliseconds;
                mediaPlayer.pause();
                mediaPlayer.seekTo(newPos);
            } catch (IllegalStateException ise) {
                // ignore if not initialized
            }
        }
    }

    private void rewind() {
        seekBy(10000);
    }

    private void startPause() {
        if (mediaPlayer != null && buttonStartPause != null) {
            try {
                // throws IllegalStateException on first click
                if (mediaPlayer.isPlaying()) {
                    buttonStartPause.setImageResource(android.R.drawable.ic_media_play);
                    mediaPlayer.pause();
                    trackProgress(false);
                } else {
                    buttonStartPause.setImageResource(android.R.drawable.ic_media_pause);
                    mediaPlayer.start();
                    trackProgress(true);
                }
            } catch (IllegalStateException ise) {
                try {
                    mediaPlayer.start();
                    buttonStartPause.setImageResource(android.R.drawable.ic_media_pause);
                } catch (IllegalStateException ise2) {
                    // ignore
                }
            }
        }
    }

    private void fastForward() {
        seekBy(-10000);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && buttonStartPause != null && textViewProgress != null && seekBarProgress != null) {
            buttonStartPause.setImageResource(android.R.drawable.ic_media_play);
            mediaPlayer.pause();
            trackProgress(false);

            String durationString = formatMillis(0);
            textViewProgress.setText(durationString);
            seekBarProgress.setProgress(0);
        }
    }

    // Progress tracking

    private String formatMillis(int millis) {
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        String durationString = String.format("%02d:%02d", minutes, seconds);
        return durationString;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && textViewTotal != null) {
            final int duration = mediaPlayer.getDuration();
            String durationString = formatMillis(duration);
            textViewTotal.setText(durationString);
        }
    }

    /**
     * @param shouldTrack true to track, false to stop tracking
     */
    public void trackProgress(boolean shouldTrack) {
        if (!shouldTrack) {
            if (progressThread != null) {
                isProgressTrackingCancelled = true;
                progressThread.interrupt();
                progressThread = null;
            }
        } else {
            isProgressTrackingCancelled = false;
            progressThread = new Thread(progressTrackingRunnable);
            progressThread.start();
        }
    }

    private Runnable progressTrackingRunnable = new Runnable() {
        @Override
        public void run() {
            while (!isProgressTrackingCancelled) {
                try {
                    if (mediaPlayer != null && seekBarProgress != null && textViewProgress != null && mediaPlayer.isPlaying()) {
                        final int duration = mediaPlayer.getDuration();
                        final int position = mediaPlayer.getCurrentPosition();
                        final int progress = (int) (((float) position / duration) * seekBarProgress.getMax());
                        final String durationString = formatMillis(position);

                        Context context = getContext();
                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    seekBarProgress.setProgress(progress);
                                    textViewProgress.setText(durationString);
                                }
                            });
                        }
                    }

                    // Sleep for less or more than a second if playback speed has been adjusted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
                        try {
                            final long sleepDuration = (long) (1000 / mediaPlayer.getPlaybackParams().getSpeed());
                            Thread.sleep(sleepDuration);
                        } catch (InterruptedException ie) {
                        }
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }
                    }
                } catch (IllegalStateException ise) {
                    // ignore
                }
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null) {
            try {
                final int progress = seekBar.getProgress();
                final int duration = mediaPlayer.getDuration();
                final float percentage = ((float) progress / seekBar.getMax());
                final int newPosition = (int) (duration * percentage);
                mediaPlayer.seekTo(newPosition);
            } catch (IllegalStateException ise) {
                // ignore
            }
        }
    }

}
