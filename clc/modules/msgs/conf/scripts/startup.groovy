import org.hibernate.HibernateException;

import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.UserEntity;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.Counters;
import com.eucalyptus.auth.UserGroupEntity;
import com.eucalyptus.auth.UserInfo;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.util.UserManagement;
import com.eucalyptus.bootstrap.DeferredInitializer;


EntityWrapper<UserEntity> dbu = Credentials.getEntityWrapper( );
try {
  for( UserEntity u : dbu.query( new UserEntity( ) ) ) {
    if( u.getIsEnabled() != Boolean.FALSE ) {
      u.setIsEnabled( Boolean.TRUE )
    }
  }
  dbu.commit();
} catch( Throwable e ) {
  dbu.rollback();
}
EntityWrapper<VmType> db2 = new EntityWrapper<VmType>( );
try {
  if( db2.query( new VmType() ).size( ) == 0 ) { 
    db2.add( new VmType( "m1.small", 1, 2, 128 ) );
    db2.add( new VmType( "c1.medium", 1, 5, 256 ) );
    db2.add( new VmType( "m1.large", 2, 10, 512 ) );
    db2.add( new VmType( "m1.xlarge", 2, 20, 1024 ) );
    db2.add( new VmType( "c1.xlarge", 4, 20, 2048 ) );
  }
  db2.commit( );
} catch ( Exception e ) {
  db2.rollback( );
  return false;
}
EntityWrapper<UserGroupEntity> db3 = new EntityWrapper<UserGroupEntity>( );
try {
  db3.getUnique( new UserGroupEntity( "all" ) );
  db3.rollback();
} catch ( EucalyptusCloudException e ) {
  db3.add( new UserGroupEntity( "all" ) );
  db3.commit( );
}
try {
  UserGroupEntity.named( "all" );
} catch ( EucalyptusCloudException e1 ) {
}
EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
try {
  db.getUnique( new UserInfo("admin") );
  db.commit( );
  return true;
} catch ( Exception e ) {
  try {//FIXME: fix this nicely
    CredentialProvider.addUser("admin",true);
  } catch ( UserExistsException e1 ) {
    e1.printStackTrace();
  }
  try {
    db.getSession( ).persist( new Counters( ) );
    UserInfo u = UserManagement.generateAdmin( );
    db.add( u );
    db.commit( );
  } catch ( HibernateException e1 ) {
    db.rollback( );
    return false;
  }
  return true;
}


