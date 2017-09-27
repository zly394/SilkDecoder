//
// Created by 朱磊跃 on 2017/9/24.
//
#include <jni.h>
#include <SKP_Silk_SDK_API.h>
#include <malloc.h>
#include <sys/time.h>
#include "log_util.h"

#define TAG "SILK"
#define ERROR_BAD_VALUE -2
#define ERROR_INVALID_OPERATION -3

#define MAX_BYTES_PER_FRAME     1024
#define MAX_INPUT_FRAMES        5
#define FRAME_LENGTH_MS         20
#define MAX_API_FS_KHZ          48

unsigned long GetHighResolutionTime() /* O: time in usec*/
{
    struct timeval tv;
    gettimeofday(&tv, 0);
    return (unsigned long) ((tv.tv_sec * 1000000) + (tv.tv_usec));
}

SKP_SILK_SDK_DecControlStruct decControl;
void *decState;


JNIEXPORT jstring JNICALL
Java_com_zly_media_silk_SilkDecoder_nativeGetSilkVersion(JNIEnv *env, jobject instance) {

    const char *hello = SKP_Silk_SDK_get_version();

    return (*env)->NewStringUTF(env, hello);
}

JNIEXPORT jint JNICALL
Java_com_zly_media_silk_SilkDecoder_nativeGetDecoderSize(JNIEnv *env, jclass type) {

    SKP_int32 ret, decSizeBytes;

    ret = SKP_Silk_SDK_Get_Decoder_Size(&decSizeBytes);

    if (ret) {
        LOG_E(TAG, "SKP_Silk_SDK_Get_Decoder_Size returned %d", ret);
        return ERROR_BAD_VALUE;
    } else {
        return decSizeBytes;
    }
}

JNIEXPORT jint JNICALL
Java_com_zly_media_silk_SilkDecoder_nativeInitDecoder(JNIEnv *env, jobject instance,
                                                      jint sampleRateInHz, jint decSizeBytes) {

    SKP_int32 ret;

    if (sampleRateInHz == 0) {
        decControl.API_sampleRate = 24000;
    } else {
        decControl.API_sampleRate = sampleRateInHz;
    }

    decState = malloc((size_t) decSizeBytes);

    ret = SKP_Silk_SDK_InitDecoder(decState);

    if (ret) {
        LOG_E(TAG, "SKP_Silk_SDK_InitDecoder returned %d", ret);
        return ERROR_BAD_VALUE;
    } else {
        return ret;
    }
}

JNIEXPORT jint JNICALL
Java_com_zly_media_silk_SilkDecoder_nativeDecode(JNIEnv *env, jobject instance,
                                                 jbyteArray inputBuffer_, jint decSizeBytes,
                                                 jshortArray outputBuffer_) {
    jbyte *inputBuffer = (*env)->GetByteArrayElements(env, inputBuffer_, NULL);
    jshort *outputBuffer = (*env)->GetShortArrayElements(env, outputBuffer_, NULL);

    SKP_uint8 *inPtr = (unsigned char *) inputBuffer;
    SKP_int16 len, *outPtr = outputBuffer;
    SKP_int32 ret, tot_len, frames;

    tot_len = 0;
    frames = 0;

    do {
        ret = SKP_Silk_SDK_Decode(decState, &decControl, 0, inPtr, decSizeBytes, outPtr, &len);
        if (ret) {
            LOG_E(TAG, "SKP_Silk_SDK_Decode returned %d", ret);
            return ERROR_INVALID_OPERATION;
        }

        frames++;
        outPtr += len;
        tot_len += len;
        if (frames > MAX_INPUT_FRAMES) {
            outPtr = outputBuffer;
            tot_len = 0;
            frames = 0;
        }
    } while (decControl.moreInternalDecoderFrames);

    (*env)->ReleaseByteArrayElements(env, inputBuffer_, inputBuffer, 0);
    (*env)->ReleaseShortArrayElements(env, outputBuffer_, outputBuffer, 0);

    return tot_len;
}

JNIEXPORT jstring JNICALL
Java_com_zly_media_silk_SilkDecoder_nativeTranscode2PCM(JNIEnv *env, jclass type,
                                                        jstring inputPath_, jint sampleRate,
                                                        jstring outputPath_) {
    const char *inputPath = (*env)->GetStringUTFChars(env, inputPath_, 0);
    const char *outputPath = (*env)->GetStringUTFChars(env, outputPath_, 0);

    unsigned long totTime, startTime;
    double fileLength;
    size_t counter;
    SKP_int32 ret, tot_len, totPackets;
    SKP_int32 decSizeBytes, frames, packetSize_ms = 0;
    SKP_int16 nBytes, len;
    SKP_uint8 payload[MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES], *payloadToDec = NULL;
    SKP_int16 out[((FRAME_LENGTH_MS * MAX_API_FS_KHZ) << 1) * MAX_INPUT_FRAMES], *outPtr;
    void *psDec;
    FILE *inFile, *outFile;
    SKP_SILK_SDK_DecControlStruct DecControl;

    LOG_I(TAG, "********** Silk Decoder (Fixed Point) v %s ********************",
          SKP_Silk_SDK_get_version());
    LOG_I(TAG, "********** Compiled for %d bit cpu *******************************",
          (int) sizeof(void *) * 8);
    LOG_I(TAG, "Input:                       %s", inputPath);
    LOG_I(TAG, "Output:                      %s", outputPath);

    // 打开输入文件
    inFile = fopen(inputPath, "rb");
    if (inFile == NULL) {
        LOG_E(TAG, "Error: could not open input file %s", inputPath);
        return NULL;
    }

    // 验证文件头
    {
        char header_buf[50];
        fread(header_buf, sizeof(char), strlen("#!SILK_V3"), inFile);
        header_buf[strlen("#!SILK_V3")] = '\0';
        if (strcmp(header_buf, "#!SILK_V3") != 0) {
            LOG_E(TAG, "Error: Wrong Header %s", header_buf);
            return NULL;
        }
        LOG_I(TAG, "Header is \"%s\"", header_buf);
    }

    // 打开输出文件
    outFile = fopen(outputPath, "wb");
    if (outFile == NULL) {
        LOG_E(TAG, "Error: could not open output file %s", outputPath);
        return NULL;
    }

    // 设置采样率
    if (sampleRate == 0) {
        DecControl.API_sampleRate = 24000;
    } else {
        DecControl.API_sampleRate = sampleRate;
    }

    // 获取 Silk 解码器状态的字节大小
    ret = SKP_Silk_SDK_Get_Decoder_Size(&decSizeBytes);
    if (ret) {
        LOG_E(TAG, "SKP_Silk_SDK_Get_Decoder_Size returned %d", ret);
    }

    psDec = malloc((size_t) decSizeBytes);

    // 初始化或充值解码器
    ret = SKP_Silk_SDK_InitDecoder(psDec);
    if (ret) {
        LOG_E(TAG, "SKP_Silk_SDK_InitDecoder returned %d", ret);
    }

    totPackets = 0;
    totTime = 0;

    while (1) {
        // 读取有效数据大小
        counter = fread(&nBytes, sizeof(SKP_int16), 1, inFile);
        if (nBytes < 0 || counter < 1) {
            break;
        }
        // 读取有效数据
        counter = fread(payload, sizeof(SKP_uint8), (size_t) nBytes, inFile);
        if ((SKP_int16) counter < nBytes) {
            break;
        }

        payloadToDec = payload;

        outPtr = out;
        tot_len = 0;
        startTime = GetHighResolutionTime();

        frames = 0;
        do {
            // 解码
            ret = SKP_Silk_SDK_Decode(psDec, &DecControl, 0, payloadToDec, nBytes, outPtr, &len);
            if (ret) {
                LOG_E(TAG, "SKP_Silk_SDK_Decode returned %d", ret);
                break;
            }
            frames++;
            outPtr += len;
            tot_len += len;
            if (frames > MAX_INPUT_FRAMES) {
                outPtr = out;
                tot_len = 0;
                frames = 0;
            }
        } while (DecControl.moreInternalDecoderFrames);

        packetSize_ms = tot_len / (DecControl.API_sampleRate / 1000);
        totTime += GetHighResolutionTime() - startTime;
        totPackets++;
        // 将解码后的数据保存到文件
        fwrite(out, sizeof(SKP_int16), (size_t) tot_len, outFile);
    }

    LOG_I(TAG, "Packets decoded:             %d", totPackets);
    LOG_I(TAG, "Decoding Finished");

    free(psDec);

    fclose(outFile);
    fclose(inFile);

    fileLength = totPackets * 1e-3 * packetSize_ms;

    LOG_I(TAG, "File length:                 %.3f s", fileLength);
    LOG_I(TAG, "Time for decoding:           %.3f s (%.3f%% of realTime)", 1e-6 * totTime,
          1e-4 * totTime / fileLength);

    (*env)->ReleaseStringUTFChars(env, inputPath_, inputPath);
    (*env)->ReleaseStringUTFChars(env, outputPath_, outputPath);

    return (*env)->NewStringUTF(env, outputPath);
}