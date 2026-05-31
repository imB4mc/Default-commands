package b4.default_commands;

import java.util.ArrayList;
import java.util.List;

public class DefaultCommandsConfig {
    public int schemaVersion = 1;
    public List<CommandEntry> commands = new ArrayList<>();
    public List<CommandPreset> presets = new ArrayList<>();

    public DefaultCommandsConfig copy() {
        DefaultCommandsConfig copy = new DefaultCommandsConfig();
        copy.schemaVersion = this.schemaVersion;
        for (CommandEntry commandEntry : commands) {
            copy.commands.add(commandEntry.copy());
        }
        for (CommandPreset preset : presets) {
            copy.presets.add(preset.copy());
        }
        return copy;
    }

    public void normalize() {
        if (commands == null) {
            commands = new ArrayList<>();
        }
        if (presets == null) {
            presets = new ArrayList<>();
        }
        for (CommandEntry entry : commands) {
            entry.normalize();
        }
        for (CommandPreset preset : presets) {
            preset.normalize();
        }
    }
}
