package com.zly.media.silk;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by zhuleiyue on 2017/9/24.
 */

public class SilkDecoder {

    public static final String TAG = "SILK";

    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_INVALID_OPERATION = -3;

    public static final String HEADER = "#!SILK_V3";

    public static final int MAX_INPUT_FRAMES = 5;
    public static final int FRAME_LENGTH_MS = 20;
    public static final int MAX_API_FS_KHZ = 48;

    public static final int SAMPLE_RATE_8KHz = 8000;
    public static final int SAMPLE_RATE_12KHz = 12000;
    public static final int SAMPLE_RATE_16KHz = 16000;
    public static final int SAMPLE_RATE_24KHz = 24000;
    public static final int SAMPLE_RATE_32KHz = 32000;
    public static final int SAMPLE_RATE_44_1KHz = 44100;
    public static final int SAMPLE_RATE_48KHz = 48000;

    private short[] mOutputBuffer;

    static {
        System.loadLibrary("native-lib");
    }

    public static String getSilkVersion() {
        return nativeGetSilkVersion();
    }

    public static boolean isSilkHeader(String header) {
        return HEADER.equals(header);
    }

    public static int getDecoderSize() {
        return nativeGetDecoderSize();
    }

    public SilkDecoder(int sampleRateInHz, int decSizeBytes) {
        if (sampleRateInHz != SAMPLE_RATE_8KHz && sampleRateInHz != SAMPLE_RATE_12KHz
                && sampleRateInHz != SAMPLE_RATE_16KHz && sampleRateInHz != SAMPLE_RATE_24KHz
                && sampleRateInHz != SAMPLE_RATE_32KHz && sampleRateInHz != SAMPLE_RATE_44_1KHz
                && sampleRateInHz != SAMPLE_RATE_48KHz) {
            throw new IllegalArgumentException("SilkDecoder: sampleRateInHz must in 8000/12000/16000/24000/32000/44100/48000");
        }
        if (decSizeBytes <= 0) {
            throw new IllegalArgumentException("SilkDecoder: decSizeBytes is error");
        }
        mOutputBuffer = new short[((FRAME_LENGTH_MS * MAX_API_FS_KHZ) << 1) * MAX_INPUT_FRAMES];
        nativeInitDecoder(sampleRateInHz, decSizeBytes);
    }

    public short[] getOutputBuffer() {
        return mOutputBuffer;
    }

    public int decode(byte[] inputBuffer, int sizeInBytes) {
        if (sizeInBytes <= 0) {
            Log.e(TAG, "decode: sizeInBytes is invalid");
            return ERROR_BAD_VALUE;
        }
        return nativeDecode(inputBuffer, sizeInBytes, mOutputBuffer);
    }

    /**
     * 转码为 PCM 文件
     *
     * @param inputPath      输入文件路径
     * @param sampleRateInHz 采样率
     * @param outputPath     输出文件路径
     * @return
     */
    public static String transcode2PCM(String inputPath, int sampleRateInHz, String outputPath) {
        if (TextUtils.isEmpty(inputPath)) {
            throw new IllegalArgumentException("transcode2PCM: inputPath is null");
        }
        if (TextUtils.isEmpty(outputPath)) {
            throw new IllegalArgumentException("transcode2PCM: outputPath is null");
        }
        if (sampleRateInHz != SAMPLE_RATE_8KHz && sampleRateInHz != SAMPLE_RATE_12KHz
                && sampleRateInHz != SAMPLE_RATE_16KHz && sampleRateInHz != SAMPLE_RATE_24KHz
                && sampleRateInHz != SAMPLE_RATE_32KHz && sampleRateInHz != SAMPLE_RATE_44_1KHz
                && sampleRateInHz != SAMPLE_RATE_48KHz) {
            throw new IllegalArgumentException("transcode2PCM: sampleRateInHz must in 8000/12000/16000/24000/32000/44100/48000");
        }
        return nativeTranscode2PCM(inputPath, sampleRateInHz, outputPath);
    }

    private native static String nativeGetSilkVersion();

    private static native int nativeGetDecoderSize();

    private native int nativeInitDecoder(int sampleRateInHz, int decSizeBytes);

    private native int nativeDecode(byte[] inputBuffer, int decSizeBytes, short[] outputBuffer);

    private native static String nativeTranscode2PCM(String inputPath, int sampleRate, String outputPath);
}
