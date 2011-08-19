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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.Runtime;
import java.lang.Process;
import org.apache.log4j.Logger;
import java.net.Socket;
import java.io.File;
import java.util.List;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DatabaseBootstrapper;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Database;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.system.SubDirectory;



/* DISCLAIMER : This is prototype code.  Not all the functionality is properly working at this time.
 *
 * REQUIREMENTS : Postgres 8.4 and Postgres jdbc driver
 *
 * SUMMARY : The PostgresqlBootstrapper class attempts to control the postgres database.  The methods
 * that control the database are : init, start, stop, hup, load, isRunning and destroy.
 */

public class PostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {

	private static Logger LOG = Logger.getLogger( "setup_db" );

	// Static definitions of postgres commands and options

	private static String EUCA_DB_DIR  = SubDirectory.DB.getFile().getPath() + "/data";
	private static String PG_PATH = "/usr/bin/"
	private static String PG_BIN = "pg_ctl";
	private static String PG_STOP = " -mf stop";
	private static String PG_START = "start";
	private static String PG_PORT_OPTS2 = "-o \"-p8777 -i\"";
	private static String PG_DB_OPT = "-D ";
	private static String PG_INITDB = "initdb ";
	private static String PG_CREATE_DB = "createdb ";
	private static String PG_CREATE_OPTS = "-Oeucalyptus -Ueucalyptus ";
	private static String PG_X_OPT = " -X ";
	private static String PG_X_DIR =  SubDirectory.TX.getFile().getPath();
	private static String PG_USER_OPT = " -Ueucalyptus ";
	private static String PG_TRUST_OPT = " --auth=md5 --pwfile=";
	private static String PG_PWD_FILE = " --pwfile= ";
	private static int    PG_PORT	   = 8777;
	private static int    PG_MAX_RETRY = 5;

	//Default constructor
	public PostgresqlBootstrapper() {
	}

	@Override
	public void init() throws Exception {

		try {
			if (!initdbPG()) {
				LOG.fatal("Unable to init the postgres database");
				System.exit(1);
			}

			if (!start()) {
				LOG.fatal("Uable to start the postgres database");
				System.exit(1);
			}

			if (!createDBSql()) {
				LOG.fatal("Unable to create the eucalyptus database");
				System.exit(1);
			}


			Component dbComp = Components.lookup( Database.class );
			dbComp.initService( );

			prepareService();
		} catch ( Exception ex ) {
			LOG.error( ex, ex );
			throw ex;
		}
	}

	public boolean initdbPG() throws Exception {

		String pass = Databases.getPassword();

		File f = new File(SubDirectory.DB.getFile().getPath() + File.separator +  'pass.txt')

		f.write(pass);

		String command = PG_INITDB + PG_USER_OPT + PG_TRUST_OPT + f + " " + PG_DB_OPT + " " + EUCA_DB_DIR + PG_X_OPT + PG_X_DIR;

		try {
			if(!runProc(command)) {
				LOG.debug("Database server did not init.");
				return false;
			} else {
				LOG.debug("Database init complete.");
				initDBFile(f);
				return true;
			}
		} catch (Exception e) {
			LOG.error("Unable to init the database.", e);
			System.exit(1);
		}

		return false;
	}

	private void initDBFile(File f) {

		f.delete();

		String cmdTouch = "/bin/touch " + EUCA_DB_DIR +  "/ibdata1";

		try {
			cmdTouch.execute();
		} catch (Exception e ) {
			LOG.debug("Unable to create the ibdata1 file");
			System.exit(1);
		}
	}

	public boolean createDB() throws Exception {

		if (!isRunning()) {
			LOG.error("Unable to create the database");
		}

		for ( String contextName : PersistenceContexts.list() ) {

			if(!contextName.startsWith("eucalyptus_")) {
				contextName = "eucalyptus_" + contextName;
			}

			String command = PG_CREATE_DB + PG_CREATE_OPTS + contextName;

			try {
				if (!runProc(command)) {
					LOG.debug(" FAILED : Did not create the eucalyptus database");
					return false;
				} else {
					LOG.debug("Database creation complete : " + contextName);
				}
			} catch (Exception e) {
				LOG.error("Unable to create the database.", e);
				System.exit(1);
			}
		}

		return true;
	}


	public boolean createDBSql() throws Exception {

		if (!isRunning()) {
			LOG.error("Unable to create the database");
		}

		Connection conn = null;

		for ( String contextName : PersistenceContexts.list() ) {

			if(!contextName.startsWith("eucalyptus_")) {
				contextName = "eucalyptus_" + contextName;
			}

			try {

				String url = "jdbc:postgresql://127.0.0.1:8777/postgres";
				conn = DriverManager.getConnection(url, Databases.getUserName(), Databases.getPassword());
				Statement createSchema = conn.createStatement();

				String dbName = "CREATE DATABASE " + contextName + " OWNER " + Databases.getUserName();
				LOG.debug("contextName : " + contextName);
				createSchema.execute(dbName);
				conn.close();
			} catch (Exception e) {
				LOG.error("Unable to create the database.", e);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean start() throws Exception {

		if(isRunning()) {
			LOG.debug("Postgresql is already started, no action taken");
			return true;
		}

		String cmdtwo = "chmod -R 700 " + EUCA_DB_DIR;

		if (!runProc(cmdtwo)) {
			LOG.fatal("Unable to chmod the database directory : " + EUCA_DB_DIR);
			System.exit(1);
		}

		File tmp = new File("/usr/bin/");

		Process worknow  = [
			PG_BIN,
			PG_PORT_OPTS2,
			PG_DB_OPT,
			EUCA_DB_DIR,
			PG_START
		].execute(null,tmp);

		int value = worknow.waitFor();

		def sout = new StringBuffer(), serr = new StringBuffer()
		worknow.consumeProcessOutput(sout, serr)

		LOG.debug("process output : " + sout.toString());
		LOG.debug("process error : " + serr.toString());

		LOG.debug("Postgres start value : " + value);

		if (value == 0)
			return true;
		else
			return false;

		/* Leaving this section of code for further investigation.
		 LOG.debug("command : " + list);
		 try {
		 if(!runProc(command)) {
		 LOG.debug("Database server did not start.");
		 return false;
		 } else {
		 LOG.debug("Database server started.");
		 return true;
		 }
		 } catch (Exception e) {
		 LOG.error("Unable to start the database.", e);
		 System.exit(1);
		 }
		 return false;
		 */
	}

	@Override
	public boolean load() throws Exception {

		if(isRunning()) {
			return true;
		}

		try {
			LOG.debug( "Initializing SSL just in case: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.crypto.util.SslSetup" ) );
		} catch ( Exception essl  ) {
			LOG.debug("Unable to load the ssl setup", essl);
		}

		try {
			if (!start()) {
				LOG.debug("Unable to start postgresql");
				System.exit(1);
			}
		} catch ( Exception e ) {
			LOG.debug( "Failed to start the database.", e );
			System.exit(1);
		}
		return true;
	}

	public void prepareService() throws Exception {
		for ( String persistCtx : PersistenceContexts.list() ) {
			testContext(persistCtx);
		}
	}

	private void testContext(String context) throws Exception {

		String ctxName = context.substring( context.indexOf("_") + 1 );
		ClassLoader.getSystemClassLoader().loadClass( this.getDriverName() );
		Connection conn = null;

		try {
			String url = String.format( "jdbc:%s_%s",
					ComponentIds.lookup( Database.class ).makeInternalRemoteUri( "127.0.0.1" ,
					ComponentIds.lookup( Database.class ).getPort( ) ).toString( ),
					context.replaceAll( "eucalyptus_", "" ) );

			LOG.debug("connecting url : " + url);

			conn = DriverManager.getConnection(url, Databases.getUserName(), Databases.getPassword());

			if (conn != null) {
				LOG.debug("Connection made via jdbc to postgres");
			} else {
				LOG.debug("Unable to connect to postgres via jdbc");
			}

			Statement testEucaDB = conn.createStatement();
			String pgPing = "SELECT 1;"
			LOG.debug("pgPing : " + pgPing);

			if(!testEucaDB.execute(pgPing)) {
				LOG.debug("Unable to ping the database : " + url);
			}
		} catch (Exception exception) {
			LOG.debug("Failed test each context : ", exception);
			System.exit(1);
		} finally {

			if ( conn != null ) {
				conn.close( );
			}
		}
	}

	@Override
	public boolean isRunning() throws Exception {

		for ( int x = 0 ; x < PG_MAX_RETRY; x++) {
			try {
				Socket connection = new Socket("127.0.0.1", PG_PORT);
				connection.close();
				return true;
			} catch (Exception e) {
				LOG.debug("Unable to open port : " + PG_PORT + " : Retry : " + x);
				sleep(1000);
			}
		}

		LOG.debug("Retry limit reached : Unable to Connect to port " + PG_PORT);
		return false;
	}

	@Override
	public void hup() {

		if(!stop()) {
			LOG.fatal("Unable to stop the postgresql server");
			System.exit(1);
		}

		if (!start()) {
			LOG.fatal("Unable to start the postgresql server");
			System.exit(1);
		}
	}

	@Override
	public boolean stop() throws Exception {

		String command = PG_BIN + PG_DB_OPT + EUCA_DB_DIR + PG_STOP;

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
			System.exit(1);
		}

		return false;
	}

	@Override
	public void destroy() throws IOException {

		boolean status = false;

		if (isRunning()) {
			status = stop();
		} else {
			LOG.debug("Database is stopped");
		}

		LOG.debug("Final status after destroy : " + status );
	}

	@Override
	public String getDriverName() {
		return "org.postgresql.Driver";
	}

	@Override
	public String getHibernateDialect() {
		return "org.hibernate.dialect.PostgreSQLDialect";
	}

	@Override
	public String getJdbcDialect() {
		return "net.sf.hajdbc.dialect.PostgreSQLDialect";
	}

	@Override
	public String getUriPattern() {
		return "postgresql://%s:%d/eucalyptus";
	}

	public boolean runProc(String command) throws IOException {

		String cmd = null;

		if (command.contains("chmod")) {
			cmd = "/bin/" + command;
		} else {
			cmd = PG_PATH + command;
		}

		try {
			LOG.debug("Running command: " + cmd);
			Runtime rt = Runtime.getRuntime();
			rt.addShutdownHook( new Thread() {
						@Override
						public void run() {
							LOG.info( "Shutting down database." );
							if (isRunning()) {
								try {
									destroy();
								} catch ( Exception ex ) {
									ex.printStackTrace();
								}
							}
						}
					});

			Process proc = rt.exec(cmd);
			int returnValue = proc.waitFor();
			int exitValue = proc.exitValue();
			def sout = new StringBuffer(), serr = new StringBuffer()
			proc.consumeProcessOutput(sout, serr)

			LOG.debug("process output : " + sout.toString());
			LOG.debug("process error : " + serr.toString());

			LOG.debug("exitValue = " + exitValue + " : returnValue = " + returnValue);

			if(returnValue != 0) {
				return false;
			} else {
				return true;
			}
		} catch (Exception t) {
			LOG.error("Unable to execute cmd : " + cmd, t);
		}

		return false;
	}
}
