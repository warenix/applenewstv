/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cast.refplayer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.widget.MiniControllerFragment;
import com.google.sample.cast.refplayer.applenewstv.AutoVideoPlayerFragment2;
import com.google.sample.cast.refplayer.browser.VideoQueueBrowserFragment;
import com.google.sample.cast.refplayer.settings.CastPreference;
import com.google.sample.cast.refplayer.utils.MediaItem;

import org.dyndns.warenix.applenewstv.R;

import java.util.ArrayList;
import java.util.List;

public class VideoBrowserActivity extends AppCompatActivity implements AutoVideoPlayerFragment2.MediaItemListener {

    private static final String TAG = "VideoBrowserActivity";
    private boolean mIsHoneyCombOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private Toolbar mToolbar;
    private CastContext mCastContext;
    private MenuItem mediaRouteMenuItem;
    private IntroductoryOverlay mIntroductoryOverlay;
    private CastStateListener mCastStateListener;
    private AutoVideoPlayerFragment2 mPlayerFragment;
    private VideoQueueBrowserFragment mVideoBrowseFragment;
    private CastSession mCastSession;
    private MiniControllerFragment mCastMiniControllerFragment;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_browser);
        setupActionBar();
        setupPlayer();
        setupCastListener();

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
//                    showIntroductoryOverlay();
                }

                if (newState == CastState.CONNECTED) {
                    Log.d(TAG, "onCastStateChanged()" + "pass cast session to player");
                    mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
                    mPlayerFragment.setCastSession(mCastSession);

                    setupCastPlayer();
                }
            }
        };

        mCastContext = CastContext.getSharedInstance(this);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        if (appLinkData != null) {
            Log.d(TAG, "onCreate() handle app link:" + appLinkAction + " data:" + appLinkData);
            MediaItem mediaItem = new MediaItem();
            mediaItem.setUrl(appLinkData.toString());
            mediaItem.setTitle("Sample Video");
            mediaItem.setContentType("video/mp4");
            mediaItem.setStudio("");
            mediaItem.setSubTitle("");
            mediaItem.setDuration(3000);
            ArrayList<MediaItem> appLinkMediaItemList = new ArrayList<>();
            appLinkMediaItemList.add(mediaItem);
            mVideoBrowseFragment.playAdhocMediaItem(mediaItem);
            onMediaItemListLoaded(appLinkMediaItemList);
        } else {
            mVideoBrowseFragment.loadPlayList();
        }
    }

    private void setupPlayer() {
        mPlayerFragment = (AutoVideoPlayerFragment2) getSupportFragmentManager().findFragmentById(R.id.autoVideoPlayer);
        mVideoBrowseFragment = (VideoQueueBrowserFragment) getSupportFragmentManager().findFragmentById(R.id.browse);
    }

    private void setupCastPlayer() {
//        mCastMiniControllerFragment = (MiniControllerFragment)getSupportFragmentManager().findFragmentById(R.id.castMiniController);
//        findViewById(R.id.castMiniController).setVisibility(View.VISIBLE);
//        findViewById(R.id.autoVideoPlayer).setVisibility(View.GONE);
        getSupportActionBar().show();
    }

    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
            }

            @Override
            public void onSessionEnding(CastSession session) {
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
            }

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                setupCastPlayer();
//                if (null != mSelectedMedia) {
//
//                    if (mPlaybackState == LocalPlayerActivity.PlaybackState.PLAYING) {
//                        mVideoView.pause();
//                        loadRemoteMedia(mSeekbar.getProgress(), true);
//                        return;
//                    } else {
//                        mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
//                        updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.REMOTE);
//                    }
//                }
//                updatePlayButton(mPlaybackState);
                // TODO continue playing in remote
                invalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
//                updatePlaybackLocation(LocalPlayerActivity.PlaybackLocation.LOCAL);
//                mPlaybackState = LocalPlayerActivity.PlaybackState.IDLE;
//                mLocation = LocalPlayerActivity.PlaybackLocation.LOCAL;
//                updatePlayButton(mPlaybackState);
                // TODO continue playing in local
                invalidateOptionsMenu();
            }
        };
    }


    @Override
    protected void onResume() {
        mCastContext.addCastStateListener(mCastStateListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mCastContext.removeCastStateListener(mCastStateListener);
        super.onPause();
    }


    private void setupActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.app_name);
        mToolbar.setBackgroundColor(Color.TRANSPARENT);
        setSupportActionBar(mToolbar);

        // Status bar :: Transparent
        Window window = this.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.action_settings:
                i = new Intent(VideoBrowserActivity.this, CastPreference.class);
                startActivity(i);
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy is called");
        super.onDestroy();
    }


    private void showIntroductoryOverlay() {
        if (mIntroductoryOverlay != null) {
            mIntroductoryOverlay.remove();
        }
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mIntroductoryOverlay = new IntroductoryOverlay.Builder(
                            VideoBrowserActivity.this, mediaRouteMenuItem)
                            .setTitleText("Introducing Cast")
                            .setSingleTime()
                            .setOnOverlayDismissedListener(
                                    new IntroductoryOverlay.OnOverlayDismissedListener() {
                                        @Override
                                        public void onOverlayDismissed() {
                                            mIntroductoryOverlay = null;
                                        }
                                    })
                            .build();
                    mIntroductoryOverlay.show();
                }
            });
        }
    }

    public void onVideoItemClicked(MediaItem mediaItem) {
        Log.d(TAG, "onVideoItemClicked() jump the queue to play this item");
        mPlayerFragment.jumpToMedia(mediaItem);
    }

    public void onMediaItemListLoaded(List<MediaItem> data) {
        if (data == null) {
            return;
        }
        for (MediaItem mediaItem : data) {
            mPlayerFragment.queueVideo(mediaItem);
        }
    }

    @Override
    public void onMediaItemOpened(MediaItem mediaItem) {
        Log.d(TAG, "highlight media item on browser fragment");
        mVideoBrowseFragment.setActiveMediaItem(mediaItem);
    }
}
