/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import net.sf.hajdbc.Balancer;
import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseActivationListener;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.DatabaseClusterDecorator;
import net.sf.hajdbc.DatabaseClusterFactory;
import net.sf.hajdbc.DatabaseClusterMBean;
import net.sf.hajdbc.DatabaseDeactivationListener;
import net.sf.hajdbc.DatabaseEvent;
import net.sf.hajdbc.DatabaseMetaDataCache;
import net.sf.hajdbc.DatabaseMetaDataCacheFactory;
import net.sf.hajdbc.Dialect;
import net.sf.hajdbc.LockManager;
import net.sf.hajdbc.Messages;
import net.sf.hajdbc.StateManager;
import net.sf.hajdbc.SynchronizationContext;
import net.sf.hajdbc.SynchronizationListener;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.local.LocalLockManager;
import net.sf.hajdbc.local.LocalStateManager;
import net.sf.hajdbc.sync.SynchronizationContextImpl;
import net.sf.hajdbc.sync.SynchronizationStrategyBuilder;
import net.sf.hajdbc.util.concurrent.CronThreadPoolExecutor;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author  Paul Ferraro
 * @param <D> either java.sql.Driver or javax.sql.DataSource
 * @since   1.0
 */
public abstract class AbstractDatabaseCluster<D> implements DatabaseCluster<D>, DatabaseClusterMBean, MBeanRegistration
{
	/** This is a work-around for Java 1.4, where Boolean does not implement Comparable */
	private static final Comparator<Boolean> booleanComparator = new Comparator<Boolean>()
	{
		@Override
		public int compare(Boolean value1, Boolean value2)
		{
			return this.valueOf(value1) - this.valueOf(value2);
		}
		
		private int valueOf(Boolean value)
		{
			return value.booleanValue() ? 1 : 0;
		}
	};

	static Logger logger = LoggerFactory.getLogger(AbstractDatabaseCluster.class);

//	private static final Method isValidMethod = Methods.findMethod(Connection.class, "isValid", Integer.TYPE);
	
	private String id;
	private Balancer<D> balancer;
	private Dialect dialect;
	private DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory;
	private DatabaseMetaDataCache databaseMetaDataCache;
	private String defaultSynchronizationStrategyId;
	private CronExpression failureDetectionExpression;
	private CronExpression autoActivationExpression;
	private int minThreads;
	private int maxThreads;
	private int maxIdle;
	private TransactionMode transactionMode;
	private boolean identityColumnDetectionEnabled;
	private boolean sequenceDetectionEnabled;
	private boolean currentDateEvaluationEnabled;
	private boolean currentTimeEvaluationEnabled;
	private boolean currentTimestampEvaluationEnabled;
	private boolean randEvaluationEnabled;
	
	private MBeanServer server;
	private URL url;
	private Map<String, SynchronizationStrategy> synchronizationStrategyMap = new HashMap<String, SynchronizationStrategy>();
	private DatabaseClusterDecorator decorator;
	private Map<String, Database<D>> databaseMap = new HashMap<String, Database<D>>();
	private ExecutorService executor;
	private CronThreadPoolExecutor cronExecutor = new CronThreadPoolExecutor(2);
	private LockManager lockManager = new LocalLockManager();
	private StateManager stateManager = new LocalStateManager(this);
	private volatile boolean active = false;
	private List<DatabaseActivationListener> activationListenerList = new CopyOnWriteArrayList<DatabaseActivationListener>();
	private List<DatabaseDeactivationListener> deactivationListenerList = new CopyOnWriteArrayList<DatabaseDeactivationListener>();
	private List<SynchronizationListener> synchronizationListenerList = new CopyOnWriteArrayList<SynchronizationListener>();
	
	protected AbstractDatabaseCluster(String id, URL url)
	{
		this.id = id;
		this.url = url;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getId()
	 */
	@Override
	public String getId()
	{
		return this.id;
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getVersion()
	 */
	@Override
	public String getVersion()
	{
		return DatabaseClusterFactory.getVersion();
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getAliveMap(java.util.Collection)
	 */
	@Override
	public Map<Boolean, List<Database<D>>> getAliveMap(Collection<Database<D>> databases)
	{
		Map<Database<D>, Future<Boolean>> futureMap = new TreeMap<Database<D>, Future<Boolean>>();

		for (final Database<D> database: databases)
		{
			Callable<Boolean> task = new Callable<Boolean>()
			{
				public Boolean call() throws Exception
				{
					return AbstractDatabaseCluster.this.isAlive(database);
				}
			};

			futureMap.put(database, this.executor.submit(task));
		}

		Map<Boolean, List<Database<D>>> map = new TreeMap<Boolean, List<Database<D>>>(booleanComparator);
		
		int size = databases.size();
		
		map.put(false, new ArrayList<Database<D>>(size));
		map.put(true, new ArrayList<Database<D>>(size));
		
		for (Map.Entry<Database<D>, Future<Boolean>> futureMapEntry: futureMap.entrySet())
		{
			try
			{
				map.get(futureMapEntry.getValue().get()).add(futureMapEntry.getKey());
			}
			catch (ExecutionException e)
			{
				// isAlive does not throw an exception
				throw new IllegalStateException(e);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}

		return map;
	}
	
	boolean isAlive(Database<D> database)
	{
		try
		{
			this.test(database);
			
			return true;
		}
		catch (SQLException e)
		{
			logger.warn(Messages.getMessage(Messages.DATABASE_NOT_ALIVE, database, this), e);
			
			return false;
		}
	}

	private void test(Database<D> database) throws SQLException
	{
		Connection connection = null;
		
		try
		{
			connection = database.connect(database.createConnectionFactory());
			
			Statement statement = connection.createStatement();
			
			statement.execute(this.dialect.getSimpleSQL());

			statement.close();
		}
		finally
		{
			if (connection != null)
			{
				try
				{
					connection.close();
				}
				catch (SQLException e)
				{
					logger.warn(e.toString(), e);
				}
			}
		}
	}
/*	
	boolean isAliveNew(Database<D> database)
	{
		Connection connection = null;
		
		try
		{
			connection = database.connect(database.createConnectionFactory());
			
			return this.isAlive(connection);
		}
		catch (SQLException e)
		{
			logger.warn(Messages.getMessage(Messages.DATABASE_NOT_ALIVE, database, this), e);
			
			return false;
		}
		finally
		{
			if (connection != null)
			{
				try
				{
					connection.close();
				}
				catch (SQLException e)
				{
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}
	
	private boolean isAlive(Connection connection)
	{
		if (isValidMethod != null)
		{
			try
			{
				return connection.isValid(0);
			}
			catch (SQLException e)
			{
				// isValid not yet supported
			}
		}

		try
		{
			Statement statement = connection.createStatement();
			
			statement.execute(this.dialect.getSimpleSQL());

			statement.close();
			
			return true;
		}
		catch (SQLException e)
		{
			logger.warn(e.toString(), e);
			
			return false;
		}
	}
*/	
	/**
	 * @see net.sf.hajdbc.DatabaseCluster#deactivate(net.sf.hajdbc.Database, net.sf.hajdbc.StateManager)
	 */
	@Override
	public boolean deactivate(Database<D> database, StateManager manager)
	{
		synchronized (this.balancer)
		{
			this.unregister(database);
			// Reregister database mbean using "inactive" interface
			this.register(database, database.getInactiveMBean());
			
			boolean removed = this.balancer.remove(database);
			
			if (removed)
			{
				DatabaseEvent event = new DatabaseEvent(database);

				manager.deactivated(event);
				
				for (DatabaseDeactivationListener listener: this.deactivationListenerList)
				{
					listener.deactivated(event);
				}
			}
			
			return removed;
		}
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseCluster#activate(net.sf.hajdbc.Database, net.sf.hajdbc.StateManager)
	 */
	@Override
	public boolean activate(Database<D> database, StateManager manager)
	{
		synchronized (this.balancer)
		{
			this.unregister(database);
			// Reregister database mbean using "active" interface
			this.register(database, database.getActiveMBean());
			
			if (database.isDirty())
			{
				this.export();
				
				database.clean();
			}
			
			boolean added = this.balancer.add(database);
			
			if (added)
			{
				DatabaseEvent event = new DatabaseEvent(database);

				manager.activated(event);
				
				for (DatabaseActivationListener listener: this.activationListenerList)
				{
					listener.activated(event);
				}
			}
			
			return added;
		}
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getActiveDatabases()
	 */
	@Override
	public Set<String> getActiveDatabases()
	{
		Set<String> databaseSet = new TreeSet<String>();
		
		for (Database<D> database: this.balancer.all())
		{
			databaseSet.add(database.getId());
		}
		
		return databaseSet;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getInactiveDatabases()
	 */
	@Override
	public Set<String> getInactiveDatabases()
	{
		synchronized (this.databaseMap)
		{
			Set<String> databaseSet = new TreeSet<String>(this.databaseMap.keySet());

			for (Database<D> database: this.balancer.all())
			{
				databaseSet.remove(database.getId());
			}
			
			return databaseSet;
		}
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getDatabase(java.lang.String)
	 */
	@Override
	public Database<D> getDatabase(String id)
	{
		synchronized (this.databaseMap)
		{
			Database<D> database = this.databaseMap.get(id);
			
			if (database == null)
			{
				throw new IllegalArgumentException(Messages.getMessage(Messages.INVALID_DATABASE, id, this));
			}
			
			return database;
		}
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getDefaultSynchronizationStrategy()
	 */
	@Override
	public String getDefaultSynchronizationStrategy()
	{
		return this.defaultSynchronizationStrategyId;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getSynchronizationStrategies()
	 */
	@Override
	public Set<String> getSynchronizationStrategies()
	{
		return new TreeSet<String>(this.synchronizationStrategyMap.keySet());
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getBalancer()
	 */
	@Override
	public Balancer<D> getBalancer()
	{
		return this.balancer;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getTransactionalExecutor()
	 */
	@Override
	public ExecutorService getTransactionalExecutor()
	{
		return this.transactionMode.getTransactionExecutor(this.executor);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getNonTransactionalExecutor()
	 */
	@Override
	public ExecutorService getNonTransactionalExecutor()
	{
		return this.executor;
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getDialect()
	 */
	@Override
	public Dialect getDialect()
	{
		return this.dialect;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getDatabaseMetaDataCache()
	 */
	@Override
	public DatabaseMetaDataCache getDatabaseMetaDataCache()
	{
		return this.databaseMetaDataCache;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getLockManager()
	 */
	@Override
	public LockManager getLockManager()
	{
		return this.lockManager;
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#isAlive(java.lang.String)
	 */
	@Override
	public boolean isAlive(String id)
	{
		return this.isAlive(this.getDatabase(id));
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#deactivate(java.lang.String)
	 */
	@Override
	public void deactivate(String databaseId)
	{
		if (this.deactivate(this.getDatabase(databaseId), this.stateManager))
		{
			logger.info(Messages.getMessage(Messages.DATABASE_DEACTIVATED, databaseId, this));
		}
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#activate(java.lang.String)
	 */
	@Override
	public void activate(String databaseId)
	{
		this.activate(databaseId, this.getDefaultSynchronizationStrategy());
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#activate(java.lang.String, java.lang.String)
	 */
	@Override
	public void activate(String databaseId, String strategyId)
	{
		SynchronizationStrategy strategy = this.synchronizationStrategyMap.get(strategyId);
		
		if (strategy == null)
		{
			throw new IllegalArgumentException(Messages.getMessage(Messages.INVALID_SYNC_STRATEGY, strategyId));
		}
		
		try
		{
			if (this.activate(this.getDatabase(databaseId), strategy))
			{
				logger.info(Messages.getMessage(Messages.DATABASE_ACTIVATED, databaseId, this));
			}
		}
		catch (SQLException e)
		{
			logger.warn(Messages.getMessage(Messages.DATABASE_ACTIVATE_FAILED, databaseId, this), e);
			
			SQLException exception = e.getNextException();
			
			while (exception != null)
			{
				logger.error(exception.getMessage(), exception);
				
				exception = exception.getNextException();
			}

			throw new IllegalStateException(e.toString());
		}
		catch (InterruptedException e)
		{
			logger.warn(e.toString(), e);
			
			Thread.currentThread().interrupt();
		}
	}
	
	protected void register(Database<D> database, DynamicMBean mbean)
	{
		try
		{
			ObjectName name = DatabaseClusterFactory.getObjectName(this.id, database.getId());
			
			this.server.registerMBean(mbean, name);
		}
		catch (JMException e)
		{
			logger.error(e.toString(), e);
			
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#remove(java.lang.String)
	 */
	@Override
	public void remove(String id)
	{
		synchronized (this.databaseMap)
		{
			Database<D> database = this.getDatabase(id);
			
			if (this.balancer.all().contains(database))
			{
				throw new IllegalStateException(Messages.getMessage(Messages.DATABASE_STILL_ACTIVE, id, this));
			}
	
      this.unregister(database);
			
			this.databaseMap.remove(id);
			
			this.export();
		}
	}

	private void unregister(Database<D> database)
	{
		try
		{
			ObjectName name = DatabaseClusterFactory.getObjectName(this.id, database.getId());
			
			if (this.server.isRegistered(name))
			{
				try {
          this.server.unregisterMBean(name);
        } catch ( InstanceNotFoundException ex ) {
        }
			}
		}
		catch (JMException e)
		{
			logger.error(e.toString(), e);
			
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return this.active;
	}

	/**
	 * @see net.sf.hajdbc.Lifecycle#start()
	 */
	public synchronized void start() throws Exception
	{
		if (this.active) return;
		
		this.lockManager.start();
		this.stateManager.start();
		
		this.executor = new ThreadPoolExecutor(this.minThreads, this.maxThreads, this.maxIdle, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
		
		Set<String> databaseSet = this.stateManager.getInitialState();
		
		if (databaseSet != null)
		{
			for (String databaseId: databaseSet)
			{
				Database<D> database = this.getDatabase(databaseId);
				
				if (database != null)
				{
					this.activate(database, this.stateManager);
				}
			}
		}
		else
		{
			for (Database<D> database: this.getAliveMap(this.databaseMap.values()).get(true))
			{
				this.activate(database, this.stateManager);
			}
		}
		
		this.databaseMetaDataCache = this.databaseMetaDataCacheFactory.createCache(this);
		
		try
		{
			this.flushMetaDataCache();
		}
		catch (IllegalStateException e)
		{
			// Ignore - cache will initialize lazily.
		}
		
		if (this.failureDetectionExpression != null)
		{
			this.cronExecutor.schedule(new FailureDetectionTask(), this.failureDetectionExpression);
		}
		
		if (this.autoActivationExpression != null)
		{
			this.cronExecutor.schedule(new AutoActivationTask(), this.autoActivationExpression);
		}
		
		this.active = true;
	}

	/**
	 * @see net.sf.hajdbc.Lifecycle#stop()
	 */
	public synchronized void stop()
	{
		if (!this.active) return;

		this.active = false;
		
		this.balancer.clear();
		
		this.stateManager.stop();
		this.lockManager.stop();
		
		this.cronExecutor.shutdownNow();
		
		if (this.executor != null)
		{
			this.executor.shutdownNow();
		}
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#flushMetaDataCache()
	 */
	@Override
	public void flushMetaDataCache()
	{
		try
		{
			this.databaseMetaDataCache.flush();
		}
		catch (SQLException e)
		{
			throw new IllegalStateException(e.toString(), e);
		}
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isIdentityColumnDetectionEnabled()
	 */
	@Override
	public boolean isIdentityColumnDetectionEnabled()
	{
		return this.identityColumnDetectionEnabled;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isSequenceDetectionEnabled()
	 */
	@Override
	public boolean isSequenceDetectionEnabled()
	{
		return this.sequenceDetectionEnabled;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isCurrentDateEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentDateEvaluationEnabled()
	{
		return this.currentDateEvaluationEnabled;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isCurrentTimeEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentTimeEvaluationEnabled()
	{
		return this.currentTimeEvaluationEnabled;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isCurrentTimestampEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentTimestampEvaluationEnabled()
	{
		return this.currentTimestampEvaluationEnabled;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#isRandEvaluationEnabled()
	 */
	@Override
	public boolean isRandEvaluationEnabled()
	{
		return this.randEvaluationEnabled;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.getId();
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object object)
	{
		if ((object == null) || !(object instanceof DatabaseCluster)) return false;
		
		String id = ((DatabaseCluster) object).getId();
		
		return (id != null) && id.equals(this.id);
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}
	
	protected DatabaseClusterDecorator getDecorator()
	{
		return this.decorator;
	}
	
	protected void setDecorator(DatabaseClusterDecorator decorator)
	{
		this.decorator = decorator;
	}
	
	protected void add(Database<D> database)
	{
		String id = database.getId();
		
		synchronized (this.databaseMap)
		{
			if (this.databaseMap.containsKey(id))
			{
				throw new IllegalArgumentException(Messages.getMessage(Messages.DATABASE_ALREADY_EXISTS, id, this));
			}
			
			this.register(database, database.getInactiveMBean());
			
			this.databaseMap.put(id, database);
		}
	}
	
	protected Iterator<Database<D>> getDatabases()
	{
		synchronized (this.databaseMap)
		{
			return this.databaseMap.values().iterator();
		}
	}
	
	/**
	 * @see net.sf.hajdbc.DatabaseCluster#getStateManager()
	 */
	@Override
	public StateManager getStateManager()
	{
		return this.stateManager;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#setStateManager(net.sf.hajdbc.StateManager)
	 */
	@Override
	public void setStateManager(StateManager stateManager)
	{
		this.stateManager = stateManager;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseCluster#setLockManager(net.sf.hajdbc.LockManager)
	 */
	@Override
	public void setLockManager(LockManager lockManager)
	{
		this.lockManager = lockManager;
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#getUrl()
	 */
	@Override
	public URL getUrl()
	{
		return this.url;
	}

	private boolean activate(Database<D> database, SynchronizationStrategy strategy) throws SQLException, InterruptedException
	{
		Lock lock = this.lockManager.writeLock(LockManager.GLOBAL);
		
		lock.lockInterruptibly();
		
		try
		{
			SynchronizationContext<D> context = new SynchronizationContextImpl<D>(this, database);
			
			if (context.getActiveDatabaseSet().contains(database))
			{
				return false;
			}
			
			this.test(database);
			
			try
			{
				DatabaseEvent event = new DatabaseEvent(database);
				
				logger.info(Messages.getMessage(Messages.DATABASE_SYNC_START, database, this));
				
				for (SynchronizationListener listener: this.synchronizationListenerList)
				{
					listener.beforeSynchronization(event);
				}
				
				strategy.synchronize(context);

				logger.info(Messages.getMessage(Messages.DATABASE_SYNC_END, database, this));
				
				for (SynchronizationListener listener: this.synchronizationListenerList)
				{
					listener.afterSynchronization(event);
				}
				
				return this.activate(database, this.stateManager);
			}
			finally
			{
				context.close();
			}
		}
		catch (NoSuchElementException e)
		{
			return this.activate(database, this.stateManager);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	/**
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister()
	{
		this.stop();
		
		this.unregisterDatabases();
	}

	private void unregisterDatabases()
	{
		synchronized (this.databaseMap)
		{
			Iterator<Database<D>> databases = this.databaseMap.values().iterator();
			
			while (databases.hasNext())
			{
				this.unregister(databases.next());
				
				databases.remove();
			}
		}
	}
	
	/**
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registered)
	{
		if (!registered)
		{
			this.postDeregister();
		}
	}

	/**
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception
	{
		// Nothing to do
	}

	/**
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
	{
		this.server = server;
		
		InputStream inputStream = null;
		
		logger.info(Messages.getMessage(Messages.HA_JDBC_INIT, this.getVersion(), this.url));
		
		try
		{
			inputStream = this.url.openStream();
			
			IUnmarshallingContext context = BindingDirectory.getFactory(this.getClass()).createUnmarshallingContext();
	
			context.setDocument(inputStream, null);
			
			context.setUserContext(this);
			
			context.unmarshalElement();
			
			if (this.decorator != null)
			{
				this.decorator.decorate(this);
			}
			
			this.start();
			
			return name;
		}
		catch (IOException e)
		{
			logger.error(Messages.getMessage(Messages.CONFIG_NOT_FOUND, this.url), e);
			
			throw e;
		}
		catch (JiBXException e)
		{
			logger.error(Messages.getMessage(Messages.CONFIG_LOAD_FAILED, this.url), e);
			
			this.unregisterDatabases();
			
			throw e;
		}
		catch (Exception e)
		{
			logger.error(Messages.getMessage(Messages.CLUSTER_START_FAILED, this), e);
			
			this.postDeregister();
			
			throw e;
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (IOException e)
				{
					logger.warn(e.toString(), e);
				}
			}
		}
	}
	
	private void export()
	{
		File file = null;
		WritableByteChannel outputChannel = null;
		FileChannel fileChannel = null;
		
		try
		{
			file = File.createTempFile("ha-jdbc", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
			
			IMarshallingContext context = BindingDirectory.getFactory(this.getClass()).createMarshallingContext();
		
			context.setIndent(1, System.getProperty("line.separator"), '\t'); //$NON-NLS-1$
			
			// This method closes the writer
			context.marshalDocument(this, null, null, new FileWriter(file));
			
			fileChannel = new FileInputStream(file).getChannel();

			outputChannel = this.getOutputChannel(this.url);

			fileChannel.transferTo(0, file.length(), outputChannel);
		}
		catch (Exception e)
		{
			logger.warn(Messages.getMessage(Messages.CONFIG_STORE_FAILED, this.url), e);
		}
		finally
		{
			if (outputChannel != null)
			{
				try
				{
					outputChannel.close();
				}
				catch (IOException e)
				{
					logger.warn(e.getMessage(), e);
				}
			}
			
			if (fileChannel != null)
			{
				try
				{
					fileChannel.close();
				}
				catch (IOException e)
				{
					logger.warn(e.getMessage(), e);
				}
			}
			
			if (file != null)
			{
				file.delete();
			}
		}
	}
	
	/**
	 * We cannot use URLConnection for files because Sun's implementation does not support output.
	 */
	private WritableByteChannel getOutputChannel(URL url) throws IOException
	{
		return this.isFile(url) ? new FileOutputStream(this.toFile(url)).getChannel() : Channels.newChannel(url.openConnection().getOutputStream());
	}
	
	private boolean isFile(URL url)
	{
		return url.getProtocol().equals("file"); //$NON-NLS-1$
	}
	
	private File toFile(URL url)
	{
		return new File(url.getPath());
	}
	
	protected void addSynchronizationStrategyBuilder(SynchronizationStrategyBuilder builder) throws Exception
	{
		this.synchronizationStrategyMap.put(builder.getId(), builder.buildStrategy());
	}
	
	protected Iterator<SynchronizationStrategyBuilder> getSynchronizationStrategyBuilders() throws Exception
	{
		List<SynchronizationStrategyBuilder> builderList = new ArrayList<SynchronizationStrategyBuilder>(this.synchronizationStrategyMap.size());
		
		for (Map.Entry<String, SynchronizationStrategy> mapEntry: this.synchronizationStrategyMap.entrySet())
		{
			builderList.add(SynchronizationStrategyBuilder.getBuilder(mapEntry.getKey(), mapEntry.getValue()));
		}
		
		return builderList.iterator();
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#addActivationListener(net.sf.hajdbc.DatabaseActivationListener)
	 */
	@Override
	public void addActivationListener(DatabaseActivationListener listener)
	{
		this.activationListenerList.add(listener);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#addDeactivationListener(net.sf.hajdbc.DatabaseDeactivationListener)
	 */
	@Override
	public void addDeactivationListener(DatabaseDeactivationListener listener)
	{
		this.deactivationListenerList.add(listener);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#addSynchronizationListener(net.sf.hajdbc.SynchronizationListener)
	 */
	@Override
	public void addSynchronizationListener(SynchronizationListener listener)
	{
		this.synchronizationListenerList.add(listener);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#removeActivationListener(net.sf.hajdbc.DatabaseActivationListener)
	 */
	@Override
	public void removeActivationListener(DatabaseActivationListener listener)
	{
		this.activationListenerList.remove(listener);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#removeDeactivationListener(net.sf.hajdbc.DatabaseDeactivationListener)
	 */
	@Override
	public void removeDeactivationListener(DatabaseDeactivationListener listener)
	{
		this.deactivationListenerList.remove(listener);
	}

	/**
	 * @see net.sf.hajdbc.DatabaseClusterMBean#removeSynchronizationListener(net.sf.hajdbc.SynchronizationListener)
	 */
	@Override
	public void removeSynchronizationListener(SynchronizationListener listener)
	{
		this.synchronizationListenerList.remove(listener);
	}

	class FailureDetectionTask implements Runnable
	{
		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			Set<Database<D>> databaseSet = AbstractDatabaseCluster.this.getBalancer().all();
			
			if (databaseSet.size() > 1)
			{
				Map<Boolean, List<Database<D>>> aliveMap = AbstractDatabaseCluster.this.getAliveMap(databaseSet);
				
				// Deactivate the dead databases, so long as at least one is alive
				// Skip deactivation if membership is empty in case of cluster panic
				if (!aliveMap.get(true).isEmpty() && !AbstractDatabaseCluster.this.getStateManager().isMembershipEmpty())
				{
					for (Database<D> database: aliveMap.get(false))
					{
						if (AbstractDatabaseCluster.this.deactivate(database, AbstractDatabaseCluster.this.getStateManager()))
						{
							logger.error(Messages.getMessage(Messages.DATABASE_DEACTIVATED, database, this));
						}
					}
				}
			}
		}
	}	
	
	class AutoActivationTask implements Runnable
	{
		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			for (String databaseId: AbstractDatabaseCluster.this.getInactiveDatabases())
			{
				AbstractDatabaseCluster.this.activate(databaseId);
			}
		}
	}
}
