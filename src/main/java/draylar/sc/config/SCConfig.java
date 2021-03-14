package draylar.sc.config;

import draylar.omegaconfig.api.Config;

public class SCConfig implements Config {

    public int maxSpawnerDamage = 100;
    public int spawnerDamageVariance = 25;
    public boolean dropExperience = true;
    public boolean dropLoot = true;
    public int baseExperience = 100;
    public int experienceVariance = 50;
    public boolean damagedSpawnersDropExperience = false;
    public boolean damagedSpawnersDropLoot = false;

    @Override
    public String getName() {
        return "spawner-config";
    }
}
