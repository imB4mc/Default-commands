package b4.default_commands;

import java.util.ArrayList;
import java.util.List;

public class CommandPreset {
    public String name = "";
    public boolean locked = false;
    public List<CommandEntry> commands = new ArrayList<>();

    public CommandPreset copy() {
        CommandPreset copy = new CommandPreset();
        copy.name = this.name;
        copy.locked = this.locked;
        for (CommandEntry commandEntry : commands) {
            copy.commands.add(commandEntry.copy());
        }
        return copy;
    }

    public void normalize() {
        if (name == null) {
            name = "";
        }
        if (commands == null) {
            commands = new ArrayList<>();
        }
        for (CommandEntry commandEntry : commands) {
            commandEntry.normalize();
        }
    }
}
