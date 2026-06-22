package co.technove.flareplatform.velocity.command;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.velocity.FlarePlatformVelocity;
import co.technove.flareplatform.velocity.config.FlareVelocityConfig;
import co.technove.flareplatform.velocity.manager.ProfilingManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.logging.Level;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class FlareCommand {

    private static final FlarePlatformVelocity PLATFORM = FlarePlatformVelocity.getInstance();
    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();
    private static String PROFILING_URI = "";

    public static BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> flareCommand = BrigadierCommand.literalArgumentBuilder("vflarep")
            .requires(css -> css.hasPermission("airplane.flare"))
            .then(BrigadierCommand.literalArgumentBuilder("profiler")
                .then(BrigadierCommand.literalArgumentBuilder("start")
                    .then(BrigadierCommand.literalArgumentBuilder("--cpu")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx, ProfileType.ITIMER);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(BrigadierCommand.literalArgumentBuilder("--alloc")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx, ProfileType.ALLOC);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(BrigadierCommand.literalArgumentBuilder("--lock")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx, ProfileType.LOCK);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(BrigadierCommand.literalArgumentBuilder("--wall")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx, ProfileType.WALL);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(BrigadierCommand.literalArgumentBuilder("--ctimer")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx, ProfileType.CTIMER);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .executes(ctx -> {
                        FlareCommand.execute(ctx, ProfileType.ITIMER);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(BrigadierCommand.literalArgumentBuilder("stop")
                    .executes(FlareCommand::executeStop))
                .then(BrigadierCommand.literalArgumentBuilder("status")
                    .executes(FlareCommand::executeStatus))
            )
            .then(BrigadierCommand.literalArgumentBuilder("reload")
                .executes(FlareCommand::executeReload))
            .then(BrigadierCommand.literalArgumentBuilder("version")
                .executes(FlareCommand::executeVersion))
            .build();

        return new BrigadierCommand(flareCommand);
    }

    public static void execute(CommandContext<CommandSource> ctx, final ProfileType type) {
        if (FlareVelocityConfig.PROFILING.BACKEND_URL == null) {
            sendPrefixed(ctx.getSource(), Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
        } else {
            sendPrefixed(ctx.getSource(),
                Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
            PLATFORM.getServer().getScheduler().buildTask(PLATFORM, task -> {
                    try {
                        if (ProfilingManager.start(type)) {
                            PROFILING_URI = ProfilingManager.getProfilingUri();
                            broadcastPrefixed(
                                Component.text("Flare has been started!", MAIN_COLOR),
                                Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                                Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                                Component.text("  ").append(Component.text("/vflareprofiler stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("vflareprofiler stop"))),
                                Component.text("Follow its progress here:", NamedTextColor.GRAY),
                                Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                            );
                        } else {
                            broadcastPrefixed(
                                Component.text("Can't start a new profiler while another profiler is already active!", NamedTextColor.RED),
                                Component.text("Please stop it using: ", NamedTextColor.RED).append(Component.text(
                                    "/vflareprofiler stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("vflareprofiler stop"))),
                                Component.text("before starting a new instance.", NamedTextColor.RED),
                                Component.text("You can follow the current profiler's progress here:", NamedTextColor.GRAY),
                                Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                            );
                        }
                    } catch (UserReportableException e) {
                        sendPrefixed(ctx.getSource(),
                            Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                        if (e.getCause() != null) {
                            PLATFORM.getLogger().log(Level.WARNING, "Flare failed to start", e);
                        }
                    }
                })
                .schedule();
        }
    }

    public static int executeStop(CommandContext<CommandSource> ctx) {
        PLATFORM.getServer().getScheduler().buildTask(PLATFORM, task -> {
            if (!ProfilingManager.isProfiling()) {
                broadcastPrefixed(
                    Component.text("There is no active profiler to disable!", NamedTextColor.RED)
                );
            } else {
                if (ProfilingManager.stop()) {
                    broadcastPrefixed(
                        Component.text("Profiling has been stopped.", MAIN_COLOR),
                        Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                    );
                }
            }
        }).schedule();
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
            ctx.getSource().sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_DENIED.getComponent());
        } else {
            try {
                FlareVelocityConfig.INSTANCE.load();
                ctx.getSource().sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_SUCCESS.getComponent());
            } catch (Exception e) {
                ctx.getSource().sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_FAILED.getComponent());
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeVersion(CommandContext<CommandSource> ctx) {
        broadcastPrefixed(
            Component.text("You're running FlarePlatform for Velocity, version " + PLATFORM.getContainer().getDescription().getVersion(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(CommandSource sender, Component... lines) {
        for (Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    private static void broadcastPrefixed(Component... lines) {
        Stream.concat(
                PLATFORM.getServer().getAllPlayers().stream(), Stream.of(PLATFORM.getServer().getConsoleCommandSource()))
            .filter(s -> s.hasPermission("airplane.flare.profiler"))
            .forEach(s -> {
                for (Component line : lines) {
                    s.sendMessage(PREFIX.append(line));
                }
            });

    }

    public static void broadcastException() {
        broadcastPrefixed(
            Component.text("An exception happened and profiling has stopped", MAIN_COLOR),
            Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
        );
    }
}
