package com.weclimb.android

import com.weclimb.media.TrimRequest
import java.io.File

internal class TrimExportCoordinator(
    private val exporterFactory: () -> EditListExporter,
) {
    private var activeExport: ActiveTrimExport? = null

    fun start(
        attemptId: String,
        request: TrimRequest,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    ): Boolean {
        if (activeExport != null) return false
        val token = Any()
        val exporter = exporterFactory()
        activeExport = ActiveTrimExport(attemptId, request.outputPath, exporter, token)
        runCatching {
            exporter.export(
                request = request,
                onCompleted = { path ->
                    if (claim(token)) {
                        onCompleted(path)
                    } else {
                        File(path).delete()
                    }
                },
                onError = { message ->
                    if (claim(token)) {
                        onError(message)
                    } else {
                        File(request.outputPath).delete()
                    }
                },
            )
        }.onFailure { error ->
            if (claim(token)) {
                File(request.outputPath).delete()
                onError(error.message ?: "트리밍을 시작하지 못했습니다")
            }
        }
        return true
    }

    fun cancel(attemptId: String): Boolean {
        val current = activeExport?.takeIf { it.attemptId == attemptId } ?: return false
        activeExport = null
        current.exporter.cancel()
        File(current.outputPath).delete()
        return true
    }

    private fun claim(token: Any): Boolean {
        if (activeExport?.token !== token) return false
        activeExport = null
        return true
    }
}

private data class ActiveTrimExport(
    val attemptId: String,
    val outputPath: String,
    val exporter: EditListExporter,
    val token: Any,
)
