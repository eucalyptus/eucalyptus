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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId.ComponentMessage;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ComponentMessages {
  private static Logger                                                                  LOG       = Logger.getLogger( ComponentMessages.class );
  private static final Map<Class<? extends BaseMessage>,Class<? extends ComponentId>> compIdMap = Maps.newHashMap( );
  
  public static Collection<Class<? extends BaseMessage>> forComponent( ComponentId compId ) {
    Class<? extends ComponentId> compIdClass = compId.getClass( );
    if ( !compIdMap.containsValue( compIdClass ) ) {
      return Collections.emptyList( );
    } else {
      return Maps.filterValues( (Map) compIdMap, Predicates.equalTo( compIdClass ) ).keySet( );
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
  
  public static void register( Class<? extends BaseMessage> componentMsg ) {
    @SuppressWarnings( "unchecked" )
    Class<? extends ComponentId> componentIdClass = Ats.from( componentMsg ).get( ComponentMessage.class ).value( );
    compIdMap.put( componentMsg, componentIdClass );
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
