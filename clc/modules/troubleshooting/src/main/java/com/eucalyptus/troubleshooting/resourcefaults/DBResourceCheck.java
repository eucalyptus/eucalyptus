package com.eucalyptus.troubleshooting.resourcefaults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ConnectionPoolStatisticsIF;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.admin.SnapshotIF;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

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
	private static final ComponentId DEFAULT_COMPONENT_ID = Eucalyptus.INSTANCE;

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

		public Integer getMinimumFreeConnections() throws ProxoolException{
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
		private ComponentId componentId;

		private Set<DBPoolInfo> alreadyFaulted = new HashSet<DBPoolInfo>();

		public DBChecker(DBPoolInfo dbPool) {
			this.dbPools.add(dbPool);
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentId = DEFAULT_COMPONENT_ID;
		}

		public DBChecker(DBPoolInfo dbPool, ComponentId componentId, long pollTime) {
			this.dbPools.add(dbPool);
			this.componentId = componentId;
			this.pollInterval = pollTime;
		}

		public DBChecker(List<DBPoolInfo> dbPools, ComponentId componentId, long pollTime) {
			this.dbPools.addAll(dbPools);
			this.componentId = componentId;
			this.pollInterval = pollTime;
		}

		@Override
		public void run() {
			if (null != dbPools) {
				for (DBPoolInfo dbPool : this.dbPools) {
					// Enclose everything between try catch because nothing should throw an exception to the executor upstream or it may halt subsequent tasks
					try {
						if (dbPool.getMaximumConnections() - dbPool.getActiveConnections() < dbPool.getMinimumFreeConnections()) {
							if (!this.alreadyFaulted.contains(dbPool)) {
								FaultSubsystem.forComponent(this.componentId).havingId(OUT_OF_DB_CONNECTIONS_FAULT_ID)
										.withVar("component", this.componentId.getName()).withVar("alias", dbPool.getAlias()).log();
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
