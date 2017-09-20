package com.google.sample.cast.refplayer.applenewstv;

import android.support.annotation.IntDef;

import com.google.sample.cast.refplayer.utils.MediaItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by warenix on 9/12/17.
 */

public interface VideoQueuePlayer {

    /**
     * Open media.
     * A state change from {@link VideoQueuePlayerListener#STATE_IDLE} to {@link VideoQueuePlayerListener#STATE_OPENED}
     *
     * @param mediaItem
     */
    void openMedia(MediaItem mediaItem);

    /**
     * Seek to certain position.
     * If autoPlay is true, a state change from {@link VideoQueuePlayerListener#STATE_PLAYING}/ {@link VideoQueuePlayerListener#STATE_PAUSE} to {@link VideoQueuePlayerListener#STATE_PLAYING}
     *
     * @param position
     * @param autoPlay
     */
    void seek(int position, boolean autoPlay);

    /**
     * Play/ Pause currently loaded video
     * If media item is not ended, a state change between {@link VideoQueuePlayerListener#STATE_PLAYING} and {@link VideoQueuePlayerListener#STATE_PAUSE}
     * Otherwise a state change from {@link VideoQueuePlayerListener#STATE_PLAYING} to {@link VideoQueuePlayerListener#STATE_IDLE}
     */
    void togglePlayback();


    /**
     * Queue a media item
     *
     * @param mediaItem
     */
    void queueMedia(MediaItem mediaItem);

    void onMediaItemCompleted();

    interface VideoQueuePlayerListener {

        int STATE_IDLE = 1;
        /**
         * Media is playing
         */
        int STATE_PLAYING = 2;
        /**
         * Media is paused. Can resume playback at paused position.
         */
        int STATE_PAUSE = 3;
        int STATE_BUFFERING = 4;

        /**
         * A media item is opened.
         */
        int STATE_OPENED = 5;

        void onStateChange(@State int oldState, @State int newState);

        @IntDef({STATE_IDLE, STATE_PLAYING, STATE_PAUSE, STATE_BUFFERING, STATE_OPENED})
        @Retention(RetentionPolicy.SOURCE)
        @interface State {
        }


    }
}
