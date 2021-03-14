package draylar.sc.api;

import net.minecraft.server.world.ServerWorld;

public interface SpawnerAccessor {
    void notifyDeath(ServerWorld world);
}
