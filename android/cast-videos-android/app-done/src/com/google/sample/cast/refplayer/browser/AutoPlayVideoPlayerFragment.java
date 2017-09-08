package com.google.sample.cast.refplayer.browser;

import android.content.Intent;
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
import com.google.sample.cast.refplayer.expandedcontrols.ExpandedControlsActivity;
import com.google.sample.cast.refplayer.mediaplayer.LocalPlayerActivity;
import com.google.sample.cast.refplayer.utils.MediaItem;
import com.google.sample.cast.refplayer.utils.Utils;

import org.dyndns.warenix.applenewstv.R;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by warenix on 9/9/17.
 */

public class AutoPlayVideoPlayerFragment extends Fragment {
    private static final String TAG = "AutoPlayVideoPlayer";
    private final Handler mHandler = new Handler();
    private final float mAspectRatio = 72f / 128;
    Queue<MediaItem> mMediaItemQueue = new ArrayDeque<>();
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
    private LocalPlayerActivity.PlaybackState mPlaybackState;
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

    /**
     * List of various states that we can be in
     */
//    public enum PlaybackState {
//        PLAYING, PAUSED, BUFFERING, IDLE
//    }
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
                togglePlayback();
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
                mVideoView.stopPlayback();
                mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);
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

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                stopTrickplayTimer();
                Log.d(TAG, "setOnCompletionListener()");
                mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);

                playNextMediaInQueue();
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

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlaybackState == LocalPlayerActivity.PlaybackState.PLAYING) {
                    play(seekBar.getProgress());
                } else if (mPlaybackState != LocalPlayerActivity.PlaybackState.IDLE) {
                    mVideoView.seekTo(seekBar.getProgress());
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

        mPlayPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mLocation == LocalPlayerActivity.PlaybackLocation.LOCAL) {
                    togglePlayback();
                }
            }
        });
    }

    private void play(int position) {
        startControllersTimer();
        switch (mLocation) {
            case LOCAL:
                mVideoView.seekTo(position);
                mVideoView.start();
                break;
            case REMOTE:
                mPlaybackState = LocalPlayerActivity.PlaybackState.BUFFERING;
                updatePlayButton(mPlaybackState);
                mCastSession.getRemoteMediaClient().seek(position);
                break;
            default:
                break;
        }
        restartTrickplayTimer();
    }


    private void togglePlayback() {
        stopControllersTimer();
        switch (mPlaybackState) {
            case PAUSED:
                switch (mLocation) {
                    case LOCAL:
                        mVideoView.start();
                        Log.d(TAG, "Playing locally...");
                        mPlaybackState = LocalPlayerActivity.PlaybackState.PLAYING;
                        startControllersTimer();
                        restartTrickplayTimer();
                        updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
                        break;
                    case REMOTE:
                        // TODO handle remote play video
//                        finish();
                        break;
                    default:
                        break;
                }
                break;

            case PLAYING:
                mPlaybackState = LocalPlayerActivity.PlaybackState.PAUSED;
                mVideoView.pause();
                break;

            case IDLE:
                switch (mLocation) {
                    case LOCAL:
                        mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
                        mVideoView.seekTo(0);
                        mVideoView.start();
                        mPlaybackState = LocalPlayerActivity.PlaybackState.PLAYING;
                        restartTrickplayTimer();
                        updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
                        break;
                    case REMOTE:
                        if (mCastSession != null && mCastSession.isConnected()) {
                            loadRemoteMedia(mSeekbar.getProgress(), true);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        updatePlayButton(mPlaybackState);
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
        mControllersTimer.schedule(new HideControllersTask(), 5000);
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


    private void updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation location) {
        mLocation = location;
        if (location == LocalPlayerActivity.PlaybackLocation.LOCAL) {
            if (mPlaybackState == LocalPlayerActivity.PlaybackState.PLAYING
                    || mPlaybackState == LocalPlayerActivity.PlaybackState.BUFFERING) {
                setCoverArtStatus(null);
                startControllersTimer();
            } else {
                stopControllersTimer();
                setCoverArtStatus(mSelectedMedia.getImage(0));
            }
        } else {
            stopControllersTimer();
            setCoverArtStatus(mSelectedMedia.getImage(0));
            updateControllersVisibility(false);
        }
    }


    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession == null) {
            return;
        }
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                Intent intent = new Intent(getActivity(), ExpandedControlsActivity.class);
                startActivity(intent);
                remoteMediaClient.removeListener(this);
            }

            @Override
            public void onMetadataUpdated() {
            }

            @Override
            public void onQueueStatusUpdated() {
            }

            @Override
            public void onPreloadStatusUpdated() {
            }

            @Override
            public void onSendingRemoteMediaRequest() {
            }

            @Override
            public void onAdBreakStatusUpdated() {

            }
        });
        remoteMediaClient.load(buildMediaInfo(), autoPlay, position);
    }


    private void setCoverArtStatus(String url) {
        if (url != null) {
            mAquery.id(mCoverArt).image(url);
            mCoverArt.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.INVISIBLE);
        } else {
            mCoverArt.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
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

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(Utils.formatMillis(position));
        mEndText.setText(Utils.formatMillis(duration));
    }


    private synchronized void playNextMediaInQueue() {
        // FIXME play queued video immediately
        if (mMediaItemQueue.size() == 0 ) {
            Log.d(TAG, "playNextMediaInQueue() no more video in queue");
            return;
        }
        if (mPlaybackState == LocalPlayerActivity.PlaybackState.PLAYING) {
            Log.d(TAG, "playNextMediaInQueue() do not interrupt currently playing video");
            return;
        }
        mSelectedMedia = mMediaItemQueue.poll();

        setupActionBar();
        boolean shouldStartPlayback = false;
        int startPosition = 0;
        mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
        Log.d(TAG, "Setting url of the VideoView to: " + mSelectedMedia.getUrl());
        if (shouldStartPlayback) {
            // this will be the case only if we are coming from the
            // CastControllerActivity by disconnecting from a device
            mPlaybackState = LocalPlayerActivity.PlaybackState.PLAYING;
            updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
            updatePlayButton(mPlaybackState);
            if (startPosition > 0) {
                mVideoView.seekTo(startPosition);
            }
            mVideoView.start();
            startControllersTimer();
        } else {
            // we should load the video but pause it
            // and show the album art.
            if (mCastSession != null && mCastSession.isConnected()) {
                updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.REMOTE);
            } else {
                updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
            }
//            mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
//            updatePlayButton(mPlaybackState);

            // auto start playing video
            Log.d(TAG, "playNextMediaInQueue() auto start playing video");
            mPlaybackState = LocalPlayerActivity.PlaybackState.PLAYING;
            updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
            updatePlayButton(mPlaybackState);
            if (startPosition > 0) {
                mVideoView.seekTo(startPosition);
            }
            mVideoView.start();
            startControllersTimer();
        }

        if (mTitleView != null) {
            updateMetadata(true);
        }
    }

    public void queueVideo(MediaItem mediaItem) {
        synchronized (this) {
            mMediaItemQueue.add(mediaItem);
            Log.d(TAG, "queueVideo" + " queue size now: " + mMediaItemQueue.size());

            // start play the first video in queue.
//            if (mMediaItemQueue.size() == 1) {
//                Log.d(TAG, "queueVideo" + " start play first video on queue");
                playNextMediaInQueue();
//            }

        }
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
//                            + getSupportActionBar().getHeight()
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

    private void setupActionBar() {
//        Toolbar toolbar = (Toolbar) getAcfindViewById(R.id.toolbar);
//        toolbar.setTitle(mSelectedMedia.getTitle());
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * indicates whether we are doing a local or a remote playback
     */
    public enum PlaybackLocation {
        LOCAL,
        REMOTE
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
                    if (mLocation == LocalPlayerActivity.PlaybackLocation.LOCAL) {
                        int currentPos = mVideoView.getCurrentPosition();
                        updateSeekbar(currentPos, mDuration);
                    }
                }
            });
        }
    }
}
