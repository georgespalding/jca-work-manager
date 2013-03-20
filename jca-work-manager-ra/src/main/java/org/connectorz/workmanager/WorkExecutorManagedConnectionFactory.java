/*
Copyright 2012 Adam Bien, adam-bien.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.connectorz.workmanager;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.*;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.work.WorkManager;
import javax.security.auth.Subject;
import javax.validation.constraints.Size;

import org.connectorz.workmanager.impl.*;

/**
 * @author adam bien, adam-bien.com
 */
@ConnectionDefinition(
		connectionFactory = WorkExecutorFactory.class,
		connectionFactoryImpl = WorkExecutorFactoryImpl.class,
		connection = Executor.class,
		connectionImpl = WorkExecutorImpl.class)
public class WorkExecutorManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation, Serializable {
	private static final String CLASS_NAME=WorkExecutorManagedConnectionFactory.class.getName();
	private static final Logger log=Logger.getLogger(CLASS_NAME);
	
	private static final long serialVersionUID = 1L;

	private PrintWriter out;
	private WorkManagerBootstrap workManagerAdapter;
	private int maxNumberOfConcurrentRequests;

	@Size(min = 1)
	@ConfigProperty(defaultValue = "2", supportsDynamicUpdates = true, description = "Maximum number of concurrent connections from different processes that an EIS instance can supportMaximum number of concurrent connections from different processes that an EIS instance can support")
	public void setMaxNumberOfConcurrentRequests(Integer maxNumberOfConcurrentRequests) {
		log.entering(CLASS_NAME, "setMaxNumberOfConcurrentRequests", maxNumberOfConcurrentRequests);
		this.maxNumberOfConcurrentRequests = maxNumberOfConcurrentRequests;
		log.exiting(CLASS_NAME, "setMaxNumberOfConcurrentRequests");
	}

	public int getMaxNumberOfConcurrentRequests() {
		log.entering(CLASS_NAME, "getMaxNumberOfConcurrentRequests");
		log.exiting(CLASS_NAME, "getMaxNumberOfConcurrentRequests", maxNumberOfConcurrentRequests);
		return maxNumberOfConcurrentRequests;
	}

	@Override
	public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
		out.println("#ConnectionFactory.createConnectionFactory,1");
		return new WorkExecutorFactoryImpl(out, this, cxManager);
	}

	@Override
	public Object createConnectionFactory() throws ResourceException {
		log.entering(CLASS_NAME, "createConnectionFactory");
		Object ret = new WorkExecutorFactoryImpl(out, this, null);
		log.exiting(CLASS_NAME, "createConnectionFactory", ret);
		return ret;
	}

	@Override
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info) {
		log.entering(CLASS_NAME, "createManagedConnection", new Object[]{subject, info});
	
		ManagedConnection ret = new WorkExecutorManagedConnection(this, null);

		log.exiting(CLASS_NAME, "createManagedConnection", ret);
		return ret;
	}

	@Override
	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info) throws ResourceException {
		log.entering(CLASS_NAME, "matchManagedConnections", new Object[]{connectionSet, subject, info});
		
		WorkExecutorManagedConnection ret = null;
		for (Iterator<?> it = connectionSet.iterator(); it.hasNext();) {
			Object object = it.next();
			if (object instanceof WorkExecutorManagedConnection) {
				WorkExecutorManagedConnection gmc = (WorkExecutorManagedConnection) object;
				ConnectionRequestInfo connectionRequestInfo = gmc.getConnectionRequestInfo();
				if (info == null || connectionRequestInfo.equals(info)) {
					ret = gmc;
					break;
				}
			} else {
				log.logp(Level.WARNING, CLASS_NAME, "matchManagedConnections", "Object {0} is not a {1} as expected.", new Object[]{object, WorkExecutorManagedConnection.class.getName()});
				out.println("#ConnectionFactory.matchManagedConnections " + object + " is not a Connection");
			}
		}
		log.exiting(CLASS_NAME, "createManagedConnection", ret);
		return ret;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws ResourceException {
		log.entering(CLASS_NAME, "setLogWriter", out);
		this.out = out;
		log.exiting(CLASS_NAME, "setLogWriter");
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		log.entering(CLASS_NAME, "getLogWriter");
		log.exiting(CLASS_NAME, "getLogWriter", out);
		return this.out;
	}

	@Override
	public ResourceAdapter getResourceAdapter() {
		log.entering(CLASS_NAME, "getResourceAdapter");
		log.exiting(CLASS_NAME, "getResourceAdapter", workManagerAdapter);
		return workManagerAdapter;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		log.entering(CLASS_NAME, "setResourceAdapter", out);
		this.workManagerAdapter = (WorkManagerBootstrap) ra;
		log.exiting(CLASS_NAME, "setResourceAdapter");
	}

	public WorkManager getWorkManager() {
		log.entering(CLASS_NAME, "getWorkManager");
		WorkManager ret = this.workManagerAdapter.getBootstrapContext().getWorkManager();
 		log.exiting(CLASS_NAME, "getWorkManager", workManagerAdapter);
		return ret;
	}
}
