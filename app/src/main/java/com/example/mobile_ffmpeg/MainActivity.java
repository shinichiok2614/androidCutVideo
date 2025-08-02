package com.example.mobile_ffmpeg;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 200;
    private Uri selectedVideoUri;

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    trimVideoFromUri(selectedVideoUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button button = new Button(this);
        button.setText("Chọn video để cắt");
        setContentView(button);

        button.setOnClickListener(v -> {
            if (checkPermission()) {
                openVideoPicker();
            } else {
                requestPermissions();
            }
        });
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_PERMISSION);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(intent);
    }

    private void trimVideoFromUri(Uri videoUri) {
        try {
            // Copy file vào thư mục riêng của app
            File inputFile = new File(getExternalFilesDir(null), "input.mp4");
            File outputFile = new File(getExternalFilesDir(null), "output_trimmed.mp4");

            ContentResolver resolver = getContentResolver();
            InputStream inputStream = resolver.openInputStream(videoUri);
            OutputStream outputStream = new FileOutputStream(inputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            String inputPath = inputFile.getAbsolutePath();
            String outputPath = outputFile.getAbsolutePath();

            int start = 5;
            int duration = 10;

            String cmd = String.format("-y -i \"%s\" -ss %d -t %d -c copy \"%s\"",
                    inputPath, start, duration, outputPath);

            Toast.makeText(this, "Đang cắt video...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                int rc = FFmpeg.execute(cmd);
                runOnUiThread(() -> {
                    if (rc == 0) {
                        Toast.makeText(this, "🎉 Cắt thành công: " + outputPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ Lỗi cắt video!", Toast.LENGTH_LONG).show();
                        Log.e("FFmpeg", "Return code: " + rc);
                    }
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý video!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openVideoPicker();
        } else {
            Toast.makeText(this, "Cần cấp quyền để chọn video!", Toast.LENGTH_LONG).show();
        }
    }
}
