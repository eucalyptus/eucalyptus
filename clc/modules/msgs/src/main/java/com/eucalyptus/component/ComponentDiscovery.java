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

package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;

public class ComponentDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( ComponentDiscovery.class );
  
  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( ComponentId.class.isAssignableFrom( candidate ) && 
        !Modifier.isAbstract( candidate.getModifiers( ) ) &&
        !Modifier.isInterface( candidate.getModifiers( ) ) &&
        !candidate.isLocalClass( ) &&
        !candidate.isAnonymousClass( ) ) {
      try {
        EventRecord.here( ComponentDiscovery.class, EventType.BOOTSTRAP_INIT_COMPONENT, candidate.toString( ) ).info( );
        final Class<? extends ComponentId> idClass = candidate;
        ComponentIds.lookup( idClass );
      } catch ( final Throwable ex ) {
        LOG.error( ex, ex );
        LOG.info( "Error occurred while trying to register ComponentId of type: " + ex.getMessage( ), ex );
      }
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public Double getPriority( ) {
    return -1.0d;
  }
  
}
