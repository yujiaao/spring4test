# spring test

A minimal spring test project, try to figure out the BeanFactoryPostProcessor usage in XML file.

run

     mvn test
     
 gives error
	 
	 org.junit.ComparisonFailure: expected:<[foo]> but was:<[${dummy}]>
		at org.junit.Assert.assertEquals(Assert.java:115)
		at org.junit.Assert.assertEquals(Assert.java:144)
		at com.ssll.SpringBeanZKTest.testCreateObjectById    (SpringBeanZKTest.java:14)