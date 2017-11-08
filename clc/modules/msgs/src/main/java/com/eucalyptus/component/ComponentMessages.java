/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.system.Ats;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker;

public class ComponentMessages {
  private static Logger LOG = Logger.getLogger( ComponentMessages.class );
  private static final Map<Class<? extends BaseMessageMarker>,Class<? extends ComponentId>> compIdMap = Maps.newHashMap();

  public static Collection<Class<? extends BaseMessageMarker>> forComponent( ComponentId compId ) {
    Class<? extends ComponentId> compIdClass = compId.getClass( );
    if ( !compIdMap.containsValue( compIdClass ) ) {
      return Collections.emptyList();
    } else {
      return Maps.filterValues( compIdMap, Predicates.<Object>equalTo( compIdClass ) ).keySet( );
    }
  }

  public static <T extends BaseMessage> Class<? extends ComponentId> lookup( T msg ) {
    Class<?> msgType = Ats.inClassHierarchy( msg ).findAncestor( ComponentMessage.class );
    if ( !compIdMap.containsKey( msgType ) ) {
      throw new NoSuchElementException( "No ComponentMessage with name: " + msgType );
    } else {
      return compIdMap.get( msgType );
    }
  }
  
  public static void register( Class<? extends BaseMessageMarker> componentMsg ) {
    @SuppressWarnings( "unchecked" )
    Class<? extends ComponentId> componentIdClass = Ats.from( componentMsg ).get( ComponentMessage.class ).value( );
    compIdMap.put( componentMsg, componentIdClass );
  }
  
  public static class ComponentMessageDiscovery extends ServiceJarDiscovery {
    
    @SuppressWarnings( "unchecked" )
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( BaseMessageMarker.class.isAssignableFrom( candidate )
           && candidate.getAnnotation( ComponentMessage.class ) != null ) {
        try {
          ComponentMessages.register( candidate );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
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
    
  }
  
}
