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

    private final Map<Option, Object> options = new HashMap<>();

    public Profile(String content){

        pattern = Pattern.compile("\\{\\s*(\\S+)\\s*:\\s*(.*)\\}");

        setContent(content);
    }


    public void setContent(String content){

        this.content = content;
        options.clear();

        Matcher matcher = pattern.matcher(this.content);

        while (matcher.find() && matcher.groupCount() == 2) {
            options.put(Option.getOption(matcher.group(1)), matcher.group(2));
        }
    }

    public Map<Option, Object> getOptions(){

        return options;
    }

    public void setOptionValue(Option option, String value){

        if (options.containsKey(option) && !options.get(option).equals(value)){

            options.put(option, value);

            content = content.replaceAll("\\{\\s*" + Pattern.quote(option.optionName) + "\\s*:\\s*(.*)\\}", "{" + option.optionName + ":" + value + "}");
        }
    }

    public void setOptionValue(Option option, boolean value){
        if (options.containsKey(option) && !options.get(option).equals(value ? 1 : 0)){

            options.put(option, value);

            content = content.replaceAll("\\{\\s*" + Pattern.quote(option.optionName) + "\\s*:\\s*(.*)\\}", "{" + option.optionName + ":" + String.valueOf(value ? 1 : 0) + "}");
        }
    }

    public void setOptionValue(Option option, long value){
        if (options.containsKey(option) && !options.get(option).equals(value)){

            options.put(option, value);

            content = content.replaceAll("\\{\\s*" + Pattern.quote(option.optionName) + "\\s*:\\s*(.*)\\}", "{" + option.optionName + ":" + String.valueOf(value) + "}");
        }
    }

    public String toString(){

        return content;
    }

    public enum Option {

        STIM_DELAY("stim-delay"),
        STIM_INTERVAL("stim-interval"),
        STIM_ENABLED("stim-enabled"),
        WAKEUP_WINDOW("wakeup-window"),
        SMART_ALARM_ENABLED("sa-enabled"),
        DAWN_STIMULATING_LIGHT("dsl-enabled"),
        STREAM_DEBUG("stream-debug"),
        WAKEUP_TIME("wakeup-time");

        private final String optionName;

        Option(String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public static Option getOption(String optionName){
            for (Option option : values()){
                if (option.optionName.equals(optionName)){
                    return option;
                }
            }

            return null;
        }
    }
}
