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
    public Map<String, Object> variableMap = new HashMap<>();
    public record forLoop(Integer startIndex, Integer endIndex, Integer start, Integer end, Integer step) {}
    public Map<String, forLoop> forLoopMap = new HashMap<>(); // 变量名对应具体循环值
    public ArrayList<String> forLoopKeys;
    public Integer currentIndex = 1;

    public JumpFuncManage() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onServerTick);
    }

    public void addVariable(String name, Integer num) {
        variableMap.put(name, num);
    }

    public void addVariable(String name, String str) {
        variableMap.put(name, str);
    }

    public void setVariable(String name, Object obj) {
        variableMap.replace(name, obj);
    }

    public Object getVariable(String name) {
        return variableMap.getOrDefault(name, 0);
    }

    public boolean containskey(String name) {
        return variableMap.containsKey(name);
    }

    public ArrayList<String> getKeys() {
        ArrayList<String> keys = new ArrayList<>();
        for (String key : this.variableMap.keySet()) {
            keys.add(key);
        }
        return keys;
    }

    public ArrayList<String> getForLoopKeys() {
        ArrayList<String> keys = new ArrayList<>();
        for (String key : this.forLoopMap.keySet()) {
            keys.add(key);
        }
        return keys;
    }

    public ArrayList<Object> getValues() {
        ArrayList<Object> values = new ArrayList<>();
        for (Object key : this.variableMap.values().toArray()) {
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
        this.currentIndex = 1;
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
            this.currentIndex = index;
            this.forLoopMap.clear();
            this.forLoopKeys.clear();
        }
    }
    
    public void forLoopGotoIndex(int index) {
        if (index > this.baseListAction.size()) {
            this.setListAction(new ArrayList<>());
        } else if (index > 0){
            this.listAction.clear();
            for (BaseActionData base : this.baseListAction.subList(index - 1, this.baseListAction.size())) {
                this.listAction.add(base.clone());
            }
            this.currentIndex = index;
        }
    }

    public void creatForLoop(String var, Integer end_index, Integer start, Integer end, Integer step) {
        if (this.listAction.size() > 0) {
            if (end_index >= this.baseListAction.size()) end_index = this.baseListAction.size();
            forLoop loop = new forLoop(this.currentIndex+1, end_index, start, end, step);
            this.forLoopMap.put(var, loop);
            this.variableMap.put(var, start);
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
                this.forLoopKeys = this.getForLoopKeys();
                if (forLoopKeys.size() > 0) {
                    String key = forLoopKeys.get(forLoopKeys.size()-1);
                    forLoop loop = this.forLoopMap.get(key);
                    if ((Integer) this.getVariable(key) >= loop.end-1) {
                        this.forLoopMap.remove(key);
                        if (this.forLoopMap.size() > 0) {
                            forLoopKeys = this.getForLoopKeys();
                            key = forLoopKeys.get(forLoopKeys.size()-1);
                            loop = this.forLoopMap.get(key);
                            this.forLoopGotoIndex(loop.startIndex);
                            this.setVariable(key, (Integer) this.getVariable(key) + loop.step);
                            continue;
                        }
                    }
                }
                a.run();
                this.listAction.remove(a);
                this.forLoopKeys = this.getForLoopKeys();
                if (forLoopKeys.size() > 0) {
                    String key = forLoopKeys.get(forLoopKeys.size()-1);
                    forLoop loop = this.forLoopMap.get(key);
                    if (this.currentIndex == loop.endIndex & (Integer) this.getVariable(key) < loop.end-1) {
                        // 处在循环中
                        this.forLoopGotoIndex(loop.startIndex);
                        this.setVariable(key, (Integer) this.getVariable(key) + loop.step);
                        continue;
                    }
                }
                this.currentIndex += 1;
                if (this.listAction.size() == 0) {
                    this.variableMap.clear();
                }
            } else {
                a.delaySub();
                break;
            }
        }
    }
}