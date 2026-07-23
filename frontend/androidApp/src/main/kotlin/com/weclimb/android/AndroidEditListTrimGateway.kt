package com.weclimb.android

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.weclimb.media.TrimGateway
import com.weclimb.media.TrimRequest
import java.io.File

interface EditListExporter {
    fun export(
        request: TrimRequest,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    )
}

class AndroidEditListTrimGateway(
    private val exporter: EditListExporter,
    private val onCompleted: (String) -> Unit,
    private val onError: (String) -> Unit,
) : TrimGateway {
    override fun start(request: TrimRequest) {
        exporter.export(request, onCompleted, onError)
    }
}

class Media3EditListExporter(
    private val context: Context,
) : EditListExporter {
    private var transformer: Transformer? = null

    override fun export(
        request: TrimRequest,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val output = File(request.outputPath)
        if (output.exists()) {
            onError("트리밍 출력 파일이 이미 존재합니다")
            return
        }

        val listener = exportListener(output, onCompleted, onError)
        transformer = Transformer.Builder(context)
            .experimentalSetMp4EditListTrimEnabled(true)
            .addListener(listener)
            .build()

        val input = MediaItem.Builder()
            .setUri(request.sourceUri())
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(request.startMillis)
                    .setEndPositionMs(request.endMillis)
                    .build(),
            )
            .build()

        runCatching {
            transformer?.start(EditedMediaItem.Builder(input).build(), output.absolutePath)
        }.onFailure { error ->
            transformer = null
            output.delete()
            onError(error.message ?: "트리밍을 시작하지 못했습니다")
        }
    }

    private fun exportListener(
        output: File,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    ): Transformer.Listener = object : Transformer.Listener {
        override fun onCompleted(composition: Composition, result: ExportResult) {
            transformer = null
            if (output.isFile && output.length() > 0L) {
                onCompleted(output.absolutePath)
            } else {
                output.delete()
                onError("트리밍 출력 파일을 만들지 못했습니다")
            }
        }

        override fun onError(
            composition: Composition,
            result: ExportResult,
            exception: ExportException,
        ) {
            transformer = null
            output.delete()
            onError(exception.message ?: "트리밍에 실패했습니다")
        }
    }
}

private fun TrimRequest.sourceUri(): Uri {
    val uri = Uri.parse(sourcePath)
    return if (uri.scheme == null) Uri.fromFile(File(sourcePath)) else uri
}
