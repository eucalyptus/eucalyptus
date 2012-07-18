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

package com.eucalyptus.auth.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class PolicyAnnotationRegistry extends ServiceJarDiscovery {
  private static Logger                               LOG                  = Logger.getLogger( PolicyAnnotationRegistry.class );
  private static final Map<Class, PolicyResourceType> classToPolicyRscType = new HashMap<Class, PolicyResourceType>( );
  
  public static PolicyResourceType extractResourceType( Object classOrInstance ) throws NoSuchElementException {
    Class type = classOrInstance instanceof Class
      ? ( Class ) classOrInstance
      : classOrInstance.getClass( );
    PolicyResourceType rscPolicy = PolicyAnnotationRegistry.extractPolicyResourceTypeFromSuperclass( type );
    if ( rscPolicy != null ) {
      classToPolicyRscType.put( type, rscPolicy );
      return rscPolicy;
    } else {
      throw new NoSuchElementException( "The argument " + type.getName( )
                                        + " does not itself have or inherit from an object with the required @PolicyResourceType annotation." );
    }
  }
  
  private static PolicyResourceType extractPolicyResourceTypeFromSuperclass( Class type ) {
    LOG.trace( "PolicyAnnotationRegistry: looking for annotations starting at " + type );
    for ( Class c = type; c != Object.class; c = c.getSuperclass( ) ) {
      LOG.trace( "PolicyAnnotationRegistry: check -> " + c );
      if ( classToPolicyRscType.containsKey( c ) ) {
        LOG.trace( "PolicyAnnotationRegistry: FOUND => " + c );
        return classToPolicyRscType.get( c );
      } else {
        PolicyResourceType rscPolicy = PolicyAnnotationRegistry.extractPolicyResourceTypeFromInterfaces( type.getInterfaces( ) );
        if ( rscPolicy != null ) {
          return rscPolicy;
        }
      }
    }
    return null;
  }
  
  private static PolicyResourceType extractPolicyResourceTypeFromInterfaces( Class[] interfaces ) {
    for ( Class i : interfaces ) {
      LOG.trace( "PolicyAnnotationRegistry: check => " + i );
      if ( classToPolicyRscType.containsKey( i ) ) {
        LOG.trace( "PolicyAnnotationRegistry: FOUND => " + i );
        return classToPolicyRscType.get( i );
      }
    }
    return null;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( Ats.from( candidate ).has( PolicyResourceType.class ) ) {
      PolicyResourceType policyRscType = Ats.from( candidate ).get( PolicyResourceType.class );
      classToPolicyRscType.put( candidate, policyRscType );
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
  
}
