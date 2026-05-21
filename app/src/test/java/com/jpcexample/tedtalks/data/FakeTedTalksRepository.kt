package com.jpcexample.tedtalks.data

/**
 * Controllable repository for tests. The [response] property is what `fetchTalks`
 * will return; tests can mutate it between calls to simulate retries, errors, etc.
 */
class FakeTedTalksRepository(
    var response: Result<List<TalkItem>> = Result.success(emptyList()),
) : TedTalksRepository {
    var fetchCount: Int = 0
        private set

    override suspend fun fetchTalks(): Result<List<TalkItem>> {
        fetchCount++
        return response
    }
}
