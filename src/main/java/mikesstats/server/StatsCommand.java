package mikesstats.server;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

/** /stats for everyone, always the chest menu (also with the client mod). */
public final class StatsCommand {
    private StatsCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                dispatcher.register(Commands.literal("stats").executes(ctx -> {
                    ChestMenus.open(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
                    return 1;
                })));
    }
}
