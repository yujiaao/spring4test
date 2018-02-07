package com.ssll;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;


public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {  
    private static final String ZK_CONFIG_SPRING_PATH="default";
    private String path=ZK_CONFIG_SPRING_PATH;
    public void setPath(String path){this.path=path;}
    
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {  

		Properties p  = Config.getZooProperties(path);
		PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
		cfg.setProperties(p);
		cfg.postProcessBeanFactory(beanFactory);     
    }  
  
}  