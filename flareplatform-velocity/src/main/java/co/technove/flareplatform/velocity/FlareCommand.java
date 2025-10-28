package co.technove.flareplatform.velocity;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.logging.Level;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> flareCommand = BrigadierCommand.literalArgumentBuilder("flareprofiler")
            .requires(css -> css.hasPermission("airplane.flare.profiler"))
            .then(BrigadierCommand.literalArgumentBuilder("start")
                .requires(source -> source.hasPermission(
                    "airplane.flare.profiler.start"))
                .executes(ctx -> {
                    FlareCommand.execute(ctx, ProfileType.ITIMER);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("alloc")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.ALLOC);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(BrigadierCommand.literalArgumentBuilder("lock")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.LOCK);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(BrigadierCommand.literalArgumentBuilder("wall")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.WALL);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(BrigadierCommand.literalArgumentBuilder("itimer")
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.ITIMER);
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(BrigadierCommand.literalArgumentBuilder("stop")
                .requires(source -> source.hasPermission(
                    "airplane.flare.profiler.stop"))
                .executes(FlareCommand::executeStop))
            .then(BrigadierCommand.literalArgumentBuilder("status")
                .requires(source -> source.hasPermission("airplane.flare.profiler.status"))
                .executes(FlareCommand::executeStatus))
            .then(BrigadierCommand.literalArgumentBuilder("reload")
                .requires(source -> source.hasPermission("airplane.flare.profiler.reload"))
                .executes(FlareCommand::executeReload))
            .then(BrigadierCommand.literalArgumentBuilder("version")
                .requires(source -> source.hasPermission("airplane.flare.profiler.version"))
                .executes(FlareCommand::executeVersion))
            .build();

        return new BrigadierCommand(flareCommand);
    }

    public static void execute(CommandContext<CommandSource> ctx, final ProfileType type) {
        if (FlarePlatformVelocity.getInstance().getFlareURI().getScheme() == null) {
            sendPrefixed(ctx.getSource(), Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
        } else {
            sendPrefixed(ctx.getSource(),
                Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
            FlarePlatformVelocity.getInstance().getServer().getScheduler().buildTask(FlarePlatformVelocity.getInstance(), task -> {
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
                        } else {
                            broadcastPrefixed(
                                Component.text("Can't start a new profiler while another profiler is already active!", NamedTextColor.RED),
                                Component.text("Please stop it using: ", NamedTextColor.RED).append(Component.text(
                                    "/flareprofiler stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("flareprofiler stop"))),
                                Component.text("before starting a new instance.", NamedTextColor.RED),
                                Component.text("You can follow the current profiler's progress here:", NamedTextColor.GRAY),
                                Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                            );
                        }
                    } catch (UserReportableException e) {
                        sendPrefixed(ctx.getSource(),
                            Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                        if (e.getCause() != null) {
                            FlarePlatformVelocity.getInstance().getLogger().log(Level.WARNING, "Flare failed to start", e);
                        }
                    }
                })
                .schedule();
        }
    }

    public static int executeStop(CommandContext<CommandSource> ctx) {
        String profile = ProfilingManager.isProfiling() ? ProfilingManager.getProfilingUri() : null;
        if (ProfilingManager.stop()) {
            broadcastPrefixed(
                Component.text("Profiling has been stopped.", MAIN_COLOR),
                Component.text(profile, HEX).clickEvent(ClickEvent.openUrl(profile))
            );
        } else {
            broadcastPrefixed(
                Component.text("There is no active profiler to disable!", NamedTextColor.RED)
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStatus(CommandContext<CommandSource> ctx) {
        if (ProfilingManager.isProfiling()) {
            sendPrefixed(ctx.getSource(),
                Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan(), HEX));
        } else {
            sendPrefixed(ctx.getSource(),
                Component.text("Flare is not running.", HEX));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeReload(CommandContext<CommandSource> ctx) {
        if (ProfilingManager.isProfiling()) {
            broadcastPrefixed(Component.text("Can't reload configuration while profiling!", NamedTextColor.RED));
        } else {
            FlarePlatformVelocity.getFlareConfig().reloadConfig();
            broadcastPrefixed(Component.text("Configuration has been reloaded.", MAIN_COLOR));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeVersion(CommandContext<CommandSource> ctx) {
        broadcastPrefixed(
            Component.text("You're running FlarePlatform for Velocity, version " + FlarePlatformVelocity.getInstance().getVersion(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(CommandSource sender, Component... lines) {
        for (Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    private static void broadcastPrefixed(Component... lines) {
        Stream.concat(
                FlarePlatformVelocity.getInstance().getServer().getAllPlayers().stream(), Stream.of(FlarePlatformVelocity.getInstance().getServer().getConsoleCommandSource()))
            .filter(s -> s.hasPermission("airplane.flare.profiler"))
            .forEach(s -> {
                for (Component line : lines) {
                    s.sendMessage(PREFIX.append(line));
                }
            });

    }
}
