/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
