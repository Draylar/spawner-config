package draylar.sc.mixin;

import draylar.sc.SpawnerConfig;
import net.minecraft.block.Block;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends Block {

    public SpawnerBlockMixin(Settings settings) {
        super(settings);
    }

    @Redirect(
            method = "onStacksDropped",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/SpawnerBlock;dropExperience(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;I)V"))
    private void redirectExperienceDrop(SpawnerBlock spawnerBlock, ServerWorld world, BlockPos pos, int size) {
        if(SpawnerConfig.CONFIG.dropExperience) {
            int experience = SpawnerConfig.CONFIG.baseExperience + world.random.nextInt(SpawnerConfig.CONFIG.experienceVariance);
            dropExperience(world, pos, experience);
        }

        if(SpawnerConfig.CONFIG.dropLoot) {
            // drop loot
            LootTable table = world.getServer().getLootManager().getTable(SpawnerConfig.SPAWNER_LOOT_ID);
            List<ItemStack> stacks = table
                    .generateLoot(new LootContext.Builder(world)
                            .parameter(LootContextParameters.ORIGIN, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                            .build(LootContextTypes.CHEST));

            stacks.forEach(stack -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack));
        }
    }
}
