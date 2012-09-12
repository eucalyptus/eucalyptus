/*
 * Copyright (c) 2012  Eucalyptus Systems, Inc.
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
 *******************************************************************************/

import com.eucalyptus.bootstrap.Bootstrapper
import com.eucalyptus.bootstrap.DatabaseBootstrapper
import com.eucalyptus.bootstrap.OrderedShutdown
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.component.Component
import com.eucalyptus.component.Components
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.auth.SystemCredentials
import com.eucalyptus.component.id.Eucalyptus.Database
import com.eucalyptus.crypto.util.PEMFiles
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.BaseDirectory
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.Internets
import com.google.common.base.Function
import com.google.common.base.Joiner
import com.google.common.collect.Iterables
import groovy.sql.Sql
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade


import static com.google.common.io.Closeables.closeQuietly
import static com.google.common.io.Flushables.flushQuietly
import static java.util.Collections.emptyMap
import static java.util.regex.Matcher.quoteReplacement
import static java.util.regex.Pattern.quote

/*
 * REQUIREMENTS : Postgres 9.1 and Postgres jdbc driver
 *
 * SUMMARY : The PostgresqlBootstrapper class attempts to control the postgres database.  The methods
 * that control the database are : init, start, stop, hup, load, isRunning and destroy.
 */
public class PostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {

    private static Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_db" )

    // Static definitions of postgres commands and options
    private static int    PG_MAX_RETRY = 5
    private static String EUCA_DB_DIR  = "data"
    private String PG_HOME = System.getProperty("euca.db.home","")
    private String PG_SUFFIX = ""
    private static String PG_BIN = "/bin/pg_ctl"
    private static String PG_START = "start"
    private static String PG_STOP = "stop"
    private static String PG_STATUS = "status"
    private static String PG_MODE = "-mf"
    private static String PG_PORT_OPTS2 = "-o -h0.0.0.0/0 -p8777 -i"
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

    private int runProcess( List<String> args ) {
        PrintStream outstream = null
        PrintStream errstream = null
        LOG.debug("Postgres 9.1 command : " + args.toString( ) )
        try {
            ProcessBuilder pb = new ProcessBuilder(args)
            Process p = pb.start()
            outstream = new PrintStream( BaseDirectory.LOG.getChildFile("db.log") )
            errstream = new PrintStream( BaseDirectory.LOG.getChildFile("db-err.log") )
            p.consumeProcessOutput(outstream, errstream)
            int result = p.waitFor()
            flushQuietly( outstream )
            flushQuietly( errstream )
            return result
        } finally {
            closeQuietly( outstream )
            closeQuietly( errstream )
        }
    }

    //Default constructor
    public PostgresqlBootstrapper( ) {
	try {
	    Properties props = new Properties() {{
                try {
		    this.load( ClassLoader.getSystemResource( "postgresql-binaries.properties" ).openStream( ) );
                } catch (Exception ex) {
                    throw new FileNotFoundException("postgresql-binaries.properties not found in the classpath")
                }
	    }};
	    
	    if ("".equals(PG_HOME)) {
		PG_HOME = props.getProperty("euca.db.home","")
	    }

            if ("".equals(PG_HOME)) {
               throw new Exception("Postgresql home directory is not set")
            }
 
	    PG_SUFFIX = props.getProperty("euca.db.suffix","")
	    
            PG_BIN = PG_HOME + PG_BIN + PG_SUFFIX
	    PG_INITDB = PG_HOME + PG_INITDB + PG_SUFFIX

            LOG.debug("PG_HOME = " + PG_HOME + " : PG_BIN = " + PG_BIN + " : PG_INITDB  = " + PG_INITDB)
	} catch ( Exception ex ) {
            LOG.error("Required Database variables are not correctly set "
              + "PG_HOME = " + PG_HOME + " : PG_BIN = " + PG_BIN + " : PG_INITDB  = " + PG_INITDB)
	    LOG.debug(ex, ex);
	    System.exit(1);
	}
    }

    @Override
    public void init( ) throws Exception {
        try {
            kernelParametersCheck( )

            if ( !versionCheck( ) ){
                throw new Exception("Postgres versions less than 9.1.X are not supported")
            }

            if ( !initdbPG( ) ) {
                throw new Exception("Unable to initialize the postgres database")
            }

            if ( !startResource( ) ) {
                throw new Exception("Unable to start the postgres database")
            }

            if ( !createDBSql( ) ) {
                throw new Exception("Unable to create the eucalyptus database tables")
            }

            if ( !createCliUser( ) ) {
                throw new Exception("Unable to create the cli user")
            }

            Component dbComp = Components.lookup( Database.class )
            dbComp.initService( )
            prepareService( )

        } catch ( Exception ex ) {
            LOG.error( ex, ex )
            throw ex
        }
    }

    private void kernelParametersCheck( ) {
        try {
            LOG.debug "Reading '/proc' kernel parameters"
            String[] semStrs = new File( PROC_SEM ).text.split("\\s")
            String shmallStr = new File( PROC_SHMALL ).text.trim()
            String shmmaxStr = new File( PROC_SHMMAX ).text.trim()

            LOG.debug "Getting page size"
            String pageSizeStr = [COMMAND_GET_CONF, GET_CONF_SYSTEM_PAGE_SIZE].execute().text.trim()

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

    // Version check to ensure only Postgres 9.1.X creates the db.
    private boolean versionCheck( ) {
        try {
            String cmd = PG_INITDB + " --version"
            return cmd.execute( ).text.contains("9.1")
        } catch ( Exception e ) {
            LOG.fatal("Unable to find the initdb command")
            return false
        }
    }

    private boolean initdbPG( ) throws Exception {
        final File passFile = SubDirectory.DB.getChildFile( PG_PASSFILE )
        try {
            passFile.write( getPassword() )
            int value = runProcess([
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
            if (value != 0) {
                LOG.debug("Database server did not init.")
                return false
            } else {
                LOG.debug( "Database init complete." )
                initDBFile()
                return true
            }
        } catch (Exception e) {
            LOG.error( "Unable to init the database.", e )
            return false
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
                    unix_socket_directory: "'" + SubDirectory.DB.getChildPath( EUCA_DB_DIR ) + "'",
                    ssl: PG_USE_SSL ? 'on' : 'off',
                    ssl_ciphers: '\'AES128-SHA:AES256-SHA\''
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
        return true
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

    private Function<String, String> contextToDatabaseNameMapper() {
        return new Function<String,String>() {
            @Override
            String apply( String contextName ) {
                if ( !contextName.startsWith("eucalyptus_") ) {
                    contextName = "eucalyptus_" + contextName
                }
                return contextName
            }
        }
    }

    private Iterable<String> databases() {
        return Iterables.transform( PersistenceContexts.list( ), contextToDatabaseNameMapper() )
    }

    private boolean createDBSql( ) throws Exception {
        if ( !isRunning( ) ) {
            throw new Exception("The database must be running to create the tables")
        }

        for ( String databaseName : databases() ) {
            try {
                String dbName = "CREATE DATABASE " + databaseName + " OWNER " + getUserName( )
                dbExecute( PG_DEFAULT_DBNAME, dbName )

                String alterUser = "ALTER ROLE " + getUserName( ) + " WITH LOGIN PASSWORD \'" + getPassword( ) + "\'"
                dbExecute( databaseName, alterUser )
            } catch (Exception e) {
	        if (!e.getMessage().contains("already exists")) {
                  LOG.error("Unable to create the database.", e)
                  return false
	        } 
            }
        }

        return true
    }

    private boolean startResource() throws Exception {
        OrderedShutdown.registerPostShutdownHook( new Runnable( ) {
                @Override
                public void run( ) {
                    try {
                        int value = runProcess([
                            PG_BIN,
                            PG_STOP,
                            PG_MODE,
                            PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
                        ])
                        if (value != 0) {
                            LOG.fatal("Postgresql shutdown failed with exit code " + value)
                        }
                        ProxoolFacade.shutdown()
                    } catch ( Exception e ) {
                        LOG.error("Postgresql shutdown failed with error", e)
                    }
                }
            } )

        if ( isRunning( ) ) {
            LOG.debug("Postgresql is already started, no action taken")
            return true
        }

        int value = runProcess([
            PG_BIN,
            PG_START,
            PG_W_OPT,
            PG_S_OPT,
            PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR),
            PG_PORT_OPTS2
        ])

        if (value != 0) {
            LOG.fatal("Postgresql startup failed with exit code " + value)
            return false
        }

        return true
    }

    @Override
    public boolean load( ) throws Exception {
        if ( isRunning( ) ) {
            return true
        }

        if (!startResource( ) ) {
            throw new Exception("Unable to start postgresql")
        }

	if ( !createDBSql( ) ) {
	    throw new Exception("Unable to create the eucalyptus database tables")
	}
	
	Component dbComp = Components.lookup( Database.class )
	dbComp.initService( )
	prepareService( )
	
        return true
    }

    private void prepareService( ) throws Exception {
        for ( String databaseName : databases() ) {
            testContext( databaseName )
        }
    }

    private boolean dbExecute( String databaseName, String statement ) throws Exception {
        String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, Internets.localHostInetAddress( ), databaseName ) )
        Sql sql = null
        try {
            sql = Sql.newInstance( url, getUserName(), getPassword(), getDriverName() )
            return sql.execute( statement )
        } finally {
            sql?.close()
        }
    }

    private void testContext( String databaseName ) throws Exception {
        try {
            String pgPing = "SELECT USER"
            if( !dbExecute( databaseName, pgPing ) ) {
                LOG.debug("Unable to ping the database : " + url)
            }
        } catch (Exception exception) {
            LOG.debug("Failed to test the context : ", exception)
            System.exit(1)
        }
    }

    public boolean isRunning() {
	int value = runProcess([
	    PG_BIN,
	    PG_STATUS,
	    PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
	])

	if (value != 0) {
	    return false
	}

       return true
    }

    public void hup( ) {
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
    public boolean stop( ) throws Exception {
        try {
            int value = runProcess([
                PG_BIN,
                PG_STOP,
                PG_MODE,
                PG_DB_OPT + SubDirectory.DB.getChildPath(EUCA_DB_DIR)
            ])
            if( value != 0 ) {
                LOG.debug("Database server did not stop.")
                return false
            } else {
                LOG.debug("Database server stopped.")
                return true
            }
        } catch (Exception e) {
            LOG.error("Unable to stop the database.", e)
            throw e
        }
    }

    @Override
    public void destroy( ) throws IOException {
        boolean status = false

        if ( isRunning( ) ) {
            status = stop( )
        } else {
            LOG.debug("Database is not running")
        }

        LOG.debug("Final status after destroy : " + status )
    }

    @Override
    public String getPassword() {
        return SystemIds.databasePassword()
    }

    @Override
    public String getUserName() {
        return DatabaseBootstrapper.DB_USERNAME
    }

    @Override
    public String getDriverName( ) {
        return "org.postgresql.Driver"
    }

    @Override
    public String getHibernateDialect( ) {
        return "org.hibernate.dialect.PostgreSQLDialect"
    }

    @Override
    public String getJdbcDialect( ) {
        return "net.sf.hajdbc.dialect.PostgreSQLDialect"
    }

    @Override
    public String getServicePath( String... pathParts ) {
        return pathParts != null && pathParts.length > 0 ? Joiner.on("/"). join(pathParts.iterator()) : "eucalyptus"
    }

    @Override
    public Map<String, String> getJdbcUrlQueryParameters() {
        return PG_USE_SSL ? [
            ssl:'true',
            sslfactory: 'com.eucalyptus.postgresql.PostgreSQLSSLSocketFactory'
        ] : emptyMap()
    }

    @Override
    public String getJdbcScheme( ) {
        return "postgresql"
    }
}
