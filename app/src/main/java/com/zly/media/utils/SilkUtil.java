package com.zly.media.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.zly.media.silk.SilkDecoder;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by zhuleiyue on 2017/9/21.
 */

public class SilkUtil {

    public interface DecodeListener {
        void onStart();

        void onProgress(int progress);

        void onEnd(String result);
    }

    public static void transcode2PCMAsync(final String inputPath, final int sampleRate, final String outputPath, final DecodeListener listener) {
        new AsyncTask<Void, Integer, String>() {

            @Override
            protected void onPreExecute() {
                if (listener != null) {
                    listener.onStart();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                return SilkDecoder.transcode2PCM(inputPath, sampleRate, outputPath);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                if (listener != null) {
                    listener.onProgress(values[0]);
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (listener != null) {
                    listener.onEnd(s);
                }
            }
        }.execute();
    }

    public static void play(final String inputPath, final int sampleRate, final DecodeListener listener) {
        new AsyncTask<Void, Integer, String>() {
            private AudioTrack mAudioTrack;
            private int mBufferSizeInBytes;

            @Override
            protected void onPreExecute() {
                if (listener != null) {
                    listener.onStart();
                }
                if (mAudioTrack == null || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                    mBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    if (mBufferSizeInBytes == AudioTrack.ERROR_BAD_VALUE) {
                        throw new IllegalArgumentException("getMinBufferSize() is error");
                    }
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSizeInBytes, AudioTrack.MODE_STREAM);
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                int decoderSizeBytes = SilkDecoder.getDecoderSize();
                Log.d("SILK", "doInBackground: decoderSizeBytes = " + decoderSizeBytes);
                SilkDecoder silkDecoder = new SilkDecoder(sampleRate, decoderSizeBytes);
                File inFile = new File(inputPath);
                int ret;
                byte[] silkData = new byte[1024];
                try {
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(inFile)));
                    byte[] header = new byte[SilkDecoder.HEADER.length()];
                    dis.read(header);
                    if (!SilkDecoder.isSilkHeader(new String(header))) {
                        return null;
                    }
                    mAudioTrack.play();
                    while (dis.available() > 0) {
                        short byteSize = Short.reverseBytes(dis.readShort());

                        Log.d("SILK", "doInBackground: byteSize = " + byteSize);

                        if (byteSize <= 0) {
                            break;
                        }

                        ret = dis.read(silkData, 0, byteSize);

                        Log.d("SILK", "doInBackground: ret = " + ret);

                        if (ret < byteSize) {
                            break;
                        }

                        int len = silkDecoder.decode(silkData, byteSize);

                        Log.d("SILK", "doInBackground: len = " + len);

                        if (len < 0) {
                            break;
                        }

                        mAudioTrack.write(silkDecoder.getOutputBuffer(), 0, len);
                    }
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                if (listener != null) {
                    listener.onProgress(values[0]);
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (listener != null) {
                    listener.onEnd(s);
                }
                mAudioTrack.release();
            }
        }.execute();
    }
}
