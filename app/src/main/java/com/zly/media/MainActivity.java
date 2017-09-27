package com.zly.media;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zly.media.utils.SilkUtil;

import java.io.File;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "SILK";
    public static final int REQUEST_FILE = 1;

    private TextInputEditText etInputPath;
    private TextView tvOutputPath;
    private ProgressBar progressBar;
    private RadioGroup radioGroup1, radioGroup2;
    private int mSampleRate = 24000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etInputPath = (TextInputEditText) findViewById(R.id.etInputPath);
        tvOutputPath = (TextView) findViewById(R.id.tvOutputPath);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        radioGroup1 = (RadioGroup) findViewById(R.id.radioGroup1);
        radioGroup2 = (RadioGroup) findViewById(R.id.radioGroup2);

        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                RadioButton radioButton = (RadioButton) radioGroup.findViewById(id);
                if (id != -1 && radioButton.isChecked()) {
                    radioGroup2.clearCheck();
                }
                switch (id) {
                    case R.id.rb8000Hz:
                        mSampleRate = 8000;
                        break;
                    case R.id.rb12000Hz:
                        mSampleRate = 12000;
                        break;
                    case R.id.rb16000Hz:
                        mSampleRate = 16000;
                        break;
                    case R.id.rb24000Hz:
                        mSampleRate = 24000;
                        break;
                }
            }
        });

        radioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                RadioButton radioButton = (RadioButton) radioGroup.findViewById(id);
                if (id != -1 && radioButton.isChecked()) {
                    radioGroup1.clearCheck();
                }
                switch (id) {
                    case R.id.rb32000Hz:
                        mSampleRate = 32000;
                        break;
                    case R.id.rb44100Hz:
                        mSampleRate = 44100;
                        break;
                    case R.id.rb48000Hz:
                        mSampleRate = 48000;
                        break;
                }
            }
        });
    }

    public void selectInput(View view) {
        new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, REQUEST_FILE);
                        }
                    }
                });

    }

    public void transcode2PCM(View view) {
        String inputPath = etInputPath.getText().toString();
        if (TextUtils.isEmpty(inputPath)) {
            Log.e(TAG, "transcode2PCM: inputPath is null");
        } else {
            File outDir = new File("/storage/emulated/0/silkOutput");
            if (!outDir.exists()) {
                outDir.mkdir();
            }
            File outFile = new File(outDir, "silk_out_" + System.currentTimeMillis() + ".pcm");

            SilkUtil.transcode2PCMAsync(inputPath, mSampleRate, outFile.getAbsolutePath(), new SilkUtil.DecodeListener() {
                @Override
                public void onStart() {
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onProgress(int progress) {

                }

                @Override
                public void onEnd(String result) {
                    progressBar.setVisibility(View.GONE);
                    tvOutputPath.setText(result);
                }
            });
        }
    }

    public void play(View view) {
        String inputPath = etInputPath.getText().toString();
        if (TextUtils.isEmpty(inputPath)) {
            Log.e(TAG, "transcode2PCM: inputPath is null");
        } else {
            SilkUtil.play(inputPath, mSampleRate, null);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_FILE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    etInputPath.setText(uri.getPath());
                }
                break;
        }
    }
}
