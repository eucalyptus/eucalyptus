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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComputationException;
import com.google.common.collect.Maps;

/**
 * Manages the set of installed provider clients, returning the currently selected item
 * 
 * @author zhill
 *
 */
// Moved provider client configuration to ObjectStorageGlobalConfiguration - EUCA-9421
public class ObjectStorageProviders extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger(ObjectStorageProviders.class);
  private static final String UNSET = "unset";

  // Moved provider client configuration to ObjectStorageGlobalConfiguration - EUCA-9421

  /**
   * The annotation for indicating that a given class is an ObjectStorageProviderClient and to specify the name to use for configuring it.
   * 
   * @author zhill
   *
   */
  @Target({ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ObjectStorageProviderClientProperty {
    String value();
  }

  /*
   * The map of available client provider classes.
   */
  private static final Map<String, Class> clients = Maps.newHashMap();

  @Override
  public boolean processClass(Class candidate) throws Exception {
    if (Ats.from(candidate).has(ObjectStorageProviderClientProperty.class) && !Modifier.isAbstract(candidate.getModifiers())
        && !Modifier.isInterface(candidate.getModifiers())) {
      ObjectStorageProviderClientProperty candidateType = Ats.from(candidate).get(ObjectStorageProviderClientProperty.class);
      String propName = candidateType.value();
      if (ObjectStorageProviderClient.class.isAssignableFrom(candidate)) {
        clients.put(propName, candidate);
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Double getPriority() {
    return 0.0d;
  }

  private static final LoadingCache<String, ObjectStorageProviderClient> clientInstances = CacheBuilder.newBuilder().build(
      new CacheLoader<String, ObjectStorageProviderClient>() {
        @Override
        public ObjectStorageProviderClient load(String arg0) {
          ObjectStorageProviderClient osp = Classes.newInstance(lookupClient(arg0));
          try {
            osp.checkPreconditions();
            return osp;
          } catch (EucalyptusCloudException ex) {
            throw new ComputationException(ex);
          }
        }
      });

  private static AtomicReference<String> lastClient = new AtomicReference<String>();

  /**
   * Request the currently configured client
   * 
   * @return
   */
  public static ObjectStorageProviderClient getInstance() throws NoSuchElementException {
    if (lastClient.get() == null || UNSET.equals(lastClient.get())) {
      String providerClient = lookupProviderClient();
      if (!Strings.isNullOrEmpty(providerClient)) {
        if (clients.containsKey(providerClient)) {
          lastClient.set(providerClient);
        }
      } else {
        throw new NoSuchElementException(
            "OSG object storage provider client not configured. Found property 'objectstorage.providerclient' empty or unset manager(" + lastClient
                + ").  Legal values are: " + Joiner.on(",").join(clients.keySet()));
      }
    }
    return clientInstances.getUnchecked(lastClient.get());
  }

  private static String lookupProviderClient() {
    // swathi - could prime cache and fetch config using ConfigurationCache.getConfiguration(ObjectStorageGlobalConfiguration.class)
    // swathi - going with direct access for now to avoid cache eviction delays during provider client initialization
    return ObjectStorageGlobalConfiguration.getConfiguration().getProviderClient();
  }

  /**
   * Request a specific instance based on name
   * 
   * @param propertyBackend
   * @return
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws EucalyptusCloudException
   */
  public static ObjectStorageProviderClient getInstance(String propertyBackend) throws InstantiationException, IllegalAccessException,
      EucalyptusCloudException {
    if (clients.containsKey(propertyBackend)) {
      lastClient.set(propertyBackend);
    }
    return getInstance();
  }

  public static Set<String> list() {
    return clients.keySet();
  }

  public static boolean contains(Object key) {
    return clients.containsKey(key);
  }

  public static synchronized void flushClientInstances() throws EucalyptusCloudException {
    LOG.debug("Flushing all object storage manager instances");
    clientInstances.invalidateAll();
    lastClient.set(UNSET);
  }

  public static synchronized void flushClientInstance(String key) throws EucalyptusCloudException {
    LOG.debug("Flusing block object manager instance: " + key);
    lastClient.set(UNSET);
    clientInstances.invalidate(key);
  }

  public static Class<? extends ObjectStorageProviderClient> lookupClient(String arg0) {
    if (!clients.containsKey(arg0)) {
      throw new NoSuchElementException("Not a valid value:  " + arg0 + ".  Legal values are: " + Joiner.on(",").join(clients.keySet()));
    } else {
      return clients.get(arg0);
    }
  }
}
