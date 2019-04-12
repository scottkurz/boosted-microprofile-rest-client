// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
// tag::manager[]
package io.openliberty.guides.inventory;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.inventory.client.SystemClient;
import io.openliberty.guides.inventory.client.UnknownUrlException;
import io.openliberty.guides.inventory.client.UnknownUrlExceptionMapper;
import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.model.InvokeTracker;
import io.openliberty.guides.inventory.model.SystemData;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.logging.Logger;


@ApplicationScoped
public class InventoryManager {
	
  private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
  
  private static String newline = System.getProperty("line.separator");
  
  Logger LOG = Logger.getLogger(InventoryManager.class.getName());
  
  @PersistenceContext(unitName = "guideapp-persister")
  private EntityManager em;

  
  /**
   * This is the port we're going to use for the dynamically-build invocation of the system resource
   * on the "back-end" port, so we don't want to confuse it with the HTTP port that this InventoryManager
   * instance is running under, but rather use a totally new configuration property.
   * 
   * This allows us to invoke the back-end service on a remote host, like we can with the MP RestClient invocation
   */
  //private final String DEFAULT_PORT = System.getProperty("default.http.port");
  @Inject @ConfigProperty(name="back.end.system.port")
  private String backEndSystemServicePort;
  
  @Inject
  @RestClient
  private SystemClient defaultRestClient;

	public Properties get(String hostname) {
		Properties properties = null;
		int cnt = 0;
		try {
			
			Context ctx = new InitialContext();
			// Before getting an EntityManager, start a global transaction
			UserTransaction tran = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
			tran.begin();
			
			if (hostname.equals("localhost")) {
				properties = getPropertiesWithDefaultHostName();
			} else {
				properties = getPropertiesWithGivenHostName(hostname);
			}

			incInvokeCnt(hostname);
			cnt = getInvokeCnt(hostname);
			Date setInvokeTS = Calendar.getInstance().getTime();
			LOG.info("current invocation timestamp: " + setInvokeTS);
			Date lastInvokeTS = getLastInvokedTS(hostname);
			
			if (lastInvokeTS != null) {
				LOG.info("The timestamp of previous invocation, as obtained from the DB: " + lastInvokeTS);
			}
			else {
				LOG.info("This is the first time the app has been invoked for the provided host: " + hostname);
			}
			
			setInvokeTS(hostname, setInvokeTS);
			tran.commit();
		} catch (Exception e) {
			LOG.info("Something went wrong. Caught exception " + e + newline);
		}
		LOG.info("App invoked : " + cnt + " times for host: " + hostname);
		return properties;
	}

  private Date getLastInvokedTS(String hostname) {
	  InvokeTracker invTracker = null;
		try {
			
			invTracker = em.find(InvokeTracker.class, hostname);
			if (invTracker == null) {
				createInvokeCnt(new InvokeTracker(hostname));
				invTracker = em.find(InvokeTracker.class, hostname);
			}
			
		} catch (Exception e) {
			LOG.info("Something went wrong. Caught exception " + e + newline);
		}
		return invTracker.getTimestamp();
	}

private void setInvokeTS(String hostname, Date time) {
		try {
			InvokeTracker invTracker = em.find(InvokeTracker.class, hostname);
			invTracker.setTimestamp(time);
		} catch (Exception e) {
			LOG.info("Something went wrong. Caught exception " + e + newline);
	    }
		
	}

public int getInvokeCnt(String hostname) {

		InvokeTracker invTracker = null;
		try {
			
			invTracker = em.find(InvokeTracker.class, hostname);
			if (invTracker == null) {
				createInvokeCnt(new InvokeTracker(hostname));
				invTracker = em.find(InvokeTracker.class, hostname);
			}
			
		} catch (Exception e) {
			LOG.info("Something went wrong. Caught exception " + e + newline);
		}
		return invTracker.getCount();
	}
  
	public void incInvokeCnt(String hostname) {
		
		try {
			InvokeTracker invTracker = em.find(InvokeTracker.class, hostname);
			invTracker.setCount();
		} catch (Exception e) {
			LOG.info("Something went wrong. Caught exception " + e + newline);
	    }
	}
	
  public void add(String hostname, Properties systemProps) {
	SystemData host = null;
    Properties props = new Properties();
    props.setProperty("os.name", systemProps.getProperty("os.name"));
    props.setProperty("user.name", systemProps.getProperty("user.name"));
	
	host = new SystemData(hostname, props);
    if (!systems.contains(host))
      systems.add(host);
  }

  // do EM / JPA stuff here?
  public void createInvokeCnt(InvokeTracker invokeTracker){
	try {
		LOG.info("Creating a new InvokeTracker with " + em.getDelegate().getClass() + newline);

        em.persist(invokeTracker);

        String hostname = invokeTracker.getHostname();
        LOG.info("Created and persisted InvokeTracker instance for host name " + hostname + newline);
	} catch (Exception e) {
		LOG.info("Something went wrong. Caught exception " + e + newline);
    }	
  }
  
  public InventoryList list() {
    return new InventoryList(systems);
  }

  private Properties getPropertiesWithDefaultHostName() {
    try {
      return defaultRestClient.getProperties();
    } catch (UnknownUrlException e) {
      System.err.println("The given URL is unreachable.");
    } catch (ProcessingException ex) {
      handleProcessingException(ex);
    }
    return null;
  }

  // tag::builder[]
  private Properties getPropertiesWithGivenHostName(String hostname) {
    String customURLString = "http://" + hostname + ":" + backEndSystemServicePort + "/system";
    URL customURL = null;
    try {
      customURL = new URL(customURLString);
      SystemClient customRestClient = RestClientBuilder.newBuilder()
                                         .baseUrl(customURL)
                                         .register(UnknownUrlExceptionMapper.class)
                                         .build(SystemClient.class);
      return customRestClient.getProperties();
    } catch (ProcessingException ex) {
      handleProcessingException(ex);
    } catch (UnknownUrlException e) {
      System.err.println("The given URL is unreachable.");
    } catch (MalformedURLException e) {
      System.err.println("The given URL is not formatted correctly.");
    }
    return null;
  }
  // end::builder[]

  private void handleProcessingException(ProcessingException ex) {
    Throwable rootEx = ExceptionUtils.getRootCause(ex);
    if (rootEx != null && (rootEx instanceof UnknownHostException || rootEx instanceof ConnectException)) {
      System.err.println("The specified host is unknown.");
    } else {
      throw ex;
    }
  }

}
// end::manager[]
