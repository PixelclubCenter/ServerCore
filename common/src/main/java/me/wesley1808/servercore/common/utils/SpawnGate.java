package me.wesley1808.servercore.common.utils;

import me.wesley1808.servercore.common.ServerCore;
import me.wesley1808.servercore.common.interfaces.IMobCategory;
import me.wesley1808.servercore.common.interfaces.ISpawnGateState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Pixel Club (A21): spawn-interval check evaluated once per spawn pass instead of once per chunk x category.
 * <p>
 * Upstream decides, for every spawnable chunk and every category, {@code interval > 1 && gameTime % interval != 0}.
 * The answer only depends on the game time and the configured interval, so it is computed once per spawn pass
 * (per level per tick: the cache sits on the ServerLevel and is reset at the head of ServerChunkCache#tickChunks) as a
 * bit mask. The {@code max <= 0} check stays live, because dynamic mobcaps can change it at any time.
 * <p>
 * JVM property {@code servercore.pxc.spawnGate}: {@code on} (default) | {@code off} (upstream code path) |
 * {@code verify} (computes both, counts disagreements, logs every 1200 ticks per level).
 */
public final class SpawnGate {
    public enum Mode { ON, OFF, VERIFY }

    public static final Mode MODE = parseMode(System.getProperty("servercore.pxc.spawnGate", "on"));
    /** Rig self-test of the verify counter only: flips the MONSTER bit of the cached mask. Never set on PRD. */
    private static final boolean SELFTEST = Boolean.getBoolean("servercore.pxc.spawnGate.selftest");
    private static final MobCategory[] CATEGORIES = MobCategory.values();
    private static final ConcurrentHashMap<String, Counters> COUNTERS = new ConcurrentHashMap<>();

    static {
        ServerCore.LOGGER.info("[servercore-pxc] spawnGate mode={}{}", MODE.name().toLowerCase(Locale.ROOT), SELFTEST ? " SELFTEST" : "");
    }

    private SpawnGate() {
    }

    public static boolean shouldCancelSpawn(ServerLevel level, MobCategory category) {
        if (category.getMaxInstancesPerChunk() <= 0) {
            return true;
        }

        if (MODE == Mode.OFF) {
            return original(level, category);
        }

        ISpawnGateState gate = (ISpawnGateState) level;
        int mask = gate.servercore$getGateMask();
        if (mask < 0) {
            long time = level.getGameTime();
            mask = computeMask(time);
            gate.servercore$setGate(mask, time);
            if (MODE == Mode.VERIFY && time % 1200L == 0L) {
                report(level);
            }
        }

        boolean cached = (mask & (1 << category.ordinal())) != 0;
        if (MODE == Mode.VERIFY) {
            verify(level, category, gate, cached);
        }
        return cached;
    }

    /** Upstream 1.5.10 logic, unchanged. */
    private static boolean original(ServerLevel level, MobCategory category) {
        final int interval = IMobCategory.getSpawnInterval(category);
        return interval > 1 && level.getGameTime() % interval != 0;
    }

    private static int computeMask(long time) {
        int mask = 0;
        for (MobCategory category : CATEGORIES) {
            final int interval = IMobCategory.getSpawnInterval(category);
            if (interval > 1 && time % interval != 0) {
                mask |= 1 << category.ordinal();
            }
        }
        if (SELFTEST) {
            mask ^= 1 << MobCategory.MONSTER.ordinal();
        }
        return mask;
    }

    private static void verify(ServerLevel level, MobCategory category, ISpawnGateState gate, boolean cached) {
        Counters c = counters(level);
        c.calls.increment();
        long now = level.getGameTime();
        final int interval = IMobCategory.getSpawnInterval(category);
        boolean upstream = interval > 1 && now % interval != 0;
        if (upstream != cached) {
            if (now != gate.servercore$getGateTime()) {
                // Shared (overworld) time moved during this level's pass: upstream itself is not constant here.
                c.drift.increment();
            } else {
                c.mismatch.increment();
            }
        }
        if (!cached) {
            c.pass.increment();
        }
    }

    private static Counters counters(ServerLevel level) {
        return COUNTERS.computeIfAbsent(level.dimension().location().toString(), k -> new Counters());
    }

    private static void report(ServerLevel level) {
        String dim = level.dimension().location().toString();
        Counters c = counters(level);
        ServerCore.LOGGER.info("[servercore-pxc] spawnGate dim={} calls={} pass={} mismatch={} drift={}",
                dim, c.calls.sum(), c.pass.sum(), c.mismatch.sum(), c.drift.sum());
    }

    private static Mode parseMode(String value) {
        try {
            return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Mode.ON;
        }
    }

    private static final class Counters {
        final LongAdder calls = new LongAdder();
        final LongAdder pass = new LongAdder();
        final LongAdder mismatch = new LongAdder();
        final LongAdder drift = new LongAdder();
    }
}
