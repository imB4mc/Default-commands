package b4.default_commands;

import java.util.UUID;

public class CommandEntry {
    public String id = UUID.randomUUID().toString();
    public String command = "";
    public String trigger = CommandTrigger.ON_WORLD_JOIN.getId();
    public boolean enabled = true;
    public boolean locked = false;

    public CommandEntry copy() {
        CommandEntry copy = new CommandEntry();
        copy.id = this.id;
        copy.command = this.command;
        copy.trigger = this.trigger;
        copy.enabled = this.enabled;
        copy.locked = this.locked;
        return copy;
    }

    public CommandTrigger getResolvedTrigger() {
        return CommandTrigger.fromId(trigger);
    }

    public void normalize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (command == null) {
            command = "";
        }
        if (trigger == null || trigger.isBlank()) {
            trigger = CommandTrigger.ON_WORLD_JOIN.getId();
        }
    }
}
