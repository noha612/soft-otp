package com.example.softotp;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.softotp.scan.ScanActivity;

public class MainActivity extends Activity {
    public static final String ACTION_IMAGE_SAVED = "ACTION_IMAGE_SAVED";
    private final int PERMISSIONS_REQUEST_CAMERA = 1;
    private Handler handler = new Handler();

    ImageView scanQR;
    TextView textCode;
    ProgressBar progressBar;
    TextView timer;
//dm
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanQR = findViewById(R.id.scanQR);
        scanQR.setOnClickListener(v -> {
            tryOpenCamera();
        });
        textCode = findViewById(R.id.token_code);
        progressBar = findViewById(R.id.progressBar);
        timer = findViewById(R.id.timer);
    }

    private void tryOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            } else {
                openCamera();
            }
        }
    }

    private void openCamera() {
        startActivityForResult(new Intent(this, ScanActivity.class), 101);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    private void doStartProgressBar(Token token) {

        final int MAX = token.getPeriod() * 1000;
        progressBar.setMax(MAX);

        Thread thread = new Thread(() -> {
            while (true) {
                TokenCode tokenCode = token.generateCodes();
                String code = tokenCode.getCurrentCode();

                StringBuilder temp = new StringBuilder();
                for (char i : code.toCharArray()) {
                    temp.append(i).append(" ");
                }
                textCode.setText(temp.toString().trim());

                for (int i = tokenCode.getTotalProgress() - tokenCode.getCurrentProgress(); i < MAX; i++) {
                    final int progress = i + 1;
                    SystemClock.sleep(1);

                    handler.post(() -> {
                        progressBar.setProgress(progress);
                        int sec = (MAX - progress) / 1000;
                        String secString = sec / 10 + " " + sec % 10;
                        timer.setText(secString);
                    });
                }
            }
        });
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(MainActivity.this, R.string.error_permission_camera_open, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == 0) {
            Token token = (Token) data.getSerializableExtra("Token");
            doStartProgressBar(token);
        }
    }
}