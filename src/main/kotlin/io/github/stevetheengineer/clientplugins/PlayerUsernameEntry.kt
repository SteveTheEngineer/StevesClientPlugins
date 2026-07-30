package io.github.stevetheengineer.clientplugins

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.time.OffsetDateTime
import java.util.*

data class PlayerUsernameEntry(
    val uuid: UUID,
    val username: String,
    val at: OffsetDateTime,
) {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec({ instance ->
            instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid")
                    .forGetter(PlayerUsernameEntry::uuid),
                Codec.STRING.fieldOf("username").forGetter(PlayerUsernameEntry::username),
                Codec.STRING.xmap(OffsetDateTime::parse, OffsetDateTime::toString).fieldOf("at")
                    .forGetter(PlayerUsernameEntry::at)
            ).apply(instance, ::PlayerUsernameEntry)
        })

        val CODEC = Codec.lazyInitialized(MAP_CODEC::codec)
    }
}
