package app.codeg.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import app.codeg.android.R
import app.codeg.android.core.designsystem.theme.CodegTheme
import app.codeg.android.core.model.AgentType

/**
 * Per-agent brand visuals, mirroring the web client's `agent-icon.tsx` and iOS
 * `AgentType.iconAsset`/`accent`. The brand marks live in `res/drawable` as
 * vector drawables ported from the same SVGs the web and iOS clients ship.
 */
object AgentVisuals {

    /** The agent's brand mark, or null when this build ships none — see [initial]. */
    @DrawableRes
    fun icon(agent: AgentType): Int? = when (agent) {
        AgentType.CLAUDE_CODE -> R.drawable.ic_agent_claude_code
        AgentType.CODEX -> R.drawable.ic_agent_codex
        AgentType.OPEN_CODE -> R.drawable.ic_agent_open_code
        AgentType.GEMINI -> R.drawable.ic_agent_gemini
        AgentType.OPEN_CLAW -> R.drawable.ic_agent_open_claw
        AgentType.CLINE -> R.drawable.ic_agent_cline
        AgentType.HERMES -> R.drawable.ic_agent_hermes
        AgentType.CODE_BUDDY -> R.drawable.ic_agent_code_buddy
        AgentType.KIMI_CODE -> R.drawable.ic_agent_kimi_code
        AgentType.PI -> R.drawable.ic_agent_pi
        AgentType.GROK -> R.drawable.ic_agent_grok
        AgentType.CURSOR -> R.drawable.ic_agent_cursor
        AgentType.DEEPSEEK -> R.drawable.ic_agent_deepseek
        AgentType.QODER -> R.drawable.ic_agent_qoder
        AgentType.ANTIGRAVITY -> R.drawable.ic_agent_antigravity
        is AgentType.Custom, is AgentType.Unknown -> null
    }

    /**
     * Whether the brand mark is a monochrome glyph the caller should tint (the
     * web's `MONO_ICONS` / iOS `iconIsTemplate`); the others carry their own
     * brand colors/gradients and render as-is.
     */
    fun iconIsTemplate(agent: AgentType): Boolean = when (agent) {
        AgentType.OPEN_CODE, AgentType.CLINE, AgentType.HERMES,
        AgentType.CODE_BUDDY, AgentType.GROK, AgentType.CURSOR,
        AgentType.QODER, AgentType.ANTIGRAVITY,
        -> true
        else -> false
    }

    /** Monogram shown for agents with no [icon]; their own mark is an SVG we can't decode. */
    fun initial(agent: AgentType): String =
        agent.displayName.trim().firstOrNull()?.uppercase() ?: "?"

    /**
     * Accent colour for badges/avatars and the default tint of monochrome
     * brand marks (iOS `AgentType.accent`). Grok's and Cursor's marks are
     * monochrome brands, so their accent follows the theme — near-black in
     * light, near-white in dark — to stay legible on either surface.
     */
    fun accent(agent: AgentType, isDark: Boolean): Color = when (agent) {
        AgentType.CLAUDE_CODE -> Color(0xFFD98557) // claude clay
        AgentType.CODEX -> Color(0xFF73C7A8) // teal
        AgentType.OPEN_CODE -> Color(0xFF8C9EF2) // indigo
        AgentType.GEMINI -> Color(0xFF80B3FA) // blue
        AgentType.OPEN_CLAW -> Color(0xFFEB9E6B) // amber
        AgentType.CLINE -> Color(0xFF9EC780) // green
        AgentType.HERMES -> Color(0xFF9980D9) // violet
        AgentType.CODE_BUDDY -> Color(0xFF3378F5) // tencent blue
        AgentType.KIMI_CODE -> Color(0xFF1782FF) // moonshot blue
        AgentType.PI -> Color(0xFF383842) // pi slate
        AgentType.DEEPSEEK -> Color(0xFF4D6BFE) // deepseek blue
        AgentType.QODER -> Color(0xFF6C4CF1) // qoder violet
        AgentType.ANTIGRAVITY -> Color(0xFF1A73E8) // google blue
        AgentType.GROK, AgentType.CURSOR ->
            if (isDark) Color(0xFFEBEBEB) else Color(0xFF1F1F1F)
        // Picked from the wire value so an agent keeps one swatch across launches.
        is AgentType.Custom, is AgentType.Unknown ->
            customAccents[(agent.wire.hashCode() and Int.MAX_VALUE) % customAccents.size]
    }

    /** [accent] resolved against the current theme. */
    @Composable
    fun accent(agent: AgentType): Color = accent(agent, CodegTheme.colors.isDark)

    private val customAccents = listOf(
        Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFFE11D48),
        Color(0xFFD97706), Color(0xFF4F46E5), Color(0xFF65A30D), Color(0xFFC026D3),
    )
}

/**
 * The per-agent brand icon (web `AgentIcon` / iOS `AgentIcon`): color agents
 * render their own colors/gradients from the vector asset; monochrome agents
 * are tinted with [tint] (the agent accent by default); agents with no
 * compiled-in mark fall back to their monogram. The surrounding UI usually
 * supplies the visible agent name; the icon keeps its own description for
 * standalone uses.
 */
@Composable
fun AgentIcon(
    agent: AgentType,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val decorated = modifier
        .size(size)
        .clearAndSetSemantics { contentDescription = agent.displayName }
    val asset = AgentVisuals.icon(agent)
    if (asset == null) {
        Box(decorated, contentAlignment = Alignment.Center) {
            Text(
                text = AgentVisuals.initial(agent),
                color = tint ?: AgentVisuals.accent(agent),
                // From Dp, so the glyph matches the brand marks at any font scale.
                fontSize = with(LocalDensity.current) { (size * 0.68f).toSp() },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        return
    }
    val painter = painterResource(asset)
    if (AgentVisuals.iconIsTemplate(agent)) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint ?: AgentVisuals.accent(agent),
            modifier = decorated,
        )
    } else {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = decorated,
        )
    }
}
