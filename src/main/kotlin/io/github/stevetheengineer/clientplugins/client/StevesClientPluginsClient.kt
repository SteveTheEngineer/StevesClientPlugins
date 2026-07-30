package io.github.stevetheengineer.clientplugins.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import io.github.stevetheengineer.clientplugins.PlayerRelationship
import io.github.stevetheengineer.clientplugins.PlayerRelationshipStorage
import io.github.stevetheengineer.clientplugins.StevesClientPlugins
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import java.time.OffsetDateTime
import java.util.*

class StevesClientPluginsClient : ClientModInitializer {
    companion object {
        lateinit var RELATIONSHIPS: PlayerRelationshipStorage
    }

    override fun onInitializeClient() {
        var lastSave = OffsetDateTime.now()

        ClientTickEvents.END_CLIENT_TICK.register {
            if (!OffsetDateTime.now().isAfter(lastSave.plusSeconds(5L))) {
                return@register
            }

            lastSave = OffsetDateTime.now()

            Thread {
                StevesClientPlugins.USERNAME_LOG.saveIfNeeded()
            }.start()
        }

        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands)

        RELATIONSHIPS = PlayerRelationshipStorage(Minecraft.getInstance().gameDirectory.toPath())
    }

    private fun getUserUuid(value: String): UUID? {
        var uuid: UUID

        try {
            uuid = UUID.fromString(value)
        } catch (t: Throwable) {
            uuid = Minecraft.getInstance().playerSocialManager.getDiscoveredUUID(value)

            if (uuid == Util.NIL_UUID) {
                return null
            }
        }

        return uuid
    }

    private fun registerCommands(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        registryAccess: CommandBuildContext
    ) {
        dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("rel").then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("get").then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("name_or_uuid", StringArgumentType.string())
                        .suggests { context, builder ->
                            val suggestions = mutableListOf<String>()

                            for (uuid in RELATIONSHIPS.get().keys) {
                                suggestions += uuid.toString()
                            }

                            for (name in Minecraft.getInstance().connection!!.suggestionsProvider.onlinePlayerNames) {
                                suggestions += name
                            }

                            SharedSuggestionProvider.suggest(suggestions, builder)
                        }.executes {
                            val value = StringArgumentType.getString(it, "name_or_uuid")

                            val uuid = this.getUserUuid(value)

                            if (uuid == null) {
                                it.source.sendError(
                                    Component.literal("Player not found, try UUID instead")
                                        .withStyle(ChatFormatting.RED)
                                )
                                return@executes 1
                            }

                            val relationship = RELATIONSHIPS.get()[uuid]
                            val status = relationship?.status ?: PlayerRelationship.Status.NEUTRAL

                            it.source.sendFeedback(Component.literal("Relationship status for $uuid is $status"))

                            return@executes 0
                        }
                )
            ).then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("set").then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("name_or_uuid", StringArgumentType.string())
                        .suggests { context, builder ->
                            val suggestions = mutableListOf<String>()

                            for (uuid in RELATIONSHIPS.get().keys) {
                                suggestions += uuid.toString()
                            }

                            for (name in Minecraft.getInstance().connection!!.suggestionsProvider.onlinePlayerNames) {
                                suggestions += name
                            }

                            SharedSuggestionProvider.suggest(suggestions, builder)
                        }.then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("status", StringArgumentType.string())
                                .suggests { context, builder ->
                                    SharedSuggestionProvider.suggest(
                                        PlayerRelationship.Status.entries.map { it.name },
                                        builder
                                    )
                                }.executes {
                                    val value = StringArgumentType.getString(it, "name_or_uuid")
                                    val statusStr = StringArgumentType.getString(it, "status")

                                    var status: PlayerRelationship.Status = PlayerRelationship.Status.NEUTRAL

                                    try {
                                        status = PlayerRelationship.Status.valueOf(statusStr)
                                    } catch (t: Throwable) {
                                        it.source.sendError(
                                            Component.literal("Invalid status").withStyle(ChatFormatting.RED)
                                        )
                                        return@executes 1
                                    }

                                    val uuid = this.getUserUuid(value)

                                    if (uuid == null) {
                                        it.source.sendError(
                                            Component.literal("Player not found, try UUID instead")
                                                .withStyle(ChatFormatting.RED)
                                        )
                                        return@executes 1
                                    }

                                    val relationship = RELATIONSHIPS.get()[uuid] ?: PlayerRelationship(
                                        uuid,
                                        PlayerRelationship.Status.NEUTRAL
                                    )
                                    RELATIONSHIPS.get()[uuid] = relationship.copy(status = status)

                                    RELATIONSHIPS.save()

                                    it.source.sendFeedback(
                                        Component.literal("Relationship status for $uuid set to $status")
                                            .withStyle(ChatFormatting.GREEN)
                                    )

                                    return@executes 0
                                }
                        )
                )
            )
        )
    }
}
