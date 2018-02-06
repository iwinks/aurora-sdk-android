package com.aurorasdk;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jayalfredprufrock on 1/30/18.
 */

public class Profile {

    private String content;
    private Pattern pattern;

    private final Map<String, String> options = new HashMap<>();

    public Profile(String content){

        pattern = Pattern.compile("\\{\\s*(\\S+)\\s*:\\s*(.*)\\}");

        setContent(content);
    }


    public void setContent(String content){

        this.content = content;
        options.clear();

        Matcher matcher = pattern.matcher(this.content);

        while (matcher.find() && matcher.groupCount() == 2) {

            options.put(matcher.group(1), matcher.group(2));
        }
    }

    public Map<String, String> getOptions(){

        return options;
    }

    public boolean setOptionValue(String option, String value){

        if (!options.containsKey(option)){

            return false;
        }

        if (!options.get(option).equals(value)){

            options.put(option, value);

            content = content.replaceAll("\\{\\s*" + Pattern.quote(option) + "\\s*:\\s*(.*)\\}", "{" + option + ":" + value + "}");
        }

        return true;
    }

    public String toString(){

        return content;
    }

}
