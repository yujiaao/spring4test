/*
 * Config.java
 *
 */

package com.ssll;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DatabaseConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ZKPropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.ZKNodeChangeEventReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * This is the single entry point for accessing configuration properties
 * 
 * @author original by Allen Gilliland
 * @author modified by Yujiaao
 */
public class Config implements AutoCloseable{

	private static final String CONFIG_ZOOKEEPER_KEY = "config.zookeeper";
	private static final String ZK_CONFIG_APP_PROPERTIES = "/config/app.properties";
	private static String default_config = "/ApplicationResources.properties";
	private static String custom_config = "/custom.properties";
	private static String custom_jvm_param = "custom.config";
	private static File custom_config_file = null;
	private static CompositeConfiguration config = null;
	private static Properties mConfig;

	private static Log mLogger = LogFactory.getFactory().getInstance(
			Config.class);

	/*
	 * Static block run once at class loading
	 * 
	 * We load the default properties and any custom properties we find
	 */
	static {
		mConfig = new Properties();
		mConfig.put("dummy", "foo");

		try {
			// we'll need this to get at our properties files in the classpath
			Class<?> config_class = Class.forName("com.ssll.Config");

			// first, lets load our default properties
			InputStream is = config_class.getResourceAsStream(default_config);
			if (is != null) {
				mConfig.load(is);
				mLogger.info("successfully loaded default properties.");
			} else {
				is = config_class.getClassLoader().getResourceAsStream(
						default_config);
				if (is != null) {
					mConfig.load(is);
					mLogger.info("successfully loaded default properties.");
				} else {
					mLogger.info("no properties file found in classpath "
							+ default_config);
				}
			}

			// now, see if we can find our custom config

			is = config_class.getResourceAsStream(custom_config);
			if (is != null) {
				mConfig.load(is);
				mLogger
						.info("successfully loaded custom properties file from classpath");
			} else {

				is = config_class.getClassLoader().getResourceAsStream(
						default_config);
				if (is != null) {
					mConfig.load(is);
					mLogger.info("successfully loaded default properties.");
				} else {
					mLogger
							.info("no custom properties file found in classpath");
				}
			}

			// finally, check for an external config file
			String env_file = System.getProperty(custom_jvm_param);
			if (env_file != null && env_file.length() > 0) {
				custom_config_file = new File(env_file);

				// make sure the file exists, then try and load it
				if (custom_config_file != null && custom_config_file.exists()) {
					is = new FileInputStream(custom_config_file);
					mConfig.load(is);
					mLogger.info("successfully loaded custom properties from "
							+ custom_config_file.getAbsolutePath());
				} else {
					mLogger.warn("failed to load custom properties from "
							+ custom_config_file==null?null:custom_config_file.getAbsolutePath());
				}

			} else {
				mLogger
						.info("no custom properties file specified via jvm option");
			}

			// Now expand system properties for properties in the
			// config.expandedProperties list,
			// replacing them by their expanded values.
			String expandedPropertiesDef = (String) mConfig
					.get("config.expandedProperties");
			if (expandedPropertiesDef != null) {
				String[] expandedProperties = expandedPropertiesDef.split(",");
				for (int i = 0; i < expandedProperties.length; i++) {
					String propName = expandedProperties[i].trim();
					String initialValue = (String) mConfig.get(propName);
					if (initialValue != null) {
						String expandedValue = PropertyExpander
								.expandSystemProperties(initialValue);
						mConfig.put(propName, expandedValue);
						if (mLogger.isDebugEnabled()) {
							mLogger.info("Expanded value of " + propName
									+ " from '" + initialValue + "' to '"
									+ expandedValue + "'");
						}
					}
				}
			}

			// -----
			try {
				
				String dbsource = getProperty("config.datasource");
				
				if (!"".equals(dbsource)
						&& dbsource != null) {
					Class<?> cls = Class.forName(dbsource);
					String table = getProperty("config.datasource.table");
					String nameColumn = getProperty("config.datasource.namecolumn");
					String keyColumn = getProperty("config.datasource.keycolumn");
					String valueColumn = getProperty("config.datasource.valuecolumn");

					config = new CompositeConfiguration();

					DataSource ds = (DataSource) (cls.newInstance());
					DatabaseConfiguration dbconfig = new DatabaseConfiguration(
							ds, table, nameColumn, keyColumn, valueColumn,
							"dbconfig");

					config.addConfiguration(dbconfig);
					config.addConfiguration(new MapConfiguration(mConfig));
					

				} else {
					config = new CompositeConfiguration();
					String fileName = 
							PathTools.getFullPathRelateClass("../classes",  //jar 包的相对路径
									Config.class)
									+ default_config;
					File f = new File(fileName);
					if(f.exists()){
						config.addConfiguration(new PropertiesConfiguration(
							fileName));
					}else{
						mLogger.debug(fileName+" not exists, not loaded");
					}
					mLogger.debug("config.datasource  not configured，use memory to store properties");
				}
				
				
				String zookeeper_url = getProperty(CONFIG_ZOOKEEPER_KEY);
				if(StringUtils.trimToNull(zookeeper_url)!=null){
					mLogger.debug("try to load zookeeper  configuration");
					Configuration zooConfig = zookeeperInit(zookeeper_url, ZK_CONFIG_APP_PROPERTIES);
					if(zooConfig!=null){
						config.addConfiguration(zooConfig);
						mLogger.debug("finished to load configuration from zookeeper");
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
			// -----

			// some debugging for those that want it
			if (mLogger.isDebugEnabled()) {
				mLogger.debug("Config looks like this ...");

				String key = null;
				Enumeration<Object> keys = mConfig.keys();
				while (keys.hasMoreElements()) {
					key = (String) keys.nextElement();
					mLogger.debug(key + "=" + mConfig.getProperty(key));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// no, you may not instantiate this class :p
	private Config() {
	}


	/**
	 * Retrieve a property value
	 * 
	 * @param key
	 *            Name of the property
	 * @return String Value of property requested, null if not found
	 */
	public static String getProperty(String key) {
		String result = mConfig.getProperty(key);
		return result == null ? config == null ? null : config.getString(key)
				: result;
	}
	
	public static String getProperty(String key, String defaultValue) {
		String result = mConfig.getProperty(key);
		result =  result == null ? config == null ? null : config.getString(key)
				: result;
		if(StringUtils.isBlank(result))
			result = defaultValue;
		return result;
	}

	/**
	 * Only DatabaseConfiguration persist values permanently.
	 */
	public static void setProperty(String name, Object value) {
		config.setProperty(name, value);

		// if it is DatabaseConfiguration
		if (config.getConfiguration(0) != null)
			config.getConfiguration(0).setProperty(name, value);

	}

	
	
	protected static byte[] getRawData(String path){
		try {
			return client.getData().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
	
	protected static String getRawDataAsString(String path){
		return new String(getRawData(path));
	}
	
	public static Properties getZooProperties(String path){
		String res = getRawDataAsString(path);
		Properties p = new Properties();
		try(Reader in = new StringReader(res)){
			p.load(in);
		} catch (IOException e) {
			mLogger.debug("Could not load properties from zookeeper: [" + path + "].");
			e.printStackTrace();
		}
		return p;

	}
	
	static CuratorFramework client;
	
	private static Configuration zookeeperInit(String connectString, String nodePath){
		// Init Curator
		//String connectString = "localhost:2181";
		client = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1000, 3));
		client.start();

		try {
		    ZKPropertiesConfiguration config = new ZKPropertiesConfiguration(client, nodePath); 

		    // add reloading strategy
		    // properties are reloaded when zookeeper node changes.
		    config.setReloadingStrategy(new ZKNodeChangeEventReloadingStrategy());

		    // add listener
		    config.addConfigurationListener(new ConfigurationListener() {
		        public void configurationChanged(final ConfigurationEvent event) {
		            if (!event.isBeforeUpdate()) {
		                switch(event.getType()) {
		                    case ZKPropertiesConfiguration.EVENT_NODE_CREATE : 
		                        System.out.println("Path '" + event.getPropertyValue() + "' has been created !");
		                        break;
		                    case ZKPropertiesConfiguration.EVENT_NODE_UPDATE : 
		                        System.out.println("Path '" + event.getPropertyValue() + "' has been updated !");
		                        break;
		                    case ZKPropertiesConfiguration.EVENT_NODE_DELETE : 
		                        System.out.println("Path '" + event.getPropertyValue() + "' has been deleted !");
		                        break;
		                }
		            }
		        }
		    });

		    return config;
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		if(client!=null){
			client.close();
		}
		
	}

}
