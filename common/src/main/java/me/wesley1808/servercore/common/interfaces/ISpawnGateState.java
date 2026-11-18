package me.wesley1808.servercore.common.interfaces;

/**
 * Pixel Club (A21): per-tick spawn-interval cache, stored on the ServerLevel.
 * <p>
 * Reset at the head of ServerChunkCache#tickChunks (once per level per tick, on that level's own thread), so the cache
 * lives exactly one spawn pass. It is deliberately NOT stored on NaturalSpawner.SpawnState: mobcapfast hands the
 * previous SpawnState back on ticks where no category is due, so a SpawnState can outlive its tick.
 */
public interface ISpawnGateState {
    /** Bit per MobCategory ordinal: set = the spawn interval cancels this category in this pass. Negative = not computed yet. */
    int servercore$getGateMask();

    long servercore$getGateTime();

    void servercore$setGate(int mask, long time);
}
