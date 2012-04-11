/*
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
 *******************************************************************************/

 import java.io.File
 import java.sql.Connection
 import java.sql.DriverManager
 import java.sql.Statement
 import org.apache.log4j.Logger
 import com.eucalyptus.bootstrap.Bootstrapper
 import com.eucalyptus.bootstrap.DatabaseBootstrapper
 import com.eucalyptus.bootstrap.Databases
 import com.eucalyptus.component.Component
 import com.eucalyptus.component.Components
 import com.eucalyptus.component.ServiceUris
 import com.eucalyptus.component.id.Eucalyptus.Database
 import com.eucalyptus.entities.PersistenceContexts
 import com.eucalyptus.system.SubDirectory
 import com.eucalyptus.util.Internets
 import com.google.common.base.Joiner
 import java.util.regex.Pattern
 import org.logicalcobwebs.proxool.ProxoolFacade
 import com.eucalyptus.bootstrap.OrderedShutdown
 import java.io.PrintStream
 
 /*
  * REQUIREMENTS : Postgres 9.1 and Postgres jdbc driver
  *
  * SUMMARY : The PostgresqlBootstrapper class attempts to control the postgres database.  The methods
  * that control the database are : init, start, stop, hup, load, isRunning and destroy.
  */
 
 public class PostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {
 
     private static Logger LOG = Logger.getLogger( "setup_db" );
 
     // Static definitions of postgres commands and options
 
     private static String EUCA_DB_DIR  = SubDirectory.DB.getFile().getPath() + "/data/";
     private static String PG_BIN = "/usr/pgsql-9.1/bin/pg_ctl";
     private static String PG_START = "start";
     private static String PG_STOP_OPTS = "-mf"
     private static String PG_STOP = "stop";
     private static String PG_PORT_OPTS2 = "-o -h0.0.0.0/0 -p8777 -i";
     private static String PG_DB_OPT = "-D";
     private static String PG_INITDB = "/usr/pgsql-9.1/bin/initdb ";
     private static String PG_X_OPT = " -X ";
     private static String PG_X_DIR =  SubDirectory.DB.getFile().getPath() + "/tx/";
     private static String PG_USER_OPT = " -Ueucalyptus ";
     private static String PG_TRUST_OPT = " --auth=password --pwfile=";
     private static String PG_PWD_FILE = " --pwfile= ";
     private static int    PG_MAX_RETRY = 5;
     private static String PG_PASSFILE = "pass.txt";
     
     //Default constructor
     public PostgresqlBootstrapper( ) {
     }
 
     @Override
     public void init( ) throws Exception {
 
	 try {
	     if ( !versionCheck( ) ){
		 throw new Exception("Postgres versions less than 9.1.X are not supported");
	     }
	     
	     if ( !initdbPG( ) ) {
		 throw new Exception("Unable to initialize the postgres database");
	     }
 
	     if ( !startResource( ) ) {
		 throw new Exception("Unable to start the postgres database");
	     }
 
	     if ( !createDBSql( ) ) {
		 throw new Exception("Unable to create the eucalyptus database tables");
	     }
 
	     Component dbComp = Components.lookup( Database.class );
	     dbComp.initService( );
	     prepareService();

	 } catch ( Exception ex ) {
	     LOG.error( ex, ex );
	     throw ex;
	 }
     }
 
    // Version check to ensure only Postgres 9.1.X creates the db. 
    private boolean versionCheck ( ) {
      try {
	String cmd = PG_INITDB + " --version";
	if ( cmd.execute( ).text.contains("9.1") )
	  return true;
      } catch ( Exception e ) {
	    LOG.fatal("Unable to find the initdb command");
	    return false;
      }
    }
     
     private boolean initdbPG( ) throws Exception {
 
	 String pass = Databases.getPassword( );
	 File passFile = new File( SubDirectory.DB.getFile( ).getPath( ) + File.separator +  PG_PASSFILE )
	 passFile.write( pass );
	 String command = PG_INITDB + PG_USER_OPT + PG_TRUST_OPT + passFile + " " + PG_DB_OPT + " " + EUCA_DB_DIR + PG_X_OPT + PG_X_DIR;
	 try {
	     if (!runProc( command ) ) {
		 LOG.debug("Database server did not init.");
		 return false;
	     } else {
		 LOG.debug( "Database init complete." );
		 initDBFile( passFile );
		 return true;
	     }
	 } catch (Exception e) {
	     LOG.error( "Unable to init the database.", e );
	     passFile.delete( );
	     return false;
	 }
 
	 return false;
     }
 
     private void initDBFile( File passFile ) throws Exception {
	 
	 passFile.delete( );
	 File ibdata1;
	 try {
	     ibdata1 = new File( EUCA_DB_DIR + File.separator + "ibdata1" ).append("");
	     initPGHBA( );
	     initPGCONF( );
	 } catch ( Exception e ) {
	     LOG.debug("Unable to create the configuration files");
	     ibdata1.delete( );
	     throw e;
	 }
     }
 
     private void initPGHBA() throws Exception {
 
	 try {
	     File orgPGHBA = new File(EUCA_DB_DIR + File.separator +  "pg_hba.conf");
	     File tmpPGHBA = new File(EUCA_DB_DIR + File.separator + "pg_hba.conf.org");
	     orgPGHBA.renameTo(tmpPGHBA);
	     File newPGHBA = new File(EUCA_DB_DIR + File.separator + "pg_hba.conf");
	     newPGHBA.append("local\tall\tall\t\tpassword\n");
	     newPGHBA.append("host\tall\tall\t0.0.0.0/0\tpassword\n");
	     newPGHBA.append("host\tall\tall\t::1/128\tpassword\n");
	 } catch (Exception e) {
	     LOG.debug("Unable to create the pg_hba.conf file", e);
	     throw e;
	 }
     }
 
     private void initPGCONF() throws Exception {
	 try {
	     File orgPGCONF = new File(EUCA_DB_DIR + File.separator +  "postgresql.conf");
	     File bakPGCONF = new File(EUCA_DB_DIR + File.separator + "postgresql.conf.org");
	     String pgconfText = orgPGCONF.getText();
	     bakPGCONF.write(pgconfText);
	     Pattern pattern_max_connection = ~/max_connections\s+=\s+\d+/;
	     orgPGCONF.setText(pgconfText.replaceAll(pattern_max_connection, "max_connections = 1000"));
	     Pattern pattern_socket_dir = ~/#unix_socket_directory\s+=\s+\'\'/;
	     String pgconfText2 = orgPGCONF.getText();
	     orgPGCONF.setText(pgconfText2.replaceAll(pattern_socket_dir, "unix_socket_directory = " + "'" + EUCA_DB_DIR + "'" ) )
	 } catch (Exception e) {
	     LOG.debug("Unable to modify the postgresql.conf file", e);
	     throw e;
	 }
     }
 
     private boolean createDBSql( ) throws Exception {
 
	 if ( !isRunning( ) ) {
	     throw new Exception("The database must be running to create the tables");
	 }
 
	 Connection conn = null;
 
	 for ( String contextName : PersistenceContexts.list( ) ) {
 
	     if (!contextName.startsWith("eucalyptus_")) {
		 contextName = "eucalyptus_" + contextName;
	     }
 
	     try {
		 String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, Internets.localHostInetAddress( ), "postgres" ) );
		 conn = DriverManager.getConnection(url, Databases.getUserName(),Databases.getPassword());
		 Statement createSchema = conn.createStatement();
		 String dbName = "CREATE DATABASE " + contextName + " OWNER " + Databases.getUserName();
		 createSchema.execute(dbName);
		 LOG.debug("executed sql command : " + dbName + " within the schema : " + contextName);
		 conn.close();
 
		 String userUrl = String.format( "jdbc:%s", ServiceUris.remote( Database.class, Internets.localHostInetAddress( ), contextName ) );
		 conn = DriverManager.getConnection(userUrl, Databases.getUserName(), Databases.getPassword());
 
		 String alterUser = "ALTER ROLE " + Databases.getUserName() + " WITH LOGIN PASSWORD \'" + Databases.getPassword() + "\'";
                 Statement alterSchema = conn.createStatement();
 
		 LOG.debug("executed successfully = ALTER ROLE "
			 + Databases.getUserName() + " within schema " + contextName);
		 alterSchema.execute(alterUser);
		 conn.close();
	     } catch (Exception e) {
		 LOG.error("Unable to create the database.", e);
		 return false;
	     }
	 }
	 return true;
     }
 
 
     public boolean startResource() throws Exception {
	 OrderedShutdown.registerPostShutdownHook(new Runnable() {
		     @Override
		     public void run( ) {
			 try {
			     ProcessBuilder pb = new ProcessBuilder(PG_BIN, PG_STOP, PG_DB_OPT, EUCA_DB_DIR);
			     LOG.fatal("Postgresql shutdown: " + pb.command().toArray().toString());
			     Process p = pb.start();
			     PrintStream outstream = new PrintStream(System.getProperty( "euca.log.dir") + "/db.log");
			     PrintStream errstream = new PrintStream(System.getProperty( "euca.log.dir") + "/db-err.log");
			     p.consumeProcessOutput(outstream, errstream)
			     int value = p.waitFor();
			     if (value != 0) { LOG.fatal("Postgresql shutdown failed with exit code " + value); }
			     ProxoolFacade.shutdown();
			 } catch ( Exception ex ) {
			     ex.printStackTrace( );
			 }
		     }
		 }
		 );
	 if ( isRunning()) {
	     LOG.debug("Postgresql is already started, no action taken");
	     return true;
	 }
 
	 ProcessBuilder pb = new ProcessBuilder(PG_BIN, PG_START, "-w", "-s", PG_DB_OPT + EUCA_DB_DIR, PG_PORT_OPTS2);
	 Process p = pb.start();
	 int value = p.waitFor();
	 LOG.debug("waitFor value : " + value);
	 
	 if (value == 0) {
	     LOG.debug("The database has started");
	     return true;
	 } else {
	     LOG.debug("The database hasn't started");
	 }
 
	 return false;
     }
 
     @Override
     public boolean load( ) throws Exception {
 
	 LOG.debug("The load method is being executed");
	 if ( isRunning( ) ) {
	     return true;
	 }
 
	 // TODO : Determine if the SSL is required.
	 //try {
	 //   LOG.debug( "Initializing SSL just in case: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.crypto.util.SslSetup" ) );
	 //} catch ( Exception essl  ) {
	 //   LOG.debug("Unable to load the ssl setup", essl);
	 //}
	 try {
	     if (!startResource( ) ) {
		 LOG.debug("Unable to start postgresql");
		 return false;
	     }
	 } catch ( Exception e ) {
	     LOG.debug( "Failed to start the database.", e );
	     return false;
	 }
 
	 return true;
     }
 
     private void prepareService( ) throws Exception {
	 for ( String persistCtx : PersistenceContexts.list( ) ) {
	     testContext( persistCtx );
	 }
     }
 
     private void testContext(String context) throws Exception {
 
	 String ctxName = context.substring( context.indexOf("_") + 1 );
	 ClassLoader.getSystemClassLoader( ).loadClass( this.getDriverName( ) );
	 Connection conn = null;
 
	 try {
	     String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, Internets.localHostInetAddress( ), context ) );
	     conn = DriverManager.getConnection(url, Databases.getUserName( ), Databases.getPassword( ) );
	     Statement testEucaDB = conn.createStatement( );
	     String pgPing = "SELECT USER;"
	     if(!testEucaDB.execute(pgPing)) {
		 LOG.debug("Unable to ping the database : " + url);
	     }
	 } catch (Exception exception) {
	     LOG.debug("Failed to test the context : ", exception);
	     System.exit(1);
	 } finally {
 
	     if ( conn != null ) {
		 conn.close( );
	     }
	 }
     }
 
     public boolean isRunning() {
	 for ( int x = 0 ; x < PG_MAX_RETRY; x++) {
	     try {
		 File pidFile = new File(EUCA_DB_DIR + "postmaster.pid");
		 if ( pidFile.size() > 0 ) {
		     LOG.debug("Found the postmaster.pid file");
		     return true;
		 }
	     } catch (Exception e) {
		 LOG.debug("The postmaster.pid file was not found");
		 sleep(1000);
	     }
	 }
 
	 return false;
     }
 
     public void hup() {
 
	 if(!stop()) {
	     LOG.fatal("Unable to stop the postgresql server");
	     System.exit(1);
	 }
 
	 if (!startResource()) {
	     LOG.fatal("Unable to start the postgresql server");
	     System.exit(1);
	 }
     }
 
     @Override
     public boolean stop() throws Exception {
 
	 String command = PG_BIN + " " + PG_DB_OPT + " " + EUCA_DB_DIR + " " + PG_STOP_OPTS + " " +  PG_STOP;
 
	 try {
	     if(!runProc(command)) {
		 LOG.debug("Database server did not stop.");
		 return false;
	     } else {
		 LOG.debug("Database server stopped.");
		 return true;
	     }
	 } catch (Exception e) {
	     LOG.error("Unable to stop the database.", e);
	     throw e;
	 }
 
	 return false;
     }
 
     @Override
     public void destroy( ) throws IOException {
 
	 boolean status = false;
 
	 if ( isRunning( ) ) {
	     status = stop( );
	 } else {
	     LOG.debug("Database is not running");
	 }
 
	 LOG.debug("Final status after destroy : " + status );
     }
 
     private boolean runProc( String command ) throws Exception {
 
	 String cmd = command;
 
	 try {
	     LOG.debug("Running command: " + cmd);
	     Runtime rt = Runtime.getRuntime( );
	     Process proc = rt.exec(cmd);
	     int returnValue = proc.waitFor( );
	     int exitValue = proc.exitValue( );
	     def sout = new StringBuffer(2000), serr = new StringBuffer(2000)
	     proc.consumeProcessOutput(sout, serr)
 
	     LOG.debug("process output : " + sout.toString( ) );
	     LOG.debug("process error : " + serr.toString( ) );
	     LOG.debug("cmd = " + cmd + " : exitValue = " + exitValue + " : returnValue = " + returnValue);
 
	     if (returnValue != 0) {
		 return false;
	     } else {
		 return true;
	     }
	 } catch (Exception e) {
	     LOG.error("Unable to execute cmd : " + cmd, e);
	 }
 
	 return false;
     }
 
     @Override
     public String getDriverName( ) {
	 return "org.postgresql.Driver";
     }
 
     @Override
     public String getHibernateDialect( ) {
	 return "org.hibernate.dialect.PostgreSQLDialect";
     }
 
     @Override
     public String getJdbcDialect( ) {
	 return "net.sf.hajdbc.dialect.PostgreSQLDialect";
     }
 
     @Override
     public String getServicePath( String... pathParts ) {
	 return pathParts != null && pathParts.length > 0 ? Joiner.on("/").join(pathParts) : "eucalyptus";
     }
 
     @Override
     public String getJdbcScheme( ) {
	 return "postgresql";
     }
 
 }

