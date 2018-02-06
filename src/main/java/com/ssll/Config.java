package com.ssll;

import java.util.Properties;

public class Config {

	public static String getProperty(String string, String string2) {
		return string2;
	}

	public static Properties getZooProperties() {
		Properties p = new Properties();
		p.setProperty("dummy", "foo");
		return p;
	}

}
