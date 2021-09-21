/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 * Authors: Siemens AG <max.wittig@siemens.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 * Copyright (C) 2017  Max Wittig, Siemens AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.softotp.scan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.softotp.R;
import com.example.softotp.Token;
import com.example.softotp.TokenPersistence;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.selector.FocusModeSelectors;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.back;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

public class ScanActivity extends Activity {
    private Fotoapparat fotoapparat;
    private static ScanBroadcastReceiver receiver;

    public class ScanBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "ACTION_CODE_SCANNED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("scanResult");
            addTokenAndFinish(text);
        }
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void addTokenAndFinish(String text) {
        Token token = null;
        try {
            token = new Token(text);
        } catch (Token.TokenUriInvalidException e) {
            e.printStackTrace();
        }

        this.unregisterReceiver(receiver);
        setResult(0,new Intent().putExtra("Token",token));
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new ScanBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ScanBroadcastReceiver.ACTION));
        setContentView(R.layout.scan);
        CameraView cameraView = findViewById(R.id.camera_view);

        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(back())
                .focusMode(firstAvailable(
                        FocusModeSelectors.continuousFocus(),
                        autoFocus(),
                        fixed()
                ))
                .frameProcessor(new ScanFrameProcessor(this))
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fotoapparat.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();
    }
}
