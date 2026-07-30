package io.github.stevetheengineer.clientplugins.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import io.github.stevetheengineer.clientplugins.PlayerRelationship;
import io.github.stevetheengineer.clientplugins.client.StevesClientPluginsClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public abstract GameProfile getGameProfile();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Component getDisplayName(Component original) {
        var relationship = StevesClientPluginsClient.RELATIONSHIPS.get().get(this.getUUID());

        if (relationship == null || relationship.getStatus() == PlayerRelationship.Status.NEUTRAL) {
            return original;
        }

        if (original.getString().equals(this.getGameProfile().name())) {
            original = Component.literal(this.getGameProfile().name());
        }

        var prefix = Component.literal("[" + relationship.getStatus().getSymbol() + "] ").withStyle(relationship.getStatus().getStyle());

        return prefix.append(original);
    }
}
