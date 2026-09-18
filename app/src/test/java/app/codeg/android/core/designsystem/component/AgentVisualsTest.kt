package app.codeg.android.core.designsystem.component

import androidx.compose.ui.graphics.luminance
import app.codeg.android.core.model.AgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentVisualsTest {

    @Test
    fun `every built-in has its own brand icon`() {
        val icons = AgentType.BUILTINS.map(AgentVisuals::icon)

        assertEquals(icons.size, icons.toSet().size)
        assertTrue("built-ins must all ship a mark", icons.none { it == null })
    }

    @Test
    fun `agents with no compiled-in mark fall back to a monogram`() {
        assertEquals(null, AgentVisuals.icon(AgentType.Custom("qwen-code")))
        assertEquals("Q", AgentVisuals.initial(AgentType.Custom("qwen-code")))
        assertEquals(null, AgentVisuals.icon(AgentType.Unknown("future_agent")))
        assertEquals("F", AgentVisuals.initial(AgentType.Unknown("future_agent")))
    }

    @Test
    fun `a custom agent's accent is stable across builds`() {
        val once = AgentVisuals.accent(AgentType.Custom("goose"), isDark = false)
        val twice = AgentVisuals.accent(AgentType.Custom("goose"), isDark = true)

        assertEquals(once, twice)
    }

    @Test
    fun `accents are fully opaque in both themes`() {
        (AgentType.BUILTINS + AgentType.Custom("goose") + AgentType.Unknown("future")).forEach { agent ->
            assertEquals(1f, AgentVisuals.accent(agent, isDark = false).alpha, 0f)
            assertEquals(1f, AgentVisuals.accent(agent, isDark = true).alpha, 0f)
        }
    }

    @Test
    fun `theme-following marks swap tint with the theme`() {
        // Grok's and Cursor's monochrome brand marks must not vanish on either
        // surface: dark tint in light theme, light tint in dark theme.
        listOf(AgentType.GROK, AgentType.CURSOR).forEach { agent ->
            val light = AgentVisuals.accent(agent, isDark = false).luminance()
            val dark = AgentVisuals.accent(agent, isDark = true).luminance()

            assertTrue("${agent.displayName} light tint should be dark", light < 0.5f)
            assertTrue("${agent.displayName} dark tint should be light", dark > 0.5f)
        }
    }
}
