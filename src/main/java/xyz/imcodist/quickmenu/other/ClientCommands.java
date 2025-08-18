package xyz.imcodist.quickmenu.other;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.command.CommandSource;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import xyz.imcodist.quickmenu.QuickMenu;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ClientCommands implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {this.registerCommands(dispatcher);});
    }
    
    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            literal("cls")
                .executes((context) -> {
                    this.clearChat();
                    return 1;
                })
        );
        dispatcher.register(
            literal("jump")
                .then(argument("Ticks", IntegerArgumentType.integer())
                    .executes(context -> {return 1;}))
        );
        dispatcher.register(
            literal("goto")
                .then(argument("Actions Number", IntegerArgumentType.integer())
                    .executes(context -> {
                        QuickMenu.funcManage.gotoIndex(IntegerArgumentType.getInteger(context, "Actions Number"));
                        return 1;
                    }))
        );
        dispatcher.register(
            literal("playerTest")
                .then(argument("Name", StringArgumentType.word())
                    .suggests((context, builder) -> {
                            String input = builder.getRemaining().toLowerCase();
                            for (String name : getPlayers(context.getSource())) {
                                if (name.toLowerCase().startsWith(input)) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        }
                    )
                    .then(literal("goto")
                        .then(argument("Actions Number", IntegerArgumentType.integer())
                            .executes(context -> {
                                if (isPlayerOnline(StringArgumentType.getString(context, "Name"), context.getSource())) {
                                    QuickMenu.funcManage.gotoIndex(IntegerArgumentType.getInteger(context, "Actions Number"));
                                }
                                return 1;
                            })
                            .then(literal("msg")
                                .then(argument("Message", ClientMessageArgumentType.message())
                                    .executes(context -> {
                                        if (isPlayerOnline(StringArgumentType.getString(context, "Name"), context.getSource())) {
                                            QuickMenu.funcManage.gotoIndex(IntegerArgumentType.getInteger(context, "Actions Number"));
                                            addMsg(ClientMessageArgumentType.getMessage(context, "Message"));
                                        }
                                        return 1;
                                    })))))
                    .then(literal("msg")
                        .then(argument("Message", ClientMessageArgumentType.message())
                            .executes(context -> {
                                if (isPlayerOnline(StringArgumentType.getString(context, "Name"), context.getSource())) {
                                    addMsg(ClientMessageArgumentType.getMessage(context, "Message"));
                                }
                                return 1;
                            }))))
                .then(literal("List")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client == null) return 1;
                        ClientPlayerEntity player = client.player;
                        if (player == null) return 1;
                        if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
                            ChatHud chatHud = client.inGameHud.getChatHud();
                            this.addMsg("╔ [Players List]");
                            for (String name : getPlayers(context.getSource())) {
                                chatHud.addMessage(Text.of("╟ " + name));
                            }
                        }
                        return 1;
                    }))
        );
        dispatcher.register(
            literal("isay")
                .then(argument("Message", ClientMessageArgumentType.message())
                    .executes(context -> {
                        this.iSay(ClientMessageArgumentType.getMessage(context, "Message"));
                        return 1;
                    }))
        );
        dispatcher.register(
            literal("osay")
                .then(argument("Message", ClientMessageArgumentType.message())
                    .executes(context -> {
                        addMsg(ClientMessageArgumentType.getMessage(context, "Message"));
                        return 1;
                    }))
        );
        dispatcher.register(
            literal("var")
                .then(literal("Str")
                    .then(argument("Variable Name", StringArgumentType.string())
                        .executes(context -> {
                            QuickMenu.funcManage.addVariable(StringArgumentType.getString(context, "Variable Name"), "");
                            return 1;
                        })
                        .then(argument("Value", StringArgumentType.string())
                            .executes(context -> {
                                QuickMenu.funcManage.addVariable(StringArgumentType.getString(context, "Variable Name"), StringArgumentType.getString(context, "Value"));
                                return 1;
                            }))))
                .then(literal("List")
                    .executes(context -> {
                        ArrayList<String> keys = QuickMenu.funcManage.getKeys();
                        ArrayList<Object> values = QuickMenu.funcManage.getValues();
                        addMsg("Variable List:");
                        for (int i = 0; i < keys.size(); i++) {
                            Object value = values.get(i);
                            if (value instanceof String) {
                                value = "\"" + value + "\"";
                            }
                            addMsg(" - "+keys.get(i) + ": " + value);
                        }
                        return 1;
                    }))
                .then(literal("Clear")
                    .executes(context -> {
                        QuickMenu.funcManage.map.clear();
                        return 1;
                    }))
                .then(literal("Int")
                    .then(argument("Variable Name", StringArgumentType.string())
                        .executes(context -> {
                            QuickMenu.funcManage.addVariable(StringArgumentType.getString(context, "Variable Name"), 0);
                            return 1;
                        })
                        .then(argument("Value", IntegerArgumentType.integer())
                            .executes(context -> {
                                QuickMenu.funcManage.addVariable(StringArgumentType.getString(context, "Variable Name"), IntegerArgumentType.getInteger(context, "Value"));
                                return 1;
                            }))))
                .then(argument("Variable Name", StringArgumentType.string())
                    .suggests((c, b) -> {
                        String input = b.getRemaining().toLowerCase();
                        for (String name : QuickMenu.funcManage.getKeys()) {
                            if (name.toLowerCase().startsWith(input)) {
                                b.suggest(name);
                            }
                        }
                        return b.buildFuture();
                    })
                    .then(argument("Expression", StringArgumentType.greedyString())
                        .executes(c -> {
                            String expr = StringArgumentType.getString(c, "Expression");
                            for (String i : QuickMenu.funcManage.getKeys()) {
                                String key = "\\{"+i+"\\}";
                                if (expr.contains("{"+i+"}")) {
                                    expr = expr.replaceAll(key, String.valueOf(QuickMenu.funcManage.getVariable(i)));
                                }
                            }
                            if (QuickMenu.funcManage.getVariable(StringArgumentType.getString(c, "Variable Name")) instanceof Integer) {
                                int result = (int) CalcExpression.getParsedExpression(c.getSource().getEntity(), expr);
                                QuickMenu.funcManage.setVariable(StringArgumentType.getString(c, "Variable Name"), result);
                            } else {
                                QuickMenu.funcManage.setVariable(StringArgumentType.getString(c, "Variable Name"), expr);
                            }
                            return 1;
                        })))
        );
    }

    private void clearChat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            ChatHud chatHud = client.inGameHud.getChatHud();
            chatHud.clear(true);
        }
    }
    
    private void iSay(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        player.networkHandler.sendChatMessage(msg);
    }

    private void iSay(Text msg) {
        this.iSay(msg.getString());
    }

    private void addMsg(Text msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            ChatHud chatHud = client.inGameHud.getChatHud();
            chatHud.addMessage(msg);
        }
    }

    private void addMsg(String msg) {
        this.addMsg(Text.of(msg));
    }

    public static boolean isPlayerOnline(String playerName, CommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || playerName == null || playerName.isEmpty()) {
            return false; // 服务器未运行或无效输入
        }
        for (String name : getPlayers(source)) {
            if (name.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<String> getPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        ArrayList<String> players = new ArrayList<String>();
        if (client == null) {
            return players;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            String playerName = player.getName().getString();
            players.add(playerName);
        }
        return players;
    }

    private static Collection<String> getPlayers(CommandSource source)
    {
        Set<String> players = new LinkedHashSet<>();
        players.addAll(source.getPlayerNames());
        return players;
    }
}
