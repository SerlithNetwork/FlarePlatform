package co.technove.flareplatform.paper;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.logging.Level;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FlareCommand {

    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("flareprofiler")
            .requires(css -> css.getSender().hasPermission("airplane.flare.profiler"))
            .then(Commands.literal("start")
                .requires(source -> !ProfilingManager.isProfiling() && source.getSender().hasPermission(
                    "airplane.flare.profiler.start"))
                .executes(ctx -> {
                    FlareCommand.execute(ctx, ProfileType.ITIMER);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("alloc")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.ALLOC);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("lock")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.LOCK);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("wall")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.WALL);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("itimer")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.ITIMER);
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("stop")
                .requires(source -> ProfilingManager.isProfiling() && source.getSender().hasPermission(
                    "airplane.flare.profiler.stop"))
                .executes(FlareCommand::executeStop))
            .then(Commands.literal("status")
                .requires(source -> source.getSender().hasPermission("airplane.flare.profiler.status"))
                .executes(FlareCommand::executeStatus))
            .then(Commands.literal("reload")
                .requires(source -> !ProfilingManager.isProfiling() && source.getSender().hasPermission("airplane.flare.profiler.reload"))
                .executes(FlareCommand::executeReload))
            .then(Commands.literal("version")
                .requires(source -> source.getSender().hasPermission("airplane.flare.profiler.version"))
                .executes(FlareCommand::executeVersion))
            .build();
    }

    public static void execute(CommandContext<CommandSourceStack> ctx, final ProfileType type) {
        if (FlarePlatformPaper.getInstance().getFlareURI().getScheme() == null) {
            sendPrefixed(ctx.getSource().getSender(), Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
        } else {
            sendPrefixed(ctx.getSource().getSender(),
                Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
            FlarePlatformPaper.getInstance().getServer().getAsyncScheduler().runNow(FlarePlatformPaper.getInstance(), task -> {
                try {
                    if (ProfilingManager.start(type)) {
                        broadcastPrefixed(
                            Component.text("Flare has been started!", MAIN_COLOR),
                            Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                            Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                            Component.text("  ").append(Component.text("/flareprofiler stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("flareprofiler stop"))),
                            Component.text("Follow its progress here:", NamedTextColor.GRAY),
                            Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                        );
                    }
                } catch (UserReportableException e) {
                    sendPrefixed(ctx.getSource().getSender(),
                        Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                    if (e.getCause() != null) {
                        FlarePlatformPaper.getInstance().getLogger().log(Level.WARNING, "Flare failed to start", e);
                    }
                }
            });
        }
    }

    public static int executeStop(CommandContext<CommandSourceStack> ctx) {
        String profile = ProfilingManager.getProfilingUri();
        if (ProfilingManager.stop()) {
            broadcastPrefixed(
                Component.text("Profiling has been stopped.", MAIN_COLOR),
                Component.text(profile, HEX).clickEvent(ClickEvent.openUrl(profile))
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        if (ProfilingManager.isProfiling()) {
            sendPrefixed(ctx.getSource().getSender(),
                Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan(), HEX));
        } else {
            sendPrefixed(ctx.getSource().getSender(),
                Component.text("Flare is not running.", HEX));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeReload(CommandContext<CommandSourceStack> ctx) {
        FlarePlatformPaper.getInstance().reloadConfig();
        broadcastPrefixed(Component.text("Configuration has been reloaded.", MAIN_COLOR));
        return Command.SINGLE_SUCCESS;
    }

    public static int executeVersion(CommandContext<CommandSourceStack> ctx) {
        broadcastPrefixed(
            Component.text("You're running FlarePlatform for Paper, version " + FlarePlatformPaper.getInstance().getPluginMeta().getVersion(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(CommandSender sender, Component... lines) {
        for (Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    private static void broadcastPrefixed(Component... lines) {
        Stream.concat(
                Bukkit.getOnlinePlayers().stream(), Stream.of(Bukkit.getConsoleSender()))
            .filter(s -> s.hasPermission("airplane.flare.profiler"))
            .forEach(s -> {
                for (Component line : lines) {
                    s.sendMessage(PREFIX.append(line));
                }
            });

    }
}
