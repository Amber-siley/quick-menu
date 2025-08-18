package xyz.imcodist.quickmenu.data.command_actions;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class JumpFuncManage {
    public List<BaseActionData> listAction = new ArrayList<>();
    public List<BaseActionData> baseListAction = new ArrayList<>();
    public Map<String, Object> map = new HashMap<>();

    public JumpFuncManage() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onServerTick);
    }

    public void addVariable(String name, Integer num) {
        map.put(name, num);
    }

    public void addVariable(String name, String str) {
        map.put(name, str);
    }

    public void setVariable(String name, Object obj) {
        map.replace(name, obj);
    }

    public Object getVariable(String name) {
        return map.getOrDefault(name, 0);
    }

    public boolean containskey(String name) {
        return map.containsKey(name);
    }

    public ArrayList<String> getKeys() {
        ArrayList<String> keys = new ArrayList<>();
        for (String key : this.map.keySet()) {
            keys.add(key);
        }
        return keys;
    }

    public ArrayList<Object> getValues() {
        ArrayList<Object> values = new ArrayList<>();
        for (Object key : this.map.values().toArray()) {
            values.add(key);
        }
        return values;
    }

    public void setListAction(List<BaseActionData> tmplist) {
        List<BaseActionData> tmp1 = new ArrayList<>();
        List<BaseActionData> tmp2 = new ArrayList<>();
        for (BaseActionData base : tmplist) {
            tmp1.add(base.clone());
            tmp2.add(base.clone());
        }
        this.listAction = tmp1;
        this.baseListAction = tmp2;
    }

    public void gotoIndex(int index) {
        if (this.listAction.isEmpty()) {

        } else if (index > this.baseListAction.size()) {
            this.setListAction(new ArrayList<>());
        } else if (index > 0){
            this.listAction.clear();
            for (BaseActionData base : this.baseListAction.subList(index - 1, this.baseListAction.size())) {
                this.listAction.add(base.clone());
            }
        }
    }

    public void onServerTick(MinecraftClient client) {
        if (this.listAction.size() == 0) return;
        if (client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        while (this.listAction.size() > 0) {
            BaseActionData a = this.listAction.get(0);
            if (a.delay <= 0) {
                if (a instanceof CommandActionData b) {
                    for (String i : this.getKeys()) {
                        String key = "\\{"+i+"\\}";
                        if (b.commandToRun.contains("{"+i+"}")) {
                            b.commandToRun = b.commandToRun.replaceAll(key, String.valueOf(this.getVariable(i)));
                        }
                    }
                }
                a.run();
                this.listAction.remove(a);
                if (this.listAction.size() == 0) {
                    this.map.clear();
                }
            } else {
                a.delaySub();
                break;
            }
        }
    }
}