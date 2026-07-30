package io.github.stevetheengineer.clientplugins

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.UUID

data class PlayerRelationship(
    val uuid: UUID,
    val status: Status,
) {
    enum class Status(
        val symbol: String? = null,
        val style: Style? = null,
    ) {
        NEUTRAL,

        ENEMY("⚔", Style.EMPTY.withColor(ChatFormatting.RED)),
        PAST_ENEMY("★", Style.EMPTY.withColor(ChatFormatting.GOLD)),
        RETURNING_ENEMY("⚔", Style.EMPTY.withColor(ChatFormatting.DARK_RED)),
        PRIORITY_ENEMY("☠", Style.EMPTY.withColor(ChatFormatting.RED).withBold(true).withUnderlined(true)),

        KNOWN("★", Style.EMPTY.withColor(ChatFormatting.DARK_GREEN)),
        FRIEND("❤", Style.EMPTY.withColor(ChatFormatting.GREEN))
    }

    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(PlayerRelationship::uuid),
                Codec.STRING.xmap(Status::valueOf, Status::name).fieldOf("status").forGetter(PlayerRelationship::status),
            ).apply(instance, ::PlayerRelationship)
        }

        val CODEC = Codec.lazyInitialized(MAP_CODEC::codec)
    }
}
