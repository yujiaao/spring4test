package com.ssll;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpringBeanZKTest {

	@Test
	public void testCreateObjectById() {

		MyFoo o = (MyFoo)new SpringBeanZK().getBean("myobj");
		
		assertEquals("foo", o.getRealname());
		 
	}

}
