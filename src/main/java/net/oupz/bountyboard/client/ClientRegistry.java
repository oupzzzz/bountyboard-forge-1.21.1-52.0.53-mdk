package net.oupz.bountyboard.client;

import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.client.model.BountyBoardModel;
import net.oupz.bountyboard.client.renderer.BountyBoardBlockEntityRenderer;
import net.oupz.bountyboard.client.screen.BountyBoardScreen;
import net.oupz.bountyboard.registry.BlockEntityRegistry;
import net.oupz.bountyboard.registry.MenuRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistry {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.BOUNTY_BOARD_BE.get(), BountyBoardBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BountyBoardModel.LAYER_LOCATION, BountyBoardModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MenuRegistry.BOUNTY_BOARD_MENU.get(), BountyBoardScreen::new);
        });
    }
}
