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

package com.eucalyptus.util.dns;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

/**
 * Facade for interacting w/ the internal domain name handling.
 * 
 * Note that there are no methods in this API which take a String. That is intentional -- if you
 * feel the need to munge strings do so on the calling side.
 * 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class DomainNames {
  private static Logger LOG = Logger.getLogger( DomainNames.class );
  
  /**
   * @return Subdomain representing the cloud internal DNS subdomain for {@code ComponentId}
   */
  public static Name internalSubdomain( Class<? extends ComponentId> componentId ) {
    return SystemSubdomain.INTERNAL.apply( componentId );
  }
  
  /**
   * @return Subdomain representing the external system DNS subdomain for {@code ComponentId}
   */
  public static Name externalSubdomain( Class<? extends ComponentId> componentId ) {
    return SystemSubdomain.EXTERNAL.apply( componentId );
  }
  
  /**
   * @return Subdomain representing the cloud internal DNS subdomain
   */
  public static Name internalSubdomain( ) {
    return SystemSubdomain.INTERNAL.get( );
  }
  
  /**
   * @return Subdomain representing the external system DNS subdomain
   */
  public static Name externalSubdomain( ) {
    return SystemSubdomain.EXTERNAL.get( );
  }
  
  /**
   * Determines whether the given {@code name} is a subdomain of some zone under the control of
   * the system.
   * 
   * @param name Name to test
   * @return true if the name is a system subdomain name.
   */
  public static boolean isSystemSubdomain( Name name ) {
    return isInternalSubdomain( name ) || isExternalSubdomain( name );
  }
  
  /**
   * Determines whether the given {@code name} is a subdomain of the external DNS subdomain.
   * 
   * @param name Name to test
   * @return true if the name is an external subdomain name.
   */
  public static boolean isExternalSubdomain( Name name ) {
    return name.subdomain( SystemSubdomain.EXTERNAL.get( ) );
  }
  
  /**
   * Determines whether the given {@code name} is a subdomain of the internal DNS subdomain.
   * 
   * @param name Name to test
   * @return true if the name is an internal subdomain name.
   */
  public static boolean isInternalSubdomain( Name name ) {
    return name.subdomain( SystemSubdomain.INTERNAL.get( ) );
  }
  
  /**
   * Get the list of Name Server Records for the given Name if we are authoritative. That is, only
   * ever return Names which refer to our interna DNS server.
   * 
   * @param name Name the name for which to return our nameserver set for if we are authoritative
   * @return List of Name's of the local systems if we are authoritative
   * @throws NoSuchElementException if the name is not authoritivately served by us
   */
  public static List<Name> nameServerRecords( Name name ) throws NoSuchElementException {
    return SystemSubdomain.lookup( name ).getNameServers( );
  }
  
  public static Name sourceOfAuthority( Name name ) throws NoSuchElementException {
    return nameServerRecords( name ).get( 0 );
  }
  
  private enum SystemSubdomain implements Function<Class<? extends ComponentId>, Name>, Supplier<Name> {
    INTERNAL {
      @Override
      public Name get( ) {
        return Name.fromConstantString( INTERNAL_SUBDOMAIN );
      }
      
    },
    EXTERNAL {
      
      @Override
      public Name get( ) {
        return Name.fromConstantString( SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ) + "." );
      }
      
    };
    private static final String INTERNAL_SUBDOMAIN = "internal."; //GRZE: this is constant per the AWS spec
                                                                  
    @Override
    public Name apply( Class<? extends ComponentId> input ) {
      Name compName = Name.fromConstantString( ComponentIds.lookup( input ).name( ) );
      return absolute( compName, this.get( ) );
    }
    
    public List<Name> getNameServers( ) {
      return Lists.newArrayList( Name.fromConstantString( "ns1." + this.get( ).toString( ) ), Name.fromConstantString( "ns2." + this.get( ).toString( ) ) );
    }
    
    public static SystemSubdomain lookup( Name name ) {
      for ( SystemSubdomain s : SystemSubdomain.values( ) ) {
        if ( name.subdomain( s.get( ) ) ) {
          return s;
        }
      }
      throw new NoSuchElementException( "Failed to lookup SystemSubdomain for the name: " + name );
    }
    
  }
  
  public static Name absolute( Name name, Name origin ) {
    if ( name.isAbsolute( ) ) {
      return name;
    } else {
      return concatenateConstant( name, origin );
    }
  }

  public static Name relativize( Name name, Name origin ) {
    return name.relativize( origin );
  }

  public static Name concatenateConstant( final Name one, final Name two ) {
    try {
      return Name.concatenate( one, two );
    } catch ( NameTooLongException ex ) {
      LOG.error( ex );
      throw Exceptions.toUndeclared( ex );
    }
  }
  
}
