package com.na.common.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaXssStringJsonDeserializer extends JsonDeserializer<String> {

    private static final List<String> regexList = new ArrayList<>();
    private static final String XSS_PATTERN;

    static {
        regexList.add("alert\\s*\\((.*?)\\)");
        regexList.add("expression\\s*\\((.*?)\\)");
        regexList.add("<script(\\s*)>(.*?)</script(\\s*)>");
        regexList.add("<style(\\s*)>(.*?)</style(\\s*)>");
        regexList.add("<applet(.*?)/>|<applet(.*?)>(.*?)</applet(\\s*)>");
        regexList.add("<object(.*?)/>|<object(.*?)>(.*?)</object(\\s*)>");
        regexList.add("<embed(.*?)/>|<embed(.*?)>(.*?)</embed(\\s*)>");
        regexList.add("eval\\s*\\((.*?)\\)");
        regexList.add("\\'javascript(\\s*):(.*?)\\'|\\\"javascript(\\s*):(.*?)\\\"");
        regexList.add("\\'vbscript(\\s*):(.*?)\\'|\\\"vbscript(\\s*):(.*?)\\\"");
        regexList.add("<iframe\\s+(.*?)/>|<iframe\\s+(.*?)>(.*?)</iframe(\\s*)>");
        regexList.add("(\\s+)onmouseover\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseover\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmouseout\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseout\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmousedown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousedown\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmouseup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseup\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmousemove\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousemove\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onclick\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)ondblclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondblclick\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeypress\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeypress\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeydown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeydown\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeyup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeyup\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)ondragstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondragstart\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onerrorupdate\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerrorupdate\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onhelp\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onhelp\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onreadystatechange\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onreadystatechange\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onrowenter\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowenter\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onrowexit\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowexit\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onselectstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onselectstart\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onunload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onbeforeunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbeforeunload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onblur\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onblur\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onerror\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerror\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onfocus\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfocus\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onresize\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onresize\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onscroll\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onscroll\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)oncontextmenu\\s*=(\\s*)\\'(.*?)\\'|(\\s+)oncontextmenu\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onbounce\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbounce\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onfinish\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfinish\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onstart\\s*=(\\s*)\\\"(.*?)\\\"");

        StringBuilder combinedPattern = new StringBuilder();
        for (String regex : regexList) {
            if (combinedPattern.length() > 0) combinedPattern.append("|");
            combinedPattern.append("(").append(regex).append(")");
        }
        XSS_PATTERN = combinedPattern.toString();
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String source = p.getText();
        if (StringUtils.isNotBlank(source)) {
            source = xssScriptReplace(source); // 正则清洗
        }
        return source;
    }

    public static String xssScriptReplace(String value) {
        if (value != null) {
            for (String regex : regexList) {
                Pattern scriptPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                value = scriptPattern.matcher(value).replaceAll(" ");
            }
        }
        return value;
    }

    public static boolean containsXss(String input) {
        Pattern pattern = Pattern.compile(XSS_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }
}
