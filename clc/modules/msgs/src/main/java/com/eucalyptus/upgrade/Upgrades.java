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
 ************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.upgrade;

import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DatabaseBootstrapper;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Database;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class Upgrades {
  private static Logger LOG = Logger.getLogger( Upgrades.class );
  
  @Target( { ElementType.TYPE, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface PreUpgrade {
    /**
     * The {@link ComponentId} for which this upgrade should be executed.
     * 
     * @return
     */
    Class<? extends ComponentId> value( );
    
    /**
     * The {@link Upgrades.Version} since which this upgrade should be executed.
     * 
     * @return
     */
    Version since( );
  }
  
  @Target( { ElementType.TYPE, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface PostUpgrade {
    /**
     * The {@link ComponentId} for which this upgrade should be executed.
     * 
     * @return
     */
    Class<? extends ComponentId> value( );
    
    /**
     * The {@link Upgrades.Version} since which this upgrade should be executed.
     * 
     * @return
     */
    Version since( );
  }
  
  @Target( { ElementType.TYPE, ElementType.METHOD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface EntityUpgrade {
    
    /**
     * The list of entity classes which are addressed by this upgrade implementation.
     * 
     * @return
     */
    Class[] entities( );
    
    /**
     * The {@link ComponentId} for which this upgrade should be executed.
     * 
     * @return
     */
    Class<? extends ComponentId> value( );
    
    /**
     * The {@link Upgrades.Version} since which this upgrade should be executed.
     * 
     * @return
     */
    Version since( );
  }
  
  enum Arguments {
    CURRENT_VERSION( "euca.version" ),
    OLD_VERSION( "euca.upgrade.old.version" ){

      @Override
      public String getValue( ) {
        return System.getProperty( this.propName, BootstrapArgs.isUpgradeSystem( ) ? null : CURRENT_VERSION.getValue( ) );
      }
      
    },
    NEW_VERSION( "euca.version" );
    String propName;
    
    private Arguments( String propName ) {
      this.propName = propName;
    }
    
    public String getValue( ) {
      return System.getProperty( this.propName );
    }
  }
  
  public enum Version {
    v3_1_0,
    v3_1_1,
    v3_1_2,
    v3_2_0,
    v3_2_1,
    v3_2_2,
    v3_3_0,
    v3_3_1,
    v3_3_2,
    v3_4_0,
    v3_4_1,
    v3_4_2;
    
    public String getVersion( ) {
      return this.name( ).substring( 1 ).replace( "_", "." );
    }
    
    public static Version getOldVersion( ) {
      return Version.valueOf( "v" + Arguments.OLD_VERSION.getValue( ).replace( ".", "_" ) );
    }
    
    public static Version getNewVersion( ) {
      return Version.valueOf( "v" + Arguments.NEW_VERSION.getValue( ).replace( ".", "_" ) );
    }
    
    public static Version getCurrentVersion( ) {
      return Version.valueOf( "v" + Arguments.CURRENT_VERSION.getValue( ).replace( ".", "_" ) );
    }
    
    /**
     * Filter {@link Version#values()} to include only those {@link Version}s which are in the
     * current upgrade path (if any).
     * 
     * @return Iterable<Version> which are in the upgrade path.
     */
    public static Iterable<Version> upgradePath( ) {
      return Iterables.filter( Arrays.asList( Version.values( ) ), new Predicate<Version>( ) {
        
        @Override
        public boolean apply( @Nullable Version input ) {
          return getOldVersion( ).ordinal( ) < input.ordinal( ) && getNewVersion( ).ordinal( ) >= input.ordinal( );
        }
      } );
    }
  }
  
  public static class UpgradeDiscovery extends ServiceJarDiscovery {
    
    @Override
    public Double getPriority( ) {
      return 0.92d;
    }
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( EntityUpgrade.class ) ) {
        if ( !Predicate.class.isAssignableFrom( candidate ) ) {
          throw new IllegalArgumentException( "@EntityUpgrade can only be used on a type which implements Predicate:  " + candidate );
        }
        EntityUpgrade upgrade = Ats.from( candidate ).get( EntityUpgrade.class );
        ComponentUpgradeInfo.put( upgrade.value( ), candidate );
      } else if ( Ats.from( candidate ).has( PreUpgrade.class ) ) {
        if ( !Callable.class.isAssignableFrom( candidate ) ) {
          throw new IllegalArgumentException( "@PreUpgrade can only be used on a type which implements Callable:  " + candidate );
        }
        PreUpgrade upgrade = Ats.from( candidate ).get( PreUpgrade.class );
        ComponentUpgradeInfo.put( upgrade.value( ), candidate );
      } else if ( Ats.from( candidate ).has( PostUpgrade.class ) ) {
        if ( !Callable.class.isAssignableFrom( candidate ) ) {
          throw new IllegalArgumentException( "@PostUpgrade can only be used on a type which implements Callable:  " + candidate );
        }
        PostUpgrade upgrade = Ats.from( candidate ).get( PostUpgrade.class );
        ComponentUpgradeInfo.put( upgrade.value( ), candidate );
      } else {
        return false;
      }
      return true;
    }
  }
  
  private static final Map<Version, Map<Class<? extends ComponentId>, ComponentUpgradeInfo>> versionedComponentUpgrades = Maps.newHashMap( );
  private static class ComponentUpgradeInfo {
    private Multimap<Class, Predicate>                                                         entityUpgrades             = ArrayListMultimap.create( );
    private List<Callable<Boolean>>                                                            preUpgrades                = Lists.newArrayList( );
    private List<Callable<Boolean>>                                                            postUpgrades               = Lists.newArrayList( );
    private Class<? extends ComponentId>                                                       component;
    
    private ComponentUpgradeInfo( Class<? extends ComponentId> component ) {
      this.component = component;
    }
    
    List<Callable<Boolean>> getPreUpgrades( ) {
      return this.preUpgrades;
    }
    
    Multimap<Class, Predicate> getEntityUpgrades( ) {
      return this.entityUpgrades;
    }
    
    List<Callable<Boolean>> getPostUpgrades( ) {
      return this.postUpgrades;
    }
    
    static ComponentUpgradeInfo get( Version version, Class<? extends ComponentId> component ) {
      Map<Class<? extends ComponentId>, ComponentUpgradeInfo> compMap = versionedComponentUpgrades.get( version );
      if ( compMap == null ) {
        compMap = Maps.newHashMap( );
        versionedComponentUpgrades.put( version, compMap );
      }
      if ( !compMap.containsKey( component ) ) {
        compMap.put( component, new ComponentUpgradeInfo( component ) );
      }
      return compMap.get( component );
    }
    
    static void put( Class<? extends ComponentId> component, Class upgradeClass ) {
      Ats ats = Ats.from( upgradeClass );
      if ( ats.has( EntityUpgrade.class ) ) {
        for ( Class c : ats.get( EntityUpgrade.class ).entities( ) ) {
          get( ats.get( EntityUpgrade.class ).since( ), component ).entityUpgrades.put( c, ( Predicate ) Classes.newInstance( upgradeClass ) );
          LOG.info( "Registered @EntityUpgrade: " + component.getSimpleName( ) + ":" + c.getSimpleName( ) + " => " + upgradeClass );
        }
      } else if ( ats.has( PreUpgrade.class ) ) {
        get( ats.get( PreUpgrade.class ).since( ), component ).preUpgrades.add( ( Callable<Boolean> ) Classes.newInstance( upgradeClass ) );
        LOG.info( "Registered @PreUpgrade: " + component.getSimpleName( ) + " => " + upgradeClass );
      } else if ( ats.has( PostUpgrade.class ) ) {
        get( ats.get( PostUpgrade.class ).since( ), component ).postUpgrades.add( ( Callable<Boolean> ) Classes.newInstance( upgradeClass ) );
        LOG.info( "Registered @PostUpgrade: " + component.getSimpleName( ) + " => " + upgradeClass );
      }
    }
    
  }
  
  public enum UpgradeEventLog {
    INSTANCE;
    private final String tableName = "database_upgrade_log";
    
    private final String schema    = "CREATE TABLE " + this.tableName + " (\n" +
                                     "    id character varying(255) NOT NULL,\n" +
                                     "    timestamp timestamp without time zone,\n" +
                                     "    upgrade_from_version character varying(255),\n" +
                                     "    upgrade_to_version character varying(255),\n" +
                                     "    upgrade_state character varying(255)\n" +
                                     ");\n" +
                                     "ALTER TABLE public." + this.tableName + " OWNER TO eucalyptus;";
    
    public void logEvent( Version fromVersion, Version toVersion, UpgradeState state ) {
      Sql sql = null;
      try {
        sql = Databases.Events.getConnection( );
        LOG.debug( "Recording upgrade event: " + fromVersion.name( ) + "=>" + toVersion.name( ) + " state=" + state.name( ) );
        //GRZE: can i make this uglier?
        sql.execute( "INSERT INTO " + this.tableName + " VALUES ('" + UUID.randomUUID( ) + "','" + new Timestamp( System.currentTimeMillis( ) ) + "','"
                     + fromVersion.name( ) + "','" + toVersion.name( ) + "','" + state.name( ) + "')" );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
      
    }
    
    public static UpgradeState getLastState( ) {
      Sql sql = null;
      try {
        sql = Databases.Events.getConnection( );
        //GRZE: again, can i make this uglier?
        List<GroovyRowResult> res = sql.rows( "select upgrade_state from database_upgrade_log order by timestamp desc limit 1;" );
        if ( !res.isEmpty( ) ) {
          return UpgradeState.valueOf( ( String ) res.listIterator( ).next( ).get( "upgrade_state" ) );
        } else {
          return UpgradeState.COMPLETED;
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return UpgradeState.ERROR;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
    
    public static Version getLastUpgradedVersion( ) {
      Sql sql = null;
      try {
        sql = Databases.Events.getConnection( );
        //GRZE: again, can i make this uglier?
        List<GroovyRowResult> res = sql.rows( "select upgrade_to_version from database_upgrade_log where upgrade_state='COMPLETED' order by timestamp desc limit 1;" );
        if ( !res.isEmpty( ) ) {
          return Version.valueOf( ( String ) res.listIterator( ).next( ).get( "upgrade_to_version" ) );
        } else {
          return Version.v3_1_2;
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return Version.v3_1_2;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
    
    private static boolean create( ) {
      if ( !exists( ) ) {
        Sql sql = null;
        try {
          sql = Databases.Events.getConnection( );
          sql.execute( UpgradeEventLog.INSTANCE.schema );
          return true;
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        } finally {
          if ( sql != null ) {
            sql.close( );
          }
        }
      } else {
        return false;
      }
    }

    public static boolean exists( ) {
      return Databases.getBootstrapper( ).listTables( Databases.Events.INSTANCE.getName( ) ).contains( UpgradeEventLog.INSTANCE.tableName );
    }
  }
  
  @RunDuring( Bootstrap.Stage.UpgradeDatabase )
  @Provides( Empyrean.class )
  @DependsLocal( Eucalyptus.class )
  public static class UpgradeBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      try {
        do {
          if ( !UpgradeState.nextState( ).callAndLog( ) ) {
            break;
          }
        } while ( !UpgradeState.isFinished( ) );
        Upgrades.runSchemaUpdate( DatabaseFilters.EUCALYPTUS );
        if ( BootstrapArgs.isUpgradeSystem( ) ) {
          System.exit( 0 );
          return false;
        }
      } catch ( Throwable ex ) {
        LOG.error( ex, ex );
//TODO:GRZE: restore        UpgradeState.currentState.rollback( );
        if ( BootstrapArgs.isUpgradeSystem( ) ) {
          System.exit( 1 );
        }
        return false;
      }
      return true;
    }
  }
  
  enum DatabaseFilters implements Predicate<String> {
    EUCALYPTUS {
      
      @Override
      public String getPrefix( ) {
        return "eucalyptus_";
      }
      
    },
    OLDVERSION {
      
      @Override
      public String getPrefix( ) {
        return Version.getOldVersion( ) + "_" + EUCALYPTUS.getPrefix( );
      }
    },
    NEWVERSION {
      
      @Override
      public String getPrefix( ) {
        return Version.getNewVersion( ) + "_" + EUCALYPTUS.getPrefix( );
      }
    };
    
    @Override
    public boolean apply( String arg0 ) {
      return arg0.startsWith( this.getPrefix( ) );
    }
    
    public abstract String getPrefix( );
    
    public String getVersionedName( String origName ) {
      if ( origName.startsWith( EUCALYPTUS.getPrefix( ) ) ) {
        return origName.replace( EUCALYPTUS.getPrefix( ), this.getPrefix( ) );
      } else if ( origName.startsWith( NEWVERSION.getPrefix( ) ) ) {
        return origName.replace( NEWVERSION.getPrefix( ), this.getPrefix( ) );
      } else if ( origName.startsWith( OLDVERSION.getPrefix( ) ) ) {
        return origName.replace( OLDVERSION.getPrefix( ), this.getPrefix( ) );
      } else {
        throw new RuntimeException( "Failed to determine correct version name for: " + origName );
      }
    }
    
    public Sql getConnection( String databaseName ) throws Exception {
      return Databases.getBootstrapper( ).getConnection( this.getVersionedName( databaseName ) ); 
    }
    
  }
  
  /**
   * <ol>
   * <li>START
   * <li>CHECKING_VERSIONS
   * <li>BACKINGUP_DATABASE
   * <li>COPYING_DATABASES
   * <li>SCHEMA_UPDATE
   * <li>SETUP_JPA
   * <li>PRE_UPGRADE
   * <li>ENTITY_UPGRADE
   * <li>POST_UPGRADE
   * <li>SHUTDOWN_JPA
   * <ul>
   * <li>From here on down rollback becomes trickier.
   * </ul>
   * <li>DELETE_ORIG_DATABASE
   * <li>COPY_NEW_DATABASE
   * <li>DELETE_NEW_DATABASE
   * <li>DELETE_OLD_DATABASE
   * <li>COMPLETED
   * </ol>
   */
  enum UpgradeState implements Callable<Boolean> {
    START {
      @Override
      public boolean callAndLog( ) throws Exception {
        return this.call( );
      }
    },
    PARSE_ARGS {
      
      @Override
      public boolean callAndLog( ) throws Exception {
        try {//GRZE: check to make sure the given version arguments make sense (parse as Version.valueOf())
          Version.getCurrentVersion( );
          Version.getOldVersion( );
          Version.getNewVersion( );
        } catch ( IllegalArgumentException ex ) {
          LOG.fatal( ex );
          throw ex;
        }
        return true;
      }
      
    },
    CHECK_ARGS {
      
      @Override
      public boolean callAndLog( ) throws Exception {
        return this.call( );
      }
      
      @Override
      public Boolean call( ) throws Exception {
        if ( BootstrapArgs.isCloudController( ) && ( BootstrapArgs.isUpgradeSystem( ) || !UpgradeEventLog.exists( ) ) ) {
          return true;
        } else {
          return false;
        }
      }
      
    },
    /**
     * Determines whether or not to execute the database upgrade code.
     * 
     * The upgrade code should be run if:
     * <ol>
     * <li>The <tt>eucalyptus_version_info</tt> indicates the most recent <tt>upgrade version</tt> was for a version prior to the <tt>current version</tt>.
     * <li>The <tt>eucalyptus_version_info</tt> does not exist.
     * <li>The <tt>--upgrade</tt> flag is set using the command line parameters.
     * </ol>
     * 
     * The steps to determine whether the <tt>eucalyptus_version_info</tt> indicates an upgrade are:
     * <ol>
     * <li>Attempt to connect to <tt>eucalyptus_version_info</tt>, if it fails, create the <tt>eucalyptus_version_info</tt>
     * <li>Once connected to the <tt>eucalyptus_version_info</tt>, then we find either:
     * <ul>
     * <li>no rows present ==> do upgrade
     * <li>most recent <tt>upgrade version</tt> is less than <tt>current version</tt> ==> do upgrade
     * <li>most recent <tt>upgrade version</tt> is <tt>preparing</tt> or <tt>in-progress</tt> ==> {@link Upgrades#rollbackUpgrade()} to
     * <tt>previous version</tt> and do upgrade
     * <li>most recent <tt>upgrade version</tt> is <tt>rolling-back</tt> ==> copy <tt>old version</tt> db to <tt>orig</tt> db and do upgrade
     * <li>most recent <tt>upgrade version</tt> matches <tt>current version</tt> ==> no upgrade
     * </ul>
     * </ol>
     * 
     * @return true if upgrade should be executed
     */
    CHECK_UPGRADE_LOG {
      
      @Override
      public boolean callAndLog( ) throws Exception {
        return this.call( );
      }
      
      @Override
      public Boolean call( ) throws Exception {
        if ( UpgradeEventLog.create( ) ) {
          return true;
        } else {
          UpgradeState previousState = UpgradeEventLog.getLastState( );
          boolean continueUpgrade = false;
          switch ( previousState ) {
            case START:
            case PARSE_ARGS:
            case CHECK_ARGS:
            case CHECK_UPGRADE_LOG:
            case PRE_SCHEMA_UPDATE:
            case CHECK_VERSIONS:
              /**
               * Here no data was changed, we can proceed.
               */
              continueUpgrade = true;
              break;
            case BEGIN_UPGRADE:
            case PRE_BACKINGUP_DATABASE:
            case PRE_COPYING_DATABASES:
            case PRE_SETUP_JPA:
            case RUN_PRE_UPGRADE:
            case RUN_ENTITY_UPGRADE:
            case RUN_POST_UPGRADE:
              /**
               * In these cases we had a previous upgrade run which aborted prior to completion,
               * BUT
               * Only modified the NEW version database and left the ORIG database untouched.
               * We delete OLD db.
               * We delete NEW db.
               * We keep ORIG db.
               */
              for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.OLDVERSION ) ) {
                Databases.getBootstrapper( ).deleteDatabase( databaseName );
              }
              for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.NEWVERSION ) ) {
                Databases.getBootstrapper( ).deleteDatabase( databaseName );
              }
              continueUpgrade = true;
              break;
            case POST_SHUTDOWN_JPA:
            case POST_DELETE_ORIG_DB:
              /**
               * Here we have modified the ORIG and NEW dbs in some unknown way
               * BUT
               * We still have the backup of OLD db.
               * We delete ORIG db.
               * We delete NEW db.
               * We rename OLD to ORIG.
               */
              for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.NEWVERSION ) ) {
                Databases.getBootstrapper( ).deleteDatabase( databaseName );
              }
              for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.EUCALYPTUS ) ) {
                Databases.getBootstrapper( ).deleteDatabase( databaseName );
              }
              for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.OLDVERSION ) ) {
                Databases.getBootstrapper( ).renameDatabase( databaseName, DatabaseFilters.EUCALYPTUS.getVersionedName( databaseName ) );
              }
              continueUpgrade = true;
              break;
            case POST_RENAME_NEW_TO_ORIG_DB:
            case POST_DELETE_OLD_DB:
              /**
               * Here we have successfully upgraded but haven't finished cleaning up, so upgrade isn't really yet COMPLETED.
               * BUT
               * We need to record in the upgrade log the final stages.
               * We need to delete OLD db.
               */
              POST_DELETE_OLD_DB.callAndLog( );
              COMPLETED.callAndLog( );
              continueUpgrade = false;
              break;
            case COMPLETED:
              continueUpgrade = true;
              break;
            case ERROR:
              /**
               * We don't know what happened. Something went wrong and we need to bail out.
               */
              LOG.fatal( "Last upgrade stage executed was ERROR!  We need to bail out and have someone look at what is going on here." );
              System.exit( 1 );
              continueUpgrade = false;
              break;
          }
          return continueUpgrade;
        }
      }
      
    },
    CHECK_VERSIONS {
      
      @Override
      public Boolean call( ) throws Exception {
        if ( Version.getCurrentVersion( ).equals( UpgradeEventLog.getLastUpgradedVersion( ) ) ) {
          return Boolean.TRUE.parseBoolean( System.getProperty( "euca.upgrade.force" ) );
        } else if ( Version.getCurrentVersion( ).equals( Version.getOldVersion( ) ) && !BootstrapArgs.isUpgradeSystem( ) ) {
          return Boolean.TRUE.parseBoolean( System.getProperty( "euca.upgrade.force" ) );
        } else {
          return true;
        }
      }
      
      @Override
      public boolean callAndLog( ) throws Exception {
        return this.call( );
      }
      
    },
    BEGIN_UPGRADE,
    /**
     * Creates a {@link System.currentTimeMillis()} timestampled backup of each database in {@link SubDirectory#BACKUPS#getChildPath(String...)} for each
     * database prefixed with <tt>eucalyptus_</tt>
     */
    PRE_BACKINGUP_DATABASE {
      
      @Override
      public Boolean call( ) {
//        String backupIdentifier = "" + System.currentTimeMillis( );
//        LOG.info( "Creating backup of databases for old version" );
//        for ( String DATABASE_EVENTS : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.EUCALYPTUS ) ) {
//          LOG.info( "Creating backup of databases for old version: " + DATABASE_EVENTS );
//          Databases.getBootstrapper( ).backupDatabase( DATABASE_EVENTS, backupIdentifier );
//        }
        return true;
      }
    },
    PRE_COPYING_DATABASES {
      
      @Override
      public Boolean call( ) {
        LOG.info( "Creating upgrade databases for old version" );
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.OLDVERSION ) ) {
          LOG.info( "Deleting stale upgrade databases for old version: " + DatabaseFilters.OLDVERSION.getVersionedName( databaseName ) );
          Databases.getBootstrapper( ).deleteDatabase( databaseName );
        }
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.EUCALYPTUS ) ) {
          LOG.info( "Creating upgrade databases for old version: " + DatabaseFilters.OLDVERSION.getVersionedName( databaseName ) );
          Databases.getBootstrapper( ).copyDatabase( databaseName, DatabaseFilters.OLDVERSION.getVersionedName( databaseName ) );
        }
        LOG.info( "Creating upgrade databases for new version" );
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.NEWVERSION ) ) {
          LOG.info( "Deleting stale upgrade databases for new version: " + DatabaseFilters.NEWVERSION.getVersionedName( databaseName ) );
          Databases.getBootstrapper( ).deleteDatabase( databaseName );
        }
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.EUCALYPTUS ) ) {
          LOG.info( "Creating upgrade databases for new version: " + DatabaseFilters.NEWVERSION.getVersionedName( databaseName ) );
          Databases.getBootstrapper( ).copyDatabase( databaseName, DatabaseFilters.NEWVERSION.getVersionedName( databaseName ) );
        }
        return true;
      }
      
    },
    PRE_SCHEMA_UPDATE {
      /**
       * Execute schema update for <tt>new version</tt> database
       */
      @Override
      public Boolean call( ) {
        Upgrades.runSchemaUpdate( DatabaseFilters.NEWVERSION );
        return true;
      }
      
    },
    /**
     * Setup entity managers for <tt>new version</tt> database
     */
    PRE_SETUP_JPA {
      @Override
      public Boolean call( ) {
        try {
          final Map<String, String> props = UpgradeState.getDatabaseProperties( );
          for ( final String ctx : PersistenceContexts.list( ) ) {
            final Properties p = new Properties( );
            p.putAll( props );
            final String ctxUrl = String.format( "jdbc:%s",
                                                 ServiceUris.remote( Components.lookup( Database.class ), DatabaseFilters.NEWVERSION.getVersionedName( ctx ) ) );
            p.put( "hibernate.connection.url", ctxUrl );
            final Ejb3Configuration config = new Ejb3Configuration( );
            config.setProperties( p );
            for ( final Class c : PersistenceContexts.listEntities( ctx ) ) {
              config.addAnnotatedClass( c );
            }
            PersistenceContexts.registerPersistenceContext( ctx, config );
          }
        } catch ( final Exception e ) {
          LOG.fatal( e, e );
          LOG.fatal( "Failed to initialize the persistence layer." );
          throw Exceptions.toUndeclared( e );
        }
        return true;
      }
      
    },
    /**
     * Execute the @{@link PreUpgrade} implementations.
     */
    RUN_PRE_UPGRADE {
      @Override
      public Boolean call( ) {
        for ( ComponentId c : ComponentIds.list( ) ) {
          for ( Version v : Version.upgradePath( ) ) {
            ComponentUpgradeInfo upgradeInfo = ComponentUpgradeInfo.get( v, c.getClass( ) );
            for ( Callable<Boolean> p : upgradeInfo.getPreUpgrades( ) ) {
              try {
                LOG.info( "Executing @PreUpgrade: " + p.getClass( ) );
                p.call( );
              } catch ( Exception ex ) {
                throw Exceptions.toUndeclared( "Upgrade failed during @PreUpgrade while executing: " + p.getClass( ) + " because of: " + ex.getMessage( ), ex );
              }
            }
          }
        }
        return true;
      }
      
    },
    /**
     * Execute the @{@link EntityUpgrade} implementations.
     */
    RUN_ENTITY_UPGRADE {
      @Override
      public Boolean call( ) {
        for ( ComponentId c : ComponentIds.list( ) ) {
          for ( Version v : Version.upgradePath( ) ) {
            ComponentUpgradeInfo upgradeInfo = ComponentUpgradeInfo.get( v, c.getClass( ) );
            for ( Entry<Class, Predicate> p : upgradeInfo.getEntityUpgrades( ).entries( ) ) {
              try {
                LOG.info( "Executing @EntityUpgrade: " + p.getValue( ).getClass( ) );
                p.getValue( ).apply( p.getKey( ) );
              } catch ( Exception ex ) {
                throw Exceptions.toUndeclared( "Upgrade failed during @EntityUpgrade while executing: "
                                               + p.getValue( ).getClass( ) + " for " + p.getKey( )
                                               + " because of: " + ex.getMessage( ), ex );
              }
            }
          }
        }
        return true;
      }
      
    },
    /**
     * Execute the @{@link PostUpgrade} implementations.
     */
    RUN_POST_UPGRADE {
      @Override
      public Boolean call( ) {
        for ( ComponentId c : ComponentIds.list( ) ) {
          for ( Version v : Version.upgradePath( ) ) {
            ComponentUpgradeInfo upgradeInfo = ComponentUpgradeInfo.get( v, c.getClass( ) );
            for ( Callable<Boolean> p : upgradeInfo.getPostUpgrades( ) ) {
              try {
                LOG.info( "Executing @PostUpgrade: " + p.getClass( ) );
                p.call( );
              } catch ( Exception ex ) {
                throw Exceptions.toUndeclared( "Upgrade failed during @PostUpgrade while executing: " + p.getClass( ) + " because of: " + ex.getMessage( ), ex );
              }
            }
          }
        }
        return true;
      }
      
    },
    POST_SHUTDOWN_JPA {
      
      @Override
      public Boolean call( ) {
        PersistenceContexts.shutdown( );
        return true;
      }
      
    },
    /**
     * <ol>
     * <li>Delete <tt>orig</tt> db
     * <li>Copy <tt>new version</tt> db to <tt>orig</tt> db
     * <li>Delete <tt>new version</tt> db
     * <li>Delete <tt>old version</tt> db
     * </ol>
     */
    POST_DELETE_ORIG_DB {
      @Override
      public Boolean call( ) {
        LOG.info( "Deleting orig databases" );
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.EUCALYPTUS ) ) {
          LOG.info( "Deleting orig database: " + databaseName );
          Databases.getBootstrapper( ).deleteDatabase( databaseName );
        }
        return true;
      }
    },
    POST_RENAME_NEW_TO_ORIG_DB {
      @Override
      public Boolean call( ) {
        LOG.info( "Renaming upgraded databases" );
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.NEWVERSION ) ) {
          LOG.info( "Renaming upgraded database: " + databaseName + " => " + DatabaseFilters.EUCALYPTUS.getVersionedName( databaseName ) );
          Databases.getBootstrapper( ).renameDatabase( databaseName, DatabaseFilters.EUCALYPTUS.getVersionedName( databaseName ) );
        }
        return true;
      }
    },
    POST_DELETE_OLD_DB {
      @Override
      public Boolean call( ) {
        LOG.info( "Deleting upgrade databases for old version" );
        for ( String databaseName : Iterables.filter( Databases.getBootstrapper( ).listDatabases( ), DatabaseFilters.OLDVERSION ) ) {
          LOG.info( "Deleting upgrade database for old version: " + databaseName );
          Databases.getBootstrapper( ).deleteDatabase( databaseName );
        }
        return true;
      }
    },
    COMPLETED {
      
      @Override
      public UpgradeState next( ) {
        LOG.info( "Finished upgrade stage: " + this.name( ) );
        return this;
      }
      
    },
    ERROR {
      
      @Override
      public UpgradeState next( ) {
        return this;
      }
      
    };
    
    @Override
    public Boolean call( ) throws Exception {
      return true;
    }
    
    /**
     * @return
     * @throws Exception
     */
    public boolean callAndLog( ) throws Exception {
      if ( this.call( ) ) {
        UpgradeEventLog.INSTANCE.logEvent( Version.getOldVersion( ), Version.getNewVersion( ), this );
        return true;
      } else {
        return false;
      }
    }
    
    UpgradeState next( ) {
      UpgradeState next = UpgradeState.values( )[this.ordinal( ) + 1];
      LOG.info( "Finished upgrade stage: " + this.name( ) + "; starting " + next.name( ) );
      return next;
    }
    
    public static Map<String, String> getDatabaseProperties( ) {
      DatabaseBootstrapper db = Databases.getBootstrapper( );
      final Map<String, String> props = ImmutableMap.<String, String> builder( )
                                                    .put( "hibernate.show_sql", "false" )
                                                    .put( "hibernate.format_sql", "false" )
                                                    .put( "hibernate.connection.autocommit", "false" )
                                                    .put( "hibernate.hbm2ddl.auto", "update" )
                                                    .put( "hibernate.generate_statistics", "false" )
                                                    .put( "hibernate.connection.driver_class", db.getDriverName( ) )
                                                    .put( "hibernate.connection.username", db.getUserName( ) )
                                                    .put( "hibernate.connection.password", db.getPassword( ) )
                                                    .put( "hibernate.bytecode.use_reflection_optimizer", "true" )
                                                    .put( "hibernate.cglib.use_reflection_optimizer", "true" )
                                                    .put( "hibernate.dialect", db.getHibernateDialect( ) )
                                                    .put( "hibernate.cache.use_second_level_cache", "false" )
                                                    .put( "hibernate.cache.use_query_cache", "false" )
                                                    .build( );
      return props;
    }

    private static UpgradeState currentState = UpgradeState.START;
    
    public static boolean isFinished( ) {
      return currentState == COMPLETED;
    }
    
    public static UpgradeState nextState( ) {
      currentState = currentState.next( );
      return currentState;
    }
  }

  private static void runSchemaUpdate( DatabaseFilters dbName ) throws RuntimeException {
    try {
      final Map<String, String> props = UpgradeState.getDatabaseProperties( );          
      for ( final String ctx : PersistenceContexts.list( ) ) {
        final Properties p = new Properties( );
        p.putAll( props );
        final String ctxUrl = String.format( "jdbc:%s",
                                             ServiceUris.remote( Components.lookup( Database.class ), dbName.getVersionedName( ctx ) ) );
        p.put( "hibernate.connection.url", ctxUrl );
        final Ejb3Configuration config = new Ejb3Configuration( );
        config.setProperties( p );
        for ( final Class c : PersistenceContexts.listEntities( ctx ) ) {
          config.addAnnotatedClass( c );
        }
        new SchemaUpdate(config.getHibernateConfiguration()).execute(true, true);
      }
    } catch ( final Exception e ) {
      LOG.fatal( e, e );
      LOG.fatal( "Failed to initialize the persistence layer." );
      throw Exceptions.toUndeclared( e );
    }
  }
  
}
