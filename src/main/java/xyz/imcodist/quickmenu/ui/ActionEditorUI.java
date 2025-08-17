package xyz.imcodist.quickmenu.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import xyz.imcodist.quickmenu.data.ActionButtonData;
import xyz.imcodist.quickmenu.data.command_actions.BaseActionData;
import xyz.imcodist.quickmenu.data.command_actions.CommandActionData;
import xyz.imcodist.quickmenu.data.command_actions.KeybindActionData;
import xyz.imcodist.quickmenu.other.ActionButtonDataHandler;
import xyz.imcodist.quickmenu.ui.components.QuickMenuButton;
import xyz.imcodist.quickmenu.ui.popups.ActionPickerUI;
import xyz.imcodist.quickmenu.ui.popups.ItemPickerUI;
import xyz.imcodist.quickmenu.ui.popups.KeybindPickerUI;
import xyz.imcodist.quickmenu.ui.surfaces.SwitcherSurface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class ActionEditorUI extends BaseOwoScreen<FlowLayout> {
    ActionButtonData actionButtonData = new ActionButtonData();
    boolean newAction = true;

    FlowLayout actionsLayout;
    ArrayList<Component> actionsSource = new ArrayList<>();
    ArrayList<Component> actionTicksSource = new ArrayList<>();
    ArrayList<BaseActionData> actionArray = new ArrayList<>();

    private OverlayContainer<FlowLayout> pickerUI;

    private TextBoxComponent customModelDataTextBox;
    private ButtonComponent keybindButton;
    private boolean settingKeybind = false;
    private boolean boundKeybind = false;
    private ArrayList<Integer> keybind = new ArrayList<>();

    public BaseOwoScreen<FlowLayout> previousScreen;

    public ActionEditorUI(ActionButtonData action) {
        if (action != null) {
            actionButtonData = action;

            actionArray = new ArrayList<>(actionButtonData.actions);

            keybind = new ArrayList<>(actionButtonData.keybind);
            if (keybind.size() >= 4) boundKeybind = true;

            newAction = false;
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // Setup root.
        rootComponent
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // Set up the main layout.
        int mainLayoutHeight = 206;
        FlowLayout mainLayout = Containers.verticalFlow(Sizing.fixed(330), Sizing.fixed(mainLayoutHeight));
        mainLayout.surface(new SwitcherSurface());
        mainLayout.zIndex(-200);
        rootComponent.child(mainLayout);

        // Set up the header.
        int headerLayoutHeight = 6*4;
        FlowLayout headerLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(headerLayoutHeight));
        headerLayout
                .surface(new SwitcherSurface(true))
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        mainLayout.child(headerLayout);

        LabelComponent headerLabel = Components.label(Text.translatable("menu.editor.title"));
        headerLayout.child(headerLabel);

        // Set up the property containers
        FlowLayout propertiesLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.content());

        int propertiesScrollHeight = mainLayoutHeight - headerLayoutHeight;
        ScrollContainer<Component> propertiesScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(propertiesScrollHeight), propertiesLayout);
        propertiesScroll.padding(Insets.of(5, 5, 8, 3));
        mainLayout.child(propertiesScroll);

        // Name property
        FlowLayout nameProperty = createNewProperty("name");
        nameProperty.padding(nameProperty.padding().get().withTop(0));
        TextBoxComponent nameTextBox = Components.textBox(Sizing.fixed(100), actionButtonData.name);
        nameTextBox.cursorStyle(CursorStyle.TEXT);

        nameProperty.child(nameTextBox);
        propertiesLayout.child(nameProperty);

        // Icon property
        FlowLayout iconProperty = createNewProperty("icon");
        QuickMenuButton iconButton = new QuickMenuButton(actionButtonData.icon, (buttonComponent) -> {
            QuickMenuButton quickMenuButton = (QuickMenuButton) buttonComponent;

            ItemPickerUI itemPicker = new ItemPickerUI();
            itemPicker.selectedItem = quickMenuButton.itemIcon;
            itemPicker.customModelData = getCustomModelData(quickMenuButton.itemIcon);

            quickMenuButton.itemIcon = null;

            itemPicker.onSelectedItem = (item) -> {
                quickMenuButton.itemIcon = item;
                updateCustomModelData(quickMenuButton.itemIcon);
            };

            rootComponent.child(itemPicker);
            pickerUI = itemPicker;
        }, (quickMenuButton) -> {});

        iconProperty.child(iconButton);
        propertiesLayout.child(iconProperty);

        // Actions
        createActions(propertiesLayout);

        // Advanced Options
        FlowLayout advancedLayout = Containers.collapsible(Sizing.fill(100), Sizing.content(), Text.translatable("menu.editor.advanced"), false);
        advancedLayout
                .padding(Insets.of(4, 0, -5, 5))
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout keybindProperty = createNewProperty("keybind", false);
        advancedLayout.child(keybindProperty);

        keybindButton = Components.button(Text.translatable("menu.editor.not_bound"), (buttonComponent) -> {
            settingKeybind = true;
            updateKeybindButton();
        });
        keybindButton.horizontalSizing(Sizing.fixed(80));
        keybindProperty.child(keybindButton);

        updateKeybindButton();

        FlowLayout customModelDataProperty = createNewProperty("custommodeldata", false);
        advancedLayout.child(customModelDataProperty);

        Integer customModelData = getCustomModelData(iconButton.itemIcon);
        String cmdText = customModelData != 0 ? customModelData.toString() : "";

        customModelDataTextBox = Components.textBox(Sizing.fixed(75), cmdText);
        customModelDataTextBox.cursorStyle(CursorStyle.TEXT);

        customModelDataTextBox.onChanged().subscribe((text) -> {
            customModelDataTextBox.setText(text.replaceAll("^0+|\\D", ""));
            updateCustomModelData(iconButton.itemIcon);
        });

        customModelDataProperty.child(customModelDataTextBox);

        propertiesLayout.child(advancedLayout);

        // Add padding to the last property in the advanced layout.
        customModelDataProperty.padding(customModelDataProperty.padding().get().add(0, 6, 0, 0));

        // Set up the editor buttons.
        FlowLayout buttonsLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        buttonsLayout
                .surface(Surface.DARK_PANEL)
                .padding(Insets.of(6))
                .margins(Insets.of(10, 0, 0, 0));

        rootComponent.child(buttonsLayout);

        ButtonComponent finishButton = Components.button(Text.translatable("menu.editor.button.finish"), (buttonComponent) -> {
            // Save the action button and close.
            // 点击保存后更新
            actionButtonData.name = nameTextBox.getText();
            if (iconButton.itemIcon != null) actionButtonData.icon = iconButton.itemIcon;

            actionButtonData.actions = actionArray;
            updateActionData();

            if (!boundKeybind) keybind.clear();
            actionButtonData.keybind = keybind;

            // Add or save the current action button.
            if (newAction) ActionButtonDataHandler.add(actionButtonData);
            else ActionButtonDataHandler.save();

            close();
        });
        ButtonComponent cancelButton = Components.button(
                // Close out without saving.
                Text.translatable("menu.editor.button.cancel"),
                (buttonComponent) -> close()
        );

        buttonsLayout.child(finishButton);
        buttonsLayout.gap(4);
        buttonsLayout.child(cancelButton);
    }

    private Integer getCustomModelData(ItemStack item) {
        if (item == null) return CustomModelDataComponent.DEFAULT.value();
        return item.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelDataComponent.DEFAULT).value();
    }

    private void updateCustomModelData(ItemStack itemStack) {
        // updates the items custom model data to that of the users input.
        String text = customModelDataTextBox.getText();

        if (itemStack == null) return;

        try {
            if (!text.equals("")) {
                itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(Integer.parseInt(text)));
            } else {
                itemStack.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
            }
        } catch (NumberFormatException ignored) {}
    }

    public FlowLayout createNewProperty(String name) {
        return createNewProperty(name, true, true);
    }

    public FlowLayout createNewProperty(String name, boolean underline) {
        return createNewProperty(name, underline, true);
    }

    public FlowLayout createNewProperty(String name, boolean underline, boolean useTranslatable) {
        // Create the property's layout.
        FlowLayout layout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        layout
                .padding(Insets.of(4, 0, 0, 4))
                .alignment(HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);

        // Create the description label.
        MutableText labelText;
        if (useTranslatable) labelText = Text.translatable("menu.editor.property." + name);
        else labelText = Text.literal(name);

        if (underline) labelText.formatted(Formatting.UNDERLINE);

        LabelComponent label = Components.label(labelText);
        label.positioning(Positioning.relative(0, 50));

        layout.child(label);

        return layout;
    }

    public void createActions(FlowLayout layout) {
        updateActionData();

        // Clear the original layout of all previous buttons.
        actionsSource.clear();
        actionTicksSource.clear();
        if (actionsLayout != null) actionsLayout.remove();

        actionsLayout = createActionsLayout();
        layout.child(2, actionsLayout);

        // Create each action in a list.
        AtomicInteger i = new AtomicInteger();
        actionArray.forEach((action) -> {
            String name = action.getTypeString();

            FlowLayout property = createNewProperty(name + " #" + (i.get() + 1), false, false);
            Component source = null;
            Component tickSource = null;
            
            // 创建上下箭头按钮
            ButtonComponent upButton = Components.button(Text.literal("↑"), button -> {
                int loadCurrentIndex = actionArray.indexOf(action);
                if (loadCurrentIndex > 0) {
                    Collections.swap(actionArray, loadCurrentIndex, loadCurrentIndex - 1);
                    Collections.swap(actionsSource, loadCurrentIndex, loadCurrentIndex - 1);
                    Collections.swap(actionTicksSource, loadCurrentIndex, loadCurrentIndex - 1);
                    createActions((FlowLayout) actionsLayout.parent());
                }
            });
            upButton.margins(Insets.right(2));

            ButtonComponent downButton = Components.button(Text.literal("↓"), button -> {
                int loadCurrentIndex = actionArray.indexOf(action);
                if (loadCurrentIndex < actionArray.size() - 1) {
                    Collections.swap(actionArray, loadCurrentIndex, loadCurrentIndex + 1);
                    Collections.swap(actionsSource, loadCurrentIndex, loadCurrentIndex + 1);
                    Collections.swap(actionTicksSource, loadCurrentIndex, loadCurrentIndex + 1);
                    createActions((FlowLayout) actionsLayout.parent());
                }
            });
            downButton.margins(Insets.right(2));

            // If the action is a command add an input text box.
            if (action instanceof CommandActionData commandAction) {
                FlowLayout inputGroup = Containers.horizontalFlow(Sizing.fill(57), Sizing.content());
                TextBoxComponent textBoxComponent = Components.textBox(Sizing.fill(79));
                TextBoxComponent tickBoxComponent = Components.textBox(Sizing.fill(20));

                textBoxComponent.setMaxLength(10000);
                textBoxComponent.text(commandAction.command);

                tickBoxComponent.setMaxLength(10);
                tickBoxComponent.text(String.valueOf(commandAction.delay));
                tickBoxComponent.onChanged().subscribe(text -> {
                    String num = text.replaceAll("\\D", "");
                    String command = textBoxComponent.getText();
                    // jump 参数强制不可修改
                    if (command.startsWith("/jump ")) {
                        num = command.substring(6).replaceAll("\\D", "");
                        if (num.length() > 0) {
                            try {
                                tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                            } catch (Exception e) {
                                tickBoxComponent.setText("0");
                            }
                        } else {
                            tickBoxComponent.setText("0");
                        }
                        return;
                    }
                    if (num.length() > 0) {
                        if (num.length() > 3) {
                            tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)).substring(0, 3));
                        } else {
                            tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                        }
                    } else {
                        tickBoxComponent.setText("0");
                    }
                });
                textBoxComponent.onChanged().subscribe(text -> {
                    String num = null;
                    if (text.startsWith("/jump ")) {
                        num = text.substring(6).replaceAll("\\D", "");
                        if (num.length() > 0) {
                            try {
                                tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                            } catch (Exception e) {
                                tickBoxComponent.setText("0");
                            }
                        } else {
                            tickBoxComponent.setText("0");
                        }
                        return;
                    }
                });

                inputGroup.child(tickBoxComponent);
                inputGroup.child(textBoxComponent);
                
                // 将上下按钮和文本框放入水平容器
                FlowLayout controlGroup = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                controlGroup.child(upButton);
                controlGroup.child(downButton);
                controlGroup.child(inputGroup);
        
                property.child(controlGroup);
                source = textBoxComponent;
                tickSource = tickBoxComponent;
            }

            // If the action is a keybind.
            if (action instanceof KeybindActionData keybindAction) {
                ButtonComponent keybindActionButton = Components.button(Text.translatable("menu.editor.not_bound"), (buttonComponent) -> {
                    KeybindPickerUI keybindPicker = new KeybindPickerUI();
                    keybindPicker.onSelectedKeybind = (item) -> {
                        keybindAction.keybindTranslationKey = item.getTranslationKey();
                        updateActionKeybindMessage(buttonComponent, keybindAction);
                    };

                    FlowLayout rootComponent = (FlowLayout) layout.root();
                    rootComponent.child(keybindPicker);

                    pickerUI = keybindPicker;
                });
                FlowLayout inputGroup = Containers.horizontalFlow(Sizing.fill(57), Sizing.content());
                TextBoxComponent tickBoxComponent = Components.textBox(Sizing.fill(20));
                tickBoxComponent.setMaxLength(10);
                tickBoxComponent.text(String.valueOf(keybindAction.delay));
                tickBoxComponent.onChanged().subscribe(text -> {
                    String num = text.replaceAll("\\D", "");
                    if (num.length() > 0) {
                        if (num.length() > 3) {
                            tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)).substring(0, 3));
                        } else {
                            tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                        }
                    } else {
                        tickBoxComponent.setText("0");
                    }
                });
                keybindActionButton.horizontalSizing(Sizing.fill(79));
                inputGroup.child(tickBoxComponent);
                inputGroup.child(keybindActionButton);

                updateActionKeybindMessage(keybindActionButton, keybindAction);
                // 将上下按钮和键按钮放入水平容器
                FlowLayout controlGroup = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                controlGroup.child(upButton);
                controlGroup.child(downButton);
                // controlGroup.child(keybindActionButton);
                controlGroup.child(inputGroup);
                property.child(controlGroup);
                source = keybindActionButton;
                tickSource = tickBoxComponent;
            }

            // Add the remove button.
            ButtonComponent removeActionButton = Components.button(Text.literal(" - "), (buttonComponent -> {
                int currentIndex = actionArray.indexOf(action);
                actionsSource.remove(currentIndex);
                actionTicksSource.remove(currentIndex);
                actionArray.remove(action);

                assert actionsLayout.parent() != null;
                createActions((FlowLayout) actionsLayout.parent());
            }));
            removeActionButton.margins(Insets.of(0, 0, 4, 0));
            property.child(removeActionButton);

            // 添加插入按钮
            ButtonComponent insertBtn = Components.button(Text.literal(" + "), btn -> {
                ActionPickerUI picker = new ActionPickerUI();
                int currentIndex = actionArray.indexOf(action);
                picker.onSelectedAction = newAction -> {
                    Component textBox = null;
                    Component tickbox = null;
                    if (newAction instanceof CommandActionData commandAction) {
                        TextBoxComponent textBoxComponent = Components.textBox(Sizing.fill(79));
                        TextBoxComponent tickBoxComponent = Components.textBox(Sizing.fill(20));

                        textBoxComponent.setMaxLength(10000);
                        textBoxComponent.text(commandAction.command);

                        tickBoxComponent.setMaxLength(10);
                        tickBoxComponent.text(String.valueOf(commandAction.delay));
                        tickBoxComponent.onChanged().subscribe(text -> {
                            String num = text.replaceAll("\\D", "");
                            if (num.length() > 0) {
                                if (num.length() > 3) {
                                    tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)).substring(0, 3));
                                } else {
                                    tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                                }
                            } else {
                                tickBoxComponent.setText("0");
                            }
                        });
                        textBox = textBoxComponent;
                        tickbox = tickBoxComponent;
                    }
                    if (newAction instanceof KeybindActionData keybindAction) {
                        ButtonComponent keybindActionButton = Components.button(Text.translatable("menu.editor.not_bound"), (buttonComponent) -> {
                            KeybindPickerUI keybindPicker = new KeybindPickerUI();
                            keybindPicker.onSelectedKeybind = (item) -> {
                                keybindAction.keybindTranslationKey = item.getTranslationKey();
                                updateActionKeybindMessage(buttonComponent, keybindAction);
                            };

                            FlowLayout rootComponent = (FlowLayout) layout.root();
                            rootComponent.child(keybindPicker);

                            pickerUI = keybindPicker;
                        });
                        TextBoxComponent tickBoxComponent = Components.textBox(Sizing.fill(20));
                        tickBoxComponent.setMaxLength(10);
                        tickBoxComponent.text(String.valueOf(keybindAction.delay));
                        tickBoxComponent.onChanged().subscribe(text -> {
                            String num = text.replaceAll("\\D", "");
                            if (num.length() > 0) {
                                if (num.length() > 3) {
                                    tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)).substring(0, 3));
                                } else {
                                    tickBoxComponent.setText(String.valueOf(Integer.valueOf(num)));
                                }
                            } else {
                                tickBoxComponent.setText("0");
                            }
                        });
                        textBox = keybindActionButton;
                        tickbox = tickBoxComponent;
                    }
                    actionsSource.add(currentIndex + 1, textBox);
                    actionTicksSource.add(currentIndex + 1, tickbox);
                    actionArray.add(currentIndex + 1, newAction);
                    createActions((FlowLayout) actionsLayout.parent());
                };
                ((FlowLayout) layout.root()).child(picker);
                pickerUI = picker;
            });
            insertBtn.margins(Insets.of(0, 0, 4, 0));
            property.child(insertBtn);

            actionsLayout.child(property);

            actionsSource.add(source);
            actionTicksSource.add(tickSource);
            i.addAndGet(1);
        });

        // New Action Button at the bottom.
        FlowLayout actionLayout = createNewProperty("new_action", false);
        actionLayout.padding(actionLayout.padding().get().add(0, 6, 0, 0));

        ButtonComponent newActionButton = Components.button(Text.literal(" + "), (buttonComponent -> {
            ActionPickerUI actionPicker = new ActionPickerUI();

            actionPicker.onSelectedAction = (action) -> {
                actionArray.add(action);
                createActions((FlowLayout) actionsLayout.parent());
            };

            FlowLayout rootComponent = (FlowLayout) layout.root();

            rootComponent.child(actionPicker);
            pickerUI = actionPicker;
        }));

        actionLayout.child(newActionButton);
        actionsLayout.child(actionLayout);
    }

    public FlowLayout createActionsLayout() {
        // Sets up the layout for each action to be added to.
        FlowLayout layout = Containers.collapsible(Sizing.fill(100), Sizing.content(), Text.translatable("menu.editor.actions"), true);
        layout
                .padding(Insets.of(4, 0, -5, 5))
                .verticalAlignment(VerticalAlignment.CENTER);

        return layout;
    }

    public void updateActionData() {
        if (actionsSource.isEmpty()) return;

        // Save each actions source to its respective action.
        AtomicInteger i = new AtomicInteger();
        actionArray.forEach((action) -> {
            if (actionsSource.size() <= i.get()) return;
            Component source = actionsSource.get(i.get());
            Component tickSource = actionTicksSource.get(i.get());

            if (action instanceof CommandActionData commandAction) {
                TextBoxComponent textBoxSource = (TextBoxComponent) source;
                TextBoxComponent tickBoxSource = (TextBoxComponent) tickSource;
                commandAction.command = textBoxSource.getText();
                commandAction.delay = Integer.valueOf(tickBoxSource.getText());
            }
            if (action instanceof KeybindActionData keybindAction) {
                TextBoxComponent tickBoxSource = (TextBoxComponent) tickSource;
                keybindAction.delay = Integer.valueOf(tickBoxSource.getText());
            }

            i.addAndGet(1);
        });
    }

    public void updateKeybindButton() {
        String message;

        if (!boundKeybind) message = "Not Bound";
        else {
            boolean isMouse = keybind.get(3) == 1;

            if (!isMouse) {
                message = InputUtil.fromKeyCode(keybind.get(0), keybind.get(1)).getLocalizedText().getString();
            } else {
                message = switch (keybind.get(0)) {
                    case 0 -> "Left Button";
                    case 1 -> "Right Button";
                    case 2 -> "Middle Button";
                    default -> "Mouse " + keybind.get(0);
                };
            }
        }

        if (settingKeybind) message = "> " + message + " <";
        keybindButton.setMessage(Text.of(message));
    }

    private void updateActionKeybindMessage(ButtonComponent button, KeybindActionData actionData) {
        if (!actionData.keybindTranslationKey.equals("")) {
            String textString = Text.translatable(actionData.keybindTranslationKey).getString();
            int maxLength = 14;

            button.tooltip(Text.literal(""));
            if (textString.length() > maxLength) {
                //button.tooltip(Text.literal(textString));
                textString = textString.substring(0, maxLength) + "...";
            }

            button.setMessage(Text.literal(textString));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean wasEscape = false;

        if (settingKeybind) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                boundKeybind = true;

                keybind.clear();

                keybind.add(keyCode);
                keybind.add(scanCode);

                keybind.add(modifiers);

                keybind.add(0);
            } else {
                boundKeybind = false;
                wasEscape = true;
            }

            settingKeybind = false;
            updateKeybindButton();
        }

        if (!wasEscape) return super.keyPressed(keyCode, scanCode, modifiers);
        else return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean keybindSet = false;

        if (settingKeybind) {
            keybindSet = true;

            keybind.clear();

            if (button <= 2) {
                boundKeybind = true;

                keybind.add(button);
                keybind.add(0);
                keybind.add(0);

                keybind.add(1);
            } else {
                boundKeybind = false;
            }

            settingKeybind = false;
            updateKeybindButton();
        }

        if (!keybindSet) return super.mouseClicked(mouseX, mouseY, button);
        else return false;
    }

    @Override
    public void close() {
        if (pickerUI != null && pickerUI.parent() != null) {
            // Close the item picker before closing the editor ui.
            pickerUI.remove();
            pickerUI = null;
            return;
        }

        if (previousScreen != null) MinecraftClient.getInstance().setScreen(previousScreen);
        else super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
