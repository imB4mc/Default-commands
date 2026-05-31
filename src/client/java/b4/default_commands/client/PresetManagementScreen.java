package b4.default_commands.client;

import b4.default_commands.CommandEntry;
import b4.default_commands.CommandPreset;
import b4.default_commands.DefaultCommandsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PresetManagementScreen extends Screen {
    private final DefaultCommandsConfigScreen parent;
    private final DefaultCommandsConfig config;

    private int selectedIndex = -1;
    private TextFieldWidget nameField;
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;
    private ButtonWidget applyButton;
    private ButtonWidget renameButton;
    private ButtonWidget deleteButton;
    private ButtonWidget lockButton;
    private Text statusMessage = Text.empty();
    private int statusTicksRemaining = 0;

    public PresetManagementScreen(DefaultCommandsConfigScreen parent) {
        super(Text.literal("Command presets"));
        this.parent = parent;
        this.config = parent.getWorkingConfig();
        if (!config.presets.isEmpty()) {
            selectedIndex = 0;
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelWidth = Math.min(420, Math.max(220, this.width - 40));
        int left = centerX - (panelWidth / 2);
        int top = this.height / 2 - 70;

        this.nameField = this.addDrawableChild(new TextFieldWidget(
            this.textRenderer,
            left,
            top,
            panelWidth,
            20,
            Text.literal("Preset name")
        ));
        this.nameField.setMaxLength(64);
        this.nameField.setPlaceholder(Text.literal("Preset name"));
        syncNameFieldToSelection();
        this.setInitialFocus(nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save current as new"), button -> saveNewPreset())
            .dimensions(left, top + 28, panelWidth, 20).build());

        this.previousButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> shiftSelection(-1))
            .dimensions(left, top + 56, 30, 20).build());
        this.nextButton = this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> shiftSelection(1))
            .dimensions(left + panelWidth - 30, top + 56, 30, 20).build());

        this.applyButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply selected"), button -> applySelected())
            .dimensions(left, top + 84, panelWidth, 20).build());
        this.renameButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Rename selected"), button -> renameSelected())
            .dimensions(left, top + 108, panelWidth, 20).build());
        this.lockButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleLockSelected())
            .dimensions(left, top + 132, panelWidth, 20).build());
        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete selected"), button -> deleteSelected())
            .dimensions(left, top + 156, panelWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(centerX - 50, top + 188, 100, 20).build());

        refreshButtons();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return super.keyPressed(input) || this.nameField.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return this.nameField.charTyped(input) || super.charTyped(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 92, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Presets snapshot your full command list, including lock and trigger settings."),
            this.width / 2,
            this.height / 2 - 80,
            0xA0A0A0
        );

        CommandPreset selected = getSelectedPreset();
        if (selected != null) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Selected: " + selected.name + " (" + selected.commands.size() + " command(s))"),
                this.width / 2,
                this.height / 2 - 10,
                0xE0E0E0
            );
        } else {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("No preset selected"),
                this.width / 2,
                this.height / 2 - 10,
                0x808080
            );
        }
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Tip: use Done on the previous screen to save preset changes to file."),
            this.width / 2,
            this.height / 2 + 16,
            0x808080
        );
        if (statusTicksRemaining > 0) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                statusMessage,
                this.width / 2,
                this.height / 2 + 30,
                0xA0FFA0
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (statusTicksRemaining > 0) {
            statusTicksRemaining--;
        }
    }

    private void saveNewPreset() {
        String rawName = nameField.getText() == null ? "" : nameField.getText().trim();
        if (rawName.isBlank()) {
            rawName = "Preset " + (config.presets.size() + 1);
        }

        CommandPreset preset = new CommandPreset();
        preset.name = makeUniqueName(rawName);
        preset.locked = false;
        for (CommandEntry entry : config.commands) {
            preset.commands.add(entry.copy());
        }

        config.presets.add(preset);
        selectedIndex = config.presets.size() - 1;
        syncNameFieldToSelection();
        setStatus("Created preset \"" + preset.name + "\". Use Done to save.");
        refreshButtons();
    }

    private void applySelected() {
        CommandPreset selected = getSelectedPreset();
        if (selected == null) {
            return;
        }

        List<CommandEntry> copiedEntries = new ArrayList<>();
        for (CommandEntry entry : selected.commands) {
            copiedEntries.add(entry.copy());
        }
        parent.replaceCommands(copiedEntries);
        setStatus("Applied preset \"" + selected.name + "\". Use Done to save.");
        close();
    }

    private void renameSelected() {
        CommandPreset selected = getSelectedPreset();
        if (selected == null || selected.locked) {
            return;
        }

        String rawName = nameField.getText() == null ? "" : nameField.getText().trim();
        if (rawName.isBlank()) {
            return;
        }

        selected.name = makeUniqueName(rawName, selectedIndex);
        syncNameFieldToSelection();
        setStatus("Renamed selected preset. Use Done to save.");
        refreshButtons();
    }

    private void toggleLockSelected() {
        CommandPreset selected = getSelectedPreset();
        if (selected == null) {
            return;
        }
        selected.locked = !selected.locked;
        setStatus("Preset protection is now " + (selected.locked ? "enabled." : "disabled.") + " Use Done to save.");
        refreshButtons();
    }

    private void deleteSelected() {
        CommandPreset selected = getSelectedPreset();
        if (selected == null || selected.locked) {
            return;
        }

        config.presets.remove(selectedIndex);
        if (config.presets.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= config.presets.size()) {
            selectedIndex = config.presets.size() - 1;
        }
        syncNameFieldToSelection();
        setStatus("Deleted selected preset. Use Done to save.");
        refreshButtons();
    }

    private void shiftSelection(int delta) {
        if (config.presets.isEmpty()) {
            selectedIndex = -1;
            refreshButtons();
            return;
        }

        selectedIndex += delta;
        if (selectedIndex < 0) {
            selectedIndex = config.presets.size() - 1;
        } else if (selectedIndex >= config.presets.size()) {
            selectedIndex = 0;
        }
        syncNameFieldToSelection();
        refreshButtons();
    }

    private CommandPreset getSelectedPreset() {
        if (selectedIndex < 0 || selectedIndex >= config.presets.size()) {
            return null;
        }
        return config.presets.get(selectedIndex);
    }

    private void refreshButtons() {
        boolean hasPresets = !config.presets.isEmpty();
        CommandPreset selected = getSelectedPreset();
        boolean hasSelection = selected != null;
        boolean canModifySelected = hasSelection && !selected.locked;

        this.previousButton.active = hasPresets;
        this.nextButton.active = hasPresets;
        this.applyButton.active = hasSelection;
        this.renameButton.active = canModifySelected;
        this.deleteButton.active = canModifySelected;
        this.lockButton.active = hasSelection;
        this.lockButton.setMessage(Text.literal(
            hasSelection
                ? "Not made for modification: " + (selected.locked ? "Yes" : "No")
                : "Not made for modification: n/a"
        ));
    }

    private String makeUniqueName(String desiredName) {
        return makeUniqueName(desiredName, -1);
    }

    private String makeUniqueName(String desiredName, int ignoredIndex) {
        String candidate = desiredName;
        int suffix = 2;
        while (nameExists(candidate, ignoredIndex)) {
            candidate = desiredName + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean nameExists(String name, int ignoredIndex) {
        for (int i = 0; i < config.presets.size(); i++) {
            if (i == ignoredIndex) {
                continue;
            }
            if (config.presets.get(i).name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void syncNameFieldToSelection() {
        if (this.nameField == null) {
            return;
        }
        CommandPreset selected = getSelectedPreset();
        this.nameField.setText(selected == null ? "" : selected.name);
    }

    private void setStatus(String message) {
        this.statusMessage = Text.literal(message);
        this.statusTicksRemaining = 120;
    }
}
