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
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun fetchTalks(): Result<List<TalkItem>> {
        fetchCount++
        gate?.await()
        return response
    }
}
