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

package com.eucalyptus.empyrean;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.system.SubDirectory;

public class EmpyreanTransactionManager extends org.eclipse.jetty.util.component.AbstractLifeCycle implements org.jboss.cache.transaction.TransactionManagerLookup, org.hibernate.transaction.TransactionManagerLookup {
//  public static final String JNDI_NAME = "eucalyptusTransactionManager";
  private static Logger             LOG = Logger.getLogger( EmpyreanTransactionManager.class );
  private static Context            ctx = getContext( );
  private static TransactionManager tm;

  static Context getContext( ) {
    if ( ctx != null ) {
      return ctx;
    } else {
      synchronized ( EmpyreanTransactionManager.class ) {
        if ( ctx == null ) {
          ctx = configureTm( );
          try {
            tm = ( TransactionManager ) ctx.lookup( TransactionManagerServices.getConfiguration( ).getJndiUserTransactionName( ) );
          } catch ( NamingException ex ) {
            LOG.error( ex , ex );
          }
        }
        return ctx;
      }
    }
  }
  
  public TransactionManager getTransactionManager( ) throws Exception {
    return tm;
  }
  
  private static BitronixContext configureTm( ) {
    Configuration tm_conf = TransactionManagerServices.getConfiguration( );
    tm_conf.setServerId( SystemIds.createCloudUniqueName( "transaction-manager" ) );
    tm_conf.setAsynchronous2Pc( false );
    tm_conf.setLogPart1Filename( SubDirectory.TX.toString( ) + "/btm1.tx" );
    tm_conf.setLogPart2Filename( SubDirectory.TX.toString( ) + "/btm2.tx" );
//    tm_conf.setJndiUserTransactionName( JNDI_NAME );
    LOG.debug( "Setting up transaction manager: " + tm_conf.getJndiUserTransactionName( ) );
    return new BitronixContext( );
  }
  
  public String getUserTransactionName( ) {
    return TransactionManagerServices.getConfiguration( ).getJndiUserTransactionName( );
  }
  
  public Object getTransactionIdentifier( Transaction transaction ) {
    return transaction;
  }
  
  @Override
  public TransactionManager getTransactionManager( Properties arg0 ) throws HibernateException {
    return tm;
  }
  
//  @Override
//  protected void doStart( ) throws Exception {
//    InitialContext ic = new InitialContext( );
//    Context env = ( Context ) ic.lookup( "java:comp/env" );
//    LOG.debug( "Unbinding " + this.getUserTransactionName( ) );
//    env.bind( getUserTransactionName( ), tm );
//  }
//  
//  @Override
//  protected void doStop( ) throws Exception {
//    InitialContext ic = new InitialContext( );
//    Context env = ( Context ) ic.lookup( "java:comp/env" );
//    LOG.debug( "Unbinding " + this.getUserTransactionName( ) );
//    env.unbind( getUserTransactionName( ) );
//  }
}
