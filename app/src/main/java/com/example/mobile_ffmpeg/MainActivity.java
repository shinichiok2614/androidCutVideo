package com.example.mobile_ffmpeg;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button button = new Button(this);
        button.setText("Cắt video");
        setContentView(button);

        button.setOnClickListener(v -> {
            if (checkPermissions()) {
                trimVideo();
            } else {
                requestPermissions();
            }
        });
    }

    private void trimVideo() {
        File inputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "input.mp4");
        File outputFile = new File(getExternalFilesDir(null), "output_trimmed.mp4"); // Thư mục riêng của app

        if (!inputFile.exists()) {
            Toast.makeText(this, "Không tìm thấy file input.mp4!", Toast.LENGTH_LONG).show();
            return;
        }

        String inputPath = inputFile.getAbsolutePath();
        String outputPath = outputFile.getAbsolutePath();

        int start = 5;   // giây bắt đầu
        int duration = 10; // thời lượng cần cắt

        String cmd = String.format("-y -i \"%s\" -ss %d -t %d -c copy \"%s\"",
                inputPath, start, duration, outputPath);

        Toast.makeText(this, "Đang xử lý video...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            int rc = FFmpeg.execute(cmd);
            runOnUiThread(() -> {
                if (rc == 0) {
                    Toast.makeText(this, "✅ Cắt video thành công!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ Thất bại khi cắt video!", Toast.LENGTH_LONG).show();
                    Log.e("FFmpeg", "Mã lỗi: " + rc);
                }
            });
        }).start();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                    REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            trimVideo();
        } else {
            Toast.makeText(this, "❗ Cần cấp quyền để đọc video!", Toast.LENGTH_LONG).show();
        }
    }
}
