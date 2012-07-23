/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId.ComponentMessage;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ComponentMessages {
  private static Logger                                                                  LOG       = Logger.getLogger( ComponentMessages.class );
  private static final BiMap<Class<? extends ComponentId>, Class<? extends BaseMessage>> compIdMap = HashBiMap.create( );
  
  public static Class<? extends BaseMessage> lookup( Class<? extends ComponentId> compIdClass ) {
    if ( !compIdMap.containsKey( compIdClass ) ) {
      throw new NoSuchElementException( "No ComponentMessage with name: " + compIdClass );
    } else {
      return compIdMap.get( compIdClass );
    }
  }
  
  public static <T extends BaseMessage> Class<? extends ComponentId> lookup( T msg ) {
    Class<?> msgType = Iterables.find( Classes.classAncestors( msg ), new Predicate<Class>( ) {
      
      @Override
      public boolean apply( Class arg0 ) {
        return Ats.from( arg0 ).has( ComponentMessage.class );
      }
    } );
    if ( !compIdMap.containsValue( msgType ) ) {
      throw new NoSuchElementException( "No ComponentMessage with name: " + msgType );
    } else {
      return compIdMap.inverse( ).get( msgType );
    }
  }
  
  public static void register( Class<? extends BaseMessage> componentMsg ) {
    @SuppressWarnings( "unchecked" )
    Class<? extends ComponentId> componentIdClass = Ats.from( componentMsg ).get( ComponentMessage.class ).value( );
    compIdMap.put( componentIdClass, componentMsg );
  }
  
  public static class ComponentMessageDiscovery extends ServiceJarDiscovery {
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( BaseMessage.class.isAssignableFrom( candidate ) && Ats.from( candidate ).has( ComponentMessage.class )
           && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
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
