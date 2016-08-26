//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.baidu.disconf.client.addons.properties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import com.baidu.disconf.client.DisconfMgr;

public class ReloadableYamlPropertiesFactoryBean extends YamlProcessor
		implements DisposableBean, ApplicationContextAware, FactoryBean<Properties>, InitializingBean {
	protected static final Logger log = LoggerFactory.getLogger(ReloadableYamlPropertiesFactoryBean.class);
	private boolean singleton = true;
	private Properties properties;
	private Resource[] locations;
	private long[] lastModified;
	private List<IReloadablePropertiesListener> preListeners;
	private ApplicationContext applicationContext;

	public ReloadableYamlPropertiesFactoryBean() {
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public boolean isSingleton() {
		return this.singleton;
	}

	public void afterPropertiesSet() throws IOException {
		if (this.isSingleton()) {
			this.properties = this.createProperties();
		}

	}

	public Properties getObject() throws IOException {
		return this.properties != null ? this.properties : this.createProperties();
	}

	public Class<?> getObjectType() {
		return Properties.class;
	}

	/**
	 * 定义资源文件
	 *
	 * @param fileNames
	 */
	public void setLocation(final String fileNames) {
		List<String> list = new ArrayList<String>();
		list.add(fileNames);
		setLocations(list);
	}

	public void setLocations(List<String> fileNames) {

		List<Resource> resources = new ArrayList<Resource>();
		for (String filename : fileNames) {

			// trim
			filename = filename.trim();

			String realFileName = getFileName(filename);

			//
			// register to disconf
			//
			DisconfMgr.getInstance().reloadableScan(realFileName);

			//
			// only properties will reload
			//
			String ext = FilenameUtils.getExtension(filename);
			if (ext.equals("yml") || ext.equals("yaml")) {

				PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
				try {
					Resource[] resourceList = pathMatchingResourcePatternResolver.getResources(filename);
					for (Resource resource : resourceList) {
						resources.add(resource);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		this.locations = resources.toArray(new Resource[resources.size()]);
		lastModified = new long[locations.length];
		this.setMatchDefault(true);
		setDocumentMatchers(new YamlProcessor.DocumentMatcher[] { new SpringProfileDocumentMatcher() });
		super.setResources(locations);

	}
    protected Yaml createYaml() {
        return new Yaml(new StrictMapAppenderConstructor(), new Representer(), new DumperOptions(), new Resolver() {
            public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
                if(tag != Tag.TIMESTAMP) {
                    super.addImplicitResolver(tag, regexp, first);
                }
            }
        });
    }
	/**
	 * get file name from resource
	 *
	 * @param fileName
	 *
	 * @return
	 */
	private String getFileName(String fileName) {

		if (fileName != null) {
			int index = fileName.indexOf(':');
			if (index < 0) {
				return fileName;
			} else {

				fileName = fileName.substring(index + 1);

				index = fileName.lastIndexOf('/');
				if (index < 0) {
					return fileName;
				} else {
					return fileName.substring(index + 1);
				}

			}
		}
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * listener , 用于通知回调
	 *
	 * @param listeners
	 */
	public void setListeners(final List listeners) {
		// early type check, and avoid aliassing
		this.preListeners = new ArrayList<IReloadablePropertiesListener>();
		for (Object o : listeners) {
			preListeners.add((IReloadablePropertiesListener) o);
		}
	}

	private ReloadablePropertiesBase reloadableProperties;

	protected Properties createProperties() throws IOException {
		return (Properties) createMyInstance();
	}

	/**
	 * 根据修改时间来判定是否reload
	 *
	 * @param forceReload
	 *
	 * @throws IOException
	 */
	protected void reload(final boolean forceReload) throws IOException {

		boolean reload = forceReload;
		for (int i = 0; i < locations.length; i++) {
			Resource location = locations[i];
			File file;

			try {
				file = location.getFile();
			} catch (IOException e) {
				// not a file resource
				// may be spring boot
				log.warn(e.toString());
				continue;
			}
			try {
				long l = file.lastModified();

				if (l > lastModified[i]) {
					lastModified[i] = l;
					reload = true;
				}
			} catch (Exception e) {
				// cannot access file. assume unchanged.
				if (log.isDebugEnabled()) {
					log.debug("can't determine modification time of " + file + " for " + location, e);
				}
			}
		}
		if (reload) {
			doReload();
		}
	}

	protected Object createMyInstance() throws IOException {
		// would like to uninherit from AbstractFactoryBean (but it's final!)
		if (!isSingleton()) {
			throw new RuntimeException("ReloadableYamlPropertiesFactoryBean only works as singleton");
		}
		// set listener
		reloadableProperties = new ReloadablePropertiesImpl();
		if (preListeners != null) {
			reloadableProperties.setListeners(preListeners);
		}

		// reload
		reload(true);

		// add for monitor
		ReloadConfigurationMonitor.addReconfigurableBean((ReconfigurableBean) reloadableProperties);

		return reloadableProperties;

	}

	/**
	 * 设置新的值
	 *
	 * @throws IOException
	 */
	private void doReload() throws IOException {
		 final LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
         this.process(new MatchCallback() {
             public void process(Properties properties, Map<String, Object> map) {
                 result.putAll(ReloadableYamlPropertiesFactoryBean.this.getFlattenedMap(map));
             }
         });
		Properties properties = new Properties();
		properties.putAll(result);
		reloadableProperties.setProperties(properties);
	}

	@Override
	public void destroy() throws Exception {
		reloadableProperties = null;

	}

	/**
	 * 回调自己
	 */
	class ReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {

		// reload myself
		public void reloadConfiguration() throws Exception {
			ReloadableYamlPropertiesFactoryBean.this.reload(false);
		}

	}

}
