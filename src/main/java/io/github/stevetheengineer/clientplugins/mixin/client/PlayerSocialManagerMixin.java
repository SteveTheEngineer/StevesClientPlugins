package io.github.stevetheengineer.clientplugins.mixin.client;

import io.github.stevetheengineer.clientplugins.StevesClientPlugins;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerSocialManager.class)
public class PlayerSocialManagerMixin {
    @Inject(method = "addPlayer", at = @At("HEAD"))
    public void addPlayer(PlayerInfo playerInfo, CallbackInfo ci) {
        StevesClientPlugins.USERNAME_LOG.submit(playerInfo.getProfile().id(), playerInfo.getProfile().name());
    }
}
