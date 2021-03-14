package draylar.sc.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public interface SpawnAccessor {
    void sc_initialize(BlockPos origin, RegistryKey<World> world);
}
