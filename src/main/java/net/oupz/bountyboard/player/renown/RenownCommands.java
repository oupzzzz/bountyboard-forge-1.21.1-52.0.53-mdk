package net.oupz.bountyboard.player.renown;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenownCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) { register(e.getDispatcher()); }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("renown")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            int total = RenownCapabilityEvents.get(sp).getTotalRenown();
                            ctx.getSource().sendSuccess(() -> Component.literal("Total renown: " + total), false);
                            return 1;
                        })
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int amt = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
                                    RenownCapabilityEvents.get(sp).setTotalRenown(amt);

                                    net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                                            new net.oupz.bountyboard.net.RenownSyncS2C( RenownCapabilityEvents.get(sp).getTotalRenown() ),
                                            sp.connection.getConnection()
                                    );

                                    ctx.getSource().sendSuccess(() -> Component.literal("Set renown to " + amt), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            RenownCapabilityEvents.get(sp).clear();

                            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                                    new net.oupz.bountyboard.net.RenownSyncS2C( 0 ),
                                    sp.connection.getConnection()
                            );

                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared renown & history"), true);
                            return 1;
                        })
                )
        );
    }
}
