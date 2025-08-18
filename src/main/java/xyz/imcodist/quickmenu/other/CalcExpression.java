package xyz.imcodist.quickmenu.other;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mariuszgromada.math.mxparser.PrimitiveElement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.Expression;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class CalcExpression {
    public static final File commandFile = new File(".", "config/quickmenu_calc.json");

    public static double getParsedExpression(Entity player, String in, Integer... nonstackable) {
        int stackSize;
        if (nonstackable.length == 0) stackSize = 64;
        else stackSize = nonstackable[0];
        HashMap<String, Double> vars = new HashMap<>();
        vars.put("dub64", 3456.0);
        vars.put("dub16", 864.0);
        vars.put("dub1", 54.0);
        vars.put("sb64", 1728.0);
        vars.put("sb16", 432.0);
        vars.put("sb1", 27.0);
        vars.put("stack64", 64.0);
        vars.put("stack16", 16.0);
        vars.put("stack1", 1.0);
        vars.put("min", 60.0);
        vars.put("hour", 3600.0);
        if (Objects.nonNull(player)) {
            vars.put("x", (double) player.getBlockPos().getX());
            vars.put("y", (double) player.getBlockPos().getY());
            vars.put("z", (double) player.getBlockPos().getZ());
            vars.put("health", (double) ((PlayerEntity) player).getHealth());
        }
       //
        vars.put("dub", vars.get("dub"+ stackSize));
        vars.put("sb", vars.get("sb"+stackSize));
        vars.put("stack", vars.get("stack"+stackSize));
        String withVars = in;
        ArrayList<String> parsedCustomFunctions = getParsedFunctions();
        parsedCustomFunctions.sort((s1, s2) -> (s2.length() - s1.length()));
        //hide funcs from replace
        for (int f = 0; f< parsedCustomFunctions.size(); f++) {
            String func = parsedCustomFunctions.get(f);
            withVars = withVars.replaceAll(func.split("[(]")[0]+"[(]", "func"+f+"(");
            func = func.replaceAll(func.split("[(]")[0]+"[(]", "func"+f+"(");
            String[] funcVars = func.split("[(]")[1].split("[)]")[0].split(", ");
            for (int v = 0; v < funcVars.length; v++) {
                func = func.replaceAll(funcVars[v], "var"+f+v);
            }


            parsedCustomFunctions.set(f, func);
        }
        ArrayList<PrimitiveElement> primitiveElements = new ArrayList<>();
        for (String key : vars.keySet()) {
            //switch out variables in func unless override by local
            for (int f = 0; f< parsedCustomFunctions.size(); f++) {
                String func = parsedCustomFunctions.get(f);
                String expression = func.split("= ")[1].replaceAll(key, "("+vars.get(key)+")");
                if (!contains(func.split(" =")[0].split("[(]")[1].replace("[)]", "").split(","), key)) {
                    parsedCustomFunctions.set(f, func.split("= ")[0] + "= " + expression);
                }
            }
            withVars = withVars.replaceAll(key, "("+vars.get(key)+")");
        }
        withVars = withVars.replaceAll("(\\d*),(\\d+)", "$1$2");


        for (int f = 0; f < parsedCustomFunctions.size(); f++) {
            primitiveElements.add(new Function(parsedCustomFunctions.get(f)));
        }
            return new Expression(withVars, primitiveElements.toArray(new PrimitiveElement[0] )).calculate();
        }

    public static ArrayList<String> getParsedFunctions() {
        ArrayList<String> parsedFuncs = new ArrayList<>();
        JsonObject funcs = getFunctions();
        for (String f : funcs.keySet()) {
            String func = funcs.get(f).getAsString();
            ArrayList<String> vars = parseEquationVariables(func);
            if (!vars.isEmpty()) {
                String combinedVars = String.join(", ", vars);
                parsedFuncs.add(f + "(" + combinedVars + ") = " + func.replace("[", "").replace("]", ""));
            } else {
                parsedFuncs.add(f+"() = " + func);
            }
        }
        return parsedFuncs;
    }
    public static ArrayList<String> parseEquationVariables(String input) {
        String patternString = "\\[([^\\]]+)\\]";//"\\[[^\\]]+\\]";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(input);
        StringBuilder commandJson = new StringBuilder("{\n\"variables\": [\n");
        ArrayList<String> variables = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            group = group.substring(1, group.length()-1);
          //  commandJson.append("\"").append(group).append("\",\n");
            if (!variables.contains(group)) {
                variables.add(group);
            }
        }
        commandJson.append("],\n\"equation\": \"").append(input).append("\"\n}");
        return variables;
    }
    
    static boolean contains(String[] array, String value) {
        for (String str : array) {
            if (str.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static JsonObject getFunctions() {
        JsonObject json = readJson();
        if (!json.has("functions")) {
            json = new JsonObject();
            json.add("functions", new JsonObject());
        }
        return json.getAsJsonObject("functions");
    }

     public static JsonObject readJson() {
         try (BufferedReader reader = new BufferedReader(new FileReader(commandFile))) {
            JsonObject tempJson;
            try {
                tempJson = JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject();
            } catch (Exception ignored) {
                tempJson = new JsonObject();
            }
            JsonObject json = tempJson; // annoying lambda requirement
            return json;
        } catch (Exception ignored) { return new JsonObject();}
    }
}
