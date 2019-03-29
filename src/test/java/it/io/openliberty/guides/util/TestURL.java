package it.io.openliberty.guides.util;

public class TestURL {
	
	/**
	 * Get base URL including context root.
	 * 
	 * @return "base" URL with path ending with '/' 
	 */
	public static String getBaseURL() {
     String port = System.getProperty("liberty.test.port");
	 if (Boolean.getBoolean("ctx.root")) {
		 return "http://localhost:" + port + "/" + System.getProperty("ctxRoot") + "/";
	 } else {
		 return "http://localhost:" + port + "/";
	 }
	}
}
