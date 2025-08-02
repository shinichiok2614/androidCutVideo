package com.example.mobile_ffmpeg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VIDEO_PICK = 1;
    private static final int REQUEST_PERMISSIONS = 100;

    private VideoView videoView;
    private Button pickVideoBtn, cutBtn;
    private SeekBar startSeekBar, endSeekBar;
    private TextView startText, endText;

    private Uri selectedVideoUri;
    private int videoDuration = 0;

    private int startMs = 0;
    private int endMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // đảm bảo bạn tạo layout như hướng dẫn bên dưới

        videoView = findViewById(R.id.videoView);
        pickVideoBtn = findViewById(R.id.pickVideoBtn);
        cutBtn = findViewById(R.id.cutBtn);
        startSeekBar = findViewById(R.id.startSeekBar);
        endSeekBar = findViewById(R.id.endSeekBar);
        startText = findViewById(R.id.startText);
        endText = findViewById(R.id.endText);

        pickVideoBtn.setOnClickListener(v -> checkPermissionAndPickVideo());

        cutBtn.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                trimVideo();
            } else {
                Toast.makeText(this, "Chọn video trước!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionAndPickVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        REQUEST_PERMISSIONS);
                return;
            }
        }
        pickVideo();
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.getData();
            videoView.setVideoURI(selectedVideoUri);
            videoView.start();

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, selectedVideoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoDuration = Integer.parseInt(durationStr);
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            startSeekBar.setMax(videoDuration);
            endSeekBar.setMax(videoDuration);
            endSeekBar.setProgress(videoDuration);
            endMs = videoDuration;

            startSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    startMs = progress;
                    startText.setText("Start: " + (progress / 1000) + "s");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            endSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    endMs = progress;
                    endText.setText("End: " + (progress / 1000) + "s");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void trimVideo() {
        try {
            // copy input uri to file
            File inputFile = createTempFileFromUri(selectedVideoUri);
            File outputFile = new File(getExternalFilesDir(null), "trimmed_output.mp4");

            int duration = endMs - startMs;
            if (duration <= 0) {
                Toast.makeText(this, "Thời gian không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }

            String cmd = "-y -i " + inputFile.getAbsolutePath()
                    + " -ss " + (startMs / 1000)
                    + " -t " + (duration / 1000)
                    + " -c copy " + outputFile.getAbsolutePath();

            Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

            FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
                if (returnCode == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Đã lưu: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show()
                    );
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Lỗi cắt video!", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        String fileName = "temp_input.mp4";
        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
        if (returnCursor != null && returnCursor.moveToFirst()) {
            fileName = returnCursor.getString(returnCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            returnCursor.close();
        }

        File file = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        }
        return file;
    }
}
