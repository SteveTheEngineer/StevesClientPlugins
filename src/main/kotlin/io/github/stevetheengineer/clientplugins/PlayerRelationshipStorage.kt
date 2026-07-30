package io.github.stevetheengineer.clientplugins

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonSerializer
import com.google.gson.stream.JsonWriter
import com.mojang.logging.LogUtils
import com.mojang.serialization.JsonOps
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.io.path.copyTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.io.path.reader
import kotlin.io.path.writeText
import kotlin.io.path.writer
import kotlin.jvm.optionals.getOrNull

class PlayerRelationshipStorage(
    path: Path,
) {
    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    private val path = path.resolve("stcp_relationships.json")
    private val pathNew = path.resolve("stcp_relationships.json.new")

    private var isLoaded = false
    private val relationships = mutableMapOf<UUID, PlayerRelationship>()

    private fun load(tryNew: Boolean = true) {
        if (path.notExists()) {
            if (tryNew) {
                load(false)
            }

            return
        }

        try {
            val data = JsonParser.parseReader((if (tryNew) pathNew else path).reader())?.asJsonArray ?: return

            this.relationships.clear()

            for (entry in data) {
                val relationship = PlayerRelationship.CODEC
                    .parse(JsonOps.INSTANCE, entry)
                    .resultOrPartial { LOGGER.warn("Failed to parse username log entry: {}", it) }
                    .getOrNull() ?: continue

                this.relationships[relationship.uuid] = relationship
            }
        } catch (exception: Exception) {
            if (tryNew) {
                LOGGER.error("Failed to load player relationships, trying backup", exception)
                load(false)
            } else {
                LOGGER.error("Failed to load player relationships", exception)
            }
        }
    }

    fun save() {
        if (!this.isLoaded) {
            LOGGER.warn("Refused to save player relationships before load")
            return
        }

        try {
            val data = JsonArray()

            for (relationship in relationships.values) {
                data.add(PlayerRelationship.CODEC.encodeStart(JsonOps.INSTANCE, relationship).orThrow)
            }

            val string = data.toString()

            pathNew.writeText(string)
            pathNew.copyTo(path, StandardCopyOption.REPLACE_EXISTING)
            pathNew.deleteExisting()
        } catch (exception: Exception) {
            LOGGER.error("Failed to save player username log", exception)
        }
    }

    fun get(): MutableMap<UUID, PlayerRelationship> {
        if (!this.isLoaded) {
            this.load()
            this.isLoaded = true
        }

        return this.relationships
    }
}