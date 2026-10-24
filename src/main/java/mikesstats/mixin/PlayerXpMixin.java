package mikesstats.mixin;

import mikesstats.server.Tracking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla keeps no total XP or highest level, so count every gain. */
@Mixin(Player.class)
abstract class PlayerXpMixin {
    @Inject(method = "giveExperiencePoints", at = @At("HEAD"))
    private void mikesstats$countXp(int amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) Tracking.onXpGained(player, amount);
    }

    @Inject(method = "giveExperienceLevels", at = @At("TAIL"))
    private void mikesstats$seeLevel(int amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) Tracking.onLevelChanged(player);
    }
}
