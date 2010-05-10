package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.VmType;
import com.google.common.collect.Lists;

@Provides( Component.bootstrap )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
@DependsLocal( Component.eucalyptus )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
  
  public boolean load( Stage current ) throws Exception {
    DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
    Users.setUserProvider( dbAuth );
    Groups.setGroupProvider( dbAuth );
    UserInfoStore.setUserInfoProvider( dbAuth );
    return true;
  }
  
  public boolean start( ) throws Exception {
    this.checkUserEnabled( );
    this.ensureStandardGroupsExists( );
    this.ensureAdminExists( );
    this.ensureCountersExist( );
    this.ensureVmTypesExist( );
    return true;
  }
  
  private void checkUserEnabled( ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    for ( UserEntity u : db.query( new UserEntity( ) ) ) {
      if ( u.isEnabled( ) != Boolean.FALSE ) {
        u.setEnabled( Boolean.TRUE );
      }
    }
  }
  
  private void ensureVmTypesExist( ) {
    EntityWrapper<VmType> db = new EntityWrapper<VmType>( "eucalyptus_general" );
    try {
      if ( db.query( new VmType( ) ).size( ) == 0 ) { //TODO: make defaults configurable?
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
  
  private void ensureStandardGroupsExists( ) {
    try {
      Groups.ALL = Groups.lookupGroup( "all" );
    } catch ( NoSuchGroupException e ) {
      try {
        Groups.ALL = Groups.addGroup( "all" );
      } catch ( GroupExistsException e1 ) {
        LOG.error( e1, e1 );
        LOG.error( "Failed to add the 'all' group.  The system may not be able to store group information." );
      }
    }
    Groups.RESTRICTED_GROUPS.add( Groups.ALL );
    
    try {
      Groups.DEFAULT = Groups.lookupGroup( "default" );
    } catch ( NoSuchGroupException e ) {
      try {
        Groups.DEFAULT = Groups.addGroup( "default" );
      } catch ( GroupExistsException e1 ) {
        LOG.error( e1, e1 );
        LOG.error( "Failed to add the 'all' group.  The system may not be able to store group information." );
      }
    }
    Groups.RESTRICTED_GROUPS.add( Groups.DEFAULT );
  }
  
  private void ensureCountersExist( ) {
    Counters.getNextId( );
  }
  
  private void ensureAdminExists( ) {
    try {
      Users.lookupUser( "admin" );
    } catch ( NoSuchUserException e ) {
      try {
        Users.addUser( "admin", true, true );
      } catch ( UserExistsException e1 ) {} catch ( UnsupportedOperationException e1 ) {}
    }
  }
}
