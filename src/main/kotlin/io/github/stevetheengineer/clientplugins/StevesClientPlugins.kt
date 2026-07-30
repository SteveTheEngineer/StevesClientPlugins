package io.github.stevetheengineer.clientplugins

import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft

class StevesClientPlugins : ModInitializer {
    companion object {
        lateinit var USERNAME_LOG: PlayerUsernameLog
    }

    override fun onInitialize() {
        USERNAME_LOG = PlayerUsernameLog(Minecraft.getInstance().gameDirectory.toPath())
        USERNAME_LOG.get()
    }
}
