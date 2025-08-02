package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.MediaMetadataRetriever;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private VideoView videoView;
    private SeekBar seekBarStart, seekBarEnd;
    private TextView textStart, textEnd, textCurrentTime;
    private Button btnCut, btnPick, btnBackFrame, btnForwardFrame;

    private Uri selectedVideoUri;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    private int videoDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();

        videoView = findViewById(R.id.videoView);
        seekBarStart = findViewById(R.id.seekBarStart);
        seekBarEnd = findViewById(R.id.seekBarEnd);
        textStart = findViewById(R.id.textStart);
        textEnd = findViewById(R.id.textEnd);
        textCurrentTime = findViewById(R.id.text_current_time);
        btnCut = findViewById(R.id.btnCut);
        btnPick = findViewById(R.id.btnPick);
        btnBackFrame = findViewById(R.id.btnBackFrame);
        btnForwardFrame = findViewById(R.id.btnForwardFrame);

        btnPick.setOnClickListener(v -> openVideoPicker());

        btnCut.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                int startMs = seekBarStart.getProgress();
                int endMs = seekBarEnd.getProgress();
                if (endMs > startMs) {
                    cutVideo(selectedVideoUri, startMs, endMs);
                } else {
                    Toast.makeText(this, "Thời gian kết thúc phải lớn hơn thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBackFrame.setOnClickListener(v -> seekByFrame(-500));
        btnForwardFrame.setOnClickListener(v -> seekByFrame(500));

        seekBarStart.setOnSeekBarChangeListener(createSeekBarListener(textStart));
        seekBarEnd.setOnSeekBarChangeListener(createSeekBarListener(textEnd));
    }

    private void seekByFrame(int deltaMs) {
        int newPosition = videoView.getCurrentPosition() + deltaMs;
        newPosition = Math.max(0, Math.min(newPosition, videoDuration));
        videoView.pause();
        videoView.seekTo(newPosition);
        textCurrentTime.setText(formatTime(newPosition));
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(TextView timeView) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.pause();
                    videoView.seekTo(progress);
                    timeView.setText(formatTime(progress));
                    textCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                videoView.pause();
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                videoView.seekTo(seekBar.getProgress());
                videoView.start();
                handler.post(updateSeekBar);
            }
        };
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        pickVideoLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    prepareVideo(selectedVideoUri);
                }
            }
    );

    private void prepareVideo(Uri uri) {
        File file = saveUriToTempFile(uri);
        if (file == null) return;

        videoView.setVideoURI(Uri.fromFile(file));
        videoView.setOnPreparedListener(mp -> {
            videoDuration = videoView.getDuration();
            seekBarStart.setMax(videoDuration);
            seekBarEnd.setMax(videoDuration);
            seekBarEnd.setProgress(videoDuration);

            textStart.setText("00:00");
            textEnd.setText(formatTime(videoDuration));

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (videoView.isPlaying()) {
                        int current = videoView.getCurrentPosition();
                        textCurrentTime.setText(formatTime(current));
                        handler.postDelayed(this, 500);
                    }
                }
            };

            videoView.start();
            handler.post(updateSeekBar);
        });
    }

    private File saveUriToTempFile(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            String fileName = getFileName(uri);
            File tempFile = new File(getCacheDir(), fileName);
            InputStream input = resolver.openInputStream(uri);
            FileOutputStream output = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            input.close();
            output.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi đọc file", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = "video.mp4";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        return result;
    }

    private void cutVideo(Uri uri, int startMs, int endMs) {
        File inputFile = saveUriToTempFile(uri);
        if (inputFile == null) return;

        String outputPath = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "output_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();

        String cmd = String.format(Locale.US,
                "-i %s -ss %.2f -to %.2f -c copy %s",
                inputFile.getAbsolutePath(),
                startMs / 1000.0,
                endMs / 1000.0,
                outputPath
        );

        FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
            if (returnCode == 0) {
                runOnUiThread(() -> Toast.makeText(this, "Cắt video thành công: " + outputPath, Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi khi cắt video", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String formatTime(int millis) {
        int sec = millis / 1000;
        int min = sec / 60;
        sec %= 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSIONS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateSeekBar);
    }
}
