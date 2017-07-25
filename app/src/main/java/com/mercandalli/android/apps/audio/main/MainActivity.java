package com.mercandalli.android.apps.audio.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mercandalli.android.apps.audio.R;
import com.mercandalli.android.apps.audio.audio.AudioFeaturesManager;
import com.mercandalli.android.apps.audio.file.FileManager;
import com.mercandalli.android.sdk.audio.SoundSystem;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple activity launching the sound system.
 */
public class MainActivity extends AppCompatActivity {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ACTION_NO_ACTION,
            ACTION_LOAD_FILE_VIA_EXTRACTION_WRAPPER,
            ACTION_LOAD_FILE_OPEN_SL,
            ACTION_LOAD_FILE_MEDIA_CODEC,
            ACTION_LOAD_FILE_FFMPEG_JAVA_THREAD,
            ACTION_LOAD_FILE_FFMPEG_NATIVE_THREAD})
    @interface Action {
    }

    private static final String ACTION_NO_ACTION = "ACTION_NO_ACTION";
    private static final String ACTION_LOAD_FILE_VIA_EXTRACTION_WRAPPER = "ACTION_LOAD_FILE_VIA_EXTRACTION_WRAPPER";
    private static final String ACTION_LOAD_FILE_OPEN_SL = "ACTION_LOAD_FILE_OPEN_SL";
    private static final String ACTION_LOAD_FILE_MEDIA_CODEC = "ACTION_LOAD_FILE_MEDIA_CODEC";
    private static final String ACTION_LOAD_FILE_FFMPEG_JAVA_THREAD = "ACTION_LOAD_FILE_FFMPEG_JAVA_THREAD";
    private static final String ACTION_LOAD_FILE_FFMPEG_NATIVE_THREAD = "ACTION_LOAD_FILE_FFMPEG_NATIVE_THREAD";

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private Button toggleStop;
    private Button btnExtractFileViaExtractionWrapper;
    private Button btnExtractFileMediaCodecNativeThread;
    private Button btnExtractFfmpegJavaThread;
    private Button btnExtractFfmpegNativeThread;
    private Button btnExtractFileOpenSlNativeThread;
    private ToggleButton togglePlayPause;
    private TextView logTextView;
    private Spinner fileSpinner;

    private long extractionStartTimestamp;
    private long extractionDuration;

    private SoundSystem soundSystem;
    private FileManager fileManager;
    private FileManager.OnInitListener fileManagerInitializeListener;
    private AudioFeaturesManager audioFeaturesManager;

    @Action
    private String action = ACTION_NO_ACTION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.activity_main_toolbar));

        fileManagerInitializeListener = createFileManagerInitializeListener();
        audioFeaturesManager = AudioFeaturesManager.init(this);
        soundSystem = SoundSystem.Instance.getInstance();
        fileManager = FileManager.Instance.getInstance(this);

        findViews();

        if (!soundSystem.isSoundSystemInit()) {
            soundSystem.initSoundSystem(
                    audioFeaturesManager.getSampleRate(),
                    audioFeaturesManager.getFramesPerBuffer());
        }
        if (!fileManager.isInitialized()) {
            fileManager.registerOnInitListener(fileManagerInitializeListener);
            fileManager.initialize();
        }

        initUI();
        attachToListeners();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            displayAvailableAudioCodecs();
        }
    }

    @Override
    protected void onDestroy() {
        if (!isChangingConfigurations()) {
            detachListeners();
            soundSystem.release();
        }
        fileManager.unregisterOnInitListener(fileManagerInitializeListener);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String path = fileManager.getFile().getAbsolutePath();
            switch (action) {
                case ACTION_LOAD_FILE_VIA_EXTRACTION_WRAPPER:
                    loadTrackViaExtractionWrapperAskPermission();
                    break;
                case ACTION_LOAD_FILE_OPEN_SL:
                    soundSystem.loadFileOpenSl(path);
                    break;
                case ACTION_LOAD_FILE_MEDIA_CODEC:
                    soundSystem.loadFileMediaCodec(path);
                    break;
                case ACTION_LOAD_FILE_FFMPEG_JAVA_THREAD:
                    soundSystem.loadFileFFmpegJavaThread(path);
                    break;
                case ACTION_LOAD_FILE_FFMPEG_NATIVE_THREAD:
                    soundSystem.loadFileFFmpegNativeThread(path);
                    break;
                case ACTION_NO_ACTION:
                    break;
            }
            action = ACTION_NO_ACTION;
        } else {
            Toast.makeText(this, "No permission, no extraction !", Toast.LENGTH_SHORT).show();
        }
    }

    private void findViews() {
        btnExtractFileOpenSlNativeThread = (Button) findViewById(R.id.btn_extract_file_opensl_native_thread);
        btnExtractFileViaExtractionWrapper = (Button) findViewById(R.id.btn_extract_file_extraction_wrapper_thread);
        btnExtractFileMediaCodecNativeThread = (Button) findViewById(R.id.btn_extract_file_mediacodec_native_thread);
        btnExtractFfmpegJavaThread = (Button) findViewById(R.id.btn_extract_file_ffmpeg_java_thread);
        btnExtractFfmpegNativeThread = (Button) findViewById(R.id.btn_extract_file_ffmpeg_native_thread);
        togglePlayPause = (ToggleButton) findViewById(R.id.toggle_play_pause);
        toggleStop = (Button) findViewById(R.id.btn_stop);
        logTextView = (TextView) findViewById(R.id.tv_logs);
        fileSpinner = (Spinner) findViewById(R.id.activity_main_file_spinner);
    }

    private void initUI() {
        btnExtractFileOpenSlNativeThread.setOnClickListener(onClickListener);
        btnExtractFileViaExtractionWrapper.setOnClickListener(onClickListener);
        btnExtractFileMediaCodecNativeThread.setOnClickListener(onClickListener);
        btnExtractFfmpegJavaThread.setOnClickListener(onClickListener);
        btnExtractFfmpegNativeThread.setOnClickListener(onClickListener);
        togglePlayPause.setOnCheckedChangeListener(mOnCheckedChangeListener);
        toggleStop.setOnClickListener(onClickListener);
        if (!fileManager.isInitialized()) {
            togglePlayPause.setEnabled(false);
            toggleStop.setEnabled(false);
            btnExtractFileOpenSlNativeThread.setEnabled(false);
            btnExtractFileMediaCodecNativeThread.setEnabled(false);
            btnExtractFfmpegJavaThread.setEnabled(false);
            btnExtractFfmpegNativeThread.setEnabled(false);
        } else {
            syncButtonsWithSoundSystem();
        }
        fileSpinner.setSelection(trackFormatToSpinnerPosition());
        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                @FileManager.TrackFormat
                String trackFormat = spinnerPositionToTrackFormat(position);
                if (FileManager.FORMAT_OGG.equals(trackFormat)) {
                    log("OGG extraction is flaky");
                }
                fileManager.setTrackFormat(trackFormat);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private int trackFormatToSpinnerPosition() {
        @FileManager.TrackFormat
        String trackFormat = fileManager.getTrackFormat();
        switch (trackFormat) {
            case FileManager.FORMAT_AAC:
                return 0;
            case FileManager.FORMAT_MP3:
                return 1;
            case FileManager.FORMAT_MP3_MONO:
                return 2;
            case FileManager.FORMAT_OGG:
                return 3;
            case FileManager.FORMAT_WAV:
                return 4;
        }
        return 0;
    }

    @FileManager.TrackFormat
    private String spinnerPositionToTrackFormat(int position) {
        switch (position) {
            case 0:
                return FileManager.FORMAT_AAC;
            case 1:
                return FileManager.FORMAT_MP3;
            case 2:
                return FileManager.FORMAT_MP3_MONO;
            case 3:
                return FileManager.FORMAT_OGG;
            case 4:
                return FileManager.FORMAT_WAV;
        }
        return FileManager.FORMAT_AAC;
    }

    private void syncButtonsWithSoundSystem() {
        if (soundSystem.isLoaded()) {
            togglePlayPause.setEnabled(true);
            toggleStop.setEnabled(true);
            btnExtractFileOpenSlNativeThread.setEnabled(false);
            btnExtractFileMediaCodecNativeThread.setEnabled(false);
            btnExtractFfmpegJavaThread.setEnabled(false);
            btnExtractFfmpegNativeThread.setEnabled(false);
        } else {
            btnExtractFileOpenSlNativeThread.setEnabled(true);
            btnExtractFileMediaCodecNativeThread.setEnabled(true);
            btnExtractFfmpegJavaThread.setEnabled(true);
            btnExtractFfmpegNativeThread.setEnabled(true);
            togglePlayPause.setEnabled(false);
            toggleStop.setEnabled(false);
        }
        togglePlayPause.setChecked(soundSystem.isPlaying());
    }

    private void attachToListeners() {
        soundSystem.addPlayingStatusListener(playingStatusObserver);
        soundSystem.addExtractionListener(extractionObserver);
    }

    private void detachListeners() {
        soundSystem.removePlayingStatusListener(playingStatusObserver);
        soundSystem.removeExtractionListener(extractionObserver);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void displayAvailableAudioCodecs() {
        final MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        final MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();
        final StringBuilder stringBuilder = new StringBuilder();
        for (final MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                boolean isAudioCodec = false;
                for (final String type : codecInfo.getSupportedTypes()) {
                    if (type.contains("audio/")) {
                        isAudioCodec = true;
                    }
                }
                if (isAudioCodec) {
                    stringBuilder.append(codecInfo.getName()).append(" : \n");
                    for (final String type : codecInfo.getSupportedTypes()) {
                        if (type.contains("audio/")) {
                            stringBuilder.append(type).append("\n");
                        }
                    }
                }
            }
        }
        log("\nAvailable codecs : \n" + stringBuilder.toString());
    }

    @SuppressLint("SetTextI18n")
    private void log(String message) {
        logTextView.setText(message + "\n" + logTextView.getText().toString());
    }

    private SoundSystem.ExtractionListener extractionObserver = new SoundSystem.ExtractionListener() {
        @Override
        public void onExtractionStarted() {
            extractionStartTimestamp = System.currentTimeMillis();
            btnExtractFileOpenSlNativeThread.setEnabled(false);
            btnExtractFileMediaCodecNativeThread.setEnabled(false);
            btnExtractFfmpegJavaThread.setEnabled(false);
            btnExtractFfmpegNativeThread.setEnabled(false);
            log("");
            log("---------------------------------");
            log("[Extraction] Started");
            log("[Extraction] Action: " + action);
            log("[Extraction] Path: " + fileManager.getFile().getAbsolutePath());
        }

        @Override
        public void onExtractionCompleted() {
            extractionDuration = System.currentTimeMillis() - extractionStartTimestamp;
            togglePlayPause.setEnabled(true);
            toggleStop.setEnabled(true);
            log("[Extraction] Ended " + action);
            log("[Extraction] Duration " + (extractionDuration / 1_000f) + "s");
            log("---------------------------------");
            log("");
            action = ACTION_NO_ACTION;

            btnExtractFileOpenSlNativeThread.setEnabled(true);
            btnExtractFileMediaCodecNativeThread.setEnabled(true);
            btnExtractFfmpegJavaThread.setEnabled(true);
            btnExtractFfmpegNativeThread.setEnabled(true);
        }
    };

    private SoundSystem.PlayingStatusListener playingStatusObserver = new SoundSystem.PlayingStatusListener() {
        @Override
        public void onPlayingStatusDidChange(final boolean playing) {
            log(playing ? "Playing" : "Pause");
        }

        @Override
        public void onEndOfMusic() {
            togglePlayPause.setChecked(false);
            log("Track finished");
        }

        @Override
        public void onStopTrack() {
            togglePlayPause.setChecked(false);
            log("Track stopped");
        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_extract_file_extraction_wrapper_thread:
                    loadTrackViaExtractionWrapperAskPermission();
                    break;

                case R.id.btn_extract_file_opensl_native_thread:
                    loadTrackOpenSlOrAskPermission();
                    break;

                case R.id.btn_extract_file_mediacodec_native_thread:
                    loadTrackMediaCodecOrAskPermission();
                    break;

                case R.id.btn_extract_file_ffmpeg_java_thread:
                    loadTrackFFmpegJavaThreadOrAskPermission();
                    break;

                case R.id.btn_extract_file_ffmpeg_native_thread:
                    loadTrackFFmpegNativeThreadOrAskPermission();
                    break;

                case R.id.btn_stop:
                    soundSystem.stopMusic();
                    break;
            }
        }
    };

    private void loadTrackViaExtractionWrapperAskPermission() {
        action = ACTION_LOAD_FILE_VIA_EXTRACTION_WRAPPER;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String folderPath = Environment.getExternalStorageDirectory() + "/pianomp3";
            File directory = new File(folderPath);
            if (!directory.exists()) {
                log("No folder pianomp3 folderPath==" + folderPath);
                return;
            }
            File[] files = directory.listFiles();
            List<String> filePaths = new ArrayList<>();
            for (File file : files) {
                String absolutePath = file.getAbsolutePath();
                if (absolutePath.toLowerCase().endsWith(".mp3")) {
                    filePaths.add(absolutePath);
                }
            }
            soundSystem.loadViaExtractionWrapper(filePaths.toArray(new String[filePaths.size()]));
        } else {
            askForReadExternalStoragePermission();
        }
    }

    private void loadTrackOpenSlOrAskPermission() {
        action = ACTION_LOAD_FILE_OPEN_SL;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String path = fileManager.getFile().getAbsolutePath();
            soundSystem.loadFileOpenSl(path);
        } else {
            askForReadExternalStoragePermission();
        }
    }

    private void loadTrackMediaCodecOrAskPermission() {
        action = ACTION_LOAD_FILE_MEDIA_CODEC;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String path = fileManager.getFile().getAbsolutePath();
            soundSystem.loadFileMediaCodec(path);
        } else {
            askForReadExternalStoragePermission();
        }
    }

    private void loadTrackFFmpegJavaThreadOrAskPermission() {
        action = ACTION_LOAD_FILE_FFMPEG_JAVA_THREAD;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String path = fileManager.getFile().getAbsolutePath();
            soundSystem.loadFileFFmpegJavaThread(path);
        } else {
            askForReadExternalStoragePermission();
        }
    }

    private void loadTrackFFmpegNativeThreadOrAskPermission() {
        action = ACTION_LOAD_FILE_FFMPEG_NATIVE_THREAD;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String path = fileManager.getFile().getAbsolutePath();
            soundSystem.loadFileFFmpegNativeThread(path);
        } else {
            askForReadExternalStoragePermission();
        }
    }

    private void askForReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.toggle_play_pause:
                    soundSystem.playMusic(isChecked);
                    break;
            }
        }
    };

    private FileManager.OnInitListener createFileManagerInitializeListener() {
        return new FileManager.OnInitListener() {
            @Override
            public void onInitEnded() {
                syncButtonsWithSoundSystem();
            }
        };
    }
}
