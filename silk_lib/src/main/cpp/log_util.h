//
// Created by 朱磊跃 on 2017/9/20.
//
#include <android/log.h>

#ifndef SILKDECODER_LOG_UTIL_H
#define SILKDECODER_LOG_UTIL_H

#define LOG_V(TAG, ...)    __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOG_D(TAG, ...)    __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOG_I(TAG, ...)    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOG_W(TAG, ...)    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOG_E(TAG, ...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#endif //SILKDECODER_LOG_UTIL_H
