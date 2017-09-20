package com.google.sample.cast.refplayer.applenewstv;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.VideoView;

import com.google.sample.cast.refplayer.utils.MediaItem;

import java.util.ArrayList;


/**
 * Created by warenix on 9/12/17.
 */

public class LocalVideoPlayerController implements VideoQueuePlayer, MediaPlayer.OnCompletionListener {
    private static final String TAG = LocalVideoPlayerController.class.getSimpleName()
            .substring(0, Math.min(25, LocalVideoPlayerController.class.getSimpleName().length()));
    private ArrayList<MediaItem> mMediaItemQueue = new ArrayList<>();
    private @VideoQueuePlayerListener.State
    int mPlaybackState = VideoQueuePlayerListener.STATE_IDLE;
    private VideoView mVideoView;
    private VideoQueuePlayerListener mVideoQueuePlayerListener;
    private MediaItem mSelectedMedia;

    private int mCurrentQueueIndex = -1;

    public void setVideoView(VideoView videoView) {
        mVideoView = videoView;
        mVideoView.setOnCompletionListener(this);
    }

    public void setVideoQueuePlayerListener(VideoQueuePlayerListener l) {
        mVideoQueuePlayerListener = l;
    }

    @Override
    public void openMedia(MediaItem mediaItem) {
        mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
        Log.d(TAG, "openMedia() Setting url of the VideoView to: " + mSelectedMedia.getUrl());
        changeState(VideoQueuePlayerListener.STATE_OPENED);
    }

    @Override
    public void seek(int position, boolean autoPlay) {
        if (position > 0) {
            mVideoView.seekTo(position);
        }

        if (autoPlay) {
            changeState(VideoQueuePlayerListener.STATE_PLAYING);
        } else {
            changeState(VideoQueuePlayerListener.STATE_PAUSE);
        }
    }

    public MediaItem getSelectedMedia() {
        return mSelectedMedia;
    }


    private void changeState(@VideoQueuePlayerListener.State int newState) {
        @VideoQueuePlayerListener.State int oldState = mPlaybackState;
        mPlaybackState = newState;
        Log.d(TAG, String.format("changeState() oldState[%d] to newState[%d]", oldState, mPlaybackState));

        if (mVideoQueuePlayerListener != null) {
            Log.d(TAG, String.format("changeState() notify video queue player listener"));
            mVideoQueuePlayerListener.onStateChange(oldState, mPlaybackState);
        }
    }

    @Override
    public void togglePlayback() {

        switch (mPlaybackState) {
            case VideoQueuePlayerListener.STATE_IDLE: {
                // play next media in queue
                playNextMediaInQueue();
                break;
            }
            case VideoQueuePlayerListener.STATE_BUFFERING:
                break;
            case VideoQueuePlayerListener.STATE_PAUSE:
//                mVideoView.start();
                changeState(VideoQueuePlayerListener.STATE_PLAYING);
                break;
            case VideoQueuePlayerListener.STATE_PLAYING:
//                mVideoView.pause();
                changeState(VideoQueuePlayerListener.STATE_PAUSE);
                break;
            case VideoQueuePlayerListener.STATE_OPENED:
                changeState(VideoQueuePlayerListener.STATE_PLAYING);
                break;
        }


    }

    @Override
    public void queueMedia(MediaItem mediaItem) {
        if (mediaItem == null) {
            return;
        }
        mMediaItemQueue.add(mediaItem);
        Log.d(TAG, String.format("queueMedia() queue size now: %d", mMediaItemQueue.size()));
    }

    @Override
    public void onMediaItemCompleted() {
        mVideoView.stopPlayback();
        changeState(VideoQueuePlayerListener.STATE_IDLE);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.stop();
        onMediaItemCompleted();
    }

    public void playNextMediaInQueue() {
        Log.d(TAG, "playNextMediaInQueue() is called");
        if (mMediaItemQueue.size() == 0) {
            return;
        }
        if (mPlaybackState == VideoQueuePlayerListener.STATE_PLAYING) {
//            TODO handle skip playing
//            return;
        }

//        seek(0, true);
        if (mCurrentQueueIndex +1 == mMediaItemQueue.size()) {
            Log.d(TAG, "No more videos");
            changeState(VideoQueuePlayerListener.STATE_IDLE);
        } else {
            ++mCurrentQueueIndex;

            mSelectedMedia = mMediaItemQueue.get(mCurrentQueueIndex);
            Log.d(TAG, "playNextMediaInQueue() loaded next media in queue. " + mSelectedMedia.getTitle());
            changeState(VideoQueuePlayerListener.STATE_OPENED);

            // play
            togglePlayback();
        }
    }

    public void jumpToMedia(MediaItem mediaItem) {
        int jumpToVideoItem = mMediaItemQueue.indexOf(mediaItem);
        mCurrentQueueIndex = jumpToVideoItem-1;
        playNextMediaInQueue();
    }
}
