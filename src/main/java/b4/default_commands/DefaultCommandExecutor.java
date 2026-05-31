package b4.default_commands;

import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DefaultCommandExecutor {
    private DefaultCommandExecutor() {
    }

    public static void runForTrigger(MinecraftServer server, ServerPlayerEntity player, CommandTrigger trigger) {
        DefaultCommandsConfig config = DefaultCommandsConfigManager.getConfig();
        int executedCount = 0;

        for (CommandEntry entry : config.commands) {
            if (!entry.enabled || entry.getResolvedTrigger() != trigger) {
                continue;
            }
            String normalizedCommand = normalizeCommand(entry.command);
            if (normalizedCommand.isBlank()) {
                continue;
            }

            ServerCommandSource source = player != null
                ? player.getCommandSource()
                : server.getCommandSource();
            final boolean[] successful = {false};
            final int[] returnValue = {0};
            source = source
                .withPermissions(LeveledPermissionPredicate.OWNERS)
                .withReturnValueConsumer((resultSuccessful, resultValue) -> {
                    successful[0] = resultSuccessful;
                    returnValue[0] = resultValue;
                })
                .withSilent();

            try {
                server.getCommandManager().parseAndExecute(source, normalizedCommand);
                executedCount++;

                if (!successful[0]) {
                    Default_commands.LOGGER.warn(
                        "Default command '{}' reported failure for trigger {} (result={}).",
                        normalizedCommand,
                        trigger.getId(),
                        returnValue[0]
                    );
                }
            } catch (Exception exception) {
                Default_commands.LOGGER.error(
                    "Failed to execute default command '{}' for trigger {}",
                    normalizedCommand,
                    trigger.getId(),
                    exception
                );
            }
        }

        if (executedCount > 0) {
            Default_commands.LOGGER.info(
                "Executed {} default command(s) for trigger {}.",
                executedCount,
                trigger.getId()
            );
        }
    }

    private static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
