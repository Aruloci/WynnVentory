package com.wynnventory.fabric;

import com.wynnventory.Wynnventory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.util.Optional;

public final class WynnventoryFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Optional<ModContainer> wynnventoryOptional = FabricLoader.getInstance().getModContainer(Wynnventory.MOD_ID);
        if (wynnventoryOptional.isEmpty()) {
            Wynnventory.error("Wynnventory not found!");
            return;
        }

        ModContainer wynnventory = wynnventoryOptional.get();

        Wynnventory.init(
                Wynnventory.ModLoader.FABRIC,
                wynnventory.getMetadata().getVersion().getFriendlyString(),
                wynnventory.getMetadata().getName(),
                FabricLoader.getInstance().isDevelopmentEnvironment(),
                new File(wynnventory.getOrigin().getPaths().getFirst().toUri()));
    }
}
