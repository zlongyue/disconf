package com.baidu.disconf.client.addons.properties;


import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.util.StringUtils;

public class ArrayDocumentMatcher implements DocumentMatcher {
    private final String key;
    private final String[] patterns;

    public ArrayDocumentMatcher(String key, String... patterns) {
        this.key = key;
        this.patterns = patterns;
    }

    public MatchStatus matches(Properties properties) {
        if(!properties.containsKey(this.key)) {
            return MatchStatus.ABSTAIN;
        } else {
            Set values = StringUtils.commaDelimitedListToSet(properties.getProperty(this.key));
            if(values.isEmpty()) {
                values = Collections.singleton("");
            }

            String[] var3 = this.patterns;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String pattern = var3[var5];
                Iterator var7 = values.iterator();

                while(var7.hasNext()) {
                    String value = (String)var7.next();
                    if(value.matches(pattern)) {
                        return MatchStatus.FOUND;
                    }
                }
            }

            return MatchStatus.NOT_FOUND;
        }
    }
}
