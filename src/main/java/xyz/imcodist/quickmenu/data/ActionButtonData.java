package xyz.imcodist.quickmenu.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.imcodist.quickmenu.QuickMenu;
import xyz.imcodist.quickmenu.data.command_actions.BaseActionData;
import xyz.imcodist.quickmenu.data.command_actions.CommandActionData;
import xyz.imcodist.quickmenu.data.command_actions.JumpFuncManage;
import xyz.imcodist.quickmenu.data.command_actions.KeybindActionData;
import xyz.imcodist.quickmenu.other.ModConfigModel;

import java.util.ArrayList;

public class ActionButtonData {
    public String name;
    public ArrayList<BaseActionData> actions = new ArrayList<>();
    public ItemStack icon;
    public ArrayList<Integer> keybind = new ArrayList<>();

    public boolean keyPressed = false;

    public ActionButtonDataJSON toJSON() {
        ActionButtonDataJSON jsonData = new ActionButtonDataJSON();

        jsonData.name = name;
        jsonData.actions = new ArrayList<>();

        jsonData.keybind = keybind;

        actions.forEach((action) -> {
            ArrayList<String> actionArray = new ArrayList<>();
            actionArray.add(action.getJsonType());
            actionArray.add(action.getJsonValue());
            actionArray.add(String.valueOf(action.delay));

            jsonData.actions.add(actionArray);
        });

        if (icon != null) {
            if (icon.getRegistryEntry().getKey().isPresent()) {
                jsonData.icon = icon.getRegistryEntry().getKey().get().getValue().toString();
            }

            jsonData.customModelData = icon.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelDataComponent.DEFAULT).value();
        }

        return jsonData;
    }

    public static ActionButtonData fromJSON(ActionButtonDataJSON json) {
        ActionButtonData data = new ActionButtonData();

        data.name = json.name;
        data.actions = new ArrayList<>();

        data.keybind = json.keybind;

        json.actions.forEach((actionArray) -> {
            BaseActionData actionData = null;
            if (actionArray.size() == 3) {
                actionData = getActionDataType(actionArray.get(0), actionArray.get(1), actionArray.get(2));
            } else {
                actionData = getActionDataType(actionArray.get(0), actionArray.get(1), "0");
            }
            data.actions.add(actionData);
        });

        if (json.icon != null) {
            data.icon = new ItemStack(Registries.ITEM.get(Identifier.of(json.icon)));
            data.icon.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(json.customModelData));
        }

        return data;
    }

    private static BaseActionData getActionDataType(String type, String value, String delay) {
        switch (type) {
            case "base" -> {
                return new BaseActionData();
            }
            case "cmd" -> {
                CommandActionData commandActionData = new CommandActionData();
                commandActionData.command = value;
                commandActionData.delay = Integer.valueOf(delay);
                return commandActionData;
            }
            case "key" -> {
                KeybindActionData keybindActionData = new KeybindActionData();
                keybindActionData.keybindTranslationKey = value;
                keybindActionData.delay = Integer.valueOf(delay);
                return keybindActionData;
            }
        }

        return null;
    }

    public InputUtil.Key getKey() {
        if (keybind.size() < 4) return null;
        return InputUtil.fromKeyCode(keybind.get(0), keybind.get(1));
    }

    public void run(JumpFuncManage funcManage) {
        run(false, funcManage);
    }

    public void run(boolean isKeybind, JumpFuncManage funcManage) {
        // Show run message.
        ModConfigModel.DisplayRunText displayRunText = QuickMenu.CONFIG.displayRunText();
        if (displayRunText == ModConfigModel.DisplayRunText.ALWAYS || displayRunText == ModConfigModel.DisplayRunText.KEYBIND_ONLY && isKeybind) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client != null && client.player != null) {
                client.player.sendMessage(Text.of("Ran action \"" + name + "\""), true);
            }
        }

        // 准备运行action初始化
        actions.forEach(BaseActionData::ready);
        if (funcManage != null){
            funcManage.setListAction(actions);
        }
    }
}

