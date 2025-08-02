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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int FRAME_TIME_MS = 500;
    final int JUMP_TIME_MS = 5000;

    private ExoPlayer player;
    private PlayerView playerView;
    ProgressBar progressBar;

    private SeekBar seekBarStart, seekBarEnd;
    private TextView textStart, textEnd, textCurrentTime;
    private Button btnCut, btnPick, btnBackFrame, btnForwardFrame;
    private Button btnRewind5s, btnForward5s;
    private Button btnEndRewind5s, btnEndForward5s, btnEndBackFrame, btnEndForwardFrame;

    private EditText editFileName;

    private Uri selectedVideoUri;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    private int videoDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        progressBar = findViewById(R.id.progressBar);

        editFileName = findViewById(R.id.editFileName);
        btnRewind5s = findViewById(R.id.btnRewind5s);
        btnForward5s = findViewById(R.id.btnForward5s);
        btnEndRewind5s = findViewById(R.id.btnEndRewind5s);
        btnEndForward5s = findViewById(R.id.btnEndForward5s);
        btnEndBackFrame = findViewById(R.id.btnEndBackFrame);
        btnEndForwardFrame = findViewById(R.id.btnEndForwardFrame);

        playerView = findViewById(R.id.playerView);

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

        btnBackFrame.setOnClickListener(v -> {
            int currentStart = seekBarStart.getProgress();
            int newStart = Math.max(currentStart - FRAME_TIME_MS, 0);
            seekBarStart.setProgress(newStart);
            player.seekTo(newStart);
            textStart.setText(formatTime(newStart));
        });

        btnForwardFrame.setOnClickListener(v -> {
            int currentStart = seekBarStart.getProgress();
            int maxEnd = seekBarEnd.getProgress();
            int newStart = Math.min(currentStart + FRAME_TIME_MS, maxEnd);
            seekBarStart.setProgress(newStart);
            player.seekTo(newStart);
            textStart.setText(formatTime(newStart));
        });

        btnRewind5s.setOnClickListener(v -> {
            int currentStart = seekBarStart.getProgress();
            int newStart = Math.max(currentStart - JUMP_TIME_MS, 0);
            seekBarStart.setProgress(newStart);
            player.seekTo(newStart);
            textStart.setText(formatTime(newStart));
        });

        btnForward5s.setOnClickListener(v -> {
            int currentStart = seekBarStart.getProgress();
            int maxEnd = seekBarEnd.getProgress();
            int newStart = Math.min(currentStart + JUMP_TIME_MS, maxEnd);
            seekBarStart.setProgress(newStart);
            player.seekTo(newStart);
            textStart.setText(formatTime(newStart));
        });

        btnEndRewind5s.setOnClickListener(v -> {
            int currentEnd = seekBarEnd.getProgress();
            int newEnd = Math.max(currentEnd - 5000, seekBarStart.getProgress());
            seekBarEnd.setProgress(newEnd);
            textEnd.setText(formatTime(newEnd));
        });

        btnEndForward5s.setOnClickListener(v -> {
            int currentEnd = seekBarEnd.getProgress();
            int newEnd = Math.min(currentEnd + 5000, videoDuration);
            seekBarEnd.setProgress(newEnd);
            textEnd.setText(formatTime(newEnd));
        });

        btnEndBackFrame.setOnClickListener(v -> {
            int currentEnd = seekBarEnd.getProgress();
            int newEnd = Math.max(currentEnd - 1000, seekBarStart.getProgress());
            seekBarEnd.setProgress(newEnd);
            textEnd.setText(formatTime(newEnd));
        });

        btnEndForwardFrame.setOnClickListener(v -> {
            int currentEnd = seekBarEnd.getProgress();
            int newEnd = Math.min(currentEnd + 1000, videoDuration);
            seekBarEnd.setProgress(newEnd);
            textEnd.setText(formatTime(newEnd));
        });

        seekBarStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override //giá trị của SeekBar thay đổi — dù là do người dùng kéo tay hay do code set
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.pause();
//                    textStart.setText(formatTime(seekBar.getProgress()));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress());
                player.pause();
                textCurrentTime.setText(formatTime(seekBar.getProgress()));
                textStart.setText(formatTime(seekBar.getProgress()));
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress());
                player.pause();
//                textCurrentTime.setText(formatTime(seekBar.getProgress()));
//                textStart.setText(formatTime(seekBar.getProgress()));
            }
        });

        seekBarEnd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.pause();
//                    textEnd.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                player.pause();
                player.seekTo(seekBar.getProgress());
                textEnd.setText(formatTime(seekBar.getProgress()));

            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress());
                player.pause();
                textCurrentTime.setText(formatTime(seekBar.getProgress()));
            }
        });

//        playerView.setOnTouchListener((v, event) -> {
//            if (!player.isPlaying()) {
//                player.play();
//            }
//            return true;
//        });
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
                    setupExoPlayer(selectedVideoUri);
                }
            }
    );

    private void setupExoPlayer(Uri uri) {
        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    videoDuration = (int) player.getDuration();
                    seekBarStart.setMax(videoDuration);
//                    seekBarStart.setProgress(0);
                    seekBarEnd.setMax(videoDuration);
//                    seekBarEnd.setProgress(videoDuration);
                    textStart.setText(formatTime(seekBarStart.getProgress()));
                    textEnd.setText(formatTime(seekBarEnd.getProgress()));
//                    textEnd.setText(formatTime(videoDuration));

                    updateSeekBar = new Runnable() {
                        @Override
                        public void run() {
                            if (player.isPlaying()) {
                                int current = (int) player.getCurrentPosition();
                                textCurrentTime.setText(formatTime(current));
                                handler.postDelayed(this, 500);
                            }
                        }
                    };
                    player.play();
                    handler.post(updateSeekBar);
                }
            }
        });
    }

    private void cutVideo(Uri uri, int startMs, int endMs) {
        File inputFile = saveUriToTempFile(uri);
        if (inputFile == null) return;

        String fileNameInput = editFileName.getText().toString().trim();
        String outputFileName = fileNameInput.isEmpty() ? "output_" + System.currentTimeMillis() + ".mp4"
                : fileNameInput + ".mp4";

        File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), outputFileName);
        String outputPath = outputFile.getAbsolutePath();

        String cmd = String.format(Locale.US,
                "-i %s -ss %.2f -to %.2f -c copy %s",
                inputFile.getAbsolutePath(),
                startMs / 1000.0,
                endMs / 1000.0,
                outputPath
        );

        // Hiển thị thanh tiến trình
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        });

        Config.enableStatisticsCallback(new StatisticsCallback() {
            @Override
            public void apply(Statistics newStatistics) {
                long time = newStatistics.getTime(); // milliseconds
                int progress = (int) ((time - startMs) * 100.0f / (endMs - startMs));
                progress = Math.max(0, Math.min(100, progress));

                int finalProgress = progress;
                runOnUiThread(() -> progressBar.setProgress(finalProgress));
            }
        });

        FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                progressBar.setProgress(0);
            });

            if (returnCode == 0) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Cắt video thành công: " + outputPath, Toast.LENGTH_LONG).show()
                );
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi khi cắt video", Toast.LENGTH_SHORT).show()
                );
            }

            // Dừng callback
            Config.enableStatisticsCallback(null);

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
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}
