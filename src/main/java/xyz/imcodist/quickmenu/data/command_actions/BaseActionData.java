package xyz.imcodist.quickmenu.data.command_actions;

public class BaseActionData implements Cloneable {
    public String getJsonType() {
        return "base";
    }
    public String getJsonValue() {
        return "";
    }

    public String getTypeString() { return "ACT"; }
    public String getString() {
        return "uh oh why are you seeing this";
    }

    public void run() {}

    public int delay = 0;
    public void delaySub() {
        this.delay--;
    }
    public void ready() {}
    @Override
    public BaseActionData clone() {
        try {
            return (BaseActionData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
