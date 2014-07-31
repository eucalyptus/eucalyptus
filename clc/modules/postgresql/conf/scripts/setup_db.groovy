/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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


import com.google.common.base.Optional

import java.sql.ResultSet
import com.eucalyptus.bootstrap.Bootstrapper
import com.eucalyptus.bootstrap.DatabaseBootstrapper
import com.eucalyptus.bootstrap.OrderedShutdown
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.component.Component
import com.eucalyptus.component.Components
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.auth.SystemCredentials
import com.eucalyptus.component.id.Database
import com.eucalyptus.crypto.util.PEMFiles
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.Internets
import com.eucalyptus.util.Pair
import com.google.common.base.Joiner
import com.google.common.collect.Iterables
import com.google.common.collect.Sets
import groovy.sql.Sql
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade


import static java.util.Collections.emptyMap
import static java.util.regex.Matcher.quoteReplacement
import static java.util.regex.Pattern.quote

/*
 * REQUIREMENTS : Postgres 9.1 and Postgres jdbc driver
 *
 * SUMMARY : The PostgresqlBootstrapper class attempts to control the postgres database.  The methods
 * that control the database are : init, start, stop, hup, load, isRunning and destroy.
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class PostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {
  
  private static Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_db" )
  
  // Static definitions of postgres commands and options
  private static String EUCA_DB_DIR  = "data"
  private String PG_HOME = System.getProperty("euca.db.home","")
  private String PG_SUFFIX = ""
  private static String PG_BIN = "/bin/pg_ctl"
  private static String PG_DUMP = "/bin/pg_dump"
  private static String PG_RESTORE = "/bin/pg_restore"
  private static String PG_START = "start"
  private static String PG_STOP = "stop"
  private static String PG_STATUS = "status"
  private static String PG_MODE = "-mf"
  private static String PG_PORT = 8777
  private static String PG_HOST = "0.0.0.0" // or "127.0.0.1,${Internets.localHostAddress( )}"
  private static String PG_PORT_OPTS2 = "-o -h${PG_HOST} -p${PG_PORT}"
  private static String PG_DB_OPT = "-D"
  private static String PG_INITDB = "/bin/initdb"
  private static String PG_X_OPT = "-X"
  private static String PG_X_DIR =  SubDirectory.DB.getChildFile("tx").getAbsolutePath()
  private static String PG_USER_OPT = "-U" + DatabaseBootstrapper.DB_USERNAME
  private static String PG_TRUST_OPT = "--auth=password"
  private static String PG_PWD_FILE = "--pwfile="
  private static String PG_PASSFILE = "pass.txt"
  private static String PG_W_OPT ="-w"
  private static String PG_S_OPT ="-s"
  private static String PG_DEFAULT_DBNAME = "postgres"
  private static String PG_ENCODING = "--encoding=UTF8"
  private static String PG_LOCALE = "--locale=C"
  private static boolean PG_USE_SSL = Boolean.valueOf( System.getProperty("euca.db.ssl", "true") )
  private static String COMMAND_GET_CONF = "getconf"
  private static String GET_CONF_SYSTEM_PAGE_SIZE = "PAGE_SIZE"
  private static String PROC_SEM = "/proc/sys/kernel/sem"
  private static String PROC_SHMALL = "/proc/sys/kernel/shmall"
  private static String PROC_SHMMAX = "/proc/sys/kernel/shmmax"
  private static long   MIN_SEMMNI = 1536L
  private static long   MIN_SEMMNS = 32000L
  private static long   MIN_SHMMAX = 536870912L //512MB

  private int runProcessWithReturn( List<String> args, Map<String,String> environment = [:] ) {
    LOG.debug("Postgres command : " + args.collect { "'${it}'" }.join(" ") )
    try {
      ProcessBuilder pb = new ProcessBuilder(args)
      pb.environment( ).putAll( environment )
      def root = new File("/")
      def outlines = []
      def errlines = []
      pb.directory(root)
      Process p = pb.start()
      OutputStream outstream = new ByteArrayOutputStream( 8192 )
      OutputStream errstream = new ByteArrayOutputStream( 8192 )
      p.consumeProcessOutput(outstream, errstream)
      int result = p.waitFor()
      outstream.toString().eachLine { line -> outlines.add(line); LOG.debug("stdout: ${line}") }
      errstream.toString().eachLine { line -> errlines.add(line); LOG.debug("stderr: ${line}") }
      result
    } catch ( Exception ex ) {
      throw new RuntimeException("Failed to run '" + args.collect { "'${it}'" }.join(" ") + "' because of: ${ex.message}", ex )
    }
  }

  private List<String> runProcessWithOutput( List<String> args ) {
    LOG.debug("Postgres command : " + args.collect { "'${it}'" }.join(" ") )
    try {
      ProcessBuilder pb = new ProcessBuilder(args)
      def root = new File("/")
      def outlines = []
      def errlines = []
      pb.directory(root)
      Process p = pb.start()
      OutputStream outstream = new ByteArrayOutputStream( 8192 )
      OutputStream errstream = new ByteArrayOutputStream( 8192 )
      p.consumeProcessOutput(outstream, errstream)
      int result = p.waitFor()
      outstream.toString().eachLine { line -> outlines.add(line); LOG.debug("stdout: ${line}") }
      errstream.toString().eachLine { line -> errlines.add(line); LOG.debug("stderr: ${line}") }
      if ( result == 0 ) {
        return outlines
      } else {
        throw new RuntimeException("(see stdout and stderr for details)")
      }
    } catch ( Exception ex ) {
      throw new RuntimeException("Failed to run '" + args.collect { "'${it}'" }.join(" ") + "' because of: ${ex.message}", ex )
    }
  }

  //Default constructor
  @SuppressWarnings("GroovyUnusedDeclaration")
  PostgresqlBootstrapper( ) {
    try {
      Properties props = new Properties() {{
              try {
                this.load( ClassLoader.getSystemResource( "postgresql-binaries.properties" ).openStream( ) )
              } catch (Exception ex) {
                throw new FileNotFoundException("postgresql-binaries.properties not found in the classpath")
              }
            }}
      
      if ("".equals(PG_HOME)) {
        PG_HOME = props.getProperty("euca.db.home","")
      }
      
      if ("".equals(PG_HOME)) {
        throw new Exception("Postgresql home directory is not set")
      }
      
      PG_SUFFIX = props.getProperty("euca.db.suffix","")
      
      PG_BIN = PG_HOME + PG_BIN + PG_SUFFIX
      PG_INITDB = PG_HOME + PG_INITDB + PG_SUFFIX
      PG_DUMP = PG_HOME + PG_DUMP + PG_SUFFIX
      PG_RESTORE = PG_HOME + PG_RESTORE + PG_SUFFIX
    
      LOG.debug("PG_HOME = " + PG_HOME + " : PG_BIN = " + PG_BIN + " : PG_INITDB  = " + PG_INITDB)
    } catch ( Exception ex ) {
      LOG.error("Required Database variables are not correctly set "
          + "PG_HOME = " + PG_HOME + " : PG_BIN = " + PG_BIN + " : PG_INITDB  = " + PG_INITDB)
      LOG.debug(ex, ex)
      System.exit(1)
    }
  }
  
  @Override
  void init( ) {
    try {
      kernelParametersCheck( )
      
      if ( !versionCheck( ) ){
        throw new RuntimeException("Postgres versions less than 9.1.X are not supported")
      }
      
      if ( !initdbPG( ) ) {
        throw new RuntimeException("Unable to initialize the postgres database")
      }
      
      if ( !startResource( ) ) {
        throw new RuntimeException("Unable to start the postgres database")
      }
      
      if ( !createDBSql( ) ) {
        throw new RuntimeException("Unable to create the eucalyptus database tables")
      }
      
      if ( !createCliUser( ) ) {
        throw new RuntimeException("Unable to create the cli user")
      }
      
      Component dbComp = Components.lookup( Database.class )
      dbComp.initService( )
      prepareService( )
      
    } catch ( Exception ex ) {
      throw new RuntimeException( ex )
    }
  }
  
  private void kernelParametersCheck( ) {
    try {
      LOG.debug "Reading '/proc' kernel parameters"
      String[] semStrs = new File( PROC_SEM ).text.split("\\s")
      String shmallStr = new File( PROC_SHMALL ).text.trim()
      String shmmaxStr = new File( PROC_SHMMAX ).text.trim()
      
      LOG.debug "Getting page size"
      String pageSizeStr = [
        COMMAND_GET_CONF,
        GET_CONF_SYSTEM_PAGE_SIZE
      ].execute().text.trim()
      
      LOG.debug "Read system values [$semStrs] [$shmallStr] [$shmmaxStr] [$pageSizeStr]"
      
      long pageSize = Long.parseLong( pageSizeStr )
      long MIN_SHMALL = MIN_SHMMAX / pageSize
      long semmni = Long.parseLong( semStrs[3] )
      long semmns = Long.parseLong( semStrs[1] )
      long shmall = Long.parseLong( shmallStr )
      long shmmax = Long.parseLong( shmmaxStr )
      
      LOG.info "Found kernel parameters semmni=$semmni, semmns=$semmns, shmall=$shmall, shmmax=$shmmax"
      
      // Parameter descriptions from "man proc"
      if ( semmni < MIN_SEMMNI ) {
        LOG.error "Insufficient operating system resources! The available number of semaphore identifiers is too low (semmni < $MIN_SEMMNI)"
      }
      if ( semmns < MIN_SEMMNS ) {
        LOG.error "Insufficient operating system resources! The available number of semaphores in all semaphore sets is too low (semmns < $MIN_SEMMNS)"
      }
      if ( shmall < MIN_SHMALL ) {
        LOG.error "Insufficient operating system resources! The total number of pages of System V shared memory is too low (shmall < $MIN_SHMALL)"
      }
      if ( shmmax < MIN_SHMMAX ) {
        LOG.error "Insufficient operating system resources! The run-time limit on the maximum (System V IPC) shared memory segment size that can be created is too low (shmmax < $MIN_SHMMAX)"
      }
    }  catch ( Exception e ) {
      LOG.error("Error checking kernel parameters: " + e.message )
    }
  }
  
  // Version check to ensure only Postgres 9.X creates the db.
  private boolean versionCheck( ) {
    try {
      String cmd = PG_INITDB + " --version"
      def pattern = ~/.*\s+9\.[1-9]\d*(\.\d+)*$/
      pattern.matcher( cmd.execute( ).text.trim( ) ).matches( )
    } catch ( Exception e ) {
      LOG.fatal("Unable to find the initdb command")
      false
    }
  }
  
  private boolean initdbPG( ) throws Exception {
    final File passFile = SubDirectory.DB.getChildFile( PG_PASSFILE )
    try {
      passFile.write( getPassword() )
      runProcessWithOutput([
        PG_INITDB,
        PG_ENCODING,
        PG_LOCALE,
        PG_USER_OPT,
        PG_TRUST_OPT,
        PG_PWD_FILE + passFile ,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
        PG_X_OPT + PG_X_DIR,
        PG_ENCODING
      ])
      LOG.info( "Database init complete." )
      initDBFile()
      true
    } catch (Exception e) {
      throw new RuntimeException(e.message, e)
    } finally {
      passFile.delete()
    }
  }
  
  private void initDBFile() throws Exception {
    File ibdata1 = null
    try {
      ibdata1 = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "ibdata1" )
      ibdata1.createNewFile()
      initPGHBA( )
      initPGCONF( )
      if (PG_USE_SSL) initSSL()
    } catch ( Exception e ) {
      LOG.debug("Unable to create the configuration files")
      ibdata1?.delete( )
      throw e
    }
  }
  
  private void initPGHBA( ) throws Exception {
    try {
      File orgPGHBA = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "pg_hba.conf")
      File tmpPGHBA = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "pg_hba.conf.org")
      orgPGHBA.renameTo(tmpPGHBA)
      String hostOrHostSSL = PG_USE_SSL ? "hostssl" : "host"
      SubDirectory.DB.getChildFile( EUCA_DB_DIR, "pg_hba.conf").write("""\
local\tall\troot\tpeer
local\tall\tall\tpassword
${hostOrHostSSL}\tall\tall\t0.0.0.0/0\tpassword
${hostOrHostSSL}\tall\tall\t::/0\tpassword
"""
          )
    } catch (Exception e) {
      LOG.debug("Unable to create the pg_hba.conf file", e)
      throw e
    }
  }
  
  private void initPGCONF() throws Exception {
    try {
      final Map<String,String> requiredProperties = [
            max_connections: '8192',
            log_line_prefix: "'%t'",
            unix_socket_directory: "'" + SubDirectory.DB.getChildPath( EUCA_DB_DIR ) + "'",
            unix_socket_directories: "'" + SubDirectory.DB.getChildPath( EUCA_DB_DIR ) + "'",
            ssl: PG_USE_SSL ? 'on' : 'off',
            ssl_ciphers: '\'AES128-SHA:AES256-SHA\'',
            log_directory: "'${System.getProperty('euca.log.dir')}'"
          ]
      
      final File orgPGCONF = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "postgresql.conf")
      String pgconfText = orgPGCONF.getText()
      
      final File bakPGCONF = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "postgresql.conf.org")
      bakPGCONF.write(pgconfText)
      
      for ( final Map.Entry<String,String> property : requiredProperties ) {
        pgconfText = pgconfText.replaceAll( "#(?=\\s{0,128}"+quote(property.key)+"\\s{0,128}=)", "" ) // ensure enabled
        pgconfText = pgconfText.replaceAll(
            "(?<=\\s{0,128}"+ quote(property.key) +"\\s{0,128}=\\s{0,128})\\S.*",
            quoteReplacement(property.value) + " # Updated by setup_db.groovy (${new Date().format( 'yyyy-MM-dd' )})")
      }
      
      orgPGCONF.write( pgconfText )
    } catch (Exception e) {
      LOG.debug("Unable to modify the postgresql.conf file", e)
      throw e
    }
  }
  
  private boolean createCliUser() throws Exception {
    if ( !isRunning( ) ) {
      throw new Exception("The database must be running to create the cli user")
    }
    
    try {
      dbExecute( PG_DEFAULT_DBNAME, "CREATE USER root WITH CREATEUSER" )
      LOG.debug("executed successfully = CREATE USER root")
    } catch (Exception e) {
      LOG.error("Unable to create cli user", e)
      return false
    }
    true
  }
  
  private void initSSL() throws Exception {
    LOG.debug("Writing SSL certificate and key files")
    final SystemCredentials.Credentials dbCredentials = SystemCredentials.lookup(Database.class)
    dbCredentials.with {
      if ( certificate != null && keyPair != null ) {
        SubDirectory.DB.getChildFile( EUCA_DB_DIR, "server.crt").with {
          PEMFiles.write( getAbsolutePath(), certificate )
          setOwnerReadonly getAbsoluteFile()
        }
        SubDirectory.DB.getChildFile( EUCA_DB_DIR, "server.key").with {
          PEMFiles.write( getAbsolutePath(), keyPair )
          setOwnerReadonly getAbsoluteFile()
        }
      } else {
        LOG.warn("Credentials not found for database, not creating SSL certificate/key files")
      }
      void
    }
  }
  
  private void setOwnerReadonly( final File file ) {
    file.setReadable false, false
    file.setReadable true
    file.setWritable false, false
    file.setWritable false
    file.setExecutable false, false
    file.setExecutable false
  }
  
  private Iterable<Pair<String,Optional<String>>> databases( ) {
    Iterables.transform(
        PersistenceContexts.list( ),
        Pair.robuilder( PersistenceContexts.toDatabaseName( ), PersistenceContexts.toSchemaName( ) ) )
  }
  
  private boolean createDBSql( ) throws Exception {
    if ( !isRunning( ) ) {
      throw new Exception("The database must be running to create the tables")
    }

    final Set<String> createdDatabases = Sets.newHashSet( )
    for ( Pair<String,Optional<String>> databasePair : databases( ) ) {
      final String databaseName = databasePair.left
      final String schemaName = databasePair.right.orNull( )
      if ( createdDatabases.add( databaseName ) ) try {
        String createDatabase = "CREATE DATABASE " + databaseName + " OWNER " + getUserName( )
        dbExecute( PG_DEFAULT_DBNAME, createDatabase )
        
        String alterUser = "ALTER ROLE " + getUserName( ) + " WITH LOGIN PASSWORD \'" + getPassword( ) + "\'"
        dbExecute( databaseName, alterUser )
      } catch (Exception e) {
        if (!e.message.contains("already exists")) {
          LOG.error("Unable to create the database.", e)
          return false
        }
      }

      if ( schemaName ) try {
        //TODO use IF NOT EXISTS when we can require postgres 9.3
        dbExecute( databaseName, "CREATE SCHEMA \"${schemaName}\" AUTHORIZATION \"${userName}\"" )
      } catch (Exception e) {
        if (!e.message.contains("already exists")) {
          LOG.error("Unable to create the database schema ${schemaName}", e)
          return false
        }
      }
    }
    
    true
  }
  
  private boolean startResource() throws Exception {
    OrderedShutdown.registerPostShutdownHook( new Runnable( ) {
          @Override
          void run( ) {
            File pidfile = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "postmaster.pid" )
            if (!pidfile.exists()) {
                return
            }
            ProxoolFacade.shutdown()
            try {
              int value = runProcessWithReturn([
                PG_BIN,
                PG_STOP,
                PG_MODE,
                PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
              ])
              if (value != 0) {
                LOG.fatal("Postgresql shutdown failed with exit code " + value)
              } else {
                LOG.info("Postgresql shutdown succeeded.")
              }
            } catch ( Exception e ) {
              LOG.error("Postgresql shutdown failed with error", e)
            }
          }
        } )
    
    if ( isRunning( ) ) {
      LOG.info("Postgresql is already started, perhaps from another process.  Will attempt shutdown")
      if ( !stop( ) )
        return false // error messages already in the STOP method
    }
    
    try {
      runProcessWithOutput([
        PG_BIN,
        PG_START,
        PG_W_OPT,
        PG_S_OPT,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
        PG_PORT_OPTS2
      ])
      LOG.info("Postgresql startup succeeded.")
    } catch ( Exception ex ) {
      LOG.fatal("Postgresql startup failed: " + ex.message)
      return false
    }
    
    true
  }
  
  @Override
  boolean load( ) throws Exception {

    if ( !startResource( ) ) {
      throw new Exception("Unable to start postgresql")
    }
    
    if ( !createDBSql( ) ) {
      throw new Exception("Unable to create the eucalyptus database tables")
    }
    
    Component dbComp = Components.lookup( Database.class )
    dbComp.initService( )
    prepareService( )
    
    true
  }
  
  private void prepareService( ) throws Exception {
    for ( String databaseName : Sets.newTreeSet( Iterables.transform( databases(  ), Pair.left( ) ) ) ) {
      testContext( databaseName )
    }
  }

  Sql getConnection( String context ) throws Exception {
    getConnection(
      PersistenceContexts.toDatabaseName( ).apply( context ),
      PersistenceContexts.toSchemaName( ).apply( context )
    )
  }

  Sql getConnection( String database, String schema ) throws Exception {
    getConnectionInternal( Internets.localHostInetAddress( ), database, schema )
  }

  private Sql getConnectionInternal( InetAddress host, String database, String schema ) throws Exception {
    String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, host, database ) )
    Sql sql = Sql.newInstance( url, getUserName(), getPassword(), getDriverName() )
    if ( schema ) sql.execute( "SET search_path TO ${schema}" as String )
    sql
  }

  private boolean dbExecute( String database, String statement ) throws Exception {
    Sql sql = null
    try {
      sql = getConnection( database, null )
      sql.execute( statement )
    } finally {
      sql?.close()
    }
  }
  
  private void testContext( String databaseName ) throws Exception {
    try {
      String pgPing = "SELECT USER"
      if( !dbExecute( databaseName, pgPing ) ) {
        LOG.error("Unable to ping the database : " + databaseName)
      }
    } catch (Exception exception) {
      LOG.error("Failed to test the context : ", exception)
      System.exit(1)
    }
  }
  
  @Override
  List<String> listDatabases( ) {
    listDatabases( Internets.localHostInetAddress( ) )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  List<String> listDatabases( InetAddress host ) {
    List<String> lines = []
    Sql sql = null
    try {
      sql = getConnectionInternal( host, "postgres", null )
      sql.query("select datname from pg_database") { ResultSet rs ->
        while (rs.next()) lines.add(rs.toRowResult().datname)
      }
    } finally {
      sql?.close()
    }
    lines
  }

  @Override
  List<String> listSchemas( String database ) {
    listSchemas( Internets.localHostInetAddress( ), database )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  List<String> listSchemas( InetAddress host, String database ) {
    List<String> lines = []
    Sql sql = null
    try {
      sql = getConnectionInternal( host, database, null )
      sql.connection.metaData.schemas.with{ ResultSet rs ->
        while (rs.next()) lines.add(rs.toRowResult().table_schem )
      }
    } finally {
      sql?.close()
    }
    lines
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  List<String> listTables( String database, String schema ) {
    List<String> lines = []
    Sql sql = null
    try {
      sql = getConnection( database, null )
      sql.connection.metaData.getTables( null, schema, '%', null ).with{ ResultSet rs ->
        while (rs.next()) lines.add(rs.toRowResult().table_name)
      }
    } finally {
      sql?.close()
    }
    lines
  }

  @Override
  void createDatabase( String name ) {
    LOG.info("Creating database ${name}")
    try {
      dbExecute("postgres", "CREATE DATABASE \"${name}\" OWNER \"${getUserName()}\"" )
    } catch( Exception ex ) {
      LOG.error( "Creating database ${name} failed because of: ${ex.message}" )
      throw ex
    }
    LOG.info("Database ${name} created successfully")
  }

  @Override
  void deleteDatabase( String name ) {
    LOG.info("Deleting database ${name}")
    try {
      dbExecute("postgres", "DROP DATABASE IF EXISTS \"${name}\"" )
    } catch( Exception ex ) {
      LOG.error( "Deleting database ${name} failed because of: ${ex.message}" )
      throw ex
    }
    LOG.info("Database ${name} deleted successfully")
  }

  @Override
  void copyDatabase( String from, String to ) {
    LOG.info("Copying database ${from} to ${to}")
    try {
      dbExecute("postgres", "CREATE DATABASE \"${to}\" TEMPLATE \"${from}\" OWNER \"${getUserName()}\"" )
    } catch( Exception ex ) {
      LOG.error( "Copying database ${from} to ${to} failed because of: ${ex.message}" )
      throw ex;
    }
    LOG.info("Database ${from} copied to ${to} successfully")
  }

  @Override
  void copyDatabaseSchema( String sourceDatabase,
                           String sourceSchema,
                           String destinationDatabase,
                           String destinationSchema ) {
    LOG.info("Copying database/schema ${sourceDatabase}/${sourceSchema} to ${destinationDatabase}/${destinationSchema}")
    String databaseDumpFile = File.createTempFile( "euca-sdb-", ".dump.tar" )
    try {
      if ( !listDatabases( ).contains( destinationDatabase ) ) {
        LOG.info( "Destination database ${destinationDatabase} not found, creating." )
        dbExecute( "postgres", "CREATE DATABASE \"${destinationDatabase}\" OWNER \"${userName}\"" )
      }
      if ( listSchemas( destinationDatabase ).contains( sourceSchema ) ) {
        if ( !listTables( destinationDatabase, sourceSchema ).isEmpty( ) ) {
          throw new Exception( "Schema ${sourceSchema} in ${destinationDatabase} has tables, not dropping schema" )
        }
        LOG.info( "Deleting schema ${sourceSchema} from ${destinationDatabase}" )
        dbExecute( destinationDatabase, "DROP SCHEMA \"${sourceSchema}\" CASCADE" )
      }
      if ( getDefaultSchemaName( ) == sourceSchema ) {
        // public schema is required by pg_restore, drop/create ensures no content
        LOG.info( "Creating schema ${sourceSchema} in ${destinationDatabase}" )
        dbExecute( destinationDatabase, "CREATE SCHEMA ${getDefaultSchemaName( )}" )
      }
      if ( listSchemas( destinationDatabase ).contains( destinationSchema ) ) {
        LOG.info( "Deleting schema ${destinationSchema} from ${destinationDatabase}" )
        dbExecute( destinationDatabase, "DROP SCHEMA \"${destinationSchema}\" CASCADE" )
      }

      LOG.info( "Dumping schema ${sourceSchema} from ${sourceDatabase}" )
      int dumpCode = runProcessWithReturn( [
          PG_DUMP,
          '-h', 'localhost',
          '-p', PG_PORT,
          PG_USER_OPT,
          '-w',
          '-f', databaseDumpFile,
          '-F', 'tar',
          '-n', sourceSchema,
          sourceDatabase ], [PGPASSWORD: getPassword( )] )
      if ( dumpCode != 0 ) {
        throw new Exception( "Database dump failed with exit code ${dumpCode}" )
      }
      LOG.info( "Restoring schema ${sourceSchema} in ${destinationDatabase}" )
      int restoreCode = runProcessWithReturn( [
          PG_RESTORE,
          '-h', 'localhost',
          '-p', PG_PORT,
          PG_USER_OPT,
          '-w',
          '-e',
          '-1',
          '-F', 'tar',
          '-d', destinationDatabase,
          databaseDumpFile ], [PGPASSWORD: getPassword()] )
      if ( restoreCode != 0 ) {
        throw new Exception( "Database restore failed with exit code ${restoreCode}" )
      }
      LOG.info( "Renaming schema ${sourceSchema} to ${destinationSchema} in ${destinationDatabase}" )
      dbExecute( destinationDatabase, "ALTER SCHEMA \"${sourceSchema}\" RENAME TO \"${destinationSchema}\"" )
      if ( getDefaultSchemaName( ) == sourceSchema ) {
        dbExecute( destinationDatabase, "CREATE SCHEMA ${getDefaultSchemaName( )}" )
      }
    } catch( Exception ex ) {
      LOG.error( "Copying database/schema ${sourceDatabase}/${sourceSchema} to ${destinationDatabase}/${destinationSchema} failed because of: ${ex.message}" )
      throw ex
    } finally {
      new File( databaseDumpFile ).delete( )
    }
    LOG.info("Database/schema ${sourceDatabase}/${sourceSchema} copied to ${destinationDatabase}/${destinationSchema} successfully")
  }

  @Override
  void renameDatabase( String from, String to ) {
    LOG.info("Renaming database ${from} to ${to}")
    try {
      dbExecute("postgres", "ALTER DATABASE \"${from}\" RENAME TO \"${to}\"" )
    } catch( RuntimeException ex ) {
      LOG.error( "Renaming database ${from} to ${to} failed because of: ${ex.message}" )
      throw ex
    }
    LOG.info("Database ${from} renamed to ${to} successfully")
  }

  boolean isRunning() {
    try {
      int value = runProcessWithReturn ([
        PG_BIN,
        PG_STATUS,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
      ])
      if (value != 0) {
        return false
      }
    } catch ( Exception ex ) {
      LOG.error("Postgresql status check failed: " + ex.message)
      return false
    }
    true
  }
  
  void hup( ) {
    if( !stop() ) {
      LOG.fatal("Unable to stop the postgresql server")
      throw new Exception("Unable to stop the postgres server")
    }
    
    if ( !startResource() ) {
      LOG.fatal("Unable to start the postgresql server")
      throw new Exception("Unable to start the postgres server")
    }
  }
  
  @Override
  boolean stop( ) throws Exception {
    int value = runProcessWithReturn([
      PG_BIN,
      PG_STOP,
      PG_MODE,
      PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
    ])
    if ( value != 0 ) {
      LOG.error("Unable to stop the postgresql server (exitcode:${value})",)
      false
    } else {
      LOG.info("Postgresql shutdown succeeded.")
      true
    }
  }
  
  @Override
  void destroy( ) throws IOException {
    boolean status = false
    
    if ( isRunning( ) ) {
      status = stop( )
    } else {
      LOG.debug("Database is not running")
    }
    
    LOG.debug("Final status after destroy : " + status )
  }
  
  @Override
  String getPassword( ) {
    SystemIds.databasePassword( )
  }
  
  @Override
  String getUserName( ) {
    DatabaseBootstrapper.DB_USERNAME
  }

  @Override
  String getDefaultSchemaName( ) {
    'public'
  }

  @Override
  String getDriverName( ) {
    'org.postgresql.Driver'
  }
  
  @Override
  String getHibernateDialect( ) {
    'org.hibernate.dialect.PostgreSQLDialect'
  }
  
  @Override
  String getJdbcDialect( ) {
    'eucalyptus-postgresql'
  }
  
  @Override
  String getServicePath( String... pathParts ) {
    pathParts != null && pathParts.length > 0 ? Joiner.on("/").join( Arrays.asList( pathParts ) ) : 'eucalyptus_shared'
  }
  
  @Override
  Map<String, String> getJdbcUrlQueryParameters() {
    PG_USE_SSL ? [
      ssl:'true',
      sslfactory: 'com.eucalyptus.postgresql.PostgreSQLSSLSocketFactory'
    ] : emptyMap()
  }
  
  @Override
  String getJdbcScheme( ) {
    'postgresql'
  }
}
