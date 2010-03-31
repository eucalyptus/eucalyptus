package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.group.Groups;
import com.eucalyptus.auth.group.NoSuchGroupException;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.util.EucalyptusCloudException;

@Provides( resource = Resource.PersistenceContext )
@Depends( resources = { Resource.Database } )
public class DatabaseAuthBootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
  public boolean load( Resource current ) throws Exception {
    DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
    Users.setUserProvider( dbAuth );
    Groups.setGroupProvider( dbAuth );
    return true;
  }
  
  public boolean start( ) throws Exception {
//    for( UserEntity u : dbu.query( new UserEntity( ) ) ) {
//      if( u.getIsEnabled() != Boolean.FALSE ) {
//        u.setIsEnabled( Boolean.TRUE )
//      }
//    }
    this.ensureAllGroupExists( );
    this.ensureAdminExists( );
    this.ensureCountersExist( );
    this.ensureVmTypesExist( );
    return true;
  }

  private void ensureVmTypesExist( ) {
    EntityWrapper<VmType> db = new EntityWrapper<VmType>( );
    try {
      if( db.query( new VmType() ).size( ) == 0 ) { //TODO: make defaults configurable?
        db.add( new VmType( "m1.small", 1, 2, 128 ) );
        db.add( new VmType( "c1.medium", 1, 5, 256 ) );
        db.add( new VmType( "m1.large", 2, 10, 512 ) );
        db.add( new VmType( "m1.xlarge", 2, 20, 1024 ) );
        db.add( new VmType( "c1.xlarge", 4, 20, 2048 ) );
      }
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
  }

  private void ensureAllGroupExists( ) {
    try {
      Groups.lookupGroup( "all" );
    } catch ( NoSuchGroupException e ) {
      try {
        Groups.addGroup( "all" );
      } catch ( GroupExistsException e1 ) {
        LOG.error( e1, e1 );
        LOG.error( "Failed to add the 'all' group.  The system may not be able to store group information." );
      }
    }
  }

  private void ensureCountersExist() {
    EntityWrapper<Counters> db = Counters.getEntityWrapper( );
    try {
      db.getUnique( new Counters() );
    } catch ( EucalyptusCloudException e ) {
      try {
        db.add( new Counters() );
        db.commit( );
      } catch ( Exception e1 ) {
        LOG.error( e1, e1 );
        LOG.fatal( "Failed to add the system counters." );
        System.exit( -1 );
      }
    }
  }
  
  private void ensureAdminExists( ) {
    try {
      Users.lookupUser( "admin" );
    } catch ( NoSuchUserException e ) {
      try {
        Users.addUser( "admin", true, true );
      } catch ( UserExistsException e1 ) {
      } catch ( UnsupportedOperationException e1 ) {
      }
    }
  }
}
