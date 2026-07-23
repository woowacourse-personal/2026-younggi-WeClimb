package com.weclimb.android

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val executor = ContextCompat.getMainExecutor(context)
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    val isRecording: Boolean
        get() = recording != null

    fun bind(onReady: () -> Unit, onError: (String) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                runCatching {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    val capture = VideoCapture.withOutput(recorder)
                    providerFuture.get().apply {
                        unbindAll()
                        bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, capture)
                    }
                    videoCapture = capture
                }.onSuccess {
                    onReady()
                }.onFailure {
                    onError("카메라를 준비하지 못했습니다")
                }
            },
            executor,
        )
    }

    fun start(output: File, onFinalized: (File) -> Unit, onError: (String) -> Unit): Boolean {
        val capture = videoCapture
        if (capture == null) {
            onError("카메라가 아직 준비되지 않았습니다")
            return false
        }
        recording = CameraRecordingStartGuard().start(
            start = {
                capture.output
                    .prepareRecording(context, FileOutputOptions.Builder(output).build())
                    .withAudioEnabled()
                    .start(executor) { event -> handleEvent(event, output, onFinalized, onError) }
            },
            onError = {
                output.delete()
                onError(it)
            },
        )
        return recording != null
    }

    fun stop() {
        recording?.stop()
        recording = null
    }

    fun release() {
        stop()
        ProcessCameraProvider.getInstance(context).addListener(
            { ProcessCameraProvider.getInstance(context).get().unbindAll() },
            executor,
        )
        videoCapture = null
    }

    private fun handleEvent(
        event: VideoRecordEvent,
        output: File,
        onFinalized: (File) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (event is VideoRecordEvent.Finalize) {
            recording = null
            if (event.hasError()) {
                output.delete()
                onError("녹화를 저장하지 못했습니다")
            } else {
                onFinalized(output)
            }
        }
    }
}

class CameraRecordingStartGuard {
    fun <T> start(start: () -> T, onError: (String) -> Unit): T? = runCatching(start)
        .getOrElse {
            onError("녹화를 시작하지 못했습니다")
            null
        }
}
