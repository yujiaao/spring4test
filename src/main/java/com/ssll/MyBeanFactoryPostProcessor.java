package com.ssll;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;


public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {  

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {  
        System.out.println("This is expected to called when the BeanFactory is created");  

		Properties p  = Config.getZooProperties();
		PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
		cfg.setProperties(p);
		cfg.postProcessBeanFactory(beanFactory);     
    }  
  
}  