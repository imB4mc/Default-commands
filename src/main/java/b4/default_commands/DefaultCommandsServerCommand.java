package b4.default_commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class DefaultCommandsServerCommand {
    private static final PermissionLevel OPERATOR_PERMISSION_LEVEL = PermissionLevel.GAMEMASTERS;

    private DefaultCommandsServerCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("defaultcommands")
                .requires(DefaultCommandsServerCommand::hasOperatorPermission)
                .executes(context -> listServerStartCommands(context.getSource()))
                .then(literal("list")
                    .executes(context -> listServerStartCommands(context.getSource())))
                .then(literal("create")
                    .then(argument("command", StringArgumentType.greedyString())
                        .executes(context -> createServerStartCommand(
                            context.getSource(),
                            StringArgumentType.getString(context, "command")
                        ))))
                .then(literal("add")
                    .then(argument("command", StringArgumentType.greedyString())
                        .executes(context -> createServerStartCommand(
                            context.getSource(),
                            StringArgumentType.getString(context, "command")
                        ))))
                .then(literal("edit")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .then(argument("command", StringArgumentType.greedyString())
                            .executes(context -> editServerStartCommand(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "index"),
                                StringArgumentType.getString(context, "command")
                            )))))
                .then(literal("modify")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .then(argument("command", StringArgumentType.greedyString())
                            .executes(context -> editServerStartCommand(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "index"),
                                StringArgumentType.getString(context, "command")
                            )))))
                .then(literal("delete")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> deleteServerStartCommand(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index")
                        ))))
                .then(literal("remove")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> deleteServerStartCommand(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index")
                        ))))
                .then(literal("enable")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> setServerStartCommandEnabled(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            true
                        ))))
                .then(literal("disable")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> setServerStartCommandEnabled(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            false
                        ))))
                .then(literal("lock")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> setServerStartCommandLocked(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            true
                        ))))
                .then(literal("unlock")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> setServerStartCommandLocked(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            false
                        ))))
                .then(literal("moveup")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> moveServerStartCommand(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            -1
                        ))))
                .then(literal("movedown")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> moveServerStartCommand(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "index"),
                            1
                        ))))
                .then(literal("run")
                    .executes(context -> runServerStartCommands(context.getSource())))
            )
        );
    }

    private static int listServerStartCommands(ServerCommandSource source) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        List<IndexedCommandEntry> commands = getServerStartCommands(config);

        if (commands.isEmpty()) {
            sendFeedback(source, "No server-start default commands are configured.");
            sendUsage(source);
            return 0;
        }

        sendFeedback(source, "Server-start default commands:");
        for (int i = 0; i < commands.size(); i++) {
            CommandEntry entry = commands.get(i).entry();
            sendFeedback(source, (i + 1) + ". "
                + (entry.enabled ? "" : "[disabled] ")
                + (entry.locked ? "[locked] " : "")
                + normalizeCommandForDisplay(entry.command));
        }
        sendUsage(source);
        return commands.size();
    }

    private static int createServerStartCommand(ServerCommandSource source, String command) {
        String normalizedCommand = normalizeCommandInput(command);
        if (normalizedCommand.isBlank()) {
            source.sendError(Text.literal("Command cannot be blank."));
            return 0;
        }

        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        CommandEntry entry = new CommandEntry();
        entry.command = normalizedCommand;
        entry.trigger = CommandTrigger.ON_SERVER_START.getId();
        entry.enabled = true;
        entry.locked = false;
        entry.normalize();

        config.commands.add(entry);
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Created server-start default command #" + getServerStartCommands(config).size() + ": " + entry.command);
        return Command.SINGLE_SUCCESS;
    }

    private static int editServerStartCommand(ServerCommandSource source, int listIndex, String command) {
        String normalizedCommand = normalizeCommandInput(command);
        if (normalizedCommand.isBlank()) {
            source.sendError(Text.literal("Command cannot be blank."));
            return 0;
        }

        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        IndexedCommandEntry target = getServerStartCommand(source, config, listIndex);
        if (target == null || !requireUnlocked(source, target.entry())) {
            return 0;
        }

        target.entry().command = normalizedCommand;
        target.entry().normalize();
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Updated server-start default command #" + listIndex + ": " + target.entry().command);
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteServerStartCommand(ServerCommandSource source, int listIndex) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        IndexedCommandEntry target = getServerStartCommand(source, config, listIndex);
        if (target == null || !requireUnlocked(source, target.entry())) {
            return 0;
        }

        String removedCommand = target.entry().command;
        config.commands.remove(target.configIndex());
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Deleted server-start default command #" + listIndex + ": " + normalizeCommandForDisplay(removedCommand));
        return Command.SINGLE_SUCCESS;
    }

    private static int setServerStartCommandEnabled(ServerCommandSource source, int listIndex, boolean enabled) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        IndexedCommandEntry target = getServerStartCommand(source, config, listIndex);
        if (target == null || !requireUnlocked(source, target.entry())) {
            return 0;
        }

        target.entry().enabled = enabled;
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Server-start default command #" + listIndex + " is now " + (enabled ? "enabled." : "disabled."));
        return Command.SINGLE_SUCCESS;
    }

    private static int setServerStartCommandLocked(ServerCommandSource source, int listIndex, boolean locked) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        IndexedCommandEntry target = getServerStartCommand(source, config, listIndex);
        if (target == null) {
            return 0;
        }

        target.entry().locked = locked;
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Server-start default command #" + listIndex + " is now " + (locked ? "locked." : "unlocked."));
        return Command.SINGLE_SUCCESS;
    }

    private static int moveServerStartCommand(ServerCommandSource source, int listIndex, int delta) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfigCopy();
        List<IndexedCommandEntry> commands = getServerStartCommands(config);
        int currentPosition = listIndex - 1;
        int targetPosition = currentPosition + delta;

        if (currentPosition < 0 || currentPosition >= commands.size()) {
            sendInvalidIndex(source, commands.size());
            return 0;
        }
        if (!requireUnlocked(source, commands.get(currentPosition).entry())) {
            return 0;
        }
        if (targetPosition < 0 || targetPosition >= commands.size()) {
            source.sendError(Text.literal("Command #" + listIndex + " cannot move " + (delta < 0 ? "up." : "down.")));
            return 0;
        }

        Collections.swap(
            config.commands,
            commands.get(currentPosition).configIndex(),
            commands.get(targetPosition).configIndex()
        );
        DefaultCommandsConfigManager.setConfig(config);
        sendFeedback(source, "Moved server-start default command #" + listIndex + " " + (delta < 0 ? "up." : "down."));
        return Command.SINGLE_SUCCESS;
    }

    private static int runServerStartCommands(ServerCommandSource source) {
        DefaultCommandExecutor.runForTrigger(source.getServer(), null, CommandTrigger.ON_SERVER_START);
        sendFeedback(source, "Ran enabled server-start default commands.");
        return Command.SINGLE_SUCCESS;
    }

    private static IndexedCommandEntry getServerStartCommand(ServerCommandSource source, DefaultCommandsConfig config, int listIndex) {
        List<IndexedCommandEntry> commands = getServerStartCommands(config);
        if (listIndex < 1 || listIndex > commands.size()) {
            sendInvalidIndex(source, commands.size());
            return null;
        }
        return commands.get(listIndex - 1);
    }

    private static List<IndexedCommandEntry> getServerStartCommands(DefaultCommandsConfig config) {
        List<IndexedCommandEntry> commands = new ArrayList<>();
        for (int i = 0; i < config.commands.size(); i++) {
            CommandEntry entry = config.commands.get(i);
            if (entry.getResolvedTrigger() == CommandTrigger.ON_SERVER_START) {
                commands.add(new IndexedCommandEntry(i, entry));
            }
        }
        return commands;
    }

    private static boolean requireUnlocked(ServerCommandSource source, CommandEntry entry) {
        if (!entry.locked) {
            return true;
        }
        source.sendError(Text.literal("That command is locked. Use /defaultcommands unlock <index> first."));
        return false;
    }

    private static void sendInvalidIndex(ServerCommandSource source, int commandCount) {
        source.sendError(Text.literal("Invalid server-start command index. Valid range: 1-" + Math.max(commandCount, 1) + "."));
    }

    private static void sendUsage(ServerCommandSource source) {
        sendFeedback(source, "Usage: /defaultcommands create <command>");
        sendFeedback(source, "Usage: /defaultcommands edit <index> <command>");
        sendFeedback(source, "Usage: /defaultcommands delete <index>");
        sendFeedback(source, "Other: enable, disable, lock, unlock, moveup, movedown, run");
    }

    private static void sendFeedback(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }

    private static boolean hasOperatorPermission(ServerCommandSource source) {
        PermissionPredicate permissions = source.getPermissions();
        if (permissions == PermissionPredicate.ALL) {
            return true;
        }
        if (permissions instanceof LeveledPermissionPredicate leveledPermissions) {
            return leveledPermissions.getLevel().isAtLeast(OPERATOR_PERMISSION_LEVEL);
        }
        return false;
    }

    private static String normalizeCommandInput(String command) {
        return command == null ? "" : command.trim();
    }

    private static String normalizeCommandForDisplay(String command) {
        String normalized = normalizeCommandInput(command);
        return normalized.isBlank() ? "<blank>" : normalized;
    }

    private record IndexedCommandEntry(int configIndex, CommandEntry entry) {
    }
}
