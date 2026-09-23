package app.codeg.android.core.network

import app.codeg.android.core.model.AcpEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the WebSocket frame router (dual-layer channel vs attach protocol). */
class FrameRoutingTest {

    private val json = CodegJson.response
    private fun parse(raw: String): StreamFrame? = EventStream.parseFrame(raw, json)

    @Test
    fun `legacy ready channel maps to Ready`() {
        assertEquals(StreamFrame.Ready, parse("""{"channel":"__ready__","payload":null}"""))
    }

    @Test
    fun `legacy side channels preserve channel and payload`() {
        val frame = parse(
            """{"channel":"conversation://changed","payload":{"kind":"deleted","id":7}}""",
        ) as StreamFrame.SideChannel
        assertEquals("conversation://changed", frame.channel)
        assertEquals("""{"kind":"deleted","id":7}""", frame.payload.toString())
    }

    @Test
    fun `event frame unwraps the envelope`() {
        val frame = parse(
            """{"type":"event","subscription_id":"s","envelope":
               {"seq":1,"connection_id":"c","type":"content_delta","text":"hello"}}""",
        )
        assertTrue(frame is StreamFrame.Event)
        val event = (frame as StreamFrame.Event).envelope.event
        assertEquals(AcpEvent.ContentDelta("hello"), event)
    }

    @Test
    fun `pong and detached route correctly`() {
        assertEquals(StreamFrame.Pong, parse("""{"type":"pong"}"""))
        assertEquals(StreamFrame.Detached("lagged"), parse("""{"type":"detached","reason":"lagged"}"""))
    }

    @Test
    fun `garbage and unknown frames are dropped`() {
        assertNull(parse("not json"))
        assertNull(parse("""{"type":"mystery"}"""))
    }

    @Test
    fun `websocket url derives ws and wss with the events path`() {
        assertEquals("ws://host:3080/ws/events", EventStream.websocketUrl("http://host:3080"))
        assertEquals("wss://host/ws/events", EventStream.websocketUrl("https://host/"))
    }
}
