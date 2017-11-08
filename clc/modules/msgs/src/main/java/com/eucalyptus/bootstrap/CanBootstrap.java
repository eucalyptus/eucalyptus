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

package com.eucalyptus.bootstrap;

import com.eucalyptus.empyrean.EmpyreanService;

public interface CanBootstrap {
  
  /**
   * Check the status of the bootstrapped resource.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true when all is clear
   * @throws Exception should contain detail any malady which may be present.
   */
  public abstract boolean check( ) throws Exception;

  /**
   * Initiate a forced shutdown releasing all used resources and effectively unloading the this
   * bootstrapper.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @throws Exception
   */
  public abstract void destroy( ) throws Exception;
  
  /**
   * Enter an idle/passive state.
   * 
   * @return
   * @throws Exception
   */
  public abstract boolean disable( ) throws Exception;
  
  /**
   * Perform the enable phase of bootstrap -- this occurs when the service associated with this
   * bootstrapper is made active and should bring the resource to an active operational state.
   * 
   * @return
   * @throws Exception
   */
  public abstract boolean enable( ) throws Exception;
  
  /**
   * Perform the {@link SystemBootstrapper#load()} phase of bootstrap.
   * NOTE: The only code which can execute with uid=0 runs during the
   * {@link EmpyreanService.Stage.PrivilegedConfiguration} stage of the {@link #load()} phase.
   * 
   * @see SystemBootstrapper#load()
   * @return true on successful completion
   * @throws Exception
   */
  public abstract boolean load( ) throws Exception;
  
  /**
   * Perform the {@link SystemBootstrapper#start()} phase of bootstrap.
   * 
   * @see SystemBootstrapper#start()
   * @return true on successful completion
   * @throws Exception
   */
  
  public abstract boolean start( ) throws Exception;
  
  /**
   * Initiate a graceful shutdown
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true on successful completion
   * @throws Exception
   */
  public abstract boolean stop( ) throws Exception;
  
}
