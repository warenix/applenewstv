package com.google.sample.cast.refplayer.applenewstv;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.sample.cast.refplayer.mediaplayer.LocalPlayerActivity;
import com.google.sample.cast.refplayer.utils.MediaItem;
import com.google.sample.cast.refplayer.utils.Utils;

import org.dyndns.warenix.applenewstv.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by warenix on 9/13/17.
 */

public class AutoVideoPlayerFragment2 extends Fragment {
    private static final String TAG = AutoVideoPlayerFragment2.class.getSimpleName()
            .substring(0, Math.min(25, AutoVideoPlayerFragment2.class.getSimpleName().length() - 1));
    private final Handler mHandler = new Handler();
    private VideoQueuePlayer mLocalPlayerController;
    private VideoView mVideoView;
    private TextView mTitleView;
    private TextView mDescriptionView;
    private TextView mStartText;
    private TextView mEndText;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private ProgressBar mLoading;
    private View mControllers;
    private View mContainer;
    private ImageView mCoverArt;
    private Timer mSeekbarTimer;
    private Timer mControllersTimer;
    private AQuery mAquery;
    private MediaItem mSelectedMedia;
    private boolean mControllersVisible;
    private int mDuration;
    private TextView mAuthorView;
    private ImageButton mPlayCircle;
    private LocalPlayerActivity.PlaybackLocation mLocation;
    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private MenuItem mediaRouteMenuItem;
    private VideoQueuePlayer.VideoQueuePlayerListener mVideQueuePlayerListener = new VideoQueuePlayer.VideoQueuePlayerListener() {
        @Override
        public void onStateChange(int oldState, int newState) {
            Log.d(TAG, String.format("onStateChange oldState[%d] newState[%d]", oldState, newState));

            switch (newState) {

                case VideoQueuePlayer.VideoQueuePlayerListener.STATE_BUFFERING:
                    break;
                case VideoQueuePlayer.VideoQueuePlayerListener.STATE_IDLE:
                    Log.d(TAG, "onStateChange() STATE_IDLE, play next video in queue");

                    // hide controller as no video is opened.
                    updateControllersVisibility(false);
                    mControllersVisible = false;

                    ((LocalVideoPlayerController) (mLocalPlayerController)).playNextMediaInQueue();

                    break;
                case VideoQueuePlayer.VideoQueuePlayerListener.STATE_OPENED:
                    mSelectedMedia = ((LocalVideoPlayerController) (mLocalPlayerController)).getSelectedMedia();
                    Log.d(TAG, "onStateChange() STATE_OPENED, set video url:" + mSelectedMedia.getUrl());
                    mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));

                    // no need to show cover art for local player
//                    Log.d(TAG, String.format("onStateChange() STATE_OPENED, set cover art"));
//                    setCoverArtStatus(mSelectedMedia.getImage(0));

                    // now you can control opened media
                    updateControllersVisibility(true);
                    mControllersVisible = true;

                    break;
                case VideoQueuePlayer.VideoQueuePlayerListener.STATE_PAUSE:
                    mVideoView.pause();
                    stopTrickplayTimer();

                    // show play circle
                    mPlayPause.setVisibility(View.INVISIBLE);
                    mPlayCircle.setVisibility(View.VISIBLE);

                    break;
                case VideoQueuePlayer.VideoQueuePlayerListener.STATE_PLAYING:
                    // turn play button to pause button
                    mLoading.setVisibility(View.INVISIBLE);
                    mPlayPause.setVisibility(View.VISIBLE);
                    mPlayPause.setImageDrawable(
                            getResources().getDrawable(R.drawable.ic_av_pause_dark));
                    mPlayCircle.setVisibility(View.GONE);

                    // no need to show cover art for local player
//                    Log.d(TAG, String.format("onStateChange() STATE_PLAYING, hide cover art"));
//                    setCoverArtStatus(null);

                    // try playing video
                    Log.d(TAG, String.format("onStateChange() STATE_PLAYING, start playing video"));
                    mVideoView.start();
                    startControllersTimer();
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.auto_play_video_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAquery = new AQuery(getContext());

        loadViews(view);
        setupControlsCallbacks();

        mLocalPlayerController = new LocalVideoPlayerController();
        ((LocalVideoPlayerController) mLocalPlayerController).setVideoView(mVideoView);
        ((LocalVideoPlayerController) mLocalPlayerController).setVideoQueuePlayerListener(mVideQueuePlayerListener);

        updateControllersVisibility(false);
        mControllersVisible = false;
    }

    private void loadViews(View view) {
        mVideoView = (VideoView) view.findViewById(R.id.videoView1);
        mTitleView = (TextView) view.findViewById(R.id.textView1);
        mDescriptionView = (TextView) view.findViewById(R.id.textView2);
        mDescriptionView.setMovementMethod(new ScrollingMovementMethod());
        mAuthorView = (TextView) view.findViewById(R.id.textView3);
        mStartText = (TextView) view.findViewById(R.id.startText);
        mStartText.setText(Utils.formatMillis(0));
        mEndText = (TextView) view.findViewById(R.id.endText);
        mSeekbar = (SeekBar) view.findViewById(R.id.seekBar1);
        mPlayPause = (ImageView) view.findViewById(R.id.imageView2);
        mLoading = (ProgressBar) view.findViewById(R.id.progressBar1);
        mControllers = view.findViewById(R.id.controllers);
        mContainer = view.findViewById(R.id.container);
        mCoverArt = (ImageView) view.findViewById(R.id.coverArtView);
//        ViewCompat.setTransitionName(mCoverArt, getString(R.string.transition_image));
        mPlayCircle = (ImageButton) view.findViewById(R.id.play_circle);
        mPlayCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLocalPlayerController.togglePlayback();
            }
        });
    }

    private void setupControlsCallbacks() {
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "OnErrorListener.onError(): VideoView encountered an "
                        + "error, what: " + what + ", extra: " + extra);
                String msg;
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = getString(R.string.video_error_server_unaccessible);
                } else {
                    msg = getString(R.string.video_error_unknown_error);
                }
                Utils.showErrorDialog(getContext(), msg);
//                mVideoView.stopPlayback();
//                mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
//                updatePlayButton(mPlaybackState);
                return true;
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "onPrepared is reached");
                mDuration = mp.getDuration();
                mEndText.setText(Utils.formatMillis(mDuration));
                mSeekbar.setMax(mDuration);
                restartTrickplayTimer();
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                if (mPlaybackState == LocalPlayerActivity.PlaybackState.PLAYING) {
//                    play(seekBar.getProgress());
//                } else if (mPlaybackState != LocalPlayerActivity.PlaybackState.IDLE) {
//                    mVideoView.seekTo(seekBar.getProgress());
//                }
//                mLocalPlayerController.seek(seekBar.getProgress(), true);

                mLocalPlayerController.seek(seekBar.getProgress(), true);

//                startControllersTimer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTrickplayTimer();
                mVideoView.pause();
                stopControllersTimer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mStartText.setText(Utils.formatMillis(progress));
            }
        });


        mVideoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mControllersVisible) {
                    updateControllersVisibility(true);
                }
                startControllersTimer();
                return false;
            }
        });


        mPlayPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                if (mLocation == LocalPlayerActivity.PlaybackLocation.LOCAL) {
                mLocalPlayerController.togglePlayback();
//                }
            }
        });
    }

    private void stopTrickplayTimer() {
        Log.d(TAG, "Stopped TrickPlay Timer");
        if (mSeekbarTimer != null) {
            mSeekbarTimer.cancel();
        }
    }


    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        Log.d(TAG, "Restarted TrickPlay Timer");
    }

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(Utils.formatMillis(position));
        mEndText.setText(Utils.formatMillis(duration));
    }

    // should be called from the main thread
    private void updateControllersVisibility(boolean show) {
        if (show) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            mControllers.setVisibility(View.VISIBLE);
        } else {
            if (!Utils.isOrientationPortrait(getContext())) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
            }
            mControllers.setVisibility(View.INVISIBLE);
        }
    }

    private void stopControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
        if (mLocation == LocalPlayerActivity.PlaybackLocation.REMOTE) {
            return;
        }
        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), 2000);
    }

    public void setCastSession(CastSession castSession) {
        this.mCastSession = castSession;
    }

    public void queueVideo(MediaItem mediaItem) {
        mLocalPlayerController.queueMedia(mediaItem);
    }

    private void updatePlayButton(LocalPlayerActivity.PlaybackState state) {
        Log.d(TAG, "Controls: PlayBackState: " + state);
        boolean isConnected = (mCastSession != null)
                && (mCastSession.isConnected() || mCastSession.isConnecting());
        mControllers.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mPlayCircle.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        switch (state) {
            case PLAYING:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_pause_dark));
                mPlayCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;
            case IDLE:
                mPlayCircle.setVisibility(View.VISIBLE);
                mControllers.setVisibility(View.GONE);
                mCoverArt.setVisibility(View.VISIBLE);
                mVideoView.setVisibility(View.INVISIBLE);
                break;
            case PAUSED:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_play_dark));
                mPlayCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;
            case BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void setCoverArtStatus(String url) {
        if (url != null) {
            Log.d(TAG, "coverart visible");
            mAquery.id(mCoverArt).image(url);
            mCoverArt.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.INVISIBLE);
        } else {
            Log.d(TAG, "coverart hidden");
            mCoverArt.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
        }
    }

    private class HideControllersTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    mControllersVisible = false;
                }
            });

        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos = mVideoView.getCurrentPosition();
                    updateSeekbar(currentPos, mDuration);
                }
            });
        }

    }

}