package xyz.imcodist.quickmenu.data.command_actions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class CommandActionData extends BaseActionData {
    public String command = "";
    public String commandToRun = "";

    @Override
    public String getJsonType() {
        return "cmd";
    }
    @Override
    public String getJsonValue() {
        return command;
    }

    @Override
    public String getTypeString() { return "CMD"; }
    @Override
    public String getString() {
        return command;
    }

    public void delaySub() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        this.delay--;
    }

    @Override
    public void funcstart() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Run the command.
        if (!commandToRun.startsWith("jump ")){
            player.networkHandler.sendChatCommand(commandToRun);
        }
    }

    @Override
    public void run() {
        // 不执行
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Run the command.
        command = command.strip();
        commandToRun = command;

        if (commandToRun != null) {
            if (commandToRun.startsWith("/")) {
                commandToRun = commandToRun.substring(1);
                if (commandToRun.startsWith("jump ")) {
                    try {
                        this.delay = Integer.parseInt(this.command.substring(5).trim());
                    } catch (NumberFormatException e) {
                        this.delay = 0;
                    }
                } 
            } else {
                if (commandToRun.length() >= 256) {
                    commandToRun = commandToRun.substring(0, 256);
                }
            }
        }
    }
}
