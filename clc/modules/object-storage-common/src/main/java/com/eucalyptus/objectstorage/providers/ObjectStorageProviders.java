/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
  private static final Map<String, Class<? extends ObjectStorageProviderClient>> clients = Maps.newHashMap();

  @SuppressWarnings( "unchecked" )
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
