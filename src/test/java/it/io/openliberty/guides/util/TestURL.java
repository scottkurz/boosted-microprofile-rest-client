package it.io.openliberty.guides.util;

public class TestURL {
	
	/**
	 * Get base URL including context root.
	 * 
	 * @return "base" URL with path ending with '/' 
	 */
	public static String getBaseURL() {
     String port = System.getProperty("liberty.test.port");
     String ctxRoot = System.getProperty("ctx.root");
     if (ctxRoot == null || ctxRoot.equals("/") || ctxRoot.equals("")) {
		 return "http://localhost:" + port + "/";
	 } else {
		 return "http://localhost:" + port + "/" + ctxRoot + "/";
	 }
	}
}
