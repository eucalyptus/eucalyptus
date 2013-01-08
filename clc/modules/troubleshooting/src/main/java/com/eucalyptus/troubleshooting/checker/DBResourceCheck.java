/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.troubleshooting.checker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.eucalyptus.records.Logs;
import com.eucalyptus.system.SubDirectory;

import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;

/**
 * <p>
 * DBResourceCheck can be used by any eucalyptus component (walrus, SC, NC etc...) to perform periodic checks on db connections and warn the user when the system
 * runs low on db connections. This class provides a static method to {@link #start(DBChecker) start} the db resource check for a particular dbPool at a specified
 * interval.
 * </p>
 * <p>
 * {@link ScheduledExecutorService} is used for scheduling the db checks at configurable intervals. The thread pool size is limited to 1
 * </p>
 * <p>
 * If the system is running low on db connections a fault is recorded in the log file for the specified component. Subsequent faults for the same dbPool are not
 * logged until the state is reset for that dbPool. A state reset occurs when the number of db connections returns to below the fault threshold.
 * </p>
 */
public class DBResourceCheck extends Thread {
	private final static Logger LOG = Logger.getLogger(DBResourceCheck.class);
	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int OUT_OF_DB_CONNECTIONS_FAULT_ID = 1006;
	private final static long DEFAULT_POLL_INTERVAL = 60 * 1000;
	private static final Class<? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	/**
	 * Marking the constructor private on purpose, so that no code can instantiate an object this class
	 */
	private DBResourceCheck() {}
	/**
	 * <p>
	 * Kicks off an infinite series of disk resource checks with a delay in between consecutive checks. {@link ScheduledExecutorService#scheduleWithFixedDelay
	 * Executor service framework} is used for scheduling the worker thread, {@link DBChecker checker}, at regular intervals. The time delay, file dbPool, logic
	 * for disk space check and other configuration is provided by checker
	 * </p>
	 * 
	 * <p>
	 * This method returns a {@link ScheduledFuture} object that can be used by the caller to cancel the execution. Thread execution can also be cancelled by
	 * shutting down the executor service
	 * </p>
	 * 
	 * @param checker
	 * @return ScheduledFuture
	 */
	public static ScheduledFuture<?> start(DBChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class DBPoolInfo {
		private String alias;
		private Integer minimumFreeConnections;
		private Double percentFreeConnections;
		public String getAlias() {
			return alias;
		}
		public Integer getMaximumConnections() throws ProxoolException {
			return ProxoolFacade.getConnectionPoolDefinition(alias).getMaximumConnectionCount();		}

		public Integer getActiveConnections() throws ProxoolException{
			return ProxoolFacade.getSnapshot(alias, true).getActiveConnectionCount();
		}

		public Integer getThreshold() throws ProxoolException{
			if (null != this.minimumFreeConnections) {
				return this.minimumFreeConnections;
			} else {
				return (int) (this.getMaximumConnections() * this.percentFreeConnections / 100);
			}
		}

		public String getURL() throws ProxoolException {
			return ProxoolFacade.getConnectionPoolDefinition(alias).getCompleteUrl();
		}
		
		public String getDriver() throws ProxoolException {
			return ProxoolFacade.getConnectionPoolDefinition(alias).getDriver();
		}
		/**
		 * Constructor to be used when free connections is an absolute quantity
		 * 
		 * @param alias
		 * @param minimumFreeConnections
		 */
		public DBPoolInfo(String alias, Integer minimumFreeConnections) {
			super();
			this.alias = alias;
			this.minimumFreeConnections = minimumFreeConnections;
		}

		/**
		 * Constructor to be used when free connections is a percentage of the total connections available
		 * 
		 * @param file
		 * @param percentFreeSpace
		 */
		public DBPoolInfo(String alias, Double percentFreeConnections) {
			super();
			this.alias = alias;
			this.percentFreeConnections = percentFreeConnections;
		}

		// Added hashCode() and equals() since we do Set related operations
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((alias == null) ? 0 : alias.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DBPoolInfo other = (DBPoolInfo) obj;
			if (alias == null) {
				if (other.alias != null)
					return false;
			} else if (!alias.equals(other.alias))
				return false;
			return true;
		}
	}

	/**
	 * Worker thread that holds the logic for db checks and all the relevant information required. An instance of this class is fed to
	 * {@link ScheduledExecutorService#scheduleWithFixedDelay} method
	 * 
	 */
	public static class DBChecker implements Runnable {

		private Set<DBPoolInfo> dbPools = new HashSet<DBPoolInfo>();
		private long pollInterval;
		private Class<? extends ComponentId> componentIdClass;

		private Set<DBPoolInfo> alreadyFaulted = new HashSet<DBPoolInfo>();

		public DBChecker(DBPoolInfo dbPool) {
			this.dbPools.add(dbPool);
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public DBChecker(DBPoolInfo dbPool, Class<? extends ComponentId> componentIdClass, long pollTime) {
			this.dbPools.add(dbPool);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		public DBChecker(List<DBPoolInfo> dbPools, Class<? extends ComponentId> componentIdClass, long pollTime) {
			this.dbPools.addAll(dbPools);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		@Override
		public void run() {
			if (null != dbPools) {
				for (DBPoolInfo dbPool : this.dbPools) {
					// Enclose everything between try catch because nothing should throw an exception to the executor upstream or it may halt subsequent tasks
					try {
						Logs.extreme().debug("Polling dbpool " + dbPool.getAlias() + ",pollInterval=" + pollInterval + ", threshold = " + dbPool.getThreshold());
						if (dbPool.getMaximumConnections() - dbPool.getActiveConnections() < dbPool.getThreshold()) {
							if (!this.alreadyFaulted.contains(dbPool)) {
								Faults.forComponent(this.componentIdClass).havingId(OUT_OF_DB_CONNECTIONS_FAULT_ID)
										.withVar("component", ComponentIds.lookup(componentIdClass).getFaultLogPrefix())
										.withVar("alias", dbPool.getAlias())
										.withVar("maxConnections", "" + dbPool.getMaximumConnections())
										.withVar("activeConnections", "" + dbPool.getActiveConnections())
								        .withVar("scriptsDir", SubDirectory.SCRIPTS.getFile().getAbsolutePath()).log();
								this.alreadyFaulted.add(dbPool);
							} else {
								// fault has already been logged. do nothing
							}
						} else {
							// Remove this dbPool from the already faulted set. If the dbPool is not in the set, this call will simply return false. no harm
							// done. another if condition is just one unnecessary step
							this.alreadyFaulted.remove(dbPool);
						}
					} catch (Exception ex) {
						// what to do when an exception is caught? should we remove the dbPool off the list?
						LOG.error("db resource check failed for " + dbPool.getAlias(), ex);
					}
				}
			} else {
				// nothing to check
			}
		}
	}
	
}
