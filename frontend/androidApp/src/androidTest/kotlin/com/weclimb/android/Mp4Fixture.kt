package com.weclimb.android

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

object Mp4Fixture {
    fun create(output: File): File {
        output.delete()
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 10)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo = MediaCodec.BufferInfo()
        var track = -1
        var muxerStarted = false
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            repeat(20) { frame ->
                val inputIndex = encoder.dequeueInputBuffer(5_000)
                if (inputIndex >= 0) {
                    val input = requireNotNull(encoder.getInputBuffer(inputIndex))
                    input.clear()
                    input.put(frameData(frame))
                    encoder.queueInputBuffer(inputIndex, 0, input.position(), frame * 100_000L, 0)
                }
                drain(encoder, muxer, bufferInfo) { outputFormat ->
                    track = muxer.addTrack(outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
            val inputIndex = encoder.dequeueInputBuffer(5_000)
            if (inputIndex >= 0) {
                encoder.queueInputBuffer(inputIndex, 0, 0, 2_000_000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            while (true) {
                val result = encoder.dequeueOutputBuffer(bufferInfo, 5_000)
                if (result >= 0) {
                    write(encoder, muxer, track, bufferInfo, result)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    track = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        } finally {
            encoder.stop()
            encoder.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
        }
        return output
    }

    private fun drain(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        onFormatChanged: (MediaFormat) -> Unit,
    ) {
        while (true) {
            when (val result = encoder.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onFormatChanged(encoder.outputFormat)
                else -> if (result >= 0) write(encoder, muxer, 0, bufferInfo, result)
            }
        }
    }

    private fun write(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        track: Int,
        bufferInfo: MediaCodec.BufferInfo,
        outputIndex: Int,
    ) {
        val output = requireNotNull(encoder.getOutputBuffer(outputIndex))
        if (bufferInfo.size > 0 && track >= 0 && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
            output.position(bufferInfo.offset)
            output.limit(bufferInfo.offset + bufferInfo.size)
            muxer.writeSampleData(track, output, bufferInfo)
        }
        encoder.releaseOutputBuffer(outputIndex, false)
    }

    private fun frameData(frame: Int): ByteArray {
        val lumaSize = 320 * 240
        return ByteArray(lumaSize * 3 / 2).apply {
            java.util.Arrays.fill(this, 0, lumaSize, (16 + frame * 10).toByte())
            java.util.Arrays.fill(this, lumaSize, size, 128.toByte())
        }
    }
}
