/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.blockstorage.ceph;

import java.io.File;

import org.apache.log4j.Logger;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.exceptions.RadosException;
import com.ceph.rbd.Rbd;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;

/**
 * Utility class for creating and managing connection to a pool in a Ceph cluster. Encapsulates all elements (Rados, IoCTX, Rbd, pool) of a ceph
 * connection if the caller needs access to a specific element.
 */
public class CephRbdConnectionManager implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(CephRbdConnectionManager.class);
  private static final String KEYRING = "keyring";
  private static final Object connectionSync = new Object();

  private final Object shutdownSync = new Object();
  private Rados rados;
  private IoCTX ioContext;
  private Rbd rbd;
  private String pool;

  private CephRbdConnectionManager(final CephRbdInfo config, final String poolName) {
    try {
      LOG.trace("Opening a new connection to Ceph cluster pool=" + poolName);
      rados = new Rados(config.getCephUser());
      rados.confSet(KEYRING, config.getCephKeyringFile());
      rados.confReadFile(new File(config.getCephConfigFile()));
      synchronized ( connectionSync ) {
        rados.connect();
      }
      pool = poolName;
      ioContext = rados.ioCtxCreate(pool);
      rbd = new Rbd(ioContext);
    } catch (RadosException e) {
      disconnect();
      LOG.warn("Unable to connect to Ceph cluster");
      throw new EucalyptusCephException("Failed to connect to pool " + poolName
          + " in Ceph cluster. Verify Ceph cluster health, privileges of Ceph user assigned to Eucalyptus, Ceph parameters configured in Eucalyptus "
          + config.toString() + " and retry operation", e);
    }
  }

  public Rados getRados() {
    return rados;
  }

  public IoCTX getIoContext() {
    return ioContext;
  }

  public Rbd getRbd() {
    return rbd;
  }

  public String getPool() {
    return pool;
  }

  public void disconnect() {
    try {
      LOG.trace("Closing connection to Ceph cluster pool=" + pool);
      synchronized ( shutdownSync ) {
        final Rados shutdownRados = rados;
        final IoCTX shutdownIoContext = ioContext;
        rbd = null;
        pool = null;
        rados = null;
        ioContext = null;
        if ( shutdownRados != null ) {
          try {
            if ( shutdownIoContext != null ) {
              shutdownRados.ioCtxDestroy( shutdownIoContext );
            }
          } finally {
            synchronized ( connectionSync ) {
              shutdownRados.shutDown();
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.debug("Caught error during teardown", e);
    }
  }

  public static CephRbdConnectionManager getConnection(CephRbdInfo config, String poolName) {
    return new CephRbdConnectionManager(config, poolName);
  }

  @Override
  public void close( ){
    disconnect();
  }
}
