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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus.Database;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.DatabaseBootstrapper;
import com.eucalyptus.util.Internets;

public class PgsqldbDestination implements DatabaseDestination {
  private static Logger LOG = Logger.getLogger( PgsqldbDestination.class );
  private DatabaseBootstrapper db;

  public DatabaseBootstrapper getDb( ) throws Exception {
    return db;
  }
  
  public void initialize( ) throws Exception {
    /** Bring up the new destination database **/
    Component dbComp = Components.lookup( Database.class );
    ComponentId dbCompId = ComponentIds.lookup( Database.class );
    db = Groovyness.newInstance( "setup_db" );
    try {
      db.init();
      
      final String pass = Databases.getPassword( );
      Map<String, String> props = new HashMap<String, String>( ) {
        {
          put( "hibernate.archive.autodetection", "jar, class, hbm" );
          put( "hibernate.show_sql", "false" );
          put( "hibernate.format_sql", "false" );
          put( "hibernate.connection.autocommit", "true" );
          put( "hibernate.hbm2ddl.auto", "update" );
          put( "hibernate.generate_statistics", "true" );
          put( "hibernate.connection.driver_class", "org.postgresql.Driver" );
          put( "hibernate.connection.username", "eucalyptus" );
          put( "hibernate.connection.password", pass );
          put( "hibernate.bytecode.use_reflection_optimizer", "true" );
          put( "hibernate.cglib.use_reflection_optimizer", "true" );
          put( "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect" );
          put( "hibernate.cache.provider_class", "org.hibernate.cache.TreeCache" );
          put( "hibernate.cache.region.factory_class", "org.hibernate.cache.jbc2.SharedJBossCacheRegionFactory" );
          put( "hibernate.cache.region.jbc2.cfg.shared", "eucalyptus_jboss_cache.xml" );
          
          put( "hibernate.cache.use_second_level_cache", "true" );
          put( "hibernate.cache.use_query_cache", "true" );
          put( "hibernate.cache.use_structured_entries", "true" );
        }
      };

      // Get a connection to the "postgresql" DB, so we can run a "drop database" command on other DBs
      String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, Internets.loopback( ), "postgres" ) );
      Connection conn = DriverManager.getConnection( url, Databases.getUserName( ), Databases.getPassword( ) );

      for ( String ctx : PersistenceContexts.list( ) ) {
        // XXX: Need to drop the old database first

        Properties p = new Properties( );
        p.putAll( props );
        String ctxUrl = String.format("jdbc:%s",ServiceUris.remote(dbComp,ctx));
        p.put( "hibernate.connection.url", ctxUrl );
        p.put( "hibernate.cache.region_prefix", "eucalyptus_" + ctx + "_cache" );
        Ejb3Configuration config = new Ejb3Configuration( );
        config.setProperties( p );
        for ( Class c : PersistenceContexts.listEntities( ctx ) ) {
          config.addAnnotatedClass( c );
        }
        PersistenceContexts.registerPersistenceContext( ctx, config );
      } 
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      LOG.fatal( "Failed to initialize the persistence layer." );
      System.exit( -1 );
    }
  }
}
