package com.sythel.censor;

import com.sythel.censor.initialization.PluginInitializer;
import com.sythel.censor.service.CensorContext;
import org.bukkit.plugin.java.JavaPlugin;

public final class Censor extends JavaPlugin {

    private CensorContext context;

    @Override
    public void onEnable() {
        context = new PluginInitializer(this).initialize();
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.moderationLogger().close();
        }
    }

    public CensorContext context() {
        return context;
    }
}