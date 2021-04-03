package draylar.sc.mixin;

import draylar.sc.SpawnerConfig;
import draylar.sc.api.SpawnAccessor;
import draylar.sc.api.SpawnerAccessor;
import draylar.sc.config.SCConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogicMixin implements SpawnerAccessor {

    @Shadow public abstract BlockPos getPos();

    @Shadow public abstract World getWorld();

    @Unique private static final String MAX_DAMAGE_KEY = "MaxDamage";
    @Unique private static final String DAMAGE_KEY = "Damage";
    @Unique private int maxDamage = -1;
    @Unique private int damage = 0;

    @Inject(
            method = "fromTag",
            at = @At("RETURN"))
    private void deserialize(CompoundTag tag, CallbackInfo ci) {
        damage = tag.getInt(DAMAGE_KEY);
        maxDamage = tag.getInt(MAX_DAMAGE_KEY);
    }

    @Inject(
            method = "toTag",
            at = @At("RETURN"))
    private void serialize(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        tag.putInt(DAMAGE_KEY, damage);
        tag.putInt(MAX_DAMAGE_KEY, maxDamage);
    }

    @Inject(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;shouldCreateNewEntityWithPassenger(Lnet/minecraft/entity/Entity;)Z"),
    locals = LocalCapture.CAPTURE_FAILHARD)
    private void onSpawnEntity(CallbackInfo ci, World world, BlockPos blockPos, boolean bl, int i, CompoundTag compoundTag, Optional optional, ListTag listTag, int j, double g, double h, double k, ServerWorld serverWorld, Entity entity) {
        ((SpawnAccessor) entity).sc_initialize(blockPos, world.getRegistryKey());
    }

    @Inject(
            method = "update",
            at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        if(!getWorld().isClient) {
            if(maxDamage != -1) {
                float i = (float) damage / maxDamage;
                getWorld().setBlockBreakingInfo(-1, getPos(), (int) (i * 10)); // progress is 1-10
            }
        }
    }

    @Override
    public void notifyDeath(ServerWorld world) {
        damage++;

        // initialize max damage if needed
        if(maxDamage == -1) {
            maxDamage = SpawnerConfig.CONFIG.maxSpawnerDamage + (SpawnerConfig.CONFIG.spawnerDamageVariance - (SpawnerConfig.CONFIG.spawnerDamageVariance > 0 ? new Random().nextInt(SpawnerConfig.CONFIG.spawnerDamageVariance * 2) : 0));
        }

        // check if spawner should break
        if(SpawnerConfig.CONFIG.breakSpawners && damage >= maxDamage) {
            world.breakBlock(getPos(), false);

            // drop loot if config option is enabled
            if(SpawnerConfig.CONFIG.damagedSpawnersDropExperience) {
                int experience = SpawnerConfig.CONFIG.baseExperience + world.random.nextInt(SpawnerConfig.CONFIG.experienceVariance);

                // drop exp
                while (experience > 0) {
                    int i = ExperienceOrbEntity.roundToOrbSize(experience);
                    experience -= i;
                    world.spawnEntity(new ExperienceOrbEntity(world, (double) getPos().getX() + 0.5D, (double) getPos().getY() + 0.5D, (double) getPos().getZ() + 0.5D, i));
                }
            }


            if(SpawnerConfig.CONFIG.damagedSpawnersDropLoot) {
                // drop loot
                LootTable table = world.getServer().getLootManager().getTable(SpawnerConfig.SPAWNER_LOOT_ID);
                List<ItemStack> stacks = table
                        .generateLoot(new LootContext.Builder(world)
                                .parameter(LootContextParameters.ORIGIN, new Vec3d(getPos().getX(), getPos().getY(), getPos().getZ()))
                                .build(LootContextTypes.CHEST));

                stacks.forEach(stack -> ItemScatterer.spawn(world, getPos().getX(), getPos().getY(), getPos().getZ(), stack));
            }
        }
    }
}
