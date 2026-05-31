package b4.default_commands.client;

import b4.default_commands.CommandEntry;
import b4.default_commands.CommandTrigger;
import b4.default_commands.DefaultCommandsConfig;
import b4.default_commands.DefaultCommandsConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DefaultCommandsConfigScreen extends Screen {
    private static final int LIST_TOP = 44;
    private static final int LIST_ROW_HEIGHT = 20;
    private static final int MIN_ROWS_PER_PAGE = 4;

    private final Screen parent;
    private final DefaultCommandsConfig workingConfig;
    private final List<ButtonWidget> rowButtons = new ArrayList<>();

    private int selectedIndex = -1;
    private int page = 0;
    private int rowsPerPage = 8;
    private boolean allowProtectedEditing = false;

    private ButtonWidget pageBackButton;
    private ButtonWidget pageNextButton;
    private ButtonWidget editButton;
    private ButtonWidget deleteButton;
    private ButtonWidget moveUpButton;
    private ButtonWidget moveDownButton;
    private ButtonWidget toggleEnabledButton;
    private ButtonWidget toggleLockButton;
    private ButtonWidget protectedEditsButton;

    public DefaultCommandsConfigScreen(Screen parent) {
        super(Text.literal("Default commands"));
        this.parent = parent;
        this.workingConfig = DefaultCommandsConfigManager.getConfigCopy();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int listWidth = Math.min(520, Math.max(260, this.width - 40));
        int listLeft = centerX - (listWidth / 2);
        int footerTop = this.height - 28;
        int thirdRowTop = footerTop - 24;
        int secondRowTop = thirdRowTop - 24;
        int buttonTop = secondRowTop - 24;
        int pageControlTop = buttonTop - 26;
        int availableListHeight = Math.max(0, pageControlTop - LIST_TOP - 6);
        this.rowsPerPage = Math.max(MIN_ROWS_PER_PAGE, availableListHeight / LIST_ROW_HEIGHT);

        rowButtons.clear();
        for (int row = 0; row < rowsPerPage; row++) {
            int targetRow = row;
            ButtonWidget rowButton = ButtonWidget.builder(Text.empty(), button -> selectIndex(page * rowsPerPage + targetRow))
                .dimensions(listLeft, LIST_TOP + (row * LIST_ROW_HEIGHT), listWidth, 18)
                .build();
            rowButtons.add(this.addDrawableChild(rowButton));
        }

        this.pageBackButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("<"), button -> {
                if (page > 0) {
                    page--;
                    refreshListButtons();
                }
            }).dimensions(listLeft, pageControlTop, 30, 20).build()
        );

        this.pageNextButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal(">"), button -> {
                if ((page + 1) * rowsPerPage < workingConfig.commands.size()) {
                    page++;
                    refreshListButtons();
                }
            }).dimensions(listLeft + listWidth - 30, pageControlTop, 30, 20).build()
        );

        int gap = 6;
        int buttonWidth = Math.max(84, Math.min(120, (listWidth - (gap * 2)) / 3));
        int groupWidth = (buttonWidth * 3) + (gap * 2);
        int actionLeft = centerX - (groupWidth / 2);
        int col1 = actionLeft;
        int col2 = col1 + buttonWidth + gap;
        int col3 = col2 + buttonWidth + gap;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> openAddScreen())
            .dimensions(col1, buttonTop, buttonWidth, 20).build());
        this.editButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> openEditScreen())
            .dimensions(col2, buttonTop, buttonWidth, 20).build());
        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelected())
            .dimensions(col3, buttonTop, buttonWidth, 20).build());

        this.moveUpButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Move up"), button -> moveSelected(-1))
            .dimensions(col1, secondRowTop, buttonWidth, 20).build());
        this.moveDownButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Move down"), button -> moveSelected(1))
            .dimensions(col2, secondRowTop, buttonWidth, 20).build());
        this.toggleEnabledButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Toggle enabled"), button -> toggleEnabled())
            .dimensions(col3, secondRowTop, buttonWidth, 20).build());

        this.toggleLockButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Toggle lock"), button -> toggleLock())
            .dimensions(col1, thirdRowTop, buttonWidth, 20).build());
        this.protectedEditsButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleProtectedEdits())
            .dimensions(col2, thirdRowTop, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Presets"), button -> openPresetScreen())
            .dimensions(col3, thirdRowTop, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> saveAndClose())
            .dimensions(centerX - 102, footerTop, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.close())
            .dimensions(centerX + 2, footerTop, 100, 20).build());

        refreshListButtons();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Commands run with server-level permissions even when cheats are disabled."),
            this.width / 2,
            28,
            0xA0A0A0
        );

        if (selectedIndex >= 0 && selectedIndex < workingConfig.commands.size()) {
            CommandEntry selectedEntry = workingConfig.commands.get(selectedIndex);
            Text summary = Text.literal(
                "Selected #" + (selectedIndex + 1)
                    + " | Trigger: " + selectedEntry.getResolvedTrigger().getLabel().getString()
                    + " | Enabled: " + yesNo(selectedEntry.enabled)
                    + " | Locked: " + yesNo(selectedEntry.locked)
            );
            context.drawTextWithShadow(this.textRenderer, summary, this.width / 2 - 160, this.height - 66, 0xE0E0E0);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    void addOrUpdateCommand(CommandEntry entry, int editIndex) {
        if (editIndex >= 0 && editIndex < workingConfig.commands.size()) {
            workingConfig.commands.set(editIndex, entry);
            selectedIndex = editIndex;
        } else {
            workingConfig.commands.add(entry);
            selectedIndex = workingConfig.commands.size() - 1;
            page = selectedIndex / rowsPerPage;
        }
        refreshListButtons();
    }

    void replaceCommands(List<CommandEntry> newCommands) {
        workingConfig.commands.clear();
        for (CommandEntry entry : newCommands) {
            workingConfig.commands.add(entry.copy());
        }
        selectedIndex = workingConfig.commands.isEmpty() ? -1 : 0;
        page = 0;
        refreshListButtons();
    }

    DefaultCommandsConfig getWorkingConfig() {
        return workingConfig;
    }

    boolean canModify(CommandEntry entry) {
        return !entry.locked || allowProtectedEditing;
    }

    private void openAddScreen() {
        this.client.setScreen(new EditCommandScreen(this, -1, new CommandEntry()));
    }

    private void openEditScreen() {
        CommandEntry selected = getSelectedEntry();
        if (selected == null || !canModify(selected)) {
            return;
        }
        this.client.setScreen(new EditCommandScreen(this, selectedIndex, selected.copy()));
    }

    private void deleteSelected() {
        CommandEntry selected = getSelectedEntry();
        if (selected == null || !canModify(selected)) {
            return;
        }
        workingConfig.commands.remove(selectedIndex);
        if (workingConfig.commands.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= workingConfig.commands.size()) {
            selectedIndex = workingConfig.commands.size() - 1;
        }
        page = Math.max(0, Math.min(page, getMaxPage()));
        refreshListButtons();
    }

    private void moveSelected(int delta) {
        CommandEntry selected = getSelectedEntry();
        if (selected == null || !canModify(selected)) {
            return;
        }
        int target = selectedIndex + delta;
        if (target < 0 || target >= workingConfig.commands.size()) {
            return;
        }
        CommandEntry moved = workingConfig.commands.remove(selectedIndex);
        workingConfig.commands.add(target, moved);
        selectedIndex = target;
        page = selectedIndex / rowsPerPage;
        refreshListButtons();
    }

    private void toggleEnabled() {
        CommandEntry selected = getSelectedEntry();
        if (selected == null || !canModify(selected)) {
            return;
        }
        selected.enabled = !selected.enabled;
        refreshListButtons();
    }

    private void toggleLock() {
        CommandEntry selected = getSelectedEntry();
        if (selected == null || !canModify(selected)) {
            return;
        }
        selected.locked = !selected.locked;
        refreshListButtons();
    }

    private void toggleProtectedEdits() {
        if (allowProtectedEditing) {
            allowProtectedEditing = false;
            refreshListButtons();
            return;
        }

        this.client.setScreen(new ConfirmScreen(
            confirmed -> {
                this.client.setScreen(this);
                if (confirmed) {
                    allowProtectedEditing = true;
                }
                refreshListButtons();
            },
            Text.literal("Enable protected edits?"),
            Text.literal("This setting lets you edit/delete commands that weren't made for modification. Be cautious of what you do !"),
            Text.literal("Enable"),
            Text.literal("Cancel")
        ));
    }

    private void openPresetScreen() {
        this.client.setScreen(new PresetManagementScreen(this));
    }

    private void saveAndClose() {
        workingConfig.normalize();
        DefaultCommandsConfigManager.setConfig(workingConfig.copy());
        this.close();
    }

    private void refreshListButtons() {
        int total = workingConfig.commands.size();
        int pageStart = page * rowsPerPage;

        for (int row = 0; row < rowButtons.size(); row++) {
            ButtonWidget button = rowButtons.get(row);
            int index = pageStart + row;
            if (index < total) {
                CommandEntry entry = workingConfig.commands.get(index);
                button.visible = true;
                button.active = true;
                button.setMessage(Text.literal((index == selectedIndex ? "> " : "  ") + formatEntryForRow(entry)));
            } else {
                button.visible = false;
                button.active = false;
            }
        }

        this.pageBackButton.active = page > 0;
        this.pageNextButton.active = (page + 1) * rowsPerPage < total;
        this.protectedEditsButton.setMessage(Text.literal("Protected edits: " + onOff(allowProtectedEditing)));

        CommandEntry selectedEntry = getSelectedEntry();
        boolean hasSelection = selectedEntry != null;
        boolean canModify = hasSelection && canModify(selectedEntry);

        this.editButton.active = canModify;
        this.deleteButton.active = canModify;
        this.moveUpButton.active = canModify && selectedIndex > 0;
        this.moveDownButton.active = canModify && selectedIndex >= 0 && selectedIndex < total - 1;
        this.toggleEnabledButton.active = canModify;
        this.toggleLockButton.active = canModify;
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= workingConfig.commands.size()) {
            return;
        }
        selectedIndex = index;
        refreshListButtons();
    }

    private CommandEntry getSelectedEntry() {
        if (selectedIndex < 0 || selectedIndex >= workingConfig.commands.size()) {
            return null;
        }
        return workingConfig.commands.get(selectedIndex);
    }

    private int getMaxPage() {
        if (workingConfig.commands.isEmpty()) {
            return 0;
        }
        return (workingConfig.commands.size() - 1) / rowsPerPage;
    }

    private String formatEntryForRow(CommandEntry entry) {
        String triggerName = switch (entry.getResolvedTrigger()) {
            case ON_SERVER_START -> "START";
            case ON_WORLD_CREATE -> "CREATE";
            case ON_WORLD_JOIN -> "JOIN";
        };
        String commandPreview = entry.command == null ? "" : entry.command;
        if (commandPreview.length() > 29) {
            commandPreview = commandPreview.substring(0, 29) + "...";
        }
        return "[" + triggerName + "] "
            + (entry.enabled ? "" : "[OFF] ")
            + (entry.locked ? "[LOCKED] " : "")
            + commandPreview;
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
