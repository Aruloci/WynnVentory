package com.wynnventory.neoforge;

import com.wynnventory.Wynnventory;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.File;
import java.nio.file.Path;

@Mod(Wynnventory.MOD_ID)
public final class WynnventoryNeoForge {
    public WynnventoryNeoForge() {
            Path path = ModLoadingContext.get()
                    .getActiveContainer()
                    .getModInfo()
                    .getOwningFile()
                    .getFile()
                    .getFilePath();

            File modFile = new File(path.toUri());

            final String modVersion = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
            final String modName    = ModLoadingContext.get().getActiveContainer().getModInfo().getDisplayName();

            Wynnventory.init(
                    Wynnventory.ModLoader.FORGE,
                    modVersion,
                    modName,
                    !FMLEnvironment.production,
                    modFile);
    }
}
