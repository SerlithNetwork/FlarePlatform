package co.technove.flareplatform.velocity.command;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.velocity.FlarePlatformVelocity;
import co.technove.flareplatform.velocity.config.FlareVelocityConfig;
import co.technove.flareplatform.velocity.manager.ProfilingManager;
import com.google.common.base.Preconditions;
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

public final class FlareCommand {

    private static final String PERMISSION = "airplane.flare";

    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static String PROFILING_URI = "";

    public static BrigadierCommand createBrigadierCommand() {
        final LiteralCommandNode<CommandSource> command = BrigadierCommand.literalArgumentBuilder("flarev")
            .requires(source -> source.hasPermission(PERMISSION))
            .then(BrigadierCommand.literalArgumentBuilder("profiler")
                .then(BrigadierCommand.literalArgumentBuilder("start")
                    .then(BrigadierCommand.literalArgumentBuilder("--cpu").executes(start(ProfileType.CPU)))
                    .then(BrigadierCommand.literalArgumentBuilder("--alloc").executes(start(ProfileType.ALLOC)))
                    .then(BrigadierCommand.literalArgumentBuilder("--lock").executes(start(ProfileType.LOCK)))
                    .then(BrigadierCommand.literalArgumentBuilder("--wall").executes(start(ProfileType.WALL)))
                    .then(BrigadierCommand.literalArgumentBuilder("--ctimer").executes(start(ProfileType.CTIMER)))
                    .executes(start(ProfileType.ITIMER)))
                .then(BrigadierCommand.literalArgumentBuilder("stop")
                    .executes(FlareCommand::executeStop))
                .then(BrigadierCommand.literalArgumentBuilder("status")
                    .executes(FlareCommand::executeStatus)))
            .then(BrigadierCommand.literalArgumentBuilder("reload")
                .executes(FlareCommand::executeReload))
            .then(BrigadierCommand.literalArgumentBuilder("version")
                .executes(FlareCommand::executeVersion))
            .build();

        return new BrigadierCommand(command);
    }

    private static Command<CommandSource> start(final ProfileType type) {
        return ctx -> {
            execute(ctx, type);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static void execute(final CommandContext<CommandSource> ctx, final ProfileType type) {
        if (FlareVelocityConfig.PROFILING.BACKEND_URL.getScheme() == null) {
            sendPrefixed(ctx.getSource(), Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
            return;
        }
        sendPrefixed(ctx.getSource(), Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
        platform().getServer().getScheduler().buildTask(platform(), task -> {
            try {
                if (ProfilingManager.start(type)) {
                    PROFILING_URI = ProfilingManager.getProfilingUri();
                    broadcastPrefixed(
                        Component.text("Flare has been started!", MAIN_COLOR),
                        Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                        Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                        Component.text("  ").append(Component.text("/flarev profiler stop", NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("flarev profiler stop"))),
                        Component.text("Follow its progress here:", NamedTextColor.GRAY),
                        Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                    );
                } else {
                    broadcastPrefixed(
                        Component.text("Can't start a new profiler while another profiler is already active!", NamedTextColor.RED),
                        Component.text("Please stop it using: ", NamedTextColor.RED).append(Component.text("/flarev profiler stop", NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("flarev profiler stop"))),
                        Component.text("before starting a new instance.", NamedTextColor.RED),
                        Component.text("You can follow the current profiler's progress here:", NamedTextColor.GRAY),
                        Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                    );
                }
            } catch (final UserReportableException e) {
                sendPrefixed(ctx.getSource(), Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                if (e.getCause() != null) {
                    platform().getLogger().log(Level.WARNING, "Flare failed to start", e);
                }
            }
        }).schedule();
    }

    private static int executeStop(final CommandContext<CommandSource> ctx) {
        final CommandSource sender = ctx.getSource();
        sendPrefixed(sender, Component.text("Stopping flare...", NamedTextColor.DARK_GRAY));
        platform().getServer().getScheduler().buildTask(platform(), task -> {
            try {
                if (!ProfilingManager.stop()) {
                    sendPrefixed(sender, Component.text("Profiling has already been stopped.", HEX));
                }
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Exception ignored) { // I can't use UserReportableException apparently
                broadcastPrefixed(
                    Component.text("Failed to submit the last batch of data, the profiler will still be available", NamedTextColor.RED)
                );
            }
        }).schedule();
        return Command.SINGLE_SUCCESS;
    }

    private static int executeStatus(final CommandContext<CommandSource> ctx) {
        final Component message = ProfilingManager.isProfiling()
            ? Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan(), HEX)
            : Component.text("Flare is not running.", HEX);
        sendPrefixed(ctx.getSource(), message);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(final CommandContext<CommandSource> ctx) {
        final CommandSource sender = ctx.getSource();
        if (ProfilingManager.isProfiling()) {
            sender.sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_DENIED.getComponent());
            return 0;
        }
        try {
            FlareVelocityConfig.INSTANCE.load();
            sender.sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_SUCCESS.getComponent());
        } catch (final Exception e) {
            sender.sendMessage(FlareVelocityConfig.MESSAGES.PLUGIN_RELOAD_FAILED.getComponent());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeVersion(final CommandContext<CommandSource> ctx) {
        broadcastPrefixed(Component.text(
            "You're running FlarePlatform for Velocity, version " + platform().getContainer().getDescription().getVersion(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(final CommandSource sender, final Component... lines) {
        for (final Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    public static void broadcastPrefixed(final Component... lines) {
        Stream.concat(
                platform().getServer().getAllPlayers().stream().map(CommandSource.class::cast),
                Stream.of(platform().getServer().getConsoleCommandSource())
            )
            .filter(sender -> sender.hasPermission(PERMISSION))
            .forEach(sender -> {
                for (final Component line : lines) {
                    sender.sendMessage(PREFIX.append(line));
                }
            });
    }

    public static void broadcastException() {
        broadcastPrefixed(
            Component.text("An exception happened and profiling has stopped", MAIN_COLOR),
            Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
        );
    }

    private static FlarePlatformVelocity platform() {
        final FlarePlatformVelocity platform = FlarePlatformVelocity.getInstance();
        Preconditions.checkState(platform != null, "The plugin has not yet been enabled");
        return platform;
    }
}
