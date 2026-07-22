package com.weclimb.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VideoPersistenceTest {
    @Test
    fun promotesSuccessfulCacheVideoToMediaStore() {
        val mediaStore = FakeMediaStore()
        val cache = FakeCache(setOf("cache/success.mp4"))
        val persistence = VideoPersistence(mediaStore, cache)

        val result = persistence.promote("cache/success.mp4")

        assertEquals("content://media/Movies/WeClimb/success.mp4", assertIs<PromotionResult.Saved>(result).uri)
        assertTrue("cache/success.mp4" in cache.paths)
    }

    @Test
    fun deletesOnlyFailedCacheVideos() {
        val mediaStore = FakeMediaStore()
        val cache = FakeCache(setOf("cache/failed-1.mp4", "cache/failed-2.mp4", "cache/success.mp4"))
        val persistence = VideoPersistence(mediaStore, cache)

        persistence.deleteFailed(listOf("cache/failed-1.mp4", "cache/failed-2.mp4"))

        assertEquals(setOf("cache/success.mp4"), cache.paths)
        assertTrue(mediaStore.savedUris.isEmpty())
    }

    @Test
    fun preservesSuccessfulCacheVideoWhenMediaStoreWriteFails() {
        val mediaStore = FakeMediaStore(shouldFail = true)
        val cache = FakeCache(setOf("cache/success.mp4"))
        val persistence = VideoPersistence(mediaStore, cache)

        val result = persistence.promote("cache/success.mp4")

        assertEquals(StoreError.WRITE_FAILED, assertIs<PromotionResult.Failed>(result).error)
        assertTrue("cache/success.mp4" in cache.paths)
    }
}

private class FakeMediaStore(
    private val shouldFail: Boolean = false,
) : MediaStoreGateway {
    val savedUris = mutableListOf<String>()

    override fun save(path: String): Result<String> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("write failed"))
        }
        val uri = "content://media/Movies/WeClimb/${path.substringAfterLast('/')}"
        savedUris += uri
        return Result.success(uri)
    }
}

private class FakeCache(initialPaths: Set<String>) : CacheGateway {
    var paths: Set<String> = initialPaths
        private set

    override fun delete(path: String): Result<Unit> {
        paths = paths - path
        return Result.success(Unit)
    }
}
