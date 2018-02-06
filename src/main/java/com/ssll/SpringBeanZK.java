package com.ssll;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class SpringBeanZK {
	private String beanConfigFile = "beans.xml";
	private static final Log mLog = LogFactory.getLog(SpringBeanZK.class);

	public Object getBean(String _sResourceId) {
		return getXMLlBeanFactory().getBean(_sResourceId);
	}

	public void destroy() {
		if (m_oXMLlBeanFactory == null) {
			return;
		} else {
			m_oXMLlBeanFactory.destroySingletons();
			m_oXMLlBeanFactory = null;
			return;
		}
	}

	private DefaultListableBeanFactory getXMLlBeanFactory() {
		if (m_oXMLlBeanFactory == null)
			initFactory();
		return m_oXMLlBeanFactory;
	}

	/**
	 * use log4j Loader
	 */
	private synchronized void initFactory() {
		if (m_oXMLlBeanFactory != null) {
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

			m_oXMLlBeanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) m_oXMLlBeanFactory);

			reader.loadBeanDefinitions(resource);

			// Properties p =
			// Config.getZooProperties(ZK_CONFIG_SPRING_PATH+"/"+configProperties);
			// PropertyPlaceholderConfigurer cfg = new
			// PropertyPlaceholderConfigurer();
			// cfg.setProperties(p);
			// cfg.postProcessBeanFactory(m_oXMLlBeanFactory);
			//
		}
	}

	private DefaultListableBeanFactory m_oXMLlBeanFactory;

}