package com.weclimb.android

import android.Manifest
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.weclimb.media.PromotionResult
import com.weclimb.media.ShareRequestFactory
import com.weclimb.media.TrimRequest
import com.weclimb.media.TrimResult
import com.weclimb.media.TrimService
import com.weclimb.media.VideoPersistence
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var recordButton: Button
    private lateinit var successButton: Button
    private lateinit var failureButton: Button
    private lateinit var trimButton: Button
    private lateinit var shareButton: Button
    private lateinit var recorder: CameraRecordingController
    private var completedFile: File? = null
    private var savedUri: String? = null
    private var trimInProgress = false
    private val persistence by lazy { VideoPersistence(AndroidMediaStoreGateway(this), AndroidCacheGateway()) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onPermissionResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorder = CameraRecordingController(this, this)
        setContentView(createContent())
        requestOrBindCamera()
    }

    private fun createContent(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        status = TextView(context)
        recordButton = Button(context).apply {
            text = "녹화 시작"
            setOnClickListener { toggleRecording() }
        }
        successButton = Button(context).apply {
            text = "성공"
            setOnClickListener { classifyCompletedRecording("성공") }
        }
        failureButton = Button(context).apply {
            text = "실패"
            setOnClickListener { classifyCompletedRecording("실패") }
        }
        trimButton = Button(context).apply {
            text = "1초-4초 트리밍"
            setOnClickListener { trimCompletedRecording() }
        }
        shareButton = Button(context).apply {
            text = "영상 공유"
            setOnClickListener { shareSavedVideo() }
        }
        addView(status)
        addView(recordButton)
        addView(successButton)
        addView(failureButton)
        addView(trimButton)
        addView(shareButton)
        updateClassificationButtons()
    }

    private fun requestOrBindCamera() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (cameraGranted && microphoneGranted) {
            bindCamera()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun onPermissionResult(result: Map<String, Boolean>) {
        if (result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true) {
            bindCamera()
        } else {
            status.text = "카메라와 마이크 권한이 필요합니다"
        }
    }

    private fun bindCamera() {
        recorder.bind(
            onReady = { status.text = "녹화할 준비가 됐습니다" },
            onError = { status.text = it },
        )
    }

    private fun toggleRecording() {
        if (recorder.isRecording) {
            recorder.stop()
            recordButton.text = "녹화 시작"
            status.text = "녹화를 정리하고 있습니다"
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        completedFile = null
        savedUri = null
        updateClassificationButtons()
        recorder.start(
            output = File(cacheDir, "attempt-${System.currentTimeMillis()}.mp4"),
            onFinalized = ::onRecordingFinalized,
            onError = { status.text = it },
        )
        recordButton.text = "녹화 중지"
        status.text = "녹화 중"
    }

    private fun onRecordingFinalized(file: File) {
        completedFile = file
        status.text = "성공 또는 실패를 선택하세요"
        updateClassificationButtons()
    }

    private fun classifyCompletedRecording(label: String) {
        val file = completedFile ?: return
        if (label == "성공") {
            saveSuccessfulRecording(file)
        } else {
            persistence.deleteFailed(listOf(file.absolutePath))
            status.text = "실패 영상을 삭제했습니다"
        }
        completedFile = null
        updateClassificationButtons()
    }

    private fun trimCompletedRecording() {
        val source = completedFile ?: return
        val durationMillis = videoDurationMillis(source) ?: run {
            status.text = "영상 길이를 읽지 못했습니다"
            return
        }
        val request = TrimRequest(
            sourcePath = source.absolutePath,
            outputPath = File(cacheDir, "trim-${System.currentTimeMillis()}.mp4").absolutePath,
            startMillis = TRIM_START_MILLIS,
            endMillis = TRIM_END_MILLIS,
            durationMillis = durationMillis,
        )
        val gateway = AndroidEditListTrimGateway(
            exporter = Media3EditListExporter(this),
            onCompleted = ::onTrimCompleted,
            onError = ::onTrimError,
        )
        trimInProgress = true
        when (TrimService(gateway).trim(request)) {
            TrimResult.Started -> {
                if (trimInProgress) {
                    status.text = "edit-list 트리밍 중"
                }
                updateClassificationButtons()
            }
            is TrimResult.Rejected -> {
                trimInProgress = false
                status.text = "트리밍 구간이 올바르지 않습니다"
                updateClassificationButtons()
            }
        }
    }

    private fun onTrimCompleted(outputPath: String) {
        completedFile = File(outputPath)
        trimInProgress = false
        status.text = "트리밍 완료, 성공 또는 실패를 선택하세요"
        updateClassificationButtons()
    }

    private fun onTrimError(message: String) {
        trimInProgress = false
        status.text = "트리밍에 실패했습니다: $message"
        updateClassificationButtons()
    }

    private fun videoDurationMillis(file: File): Long? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        }
    }.getOrNull()

    private fun saveSuccessfulRecording(file: File) {
        when (val result = persistence.promote(file.absolutePath)) {
            is PromotionResult.Saved -> {
                savedUri = result.uri
                status.text = "성공 영상을 갤러리에 저장했습니다"
            }
            is PromotionResult.Failed -> {
                status.text = "성공 영상을 저장하지 못했습니다: ${result.error}"
            }
        }
    }

    private fun shareSavedVideo() {
        val uri = savedUri ?: return
        AndroidShareLauncher(this).launch(ShareRequestFactory().create(uri))
    }

    private fun updateClassificationButtons() {
        val enabled = completedFile != null && !trimInProgress
        successButton.isEnabled = enabled
        failureButton.isEnabled = enabled
        trimButton.isEnabled = enabled
        shareButton.isEnabled = savedUri != null
        recordButton.isEnabled = !trimInProgress
    }

    private companion object {
        const val TRIM_START_MILLIS = 1_000L
        const val TRIM_END_MILLIS = 4_000L
    }
}
