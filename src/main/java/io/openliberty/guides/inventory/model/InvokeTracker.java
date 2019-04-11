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
package io.openliberty.guides.inventory.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@Entity
@Table(name = "INVOKECNT", schema = "APP")
public class InvokeTracker {

  @Id
  @Column(name = "HOST_NAME")
  private String hostname;
  
  @Column(name = "APP_INVOKE_CNT")
  int count;
  
  public InvokeTracker(){
	  this.hostname = "hostname";
	  this.count = 0;
  }

  public InvokeTracker(String hostname) {
    this.hostname = hostname;
	this.count++;
	System.out.println("AJM: put invoke cnt data into table?");
  }

  public String getHostname() {
    return hostname;
  }

  public int getCount() {
    return count;
  }
  
  public void setCount() {
	  this.count++;
  }

  
  @Override
  public boolean equals(Object host) {
    if (host instanceof InvokeTracker) {
      return hostname.equals(((SystemData) host).getHostname());
    }
    return false;
  }
}
