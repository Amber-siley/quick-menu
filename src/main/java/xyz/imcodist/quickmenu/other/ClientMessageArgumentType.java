package xyz.imcodist.quickmenu.other;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;

public class ClientMessageArgumentType implements ArgumentType<Text> {
    public static ClientMessageArgumentType message() {
        return new ClientMessageArgumentType();
    }
    
    @Override
    public Text parse(StringReader reader) throws CommandSyntaxException {
        String text = reader.getRemaining();
        reader.setCursor(reader.getTotalLength()); // 移动光标到末尾
        return Text.of(text);
    }

    public static <S> Text getMessage(CommandContext<S> context, String name) {
        return context.getArgument(name, Text.class);
    }
}
