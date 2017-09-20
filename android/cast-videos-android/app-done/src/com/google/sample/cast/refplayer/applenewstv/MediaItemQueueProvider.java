package com.google.sample.cast.refplayer.applenewstv;

import com.google.sample.cast.refplayer.utils.MediaItem;

import java.util.ArrayList;

/**
 * Created by warenix on 9/18/17.
 */

public class MediaItemQueueProvider {
    private ArrayList<MediaItem> mediaItemArrayList = new ArrayList<>();
    private int mCurrentMediaItemIndex = -1;

    public MediaItem getNextMediaItemInQueue() {
        mCurrentMediaItemIndex++;
        return mediaItemArrayList.get(mCurrentMediaItemIndex);
    }

    void queueMediaItemToFront(MediaItem mediaItem) {
        mediaItemArrayList.add(mCurrentMediaItemIndex + 1, mediaItem);
    }

    public MediaItem getMediaItem(int position) {
        return mediaItemArrayList.get(position);
    }

    public int getCurrentMediaItemIndex() {
        return mCurrentMediaItemIndex;
    }

    public int size() {
        return mediaItemArrayList.size();
    }

    public void queueMedia(MediaItem mediaItem, boolean atFront) {
        if (atFront) {
            int oldVideoIndex = mediaItemArrayList.indexOf(mediaItem);
            if (oldVideoIndex != -1) {
                mediaItemArrayList.remove(oldVideoIndex);
            }
            mediaItemArrayList.add(mCurrentMediaItemIndex + 1, mediaItem);
        } else {
            mediaItemArrayList.add(mediaItem);
        }
    }
}
