package b4.default_commands;

import net.minecraft.text.Text;

public enum CommandTrigger {
    ON_SERVER_START("on_server_start", "On server start"),
    ON_WORLD_CREATE("on_world_create", "On world create"),
    ON_WORLD_JOIN("on_world_join", "On world join");

    private final String id;
    private final String label;

    CommandTrigger(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public Text getLabel() {
        return Text.literal(label);
    }

    public static CommandTrigger fromId(String id) {
        for (CommandTrigger trigger : values()) {
            if (trigger.id.equalsIgnoreCase(id)) {
                return trigger;
            }
        }
        return ON_WORLD_JOIN;
    }
}
