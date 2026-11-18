package me.wesley1808.servercore.common.interfaces;

/**
 * Pixel Club (A21): per-pass cache stored on {@link net.minecraft.world.level.NaturalSpawner.SpawnState}.
 * One SpawnState is created per level per tick in ServerChunkCache#tickChunks, so the cache lives exactly one spawn pass.
 */
public interface ISpawnGateState {
    /** Bit per MobCategory ordinal: set = the spawn interval cancels this category in this pass. Negative = not computed yet. */
    int servercore$getGateMask();

    long servercore$getGateTime();

    void servercore$setGate(int mask, long time);
}
