package com.example.mobile_ffmpeg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 123;
    private Uri selectedVideoUri;
    private File inputFile, outputFile;
    private VideoView videoView;

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    playSelectedVideo(selectedVideoUri);
                    copyVideoToInputFile(selectedVideoUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPick = findViewById(R.id.btnPick);
        Button btnTrim = findViewById(R.id.btnTrim);
        Button btnPlayOutput = findViewById(R.id.btnPlayOutput);
        videoView = findViewById(R.id.videoView);

        inputFile = new File(getExternalFilesDir(null), "input.mp4");
        outputFile = new File(getExternalFilesDir(null), "output_trimmed.mp4");

        btnPick.setOnClickListener(v -> {
            if (checkPermission()) {
                openVideoPicker();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_PERMISSION);
            }
        });

        btnTrim.setOnClickListener(v -> {
            if (selectedVideoUri != null && inputFile.exists()) {
                trimVideo();
            } else {
                Toast.makeText(this, "Vui lòng chọn video trước!", Toast.LENGTH_SHORT).show();
            }
        });

        btnPlayOutput.setOnClickListener(v -> {
            if (outputFile.exists()) {
                videoView.setVideoURI(Uri.fromFile(outputFile));
                videoView.start();
            } else {
                Toast.makeText(this, "Chưa có video đã cắt!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(intent);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void playSelectedVideo(Uri uri) {
        videoView.setVideoURI(uri);
        videoView.start();
    }

    private void copyVideoToInputFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(inputFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi sao chép video!", Toast.LENGTH_SHORT).show();
        }
    }

    private void trimVideo() {
        String cmd = String.format("-y -i \"%s\" -ss %d -t %d -c copy \"%s\"",
                inputFile.getAbsolutePath(), 5, 10, outputFile.getAbsolutePath());

        Toast.makeText(this, "Đang cắt video...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            int rc = FFmpeg.execute(cmd);
            runOnUiThread(() -> {
                if (rc == 0) {
                    Toast.makeText(this, "🎉 Cắt video thành công!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ Lỗi cắt video!", Toast.LENGTH_LONG).show();
                    Log.e("FFmpeg", "Return code: " + rc);
                }
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openVideoPicker();
        } else {
            Toast.makeText(this, "Cần cấp quyền để chọn video!", Toast.LENGTH_SHORT).show();
        }
    }
}
