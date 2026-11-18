package me.wesley1808.servercore.mixin.features.mob_spawning;

import me.wesley1808.servercore.common.interfaces.ISpawnGateState;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pixel Club (A21): a new spawn pass starts here - forget the previous pass's spawn-interval mask. */
@Mixin(ServerChunkCache.class)
public class ServerChunkCacheSpawnGateMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "tickChunks", at = @At("HEAD"), require = 1)
    private void servercore$resetSpawnGate(CallbackInfo ci) {
        ((ISpawnGateState) this.level).servercore$setGate(-1, 0L);
    }
}
