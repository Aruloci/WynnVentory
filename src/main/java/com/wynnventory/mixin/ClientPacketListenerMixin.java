package com.wynnventory.mixin;

import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynnventory.api.WynnventoryAPI;
import com.wynnventory.util.ModUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    private static final WynnventoryAPI API = new WynnventoryAPI();

    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(
            method = "handleContainerSetSlot(Lnet/minecraft/network/protocol/game/ClientboundContainerSetSlotPacket;)V",
            at = @At("HEAD")
    )
    private void handleContainerSetSlotPre(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        ContainerSetSlotEvent event = new ContainerSetSlotEvent.Pre(packet.getContainerId(), packet.getStateId(), packet.getSlot(), packet.getItem());
        MixinHelper.post(event);

        API.sendTradeMarketResults(packet.getItem());
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void onPlayerJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        ModUpdater.checkForUpdates();
    }
}