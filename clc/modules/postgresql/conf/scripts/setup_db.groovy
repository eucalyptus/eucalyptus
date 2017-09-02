/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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


import com.eucalyptus.bootstrap.BootstrapArgs
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.crypto.Signatures
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.primitives.UnsignedLongs
import groovy.transform.Immutable

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
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
 * REQUIREMENTS : Postgres 9.2 and Postgres JDBC driver
 *
 * SUMMARY : The PostgresqlBootstrapper class controls the postgres database.
 */
@SuppressWarnings(["GroovyUnusedDeclaration", "UnnecessaryQualifiedReference"])
class PostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {
  
  private static Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_db' )
  
  // Static definitions of postgres commands and options
  private static final String EUCA_OLD_HOME = System.getProperty( 'euca.upgrade.old.dir' )
  private static final String EUCA_DB_USER  = System.getProperty( 'euca.db.user', DatabaseBootstrapper.DB_USERNAME )
  private static final String EUCA_DB_DIR  = 'data'
  private static final String EUCA_TX_DIR  = 'tx'
  private static final String PG_BIN = 'bin'
  private static final String PG_INITDB = 'initdb'
  private static final String PG_CTL = 'pg_ctl'
  private static final String PG_DUMP = 'pg_dump'
  private static final String PG_DUMPALL = 'pg_dumpall'
  private static final String PG_RESTORE = 'pg_restore'
  private static final String PG_SQL = 'psql'
  private static final String PG_START = 'start'
  private static final String PG_STOP = 'stop'
  private static final String PG_STATUS = 'status'
  private static final String PG_MODE = '-mf'
  private static final String PG_PORT = 8777
  private static final String PG_HOST = BootstrapArgs.isInitializeSystem() || BootstrapArgs.isUpgradeSystem() ?
      '127.0.0.1' :
      '0.0.0.0' // or "127.0.0.1,${Internets.localHostAddress( )}"
  private static final String PG_PORT_OPTS2 = "-o -h${PG_HOST} -p${PG_PORT}"
  private static final String PG_DB_OPT = '-D'
  private static final String PG_X_OPT = '-X'
  private static final String PG_X_DIR =  SubDirectory.DB.getChildFile( EUCA_TX_DIR ).getAbsolutePath()
  private static final String PG_USER_OPT = "-U${EUCA_DB_USER}"
  private static final String PG_TRUST_OPT = '--auth=password'
  private static final String PG_PASSFILE = SubDirectory.DB.getChildPath( 'pgpass.txt' )
  private static final String PG_PASSWORDFILE = SubDirectory.DB.getChildPath( 'pass.txt' )
  private static final String PG_PWD_FILE_OPT = "--pwfile=${PG_PASSWORDFILE}"
  private static final String PG_W_OPT = '-w'
  private static final String PG_S_OPT = '-s'
  private static final String PG_VERSION_OPT = '-V'
  private static final String PG_DEFAULT_DBNAME = 'postgres'
  private static final String PG_ENCODING = '--encoding=UTF8'
  private static final String PG_LOCALE = '--locale=C'
  private static final boolean PG_USE_SSL = Boolean.valueOf( System.getProperty('euca.db.ssl', 'true') )
  private static final String PG_TEST_QUERY = 'SELECT 1'
  private static final String COMMAND_GET_CONF = 'getconf'
  private static final String GET_CONF_SYSTEM_PAGE_SIZE = 'PAGE_SIZE'
  private static final String PROC_SEM = '/proc/sys/kernel/sem'
  private static final String PROC_SHMALL = '/proc/sys/kernel/shmall'
  private static final String PROC_SHMMAX = '/proc/sys/kernel/shmmax'
  private static final long   MIN_SEMMNI = 1536L
  private static final long   MIN_SEMMNS = 32000L
  private static final long   MIN_SHMMAX = 536870912L //512MB

  private PostgresCommands newCommands // current database version
  private PostgresCommands oldCommands // previous database version (used for upgrades)

  private CommandResult runProcess( List<String> args, Map<String,String> environment = [:] ) {
    LOG.debug("Postgres command : " + args.collect { "'${it}'" }.join(" ") )
    def outlines = []
    def errlines = []
    try {
      ProcessBuilder pb = new ProcessBuilder(args)
      pb.environment( ).putAll( environment )
      def root = new File("/")
      pb.directory(root)
      Process p = pb.start()
      OutputStream outstream = new ByteArrayOutputStream( 8192 )
      OutputStream errstream = new ByteArrayOutputStream( 8192 )
      p.consumeProcessOutput(outstream, errstream)
      int result = p.waitFor()
      outstream.toString().eachLine { line -> outlines.add(line); LOG.debug("stdout: ${line}") }
      errstream.toString().eachLine { line -> errlines.add(line); LOG.debug("stderr: ${line}") }
      return new CommandResult( result, outlines, errlines )
    } catch ( Exception ex ) {
      throw new DatabaseProcessException(
          "Failed to run '" + args.collect { "'${it}'" }.join(" ") + "' because of: ${ex.message}",
          outlines,
          errlines,
          ex )
    }
  }

  private CommandResult runLibpqProcess( List<String> args ) {
    final File passFile = new File( PG_PASSFILE )
    try {
      createOwnerReadWrite( passFile ).write( "*:*:*:*:${password}" )
      return runProcess(args, [
          PGHOST: SubDirectory.DB.getChildPath( EUCA_DB_DIR ),
          PGPORT: PG_PORT as String,
          PGUSER: userName,
          PGPASSFILE: PG_PASSFILE
      ])
    } finally {
      passFile.delete( )
    }
  }

  //Default constructor
  PostgresqlBootstrapper( ) {
    try {
      Properties props = new Properties( ) {{
        try {
          this.load( ClassLoader.getSystemResource( 'postgresql-binaries.properties' ).openStream( ) )
        } catch (Exception ex) {
          throw new FileNotFoundException('postgresql-binaries.properties not found in the classpath')
        }
      }}

      String home = getPropertyWithDefault( 'euca.db.home', props )
      String suffix = getPropertyWithDefault( 'euca.db.suffix', props )
      String prefix = home ? new File( new File( home ), PG_BIN ).path + File.separator : ''

      newCommands = new PostgresCommands(
          ctl: prefix + PG_CTL + suffix,
          initdb: prefix + PG_INITDB + suffix,
          dump: prefix + PG_DUMP + suffix,
          restore: prefix + PG_RESTORE + suffix,
          dumpall: prefix + PG_DUMPALL + suffix,
          sql: prefix + PG_SQL + suffix,
      )

      String oldHome = getPropertyWithDefault( 'euca.db.old.home', props )
      String oldSuffix = getPropertyWithDefault( 'euca.db.old.suffix', props )
      String oldPrefix = oldHome ? new File( new File( oldHome ), PG_BIN ).path + File.separator : ''

      oldCommands = new PostgresCommands(
          ctl: oldPrefix + PG_CTL + oldSuffix,
          initdb: oldPrefix + PG_INITDB + oldSuffix,
          dump: oldPrefix + PG_DUMP + oldSuffix,
          restore: oldPrefix + PG_RESTORE + oldSuffix,
          dumpall: oldPrefix + PG_DUMPALL + oldSuffix,
          sql: oldPrefix + PG_SQL + oldSuffix,
      )

      if ( oldCommands == newCommands ) {
        oldCommands = null
      }

      LOG.debug("DB_HOME = " + home + " : PG_CTL = " + newCommands.ctl + " : PG_INITDB  = " + newCommands.initdb)
    } catch ( Exception ex ) {
      LOG.error("Required Database variables are not correctly set: ${ex.message}")
      LOG.debug(ex, ex)
      System.exit(1)
    }
  }

  private String getPropertyWithDefault( String name, Properties properties ) {
    Strings.emptyToNull( System.getProperty( name, '' ) ) ?: properties.getProperty( name, '' )
  }

  @Override
  void init( ) {
    try {
      kernelParametersCheck( )
      
      if ( !versionCheck( ) ){
        throw new RuntimeException("Postgres versions less than 9.1.X are not supported")
      }
      
      if ( !initDatabase( ) ) {
        throw new RuntimeException("Unable to initialize the postgres database")
      }
      
      if ( !startDatabase( ) ) {
        throw new RuntimeException("Unable to start the postgres database")
      }
      
      if ( !createSchema( ) ) {
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
      long semmni = UnsignedLongs.parseUnsignedLong( semStrs[3] )
      long semmns = UnsignedLongs.parseUnsignedLong( semStrs[1] )
      long shmall = UnsignedLongs.parseUnsignedLong( shmallStr )
      long shmmax = UnsignedLongs.parseUnsignedLong( shmmaxStr )
      
      LOG.info "Found kernel parameters semmni=$semmni, semmns=$semmns, shmall=$shmall, shmmax=$shmmax"
      
      // Parameter descriptions from "man proc"
      if ( semmni >= 0 && semmni < MIN_SEMMNI ) {
        LOG.error "Insufficient operating system resources! The available number of semaphore identifiers is too low (semmni < $MIN_SEMMNI)"
      }
      if ( semmns >= 0 && semmns < MIN_SEMMNS ) {
        LOG.error "Insufficient operating system resources! The available number of semaphores in all semaphore sets is too low (semmns < $MIN_SEMMNS)"
      }
      if ( shmall >= 0 && shmall < MIN_SHMALL ) {
        LOG.error "Insufficient operating system resources! The total number of pages of System V shared memory is too low (shmall < $MIN_SHMALL)"
      }
      if ( shmmax >= 0 && shmmax < MIN_SHMMAX ) {
        LOG.error "Insufficient operating system resources! The run-time limit on the maximum (System V IPC) shared memory segment size that can be created is too low (shmmax < $MIN_SHMMAX)"
      }
    }  catch ( Exception e ) {
      LOG.error("Error checking kernel parameters: " + e.message )
    }
  }
  
  // Version check to ensure only Postgres 9.X creates the db.
  private boolean versionCheck( ) {
    try {
      String cmd = newCommands.initdb + " --version"
      def pattern = ~/.*\s+9\.[1-9]\d*(\.\d+)*$/
      pattern.matcher( cmd.execute( ).text.trim( ) ).matches( )
    } catch ( Exception e ) {
      LOG.fatal("Unable to find the initdb command")
      false
    }
  }
  
  private boolean initDatabase( ) throws Exception {
    final File passFile = new File( PG_PASSWORDFILE )
    try {
      createOwnerReadWrite( passFile ).write( password )
      CommandResult result = runProcess([
        newCommands.initdb,
        PG_ENCODING,
        PG_LOCALE,
        PG_USER_OPT,
        PG_TRUST_OPT,
        PG_PWD_FILE_OPT,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
        PG_X_OPT + PG_X_DIR,
        PG_ENCODING
      ])
      if ( result.code != 0 ) {
        LOG.fatal( "Database initialization failed with error code: ${result.code}" )
        result.processOut.each{ String outputLine -> LOG.info( outputLine ) }
        result.processErr.each{ String outputLine -> LOG.error( outputLine ) }
        throw new RuntimeException( "Database initialization failed with error code: ${result.code}" )
      }
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
        createOwnerReadWrite( SubDirectory.DB.getChildFile( EUCA_DB_DIR, "server.crt") ).with {
          PEMFiles.write( getAbsolutePath(), certificate )
        }
        createOwnerReadWrite( SubDirectory.DB.getChildFile( EUCA_DB_DIR, "server.key") ).with {
          PEMFiles.write( getAbsolutePath(), keyPair )
        }
      } else {
        LOG.warn("Credentials not found for database, not creating SSL certificate/key files")
      }
      void
    }
  }
  
  private File createOwnerReadWrite( final File file ) {
    Files.createFile(
        file.toPath( ),
        PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString( "rw-------" ) ) )
    file
  }

  private Iterable<Pair<String,Optional<String>>> databases( ) {
    Iterables.transform(
        PersistenceContexts.list( ),
        Pair.robuilder( PersistenceContexts.toDatabaseName( ), PersistenceContexts.toSchemaName( ) ) )
  }
  
  private boolean createSchema( ) throws Exception {
    if ( !isRunning( ) ) {
      throw new Exception("The database must be running to create the tables")
    }

    final Set<String> createdDatabases = Sets.newHashSet( )
    final Set<Pair<String,String>> createdSchemas = Sets.newHashSet( )
    for ( Pair<String,Optional<String>> databasePair : databases( ) ) {
      final String databaseName = databasePair.left
      final String schemaName = databasePair.right.orNull( )
      if ( createdDatabases.add( databaseName ) ) {
        try {
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
        listSchemas( databaseName ).each{ String schema -> createdSchemas << Pair.pair( databaseName, schema )  }
      }

      if ( schemaName && !createdSchemas.contains( Pair.pair( databaseName, schemaName ) ) ) try {
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
  
  private boolean startDatabase( ) throws Exception {
    OrderedShutdown.registerPostShutdownHook( new Runnable( ) {
          @Override
          void run( ) {
            File pidfile = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "postmaster.pid" )
            if (!pidfile.exists()) {
                return
            }
            ProxoolFacade.shutdown()
            try {
              int value = runProcess([
                newCommands.ctl,
                PG_STOP,
                PG_MODE,
                PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
              ]).code
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
      CommandResult result = runProcess([
        newCommands.ctl,
        PG_START,
        PG_W_OPT,
        PG_S_OPT,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
        PG_PORT_OPTS2
      ])
      if ( result.code != 0 ) {
        throw new DatabaseProcessException(
            "Postgresql startup failed with error code: ${result.code}",
            result.processOut,
            result.processErr )
      }

      LOG.info("Postgresql startup succeeded.")

      perhapsUpdateDbPassword( )
    } catch ( DatabaseProcessException ex ) {
      String postgresVersionError = 'database files are incompatible with server';
      if ( BootstrapArgs.isUpgradeSystem( ) && Iterables.tryFind(
          Iterables.concat( ex.processErr, ex.processOut ),
          { String line -> line.contains( postgresVersionError ) } as Predicate<String> ).isPresent( ) ) try {
        return startDatabaseWithUpgrade( )
      } catch ( DatabaseProcessException ex2 ) {
        LOG.fatal("Postgresql start with format upgrade failed: ${ex2.message} - \n${Joiner.on('\n').join( ex2.processErr )}" )
        return false
      }
      LOG.fatal("Postgresql startup failed: " + ex.message)
      return false
    } catch ( Exception ex ) {
      LOG.fatal("Postgresql startup failed: " + ex.message)
      return false
    }
    
    true
  }

  private void perhapsUpdateDbPassword( ) {
    try {
      dbExecute( 'postgres', PG_TEST_QUERY )
    } catch ( Exception e ) {
      if ( e.message?.contains('authentication') ) {
        LOG.info( "Updating database password" )
        String oldPassword = Signatures.SHA256withRSA.trySign( Eucalyptus.class, "eucalyptus".getBytes( ) )
        try {
          withConnection( getConnectionInternal( InetAddress.getByName('127.0.0.1'), 'postgres', null, userName, oldPassword ) ) { Sql sql ->
            sql.execute( "ALTER ROLE " + getUserName( ) + " WITH PASSWORD \'" + getPassword( ) + "\'" )
          }
          dbExecute( 'postgres', PG_TEST_QUERY )
        } catch ( Exception e2 ) {
          LOG.warn( "Unable to update database password: ${e2.message}" )
        }
      }
    }
  }

  private boolean startDatabaseWithUpgrade( ) throws Exception {
    LOG.info( 'Database format incompatible, upgrading ...' )

    //check for old postgres version
    LOG.info( 'Checking for previous postgres version' )
    if ( oldCommands == null ) {
      LOG.fatal( "Cannot upgrade databases to current postgres version, old postgres version unknown." )
      return false
    }
    CommandResult versionCommandResult = runProcess( [ oldCommands.initdb, PG_VERSION_OPT ] );
    if ( versionCommandResult.code == 0 ) {
      LOG.info( "Old postgres version: ${versionCommandResult.processOut.getAt( 0 )}" )
    } else {
      LOG.fatal( "Error checking old database version" )
      return false
    }

    // verify backup exists
    LOG.info( 'Verifying database backup exists' )
    File oldDbDir = EUCA_OLD_HOME ? new File( EUCA_OLD_HOME, 'var/lib/eucalyptus/db/data/postgresql.conf' ) : null
    if ( oldDbDir == null || !oldDbDir.isFile( ) ) {
      LOG.fatal( "Old eucalyptus home unset or invalid" )
      return false
    }

    // start old postgres with current data
    LOG.info( 'Starting previous postgres version' )
    runProcess([
        oldCommands.ctl,
        PG_START,
        PG_W_OPT,
        PG_S_OPT,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
        PG_PORT_OPTS2
    ]).with{ CommandResult result ->
      if ( result.code != 0 ) {
        throw new DatabaseProcessException(
            "Postgresql previous version startup failed with error code: ${result.code}",
            result.processOut,
            result.processErr )
      }
    }

    // update password if necessary
    perhapsUpdateDbPassword( )

    // dump all databases
    LOG.info( 'Dumping databases' )
    String databaseDumpAllFile = File.createTempFile( "euca-db-", ".dumpall.sql", SubDirectory.UPGRADE.file )
    try {
      runLibpqProcess([
          newCommands.dumpall, // use more recent version
          '--no-password',
          '--clean',
          "--file=${databaseDumpAllFile}" as String,
          '--lock-wait-timeout=60000',
          '--oids',
          '--quote-all-identifiers'
      ]).with { CommandResult result ->
        if (result.code != 0) {
          throw new DatabaseProcessException(
              "Postgresql dump all failed with error code: ${result.code}",
              result.processOut,
              result.processErr)
        }
      }

      // stop old postgres
      runProcess([
          oldCommands.ctl,
          PG_STOP,
          PG_MODE,
          PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
      ]).with { CommandResult result ->
        if ( result.code != 0 ) {
          throw new DatabaseProcessException(
              "Postgresql stop failed with error code: ${result.code}",
              result.processOut,
              result.processErr )
        }
      }

      // delete old format data
      LOG.info('Initializing database for current postgres version')
      SubDirectory.DB.getChildFile( EUCA_DB_DIR ).deleteDir( )
      SubDirectory.DB.getChildFile( EUCA_TX_DIR ).deleteDir( )

      // initialize and start new database
      if ( !initDatabase( ) ) {
        throw new RuntimeException("Unable to initialize the postgres database")
      }
      if ( !startDatabase( ) ) {
        throw new RuntimeException("Unable to start the postgres database")
      }

      // import databases
      LOG.info('Restoring databases')
      runLibpqProcess([
          newCommands.sql,
          '--no-password',
          '--dbname=postgres',
          "--file=${databaseDumpAllFile}" as String,
          '--quiet',
      ]).with { CommandResult result ->
        if (result.code != 0) {
          throw new DatabaseProcessException(
              "Postgresql dump all failed with error code: ${result.code}",
              result.processOut,
              result.processErr)
        }
      }

      LOG.info('Database format upgrade complete')
      return true
    } finally {
      new File( databaseDumpAllFile ).delete( )
    }
  }

  @Override
  boolean load( ) throws Exception {

    if ( !startDatabase( ) ) {
      throw new Exception("Unable to start postgresql")
    }
    
    if ( !createSchema( ) ) {
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
    getConnectionInternal( InetAddress.getByName('127.0.0.1'), database, schema )
  }

  private Sql getConnectionInternal( InetAddress host, String database, String schema ) throws Exception {
    getConnectionInternal( host, database, schema, userName, password )
  }

  private Sql getConnectionInternal( InetAddress host, String database, String schema, String connUserName, String connPassword ) throws Exception {
    String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, host, database ) )
    Sql sql = Sql.newInstance( url, connUserName, connPassword, driverName )
    if ( schema ) sql.execute( "SET search_path TO ${schema}" as String )
    sql
  }

  private boolean dbExecute( String database, String statement ) throws Exception {
    return withConnection( getConnection( database, null ) ) { Sql sql ->
      sql.execute( statement )
    }
  }

  private boolean withConnection( Sql sql, Closure<Boolean> closure ) {
    try {
      closure.call( sql )
    } finally {
      sql?.close()
    }
  }

  private void testContext( String databaseName ) throws Exception {
    try {
      if( !dbExecute( databaseName, PG_TEST_QUERY ) ) {
        LOG.error("Unable to ping the database : " + databaseName)
      }
    } catch (Exception exception) {
      LOG.error("Failed to test the context : ", exception)
      System.exit(1)
    }
  }
  
  @Override
  List<String> listDatabases( ) {
    listDatabases( InetAddress.getByName('127.0.0.1') )
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
    listSchemas( InetAddress.getByName('127.0.0.1'), database )
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
    String databaseDumpFile = File.createTempFile( "euca-sdb-", ".dump.tar", SubDirectory.UPGRADE.file )
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
      int dumpCode = runLibpqProcess( [
          newCommands.dump,
          '-w',
          '-f', databaseDumpFile,
          '-F', 'tar',
          '-n', sourceSchema,
          sourceDatabase ] ).code
      if ( dumpCode != 0 ) {
        throw new Exception( "Database dump failed with exit code ${dumpCode}" )
      }
      LOG.info( "Restoring schema ${sourceSchema} in ${destinationDatabase}" )
      int restoreCode = runLibpqProcess( [
          newCommands.restore,
          '-w',
          '-e',
          '-1',
          '-F', 'tar',
          '-d', destinationDatabase,
          databaseDumpFile ] ).code
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
      int value = runProcess([
        newCommands.ctl,
        PG_STATUS,
        PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
      ]).code
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
    
    if ( !startDatabase() ) {
      LOG.fatal("Unable to start the postgresql server")
      throw new Exception("Unable to start the postgres server")
    }
  }
  
  @Override
  boolean stop( ) throws Exception {
    int value = runProcess([
      newCommands.ctl,
      PG_STOP,
      PG_MODE,
      PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
    ]).code
    if ( value != 0 ) {
      LOG.error("Unable to stop the postgresql server (code:${value})")
      return false
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
    EUCA_DB_USER
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
  public String getServicePath( String... pathParts ) {
    return pathParts != null && pathParts.length > 0 ? Joiner.on("/").join(Arrays.asList(pathParts)) : "eucalyptus"
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

  private static class DatabaseProcessException extends RuntimeException {
    final List<String> processOut
    final List<String> processErr

    DatabaseProcessException( String message, List<String> processOut, List<String> processErr ) {
      super( message )
      this.processOut = processOut
      this.processErr = processErr
    }

    DatabaseProcessException( String message, List<String> processOut, List<String> processErr, Throwable cause ) {
      super( message, cause )
      this.processOut = processOut
      this.processErr = processErr
    }
  }

  @Immutable
  private static class CommandResult {
    int code
    List<String> processOut
    List<String> processErr
  }

  @Immutable
  private static class PostgresCommands {
    String ctl
    String initdb
    String dump
    String restore
    String dumpall
    String sql
  }
}
