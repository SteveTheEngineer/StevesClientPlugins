package io.github.stevetheengineer.clientplugins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import io.github.stevetheengineer.clientplugins.PlayerRelationship;
import io.github.stevetheengineer.clientplugins.client.StevesClientPluginsClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    public Component getTabListDisplayName(Component original) {
        var relationship = StevesClientPluginsClient.RELATIONSHIPS.get().get(this.getProfile().id());

        if (relationship == null || relationship.getStatus() == PlayerRelationship.Status.NEUTRAL) {
            return original;
        }

        if (original == null) {
            original = Component.literal(this.getProfile().name());
        }

        var prefix = Component.literal("[" + relationship.getStatus().getSymbol() + "] ").withStyle(relationship.getStatus().getStyle());

        return prefix.append(original);
    }
}
