package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.AuthBootstrapHelper;
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

@Provides( Component.bootstrap )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
  
  public static boolean ENABLE = true;
  
  public boolean load( Stage current ) throws Exception {
    if (ENABLE) {
      DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
      Users.setUserProvider( dbAuth );
      Groups.setGroupProvider( dbAuth );
      UserInfoStore.setUserInfoProvider( dbAuth );
    }
    return true;
  }
  
  public boolean start( ) throws Exception {
    if (ENABLE) {
      this.checkUserEnabled( );
      AuthBootstrapHelper.ensureStandardGroupsExists( );
      AuthBootstrapHelper.ensureAdminExists( );
    }
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

  private void ensureCountersExist( ) {
    Counters.getNextId( );
  }
}
