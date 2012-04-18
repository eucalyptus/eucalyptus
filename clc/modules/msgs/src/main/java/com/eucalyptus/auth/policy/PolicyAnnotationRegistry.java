/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

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
