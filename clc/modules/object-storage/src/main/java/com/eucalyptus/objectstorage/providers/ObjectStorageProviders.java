/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.providers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityTransaction;

import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.objectstorage.ObjectStorage;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.config.ObjectStorageConfiguration;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComputationException;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Manages the set of installed provider clients, returning the currently selected item
 * @author zhill
 *
 */

@ConfigurableClass( root = "objectstorage", description = "Basic object storage configuration.")
public class ObjectStorageProviders extends ServiceJarDiscovery {
	private static Logger LOG = Logger.getLogger( ObjectStorageProviders.class );
	private static final String UNSET = "unset";

	@ConfigurableField( description = "Object Storage Provider client to use for backend", displayName = "objectstorage.providerclient", changeListener = ObjectStorageProviderChangeListener.class)
	public static volatile String providerClient = ""; //configured by user to specify which back-end client to use

	/**
	 * Change listener for the osg provider client setting.
	 * @author zhill
	 *
	 */
	public static class ObjectStorageProviderChangeListener implements PropertyChangeListener<String> {
		/*
		 * Ensures that the proposed value is valid based on the set of valid values for OSGs
		 * Additional DB lookup required for remote OSGs where the CLC doesn't have the OSG bits
		 * installed and therefore doesn't have the same view of the set of valid values.
		 * (non-Javadoc)
		 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty, java.lang.Object)
		 */
		@Override
		public void fireChange(ConfigurableProperty t, String newValue) throws ConfigurablePropertyException {
			String existingValue = (String)t.getValue();

			List<ServiceConfiguration> objConfigs = null;
			try {
				objConfigs = ServiceConfigurations.list(ObjectStorage.class);
			} catch(NoSuchElementException e) {
				throw new ConfigurablePropertyException("No ObjectStorage configurations found");
			}
			
			final String proposedValue = newValue;
			final Set<String> validEntries = Sets.newHashSet();
			EntityTransaction tx = Entities.get(ObjectStorageConfiguration.class);
			try {
				if(!Iterables.any(Components.lookup(ObjectStorage.class).services(), new Predicate<ServiceConfiguration>( ) {
					@Override
					public boolean apply(ServiceConfiguration config) {
						if(config.isVmLocal()) {
							//Service is local, so add entries to the valid list (in case of HA configs)
							// and then check the local memory state
							validEntries.addAll(ObjectStorageProviders.list());
							return ObjectStorageProviders.contains(proposedValue);
						} else {
							try {
								//Remote SC, so check the db for the list of valid entries.
								ObjectStorageConfiguration objConfig = Entities.uniqueResult((ObjectStorageConfiguration)config);
								for(String entry : Splitter.on(",").split(objConfig.getAvailableClients())) {
									validEntries.add(entry);
								}
								return validEntries.contains(proposedValue);
							} catch(Exception e) {
								return false;
							}
						}
					}
				})) {
					//Nothing matched.
					throw new ConfigurablePropertyException("Cannot modify " + t.getQualifiedName() + "." + t.getFieldName() + " new value is not a valid value.  " +
					"Legal values are: " + Joiner.on( "," ).join( validEntries) );
				}
			} finally {
				tx.rollback();
			}
		}
	}
	
	/**
	 * The annotation for indicating that a given class is an ObjectStorageProviderClient and to specify the name
	 * to use for configuring it.
	 * @author zhill
	 *
	 */
	@Target( { ElementType.TYPE } )
	@Retention( RetentionPolicy.RUNTIME )
	public @interface ObjectStorageProviderClientProperty {
		String value( );
	}
	
	/*
	 * The map of available client provider classes.
	 */
	private static final Map<String, Class> clients = Maps.newHashMap( );
	  
	@Override
	public boolean processClass( Class candidate ) throws Exception {
		if ( Ats.from( candidate ).has( ObjectStorageProviderClientProperty.class )
				&& !Modifier.isAbstract( candidate.getModifiers( ) )
				&& !Modifier.isInterface( candidate.getModifiers( ) ) ) {
			ObjectStorageProviderClientProperty candidateType = Ats.from( candidate ).get( ObjectStorageProviderClientProperty.class );
			String propName = candidateType.value( );
			if ( ObjectStorageProviderClient.class.isAssignableFrom( candidate ) ) {
				clients.put( propName, candidate );
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Double getPriority( ) {
		return 0.0d;
	}
	
	  private static final LoadingCache<String, ObjectStorageProviderClient> clientInstances = CacheBuilder.newBuilder().build(
	    new CacheLoader<String, ObjectStorageProviderClient>() {
	      @Override
	      public ObjectStorageProviderClient load( String arg0 ) {
	        ObjectStorageProviderClient osp = Classes.newInstance( lookupClient( arg0 ) );
	        try {
	          osp.checkPreconditions( );
	          return osp;
	        } catch ( EucalyptusCloudException ex ) {
	          throw new ComputationException( ex );
	        }
	      }
	    });
	  
	  
	  private static AtomicReference<String> lastClient = new AtomicReference<String>( );
	  
	  /**
	   * Request the currently configured client
	   * @return
	   */
	  public static ObjectStorageProviderClient getInstance( ) throws NoSuchElementException {
		  if ( lastClient.get( ) == null || UNSET.equals(lastClient.get())) {
			  if(!Strings.isNullOrEmpty(providerClient)) {
				  if ( clients.containsKey( providerClient ) ) {
					  lastClient.set( providerClient );
				  }
			  } else {
				  throw new NoSuchElementException( "OSG object storage provider client not configured. Found empty or unset manager(" + lastClient + ").  Legal values are: " + Joiner.on( "," ).join( clients.keySet( ) ) );
			  }
		  }
		  return clientInstances.getUnchecked( lastClient.get( ) );
	  }
	  
	  /**
	   * Request a specific instance based on name
	   * @param propertyBackend
	   * @return
	   * @throws InstantiationException
	   * @throws IllegalAccessException
	   * @throws EucalyptusCloudException
	   */
	  public static ObjectStorageProviderClient getInstance( String propertyBackend ) throws InstantiationException, IllegalAccessException, EucalyptusCloudException {
	    if ( clients.containsKey( propertyBackend ) ) {
	      lastClient.set( propertyBackend );
	    }
	    return getInstance( );
	  }
	  
	  public static Set<String> list( ) {
	    return clients.keySet( );
	  }
	  
	  public static boolean contains( Object key ) {
	    return clients.containsKey( key );
	  }
	  
	  public static synchronized void flushClientInstances() throws EucalyptusCloudException {
	  	LOG.debug("Flushing all block storage manager instances");
	  	clientInstances.invalidateAll();
	  	lastClient.set(UNSET);
	  }
	  
	  public static synchronized void flushClientInstance(String key) throws EucalyptusCloudException {
	  	LOG.debug("Flusing block storage manager instance: " + key);
		lastClient.set(UNSET);
		clientInstances.invalidate(key);
	  }
	  	  
	  public static Class<? extends ObjectStorageProviderClient> lookupClient( String arg0 ) {
	    if ( !clients.containsKey( arg0 ) ) {
	      throw new NoSuchElementException( "Not a valid value:  " + arg0 + ".  Legal values are: " + Joiner.on( "," ).join( clients.keySet( ) ) );
	    } else {
	      return clients.get( arg0 );
	    }
	  }
}
