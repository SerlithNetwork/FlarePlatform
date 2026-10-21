package co.technove.flareplatform.paper.command;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.paper.FlarePlatformPaper;
import co.technove.flareplatform.paper.config.FlarePaperConfig;
import co.technove.flareplatform.paper.manager.ProfilingManager;
import com.google.common.base.Preconditions;
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

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("flarep")
            .requires(css -> css.getSender().hasPermission(PERMISSION))
            .then(Commands.literal("profiler")
                .then(Commands.literal("start")
                    .requires(ignored -> !ProfilingManager.isProfiling())
                    .then(Commands.literal("--cpu").executes(start(ProfileType.CPU)))
                    .then(Commands.literal("--alloc").executes(start(ProfileType.ALLOC)))
                    .then(Commands.literal("--lock").executes(start(ProfileType.LOCK)))
                    .then(Commands.literal("--wall").executes(start(ProfileType.WALL)))
                    .then(Commands.literal("--ctimer").executes(start(ProfileType.CTIMER)))
                    .executes(start(ProfileType.ITIMER)))
                .then(Commands.literal("stop")
                    .requires(ignored -> ProfilingManager.isProfiling())
                    .executes(FlareCommand::executeStop))
                .then(Commands.literal("status")
                    .executes(FlareCommand::executeStatus)))
            .then(Commands.literal("reload")
                .executes(FlareCommand::executeReload))
            .then(Commands.literal("version")
                .executes(FlareCommand::executeVersion))
            .build();
    }

    private static Command<CommandSourceStack> start(final ProfileType type) {
        return ctx -> {
            execute(ctx, type);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static void execute(final CommandContext<CommandSourceStack> ctx, final ProfileType type) {
        if (FlarePaperConfig.PROFILING.BACKEND_URL.getScheme() == null) {
            sendPrefixed(ctx.getSource().getSender(), Component.text("Invalid URL for Flare, check your config.", NamedTextColor.RED));
            return;
        }
        sendPrefixed(ctx.getSource().getSender(), Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY));
        platform().getServer().getAsyncScheduler().runNow(platform(), task -> {
            try {
                if (ProfilingManager.start(type)) {
                    PROFILING_URI = ProfilingManager.getProfilingUri();
                    broadcastPrefixed(
                        Component.text("Flare has been started!", MAIN_COLOR),
                        Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                        Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                        Component.text("  ").append(Component.text("/flarep profiler stop", NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("flarep profiler stop"))),
                        Component.text("Follow its progress here:", NamedTextColor.GRAY),
                        Component.text(PROFILING_URI, HEX).clickEvent(ClickEvent.openUrl(PROFILING_URI))
                    );
                }
            } catch (final UserReportableException e) {
                sendPrefixed(ctx.getSource().getSender(), Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                if (e.getCause() != null) {
                    platform().getLogger().log(Level.WARNING, "Flare failed to start", e);
                }
            }
        });
    }

    private static int executeStop(final CommandContext<CommandSourceStack> ctx) {
        sendPrefixed(ctx.getSource().getSender(), Component.text("Stopping flare...", NamedTextColor.DARK_GRAY));
        platform().getServer().getAsyncScheduler().runNow(platform(), task -> {
            try {
                ProfilingManager.stop();
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Exception ignored) { // I can't use UserReportableException apparently
                broadcastPrefixed(
                    Component.text("Failed to submit the last batch of data, the profiler will still be available", NamedTextColor.RED)
                );
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeStatus(final CommandContext<CommandSourceStack> ctx) {
        final Component message = ProfilingManager.isProfiling()
            ? Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan(), HEX)
            : Component.text("Flare is not running.", HEX);
        sendPrefixed(ctx.getSource().getSender(), message);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        try {
            FlarePaperConfig.INSTANCE.load();
            sender.sendMessage(FlarePaperConfig.MESSAGES.PLUGIN_RELOAD_SUCCESS.getComponent());
        } catch (final Exception e) {
            sender.sendMessage(FlarePaperConfig.MESSAGES.PLUGIN_RELOAD_FAILED.getComponent());
            platform().getSLF4JLogger().error(e.getMessage(), e);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeVersion(final CommandContext<CommandSourceStack> ctx) {
        broadcastPrefixed(Component.text(
            "You're running FlarePlatform for Paper, version " + platform().getPluginMeta().getVersion(), HEX));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendPrefixed(final CommandSender sender, final Component... lines) {
        for (final Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    public static void broadcastPrefixed(final Component... lines) {
        Stream.concat(
                Bukkit.getOnlinePlayers().stream().map(CommandSender.class::cast),
                Stream.of(Bukkit.getConsoleSender())
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

    private static FlarePlatformPaper platform() {
        final FlarePlatformPaper platform = FlarePlatformPaper.getInstance();
        Preconditions.checkState(platform != null, "The plugin has not yet been enabled");
        return platform;
    }
}
