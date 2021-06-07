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

package com.eucalyptus.bootstrap;

import groovy.sql.Sql;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public interface DatabaseBootstrapper {

  /**
   * Recommended / default database username
   */
  String DB_USERNAME = "eucalyptus";

  void init( ) throws Exception;
  
  boolean load( ) throws Exception;
  
  boolean start( ) throws Exception;
  
  boolean stop( ) throws Exception;
  
  void destroy( ) throws Exception;
  
  boolean isRunning( );

  boolean isLocal( );

  void hup( );

  List<String> listDatabases( );

  List<String> listDatabases( InetSocketAddress address );

  List<String> listSchemas( String database );

  List<String> listSchemas( InetSocketAddress address, String database );

  List<String> listTables( String database, String schema );

  void createDatabase( String database);

  void deleteDatabase( String database );

  void renameDatabase( String from, String to );

  /**
   * Copy a database or a single schema.
   */
  void copyDatabase( String sourceDatabase,
                     String destinationDatabase );

  /**
   * Copy a single database schema.
   */
  void copyDatabaseSchema( String sourceDatabase,
                           String sourceSchema,
                           String destinationDatabase,
                           String destinationSchema );

  void createSchema( String database, String schema );

  Sql getConnection( String context ) throws Exception;

  Sql getConnection( String database, String schema ) throws Exception;

  String getDefaultSchemaName( );

  String getDriverName( );
  
  String getJdbcDialect( );
  
  String getHibernateDialect( );
  
  String getJdbcScheme( );
  
  String getServicePath( String... pathParts );

  Map<String,String> getJdbcUrlQueryParameters();

  String getUserName();

  String getPassword();

}
