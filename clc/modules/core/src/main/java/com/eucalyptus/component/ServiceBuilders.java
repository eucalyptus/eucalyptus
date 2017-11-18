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
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.techpreview.TechPreviews;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ServiceBuilders {
  private static Logger                                                                            LOG               = Logger.getLogger( ServiceBuilders.class );
  private static Map<Class<? extends ComponentId>, ServiceBuilder<? extends ServiceConfiguration>> componentBuilders = Maps.newConcurrentMap( );
  
  public static class ServiceBuilderDiscovery extends ServiceJarDiscovery {
    
    @Override
    public Double getPriority( ) {
      return 0.2;
    }
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( ServiceBuilder.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
        /** GRZE: this implies that service builder is a singleton **/
        ServiceBuilder b = ( ServiceBuilder ) candidate.newInstance( );
        Ats ats = Ats.from( candidate );
        if ( !ats.has( ComponentPart.class ) ) {
          Exception ex = Exceptions.noSuchElement( "@ComponentPart(Class<? extends ComponentId>) is missing: " + candidate );
          LOG.error( ex.getMessage( ) );
          Logs.extreme( ).error( ex, ex );
        } else {
          ComponentPart at = ats.get( ComponentPart.class );
          ServiceBuilders.addComponentBuilder( at.value( ), b );
        }
        return true;
      } else {
        return false;
      }
    }
  }
  
  private static void addComponentBuilder( Class<? extends ComponentId> c, ServiceBuilder b ) {
    LOG.trace( "Registered service builder for " + c.getSimpleName( )
               + " -> "
               + b.getClass( ).getCanonicalName( ) );
    componentBuilders.put( c, b );
  }
  
  public static <T extends ServiceBuilder<? extends ServiceConfiguration>> T lookup( ComponentId componentId ) {
    try {
      return lookup( componentId.getClass( ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new RuntimeException( ex );
    }
  }
  
  public static <T extends ServiceBuilder<? extends ServiceConfiguration>> T lookup( Class<? extends ComponentId> componentId ) {
    if ( !componentBuilders.containsKey( componentId ) ) {
      Component comp = Components.lookup( componentId );
      componentBuilders.put( componentId, new DummyServiceBuilder( comp.getComponentId( ) ) );
    }
    return ( T ) componentBuilders.get( componentId );
  }

  public static List<Class<? extends ComponentId>> listRegisterableComponents( ) {
    return Lists.newArrayList( Iterables.filter( componentBuilders.keySet(), comp -> !TechPreviews.check( comp ) ) );
  }
}
