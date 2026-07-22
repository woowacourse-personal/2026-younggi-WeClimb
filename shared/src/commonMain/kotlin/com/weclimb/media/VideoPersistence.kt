package com.weclimb.media

interface MediaStoreGateway {
    fun save(path: String): Result<String>
}

interface CacheGateway {
    fun delete(path: String): Result<Unit>
}

enum class StoreError {
    WRITE_FAILED,
}

sealed interface PromotionResult {
    data class Saved(val uri: String) : PromotionResult

    data class Failed(val error: StoreError) : PromotionResult
}

class VideoPersistence(
    private val mediaStore: MediaStoreGateway,
    private val cache: CacheGateway,
) {
    fun promote(path: String): PromotionResult = mediaStore.save(path)
        .fold(
            onSuccess = { PromotionResult.Saved(it) },
            onFailure = { PromotionResult.Failed(StoreError.WRITE_FAILED) },
        )

    fun deleteFailed(paths: List<String>) {
        paths.forEach { path -> cache.delete(path).getOrThrow() }
    }
}
