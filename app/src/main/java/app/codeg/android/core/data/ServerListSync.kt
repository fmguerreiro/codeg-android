package app.codeg.android.core.data

import app.codeg.android.core.datastore.ServerProfile
import app.codeg.android.core.network.StreamFrame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Global side-channels that invalidate a folder/conversation list. */
private val LIST_CHANNELS = setOf(
    "conversation://changed",
    "conversations://bulk-changed",
    "folder://changed",
)

/**
 * Runs [refresh] on its own coroutine fed by a conflated channel, so a burst of
 * frames collapses into the in-flight refresh plus a single trailing one rather
 * than one refresh per frame, and the socket collector never blocks on a
 * suspended fetch. [onReady] fires on [StreamFrame.Ready] — the global channels
 * are not replayed on reconnect, so readiness has to trigger a refresh too.
 */
internal suspend fun Flow<StreamFrame>.collectConflatedRefreshes(
    onReady: () -> Unit = {},
    refresh: suspend () -> Unit,
) {
    coroutineScope {
        val refreshes = Channel<Unit>(Channel.CONFLATED)
        val refresher = launch {
            for (ignored in refreshes) refresh()
        }
        try {
            collect { frame ->
                when (frame) {
                    StreamFrame.Ready -> {
                        onReady()
                        refreshes.trySend(Unit)
                    }
                    is StreamFrame.SideChannel -> {
                        if (frame.channel in LIST_CHANNELS) refreshes.trySend(Unit)
                    }
                    else -> Unit
                }
            }
        } finally {
            refreshes.close()
            refresher.join()
        }
    }
}

/**
 * Keeps a list screen converged with the server by refetching whenever one of
 * [LIST_CHANNELS] fires. The server broadcasts these to every client, so changes
 * made elsewhere (desktop app, an import, an agent starting a turn) reach the
 * phone without a manual pull-to-refresh. The socket is retried with exponential
 * backoff, and each reconnect refetches once.
 */
suspend fun ServerRepository.watchListChanges(
    profile: ServerProfile,
    refresh: suspend () -> Unit,
) {
    var retryDelayMs = 1_000L
    while (true) {
        val stream = eventStream(profile) ?: return
        stream.frames().collectConflatedRefreshes(onReady = { retryDelayMs = 1_000L }, refresh = refresh)
        delay(retryDelayMs)
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(30_000L)
    }
}
