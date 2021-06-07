/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.bootstrap;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ServiceDependencyException;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.ObjectStorageGateway;
import com.eucalyptus.util.EucalyptusCloudException;

@Provides(ObjectStorage.class)
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
@DependsLocal(ObjectStorage.class)
public class OsgBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger(OsgBootstrapper.class);
  private static OsgBootstrapper singleton;

  public static Bootstrapper getInstance() {
    synchronized (OsgBootstrapper.class) {
      if (singleton == null) {
        singleton = new OsgBootstrapper();
        LOG.info("Creating ObjectStorageGateway Bootstrapper instance.");
      } else {
        LOG.info("Returning ObjectStorageGateway Bootstrapper instance.");
      }
    }
    return singleton;
  }

  @Override
  public boolean load() throws Exception {
    ObjectStorageGateway.checkPreconditions();
    return true;
  }

  @Override
  public boolean start() throws Exception {
    ObjectStorageGateway.configure();
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable() throws Exception {
    ObjectStorageGateway.enable();
    ObjectStorageSchedulerManager.start();
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop() throws Exception {
    ObjectStorageSchedulerManager.stop();
    ObjectStorageGateway.stop();
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy() throws Exception {}

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable() throws Exception {
    ObjectStorageSchedulerManager.stop();
    ObjectStorageGateway.disable();
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check() throws Exception {
    // check local storage
    try {
      ObjectStorageGateway.check( );
    } catch ( EucalyptusCloudException e ) {
      if ( "No ENABLED WalrusBackend found. Cannot initialize fully.".equals( e.getMessage( ) ) ) {
        throw new ServiceDependencyException( e.getMessage( ), e );
      }
      throw e;
    }
    return true;
  }
}
