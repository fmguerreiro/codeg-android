package app.codeg.android.core.data

import app.codeg.android.core.network.StreamFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerListSyncTest {

    private fun changed(channel: String = "conversation://changed") =
        StreamFrame.SideChannel(channel, null)

    /**
     * The regression this guards: refetching inline would run one fetch per
     * frame, so a bulk import would fire dozens of duplicate list requests.
     */
    @Test
    fun `frames arriving during a fetch collapse into one trailing refresh`() = runTest {
        val fetching = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var refreshes = 0

        val frames = flow {
            emit(changed())
            fetching.await()
            repeat(49) { emit(changed()) }
        }

        val job = launch {
            frames.collectConflatedRefreshes {
                refreshes += 1
                if (refreshes == 1) {
                    fetching.complete(Unit)
                    finish.await()
                }
            }
        }

        runCurrent()
        assertEquals("the burst must not start a second concurrent fetch", 1, refreshes)

        finish.complete(Unit)
        job.join()
        assertEquals("49 frames queued behind one fetch must coalesce", 2, refreshes)
    }

    @Test
    fun `readiness refreshes and resets the backoff`() = runTest {
        var ready = 0
        var refreshes = 0

        flow { emit(StreamFrame.Ready) }
            .collectConflatedRefreshes(onReady = { ready += 1 }) { refreshes += 1 }

        assertEquals(1, ready)
        assertEquals(1, refreshes)
    }

    @Test
    fun `unrelated channels never refetch`() = runTest {
        var refreshes = 0

        flow {
            emit(changed("feedback-settings://changed"))
            emit(changed("delegation-settings://changed"))
            emit(StreamFrame.Pong)
        }.collectConflatedRefreshes { refreshes += 1 }

        assertEquals(0, refreshes)
    }

    @Test
    fun `folder and bulk channels refetch`() = runTest {
        var refreshes = 0

        flow { emit(changed("folder://changed")) }.collectConflatedRefreshes { refreshes += 1 }
        flow { emit(changed("conversations://bulk-changed")) }.collectConflatedRefreshes { refreshes += 1 }

        assertEquals(2, refreshes)
    }
}
