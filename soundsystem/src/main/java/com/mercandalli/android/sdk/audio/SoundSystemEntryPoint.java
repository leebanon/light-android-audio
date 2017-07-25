package com.mercandalli.android.sdk.audio;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Native methods.
 */
/* package */
class SoundSystemEntryPoint {

    /**
     * List of all playing status observer.
     */
    private final List<SoundSystem.PlayingStatusListener> playingStatusListeners;

    /**
     * List of all observer listening for the extraction state.
     */
    private final List<SoundSystem.ExtractionListener> extractionListeners;

    /**
     * Handler attach to the main thread.
     */
    private final Handler mainHandler;

    /* package */ SoundSystemEntryPoint(Looper mainThreadLooper) {
        this.mainHandler = new Handler(mainThreadLooper);

        this.playingStatusListeners = new ArrayList<>();
        this.extractionListeners = new ArrayList<>();
    }

    /**
     * Notify that the track is finished
     * Called from native code.
     */
    @SuppressWarnings("unused")
    public void notifyPlayingStatusObserversEndTrack() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (playingStatusListeners) {
                    for (final SoundSystem.PlayingStatusListener observer : playingStatusListeners) {
                        observer.onEndOfMusic();
                    }
                }
            }
        });
    }

    /**
     * Notify of the new player state.
     * Called from native code.
     */
    @SuppressWarnings("unused")
    public void notifyPlayingStatusObserversPlayPause(final boolean isPlaying) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (playingStatusListeners) {
                    for (final SoundSystem.PlayingStatusListener observer : playingStatusListeners) {
                        observer.onPlayingStatusDidChange(isPlaying);
                    }
                }
            }
        });
    }

    /**
     * Notify track has stopped.
     * Called from native code.
     */
    @SuppressWarnings("unused")
    public void notifyStopTrack() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (playingStatusListeners) {
                    for (final SoundSystem.PlayingStatusListener observer : playingStatusListeners) {
                        observer.onStopTrack();
                    }
                }
            }
        });
    }

    /**
     * Notify that the extraction has started.
     * Called from native code.
     */
    @SuppressWarnings("unused")
    public void notifyExtractionStarted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (extractionListeners) {
                    for (final SoundSystem.ExtractionListener observer : extractionListeners) {
                        observer.onExtractionStarted();
                    }
                }
            }
        });
    }

    /**
     * Notify that the extraction has finished.
     * Called from native code.
     */
    @SuppressWarnings("unused")
    public void notifyExtractionCompleted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (extractionListeners) {
                    for (final SoundSystem.ExtractionListener observer : extractionListeners) {
                        observer.onExtractionCompleted();
                    }
                }
            }
        });
    }

    /* package */ boolean addPlayingStatusListener(SoundSystem.PlayingStatusListener listener) {
        synchronized (playingStatusListeners) {
            //noinspection SimplifiableIfStatement
            if (listener == null || playingStatusListeners.contains(listener)) {
                return false;
            }
            return playingStatusListeners.add(listener);
        }
    }

    /* package */ boolean removePlayingStatusListener(SoundSystem.PlayingStatusListener listener) {
        synchronized (playingStatusListeners) {
            return playingStatusListeners.remove(listener);
        }
    }

    /* package */ boolean addExtractionListener(SoundSystem.ExtractionListener listener) {
        synchronized (extractionListeners) {
            //noinspection SimplifiableIfStatement
            if (listener == null || extractionListeners.contains(listener)) {
                return false;
            }
            return extractionListeners.add(listener);
        }
    }

    /* package */ boolean removeExtractionListener(SoundSystem.ExtractionListener listener) {
        synchronized (extractionListeners) {
            return extractionListeners.remove(listener);
        }
    }

    /* package */
    native void native_init_soundsystem(int nativeFrameRate, int nativeFramesPerBuf);

    /* package */
    native boolean native_is_soundsystem_init();

    /* package */
    native void native_extraction_wrapper(String[] filePaths);

    /* package */
    native void native_load_file_open_sl(String filePath);

    /* package */
    native void native_load_file_media_codec(String filePath);

    /* package */
    native void native_load_file_ffmpeg_synchronous(String filePath);

    /* package */
    native void native_load_file_ffmpeg(String filePath);

    /* package */
    native void native_release_soundsystem();

    /* package */
    native void native_play(boolean play);

    /* package */
    native boolean native_is_playing();

    /* package */
    native boolean native_is_loaded();

    /* package */
    native void native_stop();

    /* package */
    native short[] native_get_extracted_data();

    /* package */
    native short[] native_get_extracted_data_mono();
}
