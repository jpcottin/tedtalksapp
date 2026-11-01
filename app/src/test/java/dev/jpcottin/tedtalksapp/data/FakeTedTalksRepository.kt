package dev.jpcottin.tedtalksapp.data

import kotlinx.coroutines.CompletableDeferred

/**
 * Controllable repository for tests. The [response] property is what `fetchTalks`
 * will return; tests can mutate it between calls to simulate retries, errors, etc.
 * When [gate] is set, `fetchTalks` suspends until it completes, so a test can
 * observe the in-flight state.
 */
class FakeTedTalksRepository(
    var response: Result<List<TalkItem>> = Result.success(emptyList()),
) : TedTalksRepository {
    var fetchCount: Int = 0
        private set
    /** What the last `fetchTalks` was asked for. */
    var lastIncludeAudio: Boolean? = null
        private set
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun fetchTalks(includeAudio: Boolean): Result<List<TalkItem>> {
        fetchCount++
        lastIncludeAudio = includeAudio
        gate?.await()
        return response
    }
}
