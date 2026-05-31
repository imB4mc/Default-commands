package b4.default_commands;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Default_commands implements ModInitializer {
    public static final String MOD_ID = "default_commands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String WORLD_INIT_MARKER_NAME = "default_commands_world_initialized.marker";
    private static final int JOIN_COMMAND_DELAY_TICKS = 2;

    private final Map<UUID, Integer> pendingJoinCommands = new HashMap<>();
    private boolean pendingWorldCreateCommands = false;

    @Override
    public void onInitialize() {
        DefaultCommandsConfigManager.getConfig();
        DefaultCommandsServerCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(this::handleServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> handleServerStopped());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            pendingJoinCommands.put(handler.player.getUuid(), JOIN_COMMAND_DELAY_TICKS)
        );
        ServerTickEvents.END_SERVER_TICK.register(this::handleServerTick);
    }

    private void handleServerStarted(MinecraftServer server) {
        pendingJoinCommands.clear();
        DefaultCommandExecutor.runForTrigger(server, null, CommandTrigger.ON_SERVER_START);
        pendingWorldCreateCommands = isFirstWorldStart(server);
    }

    private void handleServerStopped() {
        pendingJoinCommands.clear();
        pendingWorldCreateCommands = false;
    }

    private void handleServerTick(MinecraftServer server) {
        if (pendingJoinCommands.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = pendingJoinCommands.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remainingTicks = entry.getValue() - 1;
            if (remainingTicks > 0) {
                entry.setValue(remainingTicks);
                continue;
            }

            iterator.remove();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            if (pendingWorldCreateCommands) {
                DefaultCommandExecutor.runForTrigger(server, player, CommandTrigger.ON_WORLD_CREATE);
                markWorldInitialized(server);
                pendingWorldCreateCommands = false;
            }

            DefaultCommandExecutor.runForTrigger(server, player, CommandTrigger.ON_WORLD_JOIN);
        }
    }

    private boolean isFirstWorldStart(MinecraftServer server) {
        return !Files.exists(getWorldMarkerPath(server));
    }

    private void markWorldInitialized(MinecraftServer server) {
        Path markerPath = getWorldMarkerPath(server);
        try {
            Files.createDirectories(markerPath.getParent());
            Files.writeString(markerPath, "initialized");
        } catch (IOException e) {
            LOGGER.error("Failed to write world initialization marker at {}", markerPath, e);
        }
    }

    private Path getWorldMarkerPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(WORLD_INIT_MARKER_NAME);
    }
}
