/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */

import com.google.common.base.Optional
import com.google.common.primitives.Ints

import java.sql.ResultSet
import com.eucalyptus.bootstrap.Bootstrapper
import com.eucalyptus.bootstrap.DatabaseBootstrapper
import com.eucalyptus.bootstrap.OrderedShutdown
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.component.Component
import com.eucalyptus.component.Components
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.id.Database
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.Pair
import com.google.common.base.Joiner
import com.google.common.base.MoreObjects
import com.google.common.collect.Iterables
import com.google.common.collect.Sets
import groovy.sql.Sql
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade

import static java.util.Collections.emptyMap

/*
 * REQUIREMENTS : Postgres 9.2 and Postgres JDBC driver
 *
 * SUMMARY : ExternalPostgresqlBootstrapper manages an external postgres database
 */
@SuppressWarnings(["GroovyUnusedDeclaration", "UnnecessaryQualifiedReference"])
class ExternalPostgresqlBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {

  private static Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_extdb' )

  // Static definitions for eucalyptus
  private static final String EUCA_DB_DIR  = 'data'
  private static final String EUCA_DB_USER = System.getProperty( 'euca.db.user', DatabaseBootstrapper.DB_USERNAME )
  private static final String EUCA_DB_PASS = System.getProperty( 'euca.db.pass', SystemIds.databasePassword( ) )

  // Static definitions for postgres
  private static final String PG_DEFAULT_DBNAME = 'postgres'
  private static final String PG_TEST_QUERY = 'SELECT 1'
  private static final Integer PG_PORT = MoreObjects.firstNonNull( Ints.tryParse( System.getProperty('euca.db.port', '' ) ), 8777 )
  private static final String PG_HOST = System.getProperty( 'euca.db.host', '127.0.0.1')
  private static final boolean PG_USE_SSL = Boolean.valueOf( System.getProperty('euca.db.ssl', 'false') )

  //Default constructor
  ExternalPostgresqlBootstrapper( ) {
  }

  @Override
  void init( ) {
    try {
      initDBFile()

      if ( !createSchema( ) ) {
        throw new RuntimeException("Unable to create the eucalyptus database tables")
      }

      prepareService( )

    } catch ( Exception ex ) {
      throw new RuntimeException( ex )
    }
  }

  private void initDBFile() throws Exception {
    File ibdata1 = null
    try {
      ibdata1 = SubDirectory.DB.getChildFile( EUCA_DB_DIR, "ibdata1" )
      ibdata1.getParentFile().mkdirs()
      ibdata1.createNewFile()
    } catch ( Exception e ) {
      LOG.debug("Unable to create the configuration files")
      ibdata1?.delete( )
      throw e
    }
  }

  private Iterable<Pair<String,Optional<String>>> databases( ) {
    Iterables.transform(
        PersistenceContexts.list( ),
        Pair.robuilder( PersistenceContexts.toDatabaseName( ), PersistenceContexts.toSchemaName( ) ) )
  }

  private boolean createSchema( ) throws Exception {
    final Set<String> createdDatabases = Sets.newHashSet( )
    final Set<Pair<String,String>> createdSchemas = Sets.newHashSet( )
    for ( Pair<String,Optional<String>> databasePair : databases( ) ) {
      final String databaseName = databasePair.left
      final String schemaName = databasePair.right.orNull( )
      if ( createdDatabases.add( databaseName ) ) {
        try {
          String createDatabase = "CREATE DATABASE " + databaseName
          dbExecute( PG_DEFAULT_DBNAME, createDatabase )

          String alterUser = "ALTER ROLE " + getUserName( ) + " WITH LOGIN PASSWORD \'" + getPassword( ) + "\'"
          dbExecute( databaseName, alterUser )
        } catch (Exception e) {
          // Permission denied is OK if the database was created already
          if (!e.message.contains("already exists") && !e.message.contains("permission denied to create database")) {
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
        ProxoolFacade.shutdown()
      }
    } )

    true
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
    getConnectionInternal( new InetSocketAddress( InetAddress.getByName( PG_HOST ), PG_PORT ), database, schema )
  }

  private Sql getConnectionInternal( InetSocketAddress address, String database, String schema ) throws Exception {
    getConnectionInternal( address, database, schema, userName, password )
  }

  private Sql getConnectionInternal( InetSocketAddress address, String database, String schema, String connUserName, String connPassword ) throws Exception {
    String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, address, database ) )
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
    listDatabases( new InetSocketAddress( InetAddress.getByName( PG_HOST ), PG_PORT ) )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  List<String> listDatabases( InetSocketAddress address ) {
    List<String> lines = []
    Sql sql = null
    try {
      sql = getConnectionInternal( address, "postgres", null )
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
    listSchemas( new InetSocketAddress( InetAddress.getByName( PG_HOST ), PG_PORT ), database )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  List<String> listSchemas( InetSocketAddress address, String database ) {
    List<String> lines = []
    Sql sql = null
    try {
      sql = getConnectionInternal( address, database, null )
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
      dbExecute("postgres", "CREATE DATABASE \"${name}\"" )
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
    LOG.error( "Copying database ${from} to ${to} not supported" )
    throw new Exception("Unable to copy database")
  }

  @Override
  void copyDatabaseSchema( String sourceDatabase,
                           String sourceSchema,
                           String destinationDatabase,
                           String destinationSchema ) {
    LOG.error( "Copying database scheam ${sourceDatabase}/${sourceSchema} to ${destinationDatabase}/${destinationSchema} not supported" )
    throw new Exception("Unable to copy database schema")
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
    true
  }

  boolean isLocal( ) {
    false
  }

  void hup( ) {
    LOG.info( 'Restart not supported for external database' )
  }

  @Override
  void destroy( ) throws IOException {
    LOG.info( 'Destroy not supported for external database' )
  }

  @Override
  String getPassword( ) {
    EUCA_DB_PASS
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
  String getServicePath( String... pathParts ) {
    return pathParts != null && pathParts.length > 0 ? Joiner.on("/").join(Arrays.asList(pathParts)) : "eucalyptus"
  }

  @Override
  Map<String, String> getJdbcUrlQueryParameters() {
    PG_USE_SSL ? [
        ssl:'true',
        sslmode:'require'
    ] : emptyMap()
  }

  @Override
  String getJdbcScheme( ) {
    'postgresql'
  }
}
