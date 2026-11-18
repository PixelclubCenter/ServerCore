package me.wesley1808.servercore.mixin.features.mob_spawning;

import me.wesley1808.servercore.common.interfaces.ISpawnGateState;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Pixel Club (A21): holds the spawn-interval mask for one spawn pass. */
@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnStateMixin implements ISpawnGateState {
    @Unique
    private int servercore$gateMask = -1;
    @Unique
    private long servercore$gateTime;

    @Override
    public int servercore$getGateMask() {
        return this.servercore$gateMask;
    }

    @Override
    public long servercore$getGateTime() {
        return this.servercore$gateTime;
    }

    @Override
    public void servercore$setGate(int mask, long time) {
        this.servercore$gateMask = mask;
        this.servercore$gateTime = time;
    }
}
