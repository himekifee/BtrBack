package net.himeki.btrback.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "btrback")
public class BtrBackConfig implements ConfigData {
    public int backupDays = 0;
    public int backupHours = 0;
    public int backupMinutes = 30;
    public int autoPurgeDays = 7;
    public int autoPurgeHours = 0;
    public int autoPurgeMinutes = 0;

    @Override
    public void validatePostLoad() throws ValidationException {
        if (backupDays >= 0 && backupHours >= 0 && backupMinutes >= 0)
            if (autoPurgeDays >= 0 && autoPurgeHours >= 0 && autoPurgeMinutes >= 0) {
                    return;
            }
        throw new ValidationException("Failed to validate btrback config.");
    }
}

