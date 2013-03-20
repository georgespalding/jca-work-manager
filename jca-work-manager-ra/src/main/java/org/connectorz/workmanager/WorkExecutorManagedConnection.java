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
import java.security.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.*;

import javax.resource.ResourceException;
import static javax.resource.spi.ConnectionEvent.*;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.connectorz.workmanager.impl.WorkExecutorImpl;

/**
 * 
 * @author adam bien, adam-bien.com
 */
public class WorkExecutorManagedConnection implements ManagedConnection {
	private static final String CLASS_NAME=WorkExecutorManagedConnection.class.getName();
	private static final Logger log=Logger.getLogger(CLASS_NAME);
	
	private WorkExecutorManagedConnectionFactory connectionFactory;
	private PrintWriter out;
	private WorkExecutorImpl executorConnection;
	private ConnectionRequestInfo connectionRequestInfo;
	private List<ConnectionEventListener> listeners;

	WorkExecutorManagedConnection(WorkExecutorManagedConnectionFactory connectionFactory, ConnectionRequestInfo connectionRequestInfo) {
		log.entering(CLASS_NAME, "Connection", new Object[]{connectionFactory,connectionRequestInfo});
		
		this.connectionFactory = connectionFactory;
		this.connectionRequestInfo = connectionRequestInfo;
		this.listeners = new LinkedList<>();
		
		log.exiting(CLASS_NAME, "Connection");
	}

	@Override
	public WorkExecutor getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
		log.entering(CLASS_NAME, "getConnection", new Object[]{subject,connectionRequestInfo});
		
		log.logp(Level.FINER, CLASS_NAME, "getConnection", "creating new WorkManagerExecutorImpl");
		executorConnection = new WorkExecutorImpl(this, connectionFactory, connectionRequestInfo);

		log.exiting(CLASS_NAME, "getConnection",executorConnection);
		return executorConnection;
	}

	@Override
	public void associateConnection(Object connection) {
		log.entering(CLASS_NAME, "associateConnection", connection);

		this.executorConnection = (WorkExecutorImpl) connection;

		log.exiting(CLASS_NAME, "associateConnection");
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener listener) {
		log.entering(CLASS_NAME, "addConnectionEventListener", listener);
		listeners.add(listener);
		log.exiting(CLASS_NAME, "addConnectionEventListener");
	}

	@Override
	public void removeConnectionEventListener(ConnectionEventListener listener) {
		log.entering(CLASS_NAME, "removeConnectionEventListener", listener);
		this.listeners.remove(listener);
		log.exiting(CLASS_NAME, "removeConnectionEventListener");
	}

	@Override
	public XAResource getXAResource() throws ResourceException {
		log.entering(CLASS_NAME, "getXAResource");
		
		XAResource ret=null;

		log.exiting(CLASS_NAME, "getXAResource", ret);
		return ret;
	}

	@Override
	public ManagedConnectionMetaData getMetaData() throws ResourceException {
		log.entering(CLASS_NAME, "getMetaData");
		ManagedConnectionMetaData ret = new ManagedConnectionMetaData() {

			private final String CLASS_NAME=WorkExecutorManagedConnection.class.getName();
			private final Logger log=Logger.getLogger(CLASS_NAME);

			public String getEISProductName() throws ResourceException {
				String ret="Work Manager JCA";

				log.logp(Level.FINEST, CLASS_NAME, "getEISProductName", "call returns:{0}", ret);
				return ret;
			}

			@Override
			public String getEISProductVersion() throws ResourceException {
				String ret="1.0";

				log.logp(Level.FINEST, CLASS_NAME, "getEISProductVersion", "call returns:{0}", ret);
				return ret;
			}

			@Override
			public int getMaxConnections() throws ResourceException {
				int ret=connectionFactory.getMaxNumberOfConcurrentRequests();

				log.logp(Level.FINEST, CLASS_NAME, "getMaxConnections", "call returns:{0}", ret);
				return ret;
			}

			@Override
			public String getUserName() throws ResourceException {
				Subject s=Subject.getSubject(AccessController.getContext());
				String ret=s.getPrincipals().iterator().next().getName();

				log.logp(Level.FINEST, CLASS_NAME, "getUserName", "call returns:{0}", ret);
				return ret;
			}
		};

		log.exiting(CLASS_NAME, "getMetaData", ret);
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
		log.entering(CLASS_NAME, "setLogWriter");
		log.exiting(CLASS_NAME, "setLogWriter", out);
		return out;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((connectionRequestInfo == null) ? 0 : connectionRequestInfo
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WorkExecutorManagedConnection other = (WorkExecutorManagedConnection) obj;
		if (connectionRequestInfo == null) {
			return other.connectionRequestInfo == null;
		} else {
			return connectionRequestInfo.equals(other.connectionRequestInfo);
		}
	}

	public ConnectionRequestInfo getConnectionRequestInfo() {
		log.entering(CLASS_NAME, "getConnectionRequestInfo");
		log.exiting(CLASS_NAME, "getConnectionRequestInfo", connectionRequestInfo);
		return connectionRequestInfo;
	}

	private void fireConnectionEvent(int event) {
		log.entering(CLASS_NAME, "fireConnectionEvent", event);
		ConnectionEvent connnectionEvent = new ConnectionEvent(this, event);
		connnectionEvent.setConnectionHandle(this.executorConnection);
		for (ConnectionEventListener listener : this.listeners) {
			switch (event) {
			case LOCAL_TRANSACTION_STARTED:
				listener.localTransactionStarted(connnectionEvent);
				break;
			case LOCAL_TRANSACTION_COMMITTED:
				listener.localTransactionCommitted(connnectionEvent);
				break;
			case LOCAL_TRANSACTION_ROLLEDBACK:
				listener.localTransactionRolledback(connnectionEvent);
				break;
			case CONNECTION_CLOSED:
				listener.connectionClosed(connnectionEvent);
				break;
			default:
				throw new IllegalArgumentException("Unknown event: " + event);
			}
		}
		log.exiting(CLASS_NAME, "fireConnectionEvent");
	}

	public void close() {
		log.entering(CLASS_NAME, "close");
		fireConnectionEvent(CONNECTION_CLOSED);
		log.exiting(CLASS_NAME, "close");
	}

	@Override
	public LocalTransaction getLocalTransaction() throws ResourceException {
		log.entering(CLASS_NAME, "getLocalTransaction");
		return new LocalTransaction() {
			
			@Override
			public void rollback() throws ResourceException {
				log.logp(Level.FINER,getClass().getName(), "rollback","Äsch");
			}
			
			@Override
			public void commit() throws ResourceException {
				log.logp(Level.FINER,getClass().getName(), "commit","Gört");
			}
			
			@Override
			public void begin() throws ResourceException {
				log.logp(Level.FINER,getClass().getName(), "begin","Börja");
			}
		};
	}

	@Override
	public void destroy() {
		log.entering(CLASS_NAME, "destroy");
		log.exiting(CLASS_NAME, "destroy");
	}

	@Override
	public void cleanup() {
		log.entering(CLASS_NAME, "cleanup");
		log.exiting(CLASS_NAME, "cleanup");
	}


}
