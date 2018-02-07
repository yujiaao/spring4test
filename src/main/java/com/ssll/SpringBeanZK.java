package com.ssll;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class SpringBeanZK {
	private String beanConfigFile = "beans.xml";
	private static final Log mLog = LogFactory.getLog(SpringBeanZK.class);

	public Object getBean(String _sResourceId) {
		return getXMLlBeanFactory().getBean(_sResourceId);
	}

	public void destroy() {
		if (m_oXMLBeanFactory != null) {
		 
			m_oXMLBeanFactory.destroy();
			
			m_oXMLBeanFactory = null;
		}
	}

	private GenericApplicationContext getXMLlBeanFactory() {
		if (m_oXMLBeanFactory == null)
			initFactory();
		return m_oXMLBeanFactory;
	}

	/**
	 * use log4j Loader
	 */
	private synchronized void initFactory() {
		if (m_oXMLBeanFactory != null) {
			return;
		} else {

			Resource resource = null;

			URL url = this.getClass().getResource("/" + beanConfigFile);

			if (url != null) {
				mLog.debug("Using URL [" + url + "] for automatic spring configuration.");
				resource = new UrlResource(url);
			} else {
				mLog.debug("Could not find resource: [" + beanConfigFile + "].");
			}

			m_oXMLBeanFactory = new GenericApplicationContext();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) m_oXMLBeanFactory);

			reader.loadBeanDefinitions(resource);
			
			m_oXMLBeanFactory.refresh();

			// Properties p =
			// Config.getZooProperties(ZK_CONFIG_SPRING_PATH+"/"+configProperties);
			// PropertyPlaceholderConfigurer cfg = new
			// PropertyPlaceholderConfigurer();
			// cfg.setProperties(p);
			// cfg.postProcessBeanFactory(m_oXMLlBeanFactory);
			//
		}
	}

	private GenericApplicationContext m_oXMLBeanFactory;

}