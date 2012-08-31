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

package com.eucalyptus.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestListener extends RunListener {
  private static Logger                         LOG       = Logger.getLogger( TestListener.class );
  private volatile boolean                      failed    = false;
  private final static Map<String, Description> passedMap = new TreeMap<String, Description>( );
  private final static Map<String, Long>        timerMap  = new HashMap<String, Long>( );
  private final static Map<String, Failure>     failedMap = new TreeMap<String, Failure>( );
  
  public static String key( Description desc ) {
    return desc.getClassName( ) + "." + ( desc.getMethodName( ) != null ? desc.getMethodName( ) : "class" );
  }
  @Override public void testFailure( Failure failure ) {
    try {
      Description desc = failure.getDescription( );
      String key = key( desc );
      LOG.info( "FAILED " + key );
      failedMap.put( key, failure );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  @Override public void testFinished( Description description ) throws Exception {
    try {
      timerMap.put( key( description ), System.currentTimeMillis( ) - timerMap.get( key( description ) ) );
      if( !failedMap.containsKey( key(description) )) {
        LOG.info( "PASSED " + key( description ) );        
      }
      passedMap.put( key( description ), description );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  @Override public void testStarted( Description description ) throws Exception {
    try {
      failed = false;
      timerMap.put( key( description ), System.currentTimeMillis( ) );
      LOG.info( "TEST_RUN" );
      LOG.info( "START  " + key( description ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  public static TestDescription getTestDescription( Description description ) throws ClassNotFoundException, NoSuchMethodException {
    TestDescription d;
    try {
      Class testClass = Class.forName( description.getClassName( ) );
      d = ( TestDescription ) testClass.getDeclaredMethod( description.getMethodName( ), new Class[] {} ).getAnnotation( TestDescription.class );
      return d;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
  public static Test getTest( Description description ) throws ClassNotFoundException, NoSuchMethodException {
    Test d;
    try {
      Class testClass = Class.forName( description.getClassName( ) );
      d = ( Test ) testClass.getDeclaredMethod( description.getMethodName( ), new Class[] {} ).getAnnotation( Test.class );
      return d;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
  @Override public void testRunFinished( Result result ) throws Exception {
    if ( !failedMap.isEmpty( ) ) {
      try {
        LOG.info( "FAILURES" );
        for ( String failed : failedMap.keySet( ) ) {
          errorLog( failedMap.get( failed ),  failed );
        }
      } catch ( Exception e ) {
        e.printStackTrace( );
      }
    }
    LOG.info( "SUMMARY" );
    try {
      for ( Description description : passedMap.values( ) ) {
        if ( !failedMap.containsKey( key( description ) ) ) {
          LOG.info( description ) ;
        }
      }
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    if ( !failedMap.isEmpty( ) ) {
      for ( String failed : failedMap.keySet( ) ) {
        try {
          if( passedMap.containsKey( failed ) && timerMap.containsKey( failed ) ) {
            LOG.info( failed ) ;            
          }
        } catch ( Exception e ) {
          e.printStackTrace( );
        }
      }
    }
  }
  public static void errorLog( Failure f, String key ) {
	   try {
	     LOG.error( f.getTrace() );
	     LOG.error( "+=============================================================================+" );
	     LOG.error( String.format( "| Test:      %-60.60s", key ) );
	     int i = 0;
	     for( Throwable t = f.getException( ); t != null && ++i < 10; t = t.getCause( ) ) {
	    	 if (t.getMessage() != null)
	    		 LOG.error( String.format( "| Cause:     %s", t.getMessage( ).replaceAll("\n","") ) );        
	     }
	     LOG.error( "+-----------------------------------------------------------------------------+" );
	     LOG.error(f.getTrace());
	     LOG.error( "+-----------------------------------------------------------------------------+" );
	   } catch ( Exception e ) {
	     e.printStackTrace( );
	   }
	 }
}
