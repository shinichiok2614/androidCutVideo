package com.example.mobile_ffmpeg;


import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_VIDEO = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 200;

    private VideoView videoView;
    private SeekBar startSeekBar, endSeekBar;
    private Button pickButton, cutButton;
    private TextView startTimeView, endTimeView;

    private Uri selectedVideoUri;
    private File selectedVideoFile;
    private int videoDurationMs = 0;
    private int startMs = 0, endMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        startSeekBar = findViewById(R.id.startSeekBar);
        endSeekBar = findViewById(R.id.endSeekBar);
        pickButton = findViewById(R.id.pickButton);
        cutButton = findViewById(R.id.cutButton);
        startTimeView = findViewById(R.id.startTime);
        endTimeView = findViewById(R.id.endTime);

        pickButton.setOnClickListener(v -> openVideoPicker());

        cutButton.setOnClickListener(v -> cutVideo());

        setupSeekBars();

        requestPermissions();
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        videoPickerLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    if (selectedVideoUri != null) {
                        selectedVideoFile = copyVideoToCache(selectedVideoUri);
                        videoView.setVideoPath(selectedVideoFile.getAbsolutePath());
                        videoView.setOnPreparedListener(mp -> {
                            videoDurationMs = videoView.getDuration();
                            endMs = videoDurationMs;

                            startSeekBar.setMax(videoDurationMs);
                            endSeekBar.setMax(videoDurationMs);

                            endSeekBar.setProgress(videoDurationMs);
                            updateTimeViews();

                            videoView.start();
                        });
                    }
                }
            });

    private File copyVideoToCache(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            String fileName = getFileName(uri);
            File outputFile = new File(getCacheDir(), fileName);

            InputStream inputStream = resolver.openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi sao chép video!", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = "video.mp4";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        }
        return result;
    }

    private void setupSeekBars() {
        startSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                fromUser = b;
                if (fromUser) {
                    videoView.pause();
                    videoView.seekTo(progress);
                    startMs = progress;
                    updateTimeViews();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                videoView.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Không phát lại tự động
            }
        });

        endSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                fromUser = b;
                if (fromUser) {
                    videoView.pause();
                    videoView.seekTo(progress);
                    endMs = progress;
                    updateTimeViews();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                videoView.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateTimeViews() {
        startTimeView.setText("Bắt đầu: " + formatTime(startMs));
        endTimeView.setText("Kết thúc: " + formatTime(endMs));
    }

    private String formatTime(int ms) {
        int sec = ms / 1000;
        int min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void cutVideo() {
        if (selectedVideoFile == null) return;

        String inputPath = selectedVideoFile.getAbsolutePath();
        File outputDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Output");
        if (!outputDir.exists()) outputDir.mkdirs();

        String outputPath = new File(outputDir, "cut_output.mp4").getAbsolutePath();

        float startSec = startMs / 1000f;
        float durationSec = (endMs - startMs) / 1000f;

        String[] cmd = {
                "-ss", String.valueOf(startSec),
                "-i", inputPath,
                "-t", String.valueOf(durationSec),
                "-c", "copy",
                outputPath
        };

        FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                runOnUiThread(() -> Toast.makeText(this, "Cắt video thành công!\n" + outputPath, Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi cắt video!", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_CODE_PERMISSIONS);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        }
    }
}
