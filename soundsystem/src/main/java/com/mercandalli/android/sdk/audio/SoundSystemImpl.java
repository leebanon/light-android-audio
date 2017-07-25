package com.mercandalli.android.sdk.audio;

import android.os.Process;

/**
 * Class used to communicate with the sound system.
 */
/* package */
class SoundSystemImpl implements SoundSystem {

    static {
        // Load native library
        System.loadLibrary("soundsystem");
    }

    private final SoundSystemEntryPoint soundSystemEntryPoint;

    /**
     * Private constructor.
     */
    /* package */
    SoundSystemImpl(SoundSystemEntryPoint soundSystemEntryPoint) {
        this.soundSystemEntryPoint = soundSystemEntryPoint;
    }

    /**
     * Initialize the sound system.
     *
     * @param nativeFrameRate    Native value tf the device frame rate
     * @param nativeFramesPerBuf Native value of the number of frames per buffer.
     */
    public void initSoundSystem(final int nativeFrameRate, final int nativeFramesPerBuf) {
        soundSystemEntryPoint.native_init_soundsystem(nativeFrameRate, nativeFramesPerBuf);
    }

    /**
     * Release native objects and this object.
     */
    public void release() {
        soundSystemEntryPoint.native_release_soundsystem();
    }

    public boolean isSoundSystemInit() {
        return soundSystemEntryPoint.native_is_soundsystem_init();
    }

    /**
     * Load track file into the RAM.
     *
     * @param filePaths Path of the file on the hard disk.
     */
    public void loadViaExtractionWrapper(final String[] filePaths) {
        Preconditions.checkNotNull(filePaths);
        soundSystemEntryPoint.native_extraction_wrapper(filePaths);
    }

    /**
     * Load track file into the RAM.
     *
     * @param filePath Path of the file on the hard disk.
     */
    public void loadFileOpenSl(final String filePath) {
        Preconditions.checkNotNull(filePath);
        soundSystemEntryPoint.native_load_file_open_sl(filePath);
    }

    /**
     * Load track file into the RAM.
     *
     * @param filePath Path of the file on the hard disk.
     */
    public void loadFileMediaCodec(final String filePath) {
        Preconditions.checkNotNull(filePath);
        soundSystemEntryPoint.native_load_file_media_codec(filePath);
    }

    /**
     * Load track file into the RAM.
     *
     * @param filePath Path of the file on the hard disk.
     */
    public void loadFileFFmpegJavaThread(final String filePath) {
        Preconditions.checkNotNull(filePath);
        final Thread thread = new Thread("extractor-thread") {
            @Override
            public void run() {
                super.run();
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                soundSystemEntryPoint.native_load_file_ffmpeg_synchronous(filePath);
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Load track file into the RAM.
     *
     * @param filePath Path of the file on the hard disk.
     */
    @Override
    public void loadFileFFmpegNativeThread(final String filePath) {
        soundSystemEntryPoint.native_load_file_ffmpeg(filePath);
    }

    /**
     * Play music if params is true, otherwise, pause the music.
     *
     * @param isPlaying True if music is playing.
     */
    public void playMusic(final boolean isPlaying) {
        soundSystemEntryPoint.native_play(isPlaying);
    }

    /**
     * Stop music.
     */
    public void stopMusic() {
        soundSystemEntryPoint.native_stop();
    }

    /**
     * Get if a track is currently playing or not.
     *
     * @return True if a track is played.
     */
    public boolean isPlaying() {
        return soundSystemEntryPoint.native_is_playing();
    }

    /**
     * Get if a track has been loaded .
     *
     * @return True if a track is loaded.
     */
    public boolean isLoaded() {
        return soundSystemEntryPoint.native_is_loaded();
    }

    /**
     * Provide extracted data from audio file. Data[2n] represent one channel and Data[2n+1]
     * represente the other channel.
     *
     * @return A short array containing all extracted data from audio file.
     */
    public short[] getExtractedData() {
        return soundSystemEntryPoint.native_get_extracted_data();
    }

    /**
     * Provide mono data generate from extracted data which where in stereo mode.
     *
     * @return A shorrt array containing mono data of extracted audio file.
     */
    public short[] getExtractedDataMono() {
        return soundSystemEntryPoint.native_get_extracted_data_mono();
    }

    public boolean addPlayingStatusListener(PlayingStatusListener listener) {
        return soundSystemEntryPoint.addPlayingStatusListener(listener);
    }

    public boolean removePlayingStatusListener(PlayingStatusListener listener) {
        return soundSystemEntryPoint.removePlayingStatusListener(listener);
    }

    public boolean addExtractionListener(ExtractionListener listener) {
        return soundSystemEntryPoint.addExtractionListener(listener);
    }

    public boolean removeExtractionListener(ExtractionListener observer) {
        return soundSystemEntryPoint.removeExtractionListener(observer);
    }
}
