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

class PlayerUsernameLog(
    path: Path,
) {
    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    private val path = path.resolve("stcp_username_log.json")
    private val pathNew = path.resolve("stcp_username_log.json.new")

    private var isLoaded = false
    private var isDirty = false

    private val log = mutableListOf<PlayerUsernameEntry>()

    private fun load(tryNew: Boolean = true) {
        if (path.notExists()) {
            if (tryNew) {
                load(false)
            }

            return
        }

        try {
            val data = JsonParser.parseReader((if (tryNew) pathNew else path).reader())?.asJsonArray ?: return

            synchronized(this.log) {
                this.log.clear()

                for (entry in data) {
                    this.log += PlayerUsernameEntry.CODEC
                        .parse(JsonOps.INSTANCE, entry)
                        .resultOrPartial { LOGGER.warn("Failed to parse username log entry: {}", it) }
                        .getOrNull() ?: continue
                }
            }
        } catch (exception: Exception) {
            if (tryNew) {
                LOGGER.error("Failed to load player username log, trying backup", exception)
                load(false)
            } else {
                LOGGER.error("Failed to load player username log", exception)
            }
        }
    }

    private fun save() {
        if (!this.isLoaded) {
            LOGGER.warn("Refused to save player username log before load")
            return
        }

        try {
            val data = JsonArray()

            synchronized(log) {
                for (entry in log) {
                    data.add(PlayerUsernameEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry).orThrow)
                }
            }

            val string = data.toString()

            pathNew.writeText(string)
            pathNew.copyTo(path, StandardCopyOption.REPLACE_EXISTING)
            pathNew.deleteExisting()
        } catch (exception: Exception) {
            LOGGER.error("Failed to save player username log", exception)
        }
    }

    fun saveIfNeeded() {
        if (!this.isDirty) {
            return
        }

        this.save()
        this.isDirty = false
    }

    fun get(): MutableList<PlayerUsernameEntry> {
        if (!this.isLoaded) {
            this.load()
            this.isLoaded = true
        }

        return this.log
    }

    fun markDirty() {
        this.isDirty = true
    }

    fun submit(uuid: UUID, username: String) {
        val log = this.get()

        synchronized(log) {
            if (log.any { it.uuid == uuid && it.username == username }) {
                return
            }

            log += PlayerUsernameEntry(uuid, username, OffsetDateTime.now())
        }

        this.markDirty()
    }
}