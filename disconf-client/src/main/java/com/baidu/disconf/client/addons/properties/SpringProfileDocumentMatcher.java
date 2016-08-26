package com.baidu.disconf.client.addons.properties;


import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
 

public class SpringProfileDocumentMatcher implements DocumentMatcher {
    private static final String[] DEFAULT_PROFILES = new String[]{"^\\s*$"};
    private String[] activeProfiles = new String[0];

    public SpringProfileDocumentMatcher() {
    }

    public SpringProfileDocumentMatcher(String... profiles) {
        this.addActiveProfiles(profiles);
    }

    public void addActiveProfiles(String... profiles) {
        LinkedHashSet set = new LinkedHashSet(Arrays.asList(this.activeProfiles));
        Collections.addAll(set, profiles);
        this.activeProfiles = (String[])set.toArray(new String[set.size()]);
    }

    public MatchStatus matches(Properties properties) {
        String[] profiles = this.activeProfiles;
        if(profiles.length == 0) {
            profiles = DEFAULT_PROFILES;
        }

        return (new ArrayDocumentMatcher("spring.profiles", profiles)).matches(properties);
    }
}
