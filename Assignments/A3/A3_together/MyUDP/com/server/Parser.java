package com.server;

import java.util.HashMap;
import java.util.Map;

public class Parser {
    private HashMap<String, String> options;
    private HashMap<String, OptionType> legitOptions;
    private String prefix;

    public Parser() {
        options=new HashMap<>();
        legitOptions=new HashMap<>();
    }

    public void parse(String[] args){
        if (args.length>0) {
            String key, value;
            for (int i=0;i< args.length;i++){
                key=args[i];

                //strip prefix
                key=key.replaceFirst(getPrefix(),"");

                if (!isOptionValid(key)) {
                    throw new IllegalArgumentException("Unrecognized option \""+key+"\"");
                }

                if (i+1<args.length && !args[i+1].startsWith(getPrefix())){
                    value=args[i+1];
                    ++i;
                } else {
                    value="";
                }

                boolean isOptionBool=getOptionType(key).isOptionBool;

                if (!isOptionBool && value.isEmpty()) {
                    throw new IllegalArgumentException("Option \""+key+"\" should come with a value" );
                } else if (isOptionBool && !value.isEmpty()) {
                    throw new IllegalArgumentException("Option \""+key+"\" should not have value followed by" );
                }
                putOption(key, value);
            }

            for (Map.Entry<String, OptionType> entry:legitOptions.entrySet()){
                if (!entry.getValue().isOptional && !options.containsKey(entry.getKey())){
                    throw new IllegalArgumentException("Option \""+getPrefix()+entry.getKey()+"\" is required" );
                }
            }

        } else {
            throw new IllegalArgumentException("No argument found" );
        }
    }

    public void addOption(String option, boolean isOptional, boolean isOptionBool) {
        OptionType type=new OptionType();
        type.isOptionBool=isOptionBool;
        type.isOptional=isOptional;
        legitOptions.put(option, type);
    }

    public boolean isOptionValid(String option) {
        return legitOptions.containsKey(option);
    }

    public OptionType getOptionType(String option) {
        return legitOptions.get(option);
    }

    private void putOption(String key, String value) {
        options.put(key, value);
    }

    public void setPrefix(String prefix) {
        this.prefix=prefix;
    }

    private String getPrefix() {
        return  this.prefix;
    }

    public String getValue(String key) {
        return options.get(key);
    }


}
