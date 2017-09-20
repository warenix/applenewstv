package com.google.sample.cast.refplayer.applenewstv;

import android.graphics.Point;
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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
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
    private final float mAspectRatio = 360f / 640;
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
    private RemoteMediaClient.ProgressListener mCastProgressListener = new RemoteMediaClient.ProgressListener() {
        private float mPercentage;

        @Override
        public void onProgressUpdated(long progressMs, long durationMs) {
            mPercentage = ((float) progressMs) / durationMs;
//            Log.d(TAG, String.format("onProgressUpdated() %f %%", mPercentage));
//            if (mPercentage > 0.985) {
            if (progressMs + 1000 > durationMs) {
                Log.d(TAG, String.format("onProgressUpdated() treat video completed"));
                mLocalPlayerController.onMediaItemCompleted();
            } else {
                Log.d(TAG, String.format("onProgressUpdated() progressMs[%d] durationMs[%d]", progressMs, durationMs));
                updateSeekbar((int) Math.round(progressMs), (int) Math.round(durationMs));
            }

        }
    };
    private RemoteMediaClient remoteMediaClient;
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

                    if (isCastConnected()) {
                        setCoverArtStatus(mSelectedMedia.getImage(0));
                    } else {
                        mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
                        setCoverArtStatus(null);
                    }
                    mSeekbar.setProgress(0);

                    // no need to show cover art for local player
//                    Log.d(TAG, String.format("onStateChange() STATE_OPENED, set cover art"));
//                    setCoverArtStatus(mSelectedMedia.getImage(0));

                    // now you can control opened media
                    updateControllersVisibility(true);
                    mControllersVisible = true;


                    // update toolbar title
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(String.format(mSelectedMedia.getTitle()));
                    ((MediaItemListener) getActivity()).onMediaItemOpened(mSelectedMedia);

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
                    Log.d(TAG, "onStateChange() STATE_PLAYING, set video url:" + mSelectedMedia.getUrl());
                    if (isCastConnected()) {
//                        mVideoView.pause();
//                        mVideoView.seekTo(mSeekbar.getProgress());
                        Log.d(TAG, String.format("onStateChange() STATE_PLAYING, play on remote"));
                        loadRemoteMedia(mSeekbar.getProgress(), true);
                    } else {
                        Log.d(TAG, "onStateChange() STATE_PLAYING, play on local player");
                        mVideoView.start();
                        restartTrickplayTimer();
                    }
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

        mControllersVisible = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateMetadata(mControllersVisible);
        updateControllersVisibility(mControllersVisible);
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
                // handle when dragging seekbar to the end
                if (seekBar.getProgress() == seekBar.getMax()) {
                    mLocalPlayerController.onMediaItemCompleted();
                } else {
                    mLocalPlayerController.seek(seekBar.getProgress(), true);
                }
                startControllersTimer();

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
        if (!isCastConnected()) {
            Log.d(TAG, "restartTrickplayTimer() schedule update seekbar task");
            mSeekbarTimer = new Timer();
            mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        }
        Log.d(TAG, "Restarted TrickPlay Timer");
    }

    private void updateSeekbar(int position, int duration) {
//        Log.d(TAG, String.format("updateSeekbar() position[%d] duration[%d]", position, duration));
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(Utils.formatMillis(position));
        mEndText.setText(Utils.formatMillis(duration));
    }

    // should be called from the main thread
    private void updateControllersVisibility(boolean show) {
        if (getContext() == null) {
            return;
        }
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

        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), 2000);
    }

    public void setCastSession(CastSession castSession) {
        this.mCastSession = castSession;
        Log.d(TAG, "setCastSession() is called");
    }

    public void queueVideo(MediaItem mediaItem) {
        mLocalPlayerController.queueMedia(mediaItem);
    }

    protected boolean isCastConnected() {
        boolean isConnected = (mCastSession != null)
                && (mCastSession.isConnected() || mCastSession.isConnecting());
        return isConnected;
    }

    private void setCoverArtStatus(String url) {
        if (url != null) {
            Log.d(TAG, "coverart visible");
            mAquery.id(mCoverArt).image(url);
            mCoverArt.setVisibility(View.VISIBLE);
//            mVideoView.setVisibility(View.INVISIBLE);
        } else {
            Log.d(TAG, "coverart hidden");
            mCoverArt.setVisibility(View.GONE);
//            mVideoView.setVisibility(View.VISIBLE);
        }
    }

    public void jumpToMedia(MediaItem mediaItem) {
        ((LocalVideoPlayerController) (mLocalPlayerController)).jumpToMedia(mediaItem);
    }

    private void updateMetadata(boolean visible) {
        Point displaySize;
        if (!visible) {
            mDescriptionView.setVisibility(View.GONE);
            mTitleView.setVisibility(View.GONE);
            mAuthorView.setVisibility(View.GONE);
            displaySize = Utils.getDisplaySize(getContext());
            RelativeLayout.LayoutParams lp = new
                    RelativeLayout.LayoutParams(displaySize.x,
                    displaySize.y
//                            + ((AppCompatActivity)getActivity()).getSupportActionBar().getHeight()
            );
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        } else {
            mDescriptionView.setText(mSelectedMedia.getSubTitle());
            mTitleView.setText(mSelectedMedia.getTitle());
            mAuthorView.setText(mSelectedMedia.getStudio());
            mDescriptionView.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.VISIBLE);
            mAuthorView.setVisibility(View.VISIBLE);
            displaySize = Utils.getDisplaySize(getContext());
            RelativeLayout.LayoutParams lp = new
                    RelativeLayout.LayoutParams(displaySize.x,
                    (int) (displaySize.x * mAspectRatio));
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar);
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        }
    }

    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession == null) {
            return;
        }
        remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
//        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
//            @Override
//            public void onStatusUpdated() {
////                Intent intent = new Intent(getContext(), ExpandedControlsActivity.class);
////                startActivity(intent);
//                remoteMediaClient.removeListener(this);
//            }
//
//            @Override
//            public void onMetadataUpdated() {
//            }
//
//            @Override
//            public void onQueueStatusUpdated() {
//            }
//
//            @Override
//            public void onPreloadStatusUpdated() {
//            }
//
//            @Override
//            public void onSendingRemoteMediaRequest() {
//            }
//
//            @Override
//            public void onAdBreakStatusUpdated() {
//
//            }
//        });
        remoteMediaClient.addProgressListener(mCastProgressListener, 500);
        if (position == 0) {
            remoteMediaClient.load(buildMediaInfo(), autoPlay, position);
        } else {
            remoteMediaClient.seek(position);
        }
    }

    private MediaInfo buildMediaInfo() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mSelectedMedia.getTitle());
        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));

        return new MediaInfo.Builder(mSelectedMedia.getUrl())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                .setStreamDuration(mSelectedMedia.getDuration() * 1000)
                .build();
    }

    public interface MediaItemListener {
        void onMediaItemOpened(MediaItem mediaItem);
    }

    private class HideControllersTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    mControllersVisible = false;

//                    Window window = getActivity().getWindow();
//                    View decorView = window.getDecorView();
//                    // Hide both the navigation bar and the status bar.
//                    // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
//                    // a general rule, you should design your app to hide the status bar whenever you
//                    // hide the navigation bar.
//                    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
//                    decorView.setSystemUiVisibility(uiOptions);
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