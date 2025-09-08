package net.oupz.bountyboard.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDebugCommands {
    private ClientDebugCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = (CommandDispatcher<CommandSourceStack>) event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal("bbwp");

        root.then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                .executes(ctx -> {
                    var mc = Minecraft.getInstance();
                    if (mc.player == null || mc.level == null) return 0;
                    ClientWaypoint.set(mc.level.dimension(), mc.player.blockPosition());
                    mc.player.displayClientMessage(Component.literal("[bountyboard] waypoint set."), true);
                    return 1;
                })
        );

        root.then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear")
                .executes(ctx -> {
                    ClientWaypoint.clear();
                    var mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.displayClientMessage(Component.literal("[bountyboard] waypoint cleared."), true);
                    }
                    return 1;
                })
        );

        dispatcher.register(root);
    }
}
