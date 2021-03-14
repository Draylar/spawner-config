package draylar.sc;

import draylar.omegaconfig.OmegaConfig;
import draylar.sc.config.SCConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class SpawnerConfig implements ModInitializer {

    public static final Identifier SPAWNER_LOOT_ID = new Identifier("spawnerconfig", "gameplay/spawner");
    public static final SCConfig CONFIG = OmegaConfig.register(SCConfig.class);

    @Override
    public void onInitialize() {

    }
}
