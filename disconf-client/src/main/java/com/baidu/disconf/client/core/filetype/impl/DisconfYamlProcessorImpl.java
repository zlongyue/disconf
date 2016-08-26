package com.baidu.disconf.client.core.filetype.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import com.baidu.disconf.client.core.filetype.DisconfFileTypeProcessor;

public class DisconfYamlProcessorImpl extends YamlProcessor implements DisconfFileTypeProcessor {
	 ResourceLoader resourceLoader = new FileSystemResourceLoader();

	@Override
	public Map<String, Object> getKvMap(String fileName) throws Exception {

		File file = ResourceUtils.getFile(fileName);
		if (!file.exists()) {
			return null;
		}
		setResources(resourceLoader.getResource(fileName));
		final Map<String, Object> result = new HashMap<String, Object>();
		process(new MatchCallback() {
			public void process(Properties properties, Map<String, Object> map) {
				result.putAll(DisconfYamlProcessorImpl.this.getFlattenedMap(map));
			}
		});
		return result;
	}

 
	protected Yaml createYaml() {
		return new Yaml(new StrictMapAppenderConstructor(), new Representer(), new DumperOptions(), new org.yaml.snakeyaml.resolver.Resolver() {
			public void addImplicitResolver(org.yaml.snakeyaml.nodes.Tag tag, Pattern regexp, String first) {
				if (tag != org.yaml.snakeyaml.nodes.Tag.TIMESTAMP) {
					super.addImplicitResolver(tag, regexp, first);
				}
			}
		});
	}

}
