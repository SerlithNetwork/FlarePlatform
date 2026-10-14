package co.technove.flareplatform.fidorial.command;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.fidorial.FlarePlatformFidorial;
import co.technove.flareplatform.fidorial.config.FlareFidorialConfig;
import co.technove.flareplatform.fidorial.manager.ProfilingManager;
import com.google.common.base.Preconditions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fr.fidorial.command.CommandSender;
import fr.fidorial.command.CommandSource;
import fr.fidorial.command.Commands;
import fr.fidorial.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class FlareCommand {

    public static final String PERMISSION = "airplane.flare";

    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static volatile String PROFILING_URI = "";

    private FlareCommand() {
    }

    public static LiteralCommandNode<CommandSource> createCommand() {
        return Commands.literal("flaref")
            .requires(source -> source.sender().hasPermission(PERMISSION))
            .then(Commands.literal("profiler")
                .then(Commands.literal("start")
                    .requires(_ -> !ProfilingManager.isProfiling())
                    .then(Commands.literal("--cpu").executes(start(ProfileType.CPU)))
                    .then(Commands.literal("--alloc").executes(start(ProfileType.ALLOC)))
                    .then(Commands.literal("--lock").executes(start(ProfileType.LOCK)))
                    .then(Commands.literal("--wall").executes(start(ProfileType.WALL)))
                    .then(Commands.literal("--ctimer").executes(start(ProfileType.CTIMER)))
                    .executes(start(ProfileType.ITIMER)))
                .then(Commands.literal("stop")
                    .requires(_ -> ProfilingManager.isProfiling())
                    .executes(FlareCommand::executeStop))
                .then(Commands.literal("status")
                    .executes(FlareCommand::executeStatus)))
            .then(Commands.literal("reload")
                .executes(FlareCommand::executeReload))
            .then(Commands.literal("version")
                .executes(FlareCommand::executeVersion))
            .build();
    }

    private static Command<CommandSource> start(final ProfileType type) {
        return ctx -> {
            execute(ctx.getSource().sender(), type);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static void execute(final CommandSender sender, final ProfileType type) {
        if (FlareFidorialConfig.PROFILING.BACKEND_URL.getScheme() == null) {
            sendPrefixed(sender, Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
            return;
        }
        sendPrefixed(sender, Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
        runAsync(() -> {
            try {
                if (ProfilingManager.start(type)) {
                    PROFILING_URI = ProfilingManager.getProfilingUri();
                    broadcastPrefixed(
                        Component.text("Flare has been started!", MAIN_COLOR),
                        Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                        Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                        Component.text("  ").append(Component.text("/flarep profiler stop", NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("/flarep profiler stop"))),
                        Component.text("Follow its progress here:", NamedTextColor.GRAY),
                        Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                    );
                }
            } catch (final UserReportableException e) {
                sendPrefixed(sender, Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                if (e.getCause() != null) {
                    platform().context().logger().warn("Flare failed to start", e);
                }
            }
        });
    }

    private static int executeStop(final CommandContext<CommandSource> ctx) {
        sendPrefixed(ctx.getSource().sender(), Component.text("Stopping flare...", NamedTextColor.DARK_GRAY));
        runAsync(() -> {
            try {
                ProfilingManager.stop();
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Exception _) { // I can't use UserReportableException apparently
                broadcastPrefixed(
                    Component.text("Failed to submit the last batch of data, the profiler will still be available", NamedTextColor.RED)
                );
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeStatus(final CommandContext<CommandSource> ctx) {
        final Component message = ProfilingManager.isProfiling()
            ? Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan(), HEX)
            : Component.text("Flare is not running.", HEX);
        sendPrefixed(ctx.getSource().sender(), message);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(final CommandContext<CommandSource> ctx) {
        final CommandSender sender = ctx.getSource().sender();
        try {
            FlareFidorialConfig.INSTANCE.load();
            sender.sendMessage(FlareFidorialConfig.MESSAGES.PLUGIN_RELOAD_SUCCESS.getComponent());
        } catch (final Exception e) {
            sender.sendMessage(FlareFidorialConfig.MESSAGES.PLUGIN_RELOAD_FAILED.getComponent());
            platform().context().logger().error(e.getMessage(), e);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeVersion(final CommandContext<CommandSource> ctx) {
        broadcastPrefixed(Component.text(
            "You're running FlarePlatform for Fidorial, version " + platform().context().meta().version(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(final CommandSender sender, final Component... lines) {
        for (final Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    public static void broadcastPrefixed(final Component... lines) {
        final FlarePlatformFidorial platform = platform();
        for (final Player player : platform.context().server().onlinePlayers()) {
            if (player.hasPermission(PERMISSION)) {
                sendPrefixed(player, lines);
            }
        }
        for (final Component line : lines) {
            platform.context().logger().info(PREFIX.append(line));
        }
    }

    public static void broadcastException() {
        broadcastPrefixed(
            Component.text("An exception happened and profiling has stopped", MAIN_COLOR),
            Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
        );
    }

    private static void runAsync(final Runnable task) {
        Thread.ofVirtual().name("flare-worker").start(task);
    }

    private static FlarePlatformFidorial platform() {
        final FlarePlatformFidorial platform = FlarePlatformFidorial.getInstance();
        Preconditions.checkState(platform != null, "The plugin has not yet been enabled");
        return platform;
    }
}
