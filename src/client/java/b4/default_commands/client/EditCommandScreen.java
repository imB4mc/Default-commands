package b4.default_commands.client;

import b4.default_commands.CommandEntry;
import b4.default_commands.CommandTrigger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class EditCommandScreen extends Screen {
    private final DefaultCommandsConfigScreen parent;
    private final int editIndex;
    private final CommandEntry workingEntry;

    private TextFieldWidget commandField;
    private CommandTrigger trigger;
    private boolean enabled;
    private boolean locked;

    private ButtonWidget triggerButton;
    private ButtonWidget enabledButton;
    private ButtonWidget lockedButton;
    private ButtonWidget saveButton;

    public EditCommandScreen(DefaultCommandsConfigScreen parent, int editIndex, CommandEntry entry) {
        super(Text.literal(editIndex < 0 ? "Add command" : "Edit command"));
        this.parent = parent;
        this.editIndex = editIndex;
        this.workingEntry = entry;
        this.trigger = entry.getResolvedTrigger();
        this.enabled = entry.enabled;
        this.locked = entry.locked;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = Math.min(420, Math.max(220, this.width - 40));
        int left = centerX - (fieldWidth / 2);
        int top = this.height / 2 - 60;

        this.commandField = this.addDrawableChild(new TextFieldWidget(
            this.textRenderer,
            left,
            top,
            fieldWidth,
            20,
            Text.literal("Command")
        ));
        this.commandField.setMaxLength(1024);
        this.commandField.setText(workingEntry.command);
        this.commandField.setPlaceholder(Text.literal("/gamerule doDaylightCycle false"));
        this.commandField.setChangedListener(value -> refreshButtons());

        this.triggerButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            CommandTrigger[] triggers = CommandTrigger.values();
            this.trigger = triggers[(this.trigger.ordinal() + 1) % triggers.length];
            refreshButtons();
        }).dimensions(left, top + 28, fieldWidth, 20).build());

        this.enabledButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            this.enabled = !this.enabled;
            refreshButtons();
        }).dimensions(left, top + 52, fieldWidth, 20).build());

        this.lockedButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            this.locked = !this.locked;
            refreshButtons();
        }).dimensions(left, top + 76, fieldWidth, 20).build());

        this.saveButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
            .dimensions(centerX - 102, top + 112, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
            .dimensions(centerX + 2, top + 112, 100, 20).build());

        refreshButtons();
        this.setInitialFocus(commandField);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 86, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Command"), this.width / 2 - 160, this.height / 2 - 72, 0xE0E0E0);
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Use \"Not made for modification\" to protect entries from accidental edits."),
            this.width / 2 - 160,
            this.height / 2 + 34,
            0xA0A0A0
        );
        super.render(context, mouseX, mouseY, delta);
    }

    private void save() {
        String commandText = commandField.getText() == null ? "" : commandField.getText().trim();
        if (commandText.isBlank()) {
            return;
        }

        workingEntry.command = commandText;
        workingEntry.trigger = trigger.getId();
        workingEntry.enabled = enabled;
        workingEntry.locked = locked;
        workingEntry.normalize();

        parent.addOrUpdateCommand(workingEntry, editIndex);
        close();
    }

    private void refreshButtons() {
        this.triggerButton.setMessage(Text.literal("Trigger: " + trigger.getLabel().getString()));
        this.enabledButton.setMessage(Text.literal("Enabled: " + (enabled ? "Yes" : "No")));
        this.lockedButton.setMessage(Text.literal("Not made for modification: " + (locked ? "Yes" : "No")));
        this.saveButton.active = !commandField.getText().trim().isBlank();
    }
}
