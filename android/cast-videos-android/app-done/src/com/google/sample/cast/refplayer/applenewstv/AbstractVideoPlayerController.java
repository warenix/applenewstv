package com.google.sample.cast.refplayer.applenewstv;

import android.util.Log;

import com.google.sample.cast.refplayer.utils.MediaItem;

import java.util.ArrayList;


/**
 * Play opened {@link MediaItem}
 * Created by warenix on 9/12/17.
 */

public abstract class AbstractVideoPlayerController implements VideoQueuePlayer {
    private static final String TAG = AbstractVideoPlayerController.class.getSimpleName()
            .substring(0, Math.min(25, AbstractVideoPlayerController.class.getSimpleName().length()));
    private @VideoQueuePlayerListener.State
    int mPlaybackState = VideoQueuePlayerListener.STATE_IDLE;
    private ArrayList<VideoQueuePlayerListener> mVideoQueuePlayerListenerList = new ArrayList<>();
    private MediaItem mSelectedMedia;

    private int mCurrentQueueIndex = -1;
    private ArrayList<MediaItem> mMediaItemList = new ArrayList<>();

    public int getCurentQueueIndex() {
        return mCurrentQueueIndex;
    }

    public void addVideoQueuePlayerListener(VideoQueuePlayerListener l) {
        mVideoQueuePlayerListenerList.add(l);
    }

    public void removeVideoQueuePlayerListener(VideoQueuePlayerListener l) {
        mVideoQueuePlayerListenerList.remove(l);
    }


    @Override
    public void openMedia(MediaItem mediaItem) {
        if (mSelectedMedia == null) {
            Log.e(TAG, "openMedia() no selected media");
            return;
        }
        Log.d(TAG, "openMedia() Setting url of the VideoView to: " + mSelectedMedia.getUrl());
        changeState(VideoQueuePlayerListener.STATE_OPENED);
    }

    @Override
    public void seek(int position, boolean autoPlay) {
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

//        VideoQueuePlayerListener l = null;
//        for (int i = 0; i < mVideoQueuePlayerListenerList.size(); ++i) {
//            if (l != null && l.isActive()) {
//                Log.d(TAG, String.format("changeState() notify video queue player listener"));
//                l.onStateChange(oldState, mPlaybackState);
//            }
//        }

    }

    @Override
    public void togglePlayback() {

        switch (mPlaybackState) {
            case VideoQueuePlayerListener.STATE_IDLE: {
                playNextMediaInQueue();
                break;
            }
            case VideoQueuePlayerListener.STATE_BUFFERING:
                break;
            case VideoQueuePlayerListener.STATE_PAUSE:
                changeState(VideoQueuePlayerListener.STATE_PLAYING);
                break;
            case VideoQueuePlayerListener.STATE_PLAYING:
                changeState(VideoQueuePlayerListener.STATE_PAUSE);
                break;
            case VideoQueuePlayerListener.STATE_OPENED:
                changeState(VideoQueuePlayerListener.STATE_PLAYING);
                break;
        }


    }

    public void queueMedia(MediaItem mediaItem, boolean atFront) {
        if (mediaItem == null) {
            return;
        }
        if (atFront) {
            mMediaItemList.add(mCurrentQueueIndex + 1, mediaItem);
            Log.d(TAG, "queueMedia() added media as next item at " + (mCurrentQueueIndex + 1));

//            int currentMediaItemIndex = mMediaItemQueue.indexOf(mediaItem);
//            if (currentMediaItemIndex > 0) {
//                Log.d(TAG, "queueMedia: found media at " + currentMediaItemIndex);
//                // swap it to the front
//                mMediaItemQueue.remove(currentMediaItemIndex);
//                mMediaItemQueue.add(currentMediaItemIndex, mediaItem);
//            }
            // should play it now
            playNextMediaInQueue();
        } else {
            mMediaItemList.add(mCurrentQueueIndex + 1, mediaItem);
            Log.d(TAG, "queueMedia() added media as last " + (mMediaItemList.size()));
            mMediaItemList.add(mMediaItemList.size(), mediaItem);
        }

//        Log.d(TAG, String.format("queueMedia() queue size now: %d", mMediaItemQueue.size()));
    }

    protected abstract void onMediaItemListChanged();

    public void setSelectedMediaItem(MediaItem mediaItem) {
        mSelectedMedia = mediaItem;
    }


    public void playNextMediaInQueue() {
        if (mSelectedMedia == null) {
            Log.d(TAG, "playNextMediaInQueue() is called, but no selected media");
            return;
        }
        mCurrentQueueIndex++;
        mSelectedMedia = mMediaItemList.get(mCurrentQueueIndex);
        onMediaItemListChanged();

        Log.d(TAG, "playNextMediaInQueue() loaded next media in queue. index:" + mCurrentQueueIndex + " url:" + mSelectedMedia.getTitle());
        changeState(VideoQueuePlayerListener.STATE_OPENED);

        // play
        togglePlayback();
    }
}
