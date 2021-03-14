package draylar.sc.mixin;

import draylar.sc.api.SpawnAccessor;
import draylar.sc.api.SpawnerAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements SpawnAccessor {

    @Unique private static final String POSITION_KEY = "SpawnerPosition";
    @Unique private static final String WORLD_KEY = "SpawnWorld";
    @Nullable @Unique private BlockPos spawnerPosition = null;
    @Nullable @Unique private RegistryKey<World> spawnWorld = null;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(
            method = "writeCustomDataToTag",
            at = @At("RETURN"))
    private void serialize(CompoundTag tag, CallbackInfo ci) {
        if(spawnerPosition != null) {
            tag.putLong(POSITION_KEY, spawnerPosition.asLong());
        }

        if(spawnWorld != null) {
            tag.putString(WORLD_KEY, spawnWorld.getValue().toString());
        }
    }

    @Inject(
            method = "readCustomDataFromTag",
            at = @At("RETURN"))
    private void deserialize(CompoundTag tag, CallbackInfo ci) {
        if(tag.contains(POSITION_KEY)) {
            spawnerPosition = BlockPos.fromLong(tag.getLong(POSITION_KEY));
        }

        if(tag.contains(WORLD_KEY)) {
            spawnWorld = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString(WORLD_KEY)));
        }
    }

    @Inject(
            method = "onDeath",
            at = @At("RETURN"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if(spawnWorld != null && spawnerPosition != null && !world.isClient) {
            ServerWorld sWorld = (ServerWorld) world;
            MinecraftServer server = sWorld.getServer();
            ServerWorld originWorld = server.getWorld(spawnWorld);

            // retrieve spawner from world
            if(originWorld != null) {
                BlockEntity blockEntity = originWorld.getBlockEntity(spawnerPosition);

                if(blockEntity instanceof MobSpawnerBlockEntity) {
                    MobSpawnerBlockEntity spawner = (MobSpawnerBlockEntity) blockEntity;
                    ((SpawnerAccessor) spawner.getLogic()).notifyDeath(sWorld);
                }
            }
        }
    }

    @Override
    public void sc_initialize(BlockPos origin, RegistryKey<World> world) {
        this.spawnerPosition = origin;
        this.spawnWorld = world;
    }
}
