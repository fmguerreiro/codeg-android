package app.codeg.android.core.model

import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Wire prefix marking a user-registered ACP agent. */
const val CUSTOM_AGENT_WIRE_PREFIX = "custom:"

/**
 * The coding agents codeg can drive. Wire value is snake_case (Rust serde
 * `rename_all = "snake_case"`).
 *
 * Open, like the web client's `AgentType`: a user can register any ACP agent
 * (`custom:<registry-id>`) and a newer server may ship built-ins this build has
 * never heard of. Both keep an identity of their own so they stay listable and
 * selectable instead of being dropped or collapsed onto Claude Code.
 *
 * Kept free of any Compose/UI types so the model layer stays pure Kotlin — the
 * per-agent accent colours and monogram rendering live in the design system
 * (`AgentVisuals`), keyed off this type.
 */
@Serializable(with = AgentType.AgentTypeSerializer::class)
sealed class AgentType {
    abstract val wire: String

    /** Human-readable name. */
    abstract val displayName: String

    /** Short label for dense badges. */
    abstract val shortName: String

    /** An agent codeg itself knows how to install and launch. */
    sealed class Builtin(
        override val wire: String,
        override val displayName: String,
        override val shortName: String,
    ) : AgentType()

    data object CLAUDE_CODE : Builtin("claude_code", "Claude Code", "Claude")
    data object CODEX : Builtin("codex", "Codex CLI", "Codex")
    data object OPEN_CODE : Builtin("open_code", "OpenCode", "OpenCode")
    data object GEMINI : Builtin("gemini", "Gemini CLI", "Gemini")
    data object OPEN_CLAW : Builtin("open_claw", "OpenClaw", "OpenClaw")
    data object CLINE : Builtin("cline", "Cline", "Cline")
    data object HERMES : Builtin("hermes", "Hermes", "Hermes")
    data object CODE_BUDDY : Builtin("code_buddy", "CodeBuddy", "CodeBuddy")
    data object KIMI_CODE : Builtin("kimi_code", "Kimi Code", "Kimi")
    data object PI : Builtin("pi", "Pi", "Pi")
    data object GROK : Builtin("grok", "Grok", "Grok")
    data object CURSOR : Builtin("cursor", "Cursor", "Cursor")
    data object DEEPSEEK : Builtin("deepseek", "DeepSeek Harness", "DeepSeek")
    data object QODER : Builtin("qoder", "Qoder", "Qoder")
    data object ANTIGRAVITY : Builtin("antigravity", "Google Antigravity", "Antigravity")

    /** A user-registered ACP agent; its name is only known at runtime, from
     *  the agent list — see [CustomAgentNames]. */
    data class Custom(val id: String) : AgentType() {
        override val wire: String get() = CUSTOM_AGENT_WIRE_PREFIX + id
        override val displayName: String get() = CustomAgentNames.name(id) ?: humanizeAgentId(id)
        override val shortName: String get() = displayName
    }

    /** An agent type this build doesn't know: a newer server's built-in. */
    data class Unknown(override val wire: String) : AgentType() {
        override val displayName: String get() = humanizeAgentId(wire)
        override val shortName: String get() = displayName
    }

    companion object {
        /** Every built-in, in the order the picker and chip rows show them. */
        val BUILTINS: List<Builtin> = listOf(
            CLAUDE_CODE, CODEX, OPEN_CODE, GEMINI, OPEN_CLAW, CLINE, HERMES,
            CODE_BUDDY, KIMI_CODE, PI, GROK, CURSOR, DEEPSEEK, QODER, ANTIGRAVITY,
        )

        private val byWire: Map<String, Builtin> = BUILTINS.associateBy { it.wire }

        /** The agent for [raw]; total, so no row is ever lost or merged into another. */
        fun fromWire(raw: String): AgentType = byWire[raw]
            ?: raw.removePrefix(CUSTOM_AGENT_WIRE_PREFIX)
                .takeIf { it != raw && it.isNotEmpty() }
                ?.let(::Custom)
            ?: Unknown(raw)

        /** The built-in for [raw], or null when it isn't one. */
        fun knownFromWire(raw: String): Builtin? = byWire[raw]
    }

    object AgentTypeSerializer : KSerializer<AgentType> {
        override val descriptor =
            PrimitiveSerialDescriptor("com.codeg.AgentType", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: AgentType) =
            encoder.encodeString(value.wire)

        override fun deserialize(decoder: Decoder): AgentType =
            fromWire(decoder.decodeString())
    }
}

/** Turn `github-copilot-cli` into `Github Copilot Cli`. */
internal fun humanizeAgentId(id: String): String = id
    .split('-', '_', '.')
    .filter { it.isNotEmpty() }
    .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

/**
 * Names for the user-registered ACP agents, published from the agent list —
 * [AgentType.Custom] carries only the registry id, but it is what the badges,
 * chips and picker rows are keyed by. Process-wide, like the web client's
 * `custom-agents.ts`: every one of those rows renders off an agent list, so the
 * publish has already happened by the time one paints.
 */
object CustomAgentNames {
    private val names = ConcurrentHashMap<String, String>()

    /** Replaces the whole set, so a removed agent stops resolving. */
    fun publish(agents: List<AcpAgentInfo>) {
        val next = agents.mapNotNull { agent ->
            val id = (agent.agentType as? AgentType.Custom)?.id ?: return@mapNotNull null
            agent.name.trim().takeIf { it.isNotEmpty() }?.let { id to it }
        }.toMap()
        names.keys.retainAll(next.keys)
        names.putAll(next)
    }

    fun name(id: String): String? = names[id]
}
