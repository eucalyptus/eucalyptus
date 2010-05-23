import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.UserEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.UserInfoStore;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Tx;
import com.eucalyptus.auth.util.AuthBootstrapHelper;

import groovy.sql.Sql;

class UpgradeAuth implements UpgradeScript {
  public Boolean accepts( String from, String to ) {
    return true;
  }
  
  public void upgrade( File oldEucaHome, File newEucaHome ) {
    def userName;
    def userIdToUserName = [:];
    def certIdToUserName = [:];
    def ccCertIdToCluster = [:];
    def ncCertIdToCluster = [:];
    
    AuthBootstrapHelper.ensureStandardGroupsExists( );

    println "----- eucalyptus_auth.AUTH_CLUSTERS -----";
    StandalonePersistence.getConnection("eucalyptus_auth").rows('SELECT * FROM AUTH_CLUSTERS').each{
      println "ROW: ${it}";
      EntityWrapper<ClusterCredentials> db = Authentication.getEntityWrapper( );
      try {
        db.add( new ClusterCredentials( it.AUTH_CLUSTER_NAME ) );
        db.commit( );
        ccCertIdToCluster[it.AUTH_CLUSTER_X509_CERTIFICATE] = it.AUTH_CLUSTER_NAME;
        ncCertIdToCluster[it.AUTH_CLUSTER_NODE_X509_CERTIFICATE] = it.AUTH_CLUSTER_NAME;
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        println "Failed to add credentials for cluster ${it.AUTH_CLUSTER_NAME}";
        e.printStackTrace( );
      }
    }       
    println "----- eucalyptus_auth.AUTH_USERS -----";
    StandalonePersistence.getConnection("eucalyptus_auth").rows('SELECT * FROM AUTH_USERS').each{
      println "ROW: ${it}";
      Users.addUser( it.AUTH_USER_NAME, it.AUTH_USER_IS_ADMIN, it.AUTH_USER_IS_ENABLED );
      Users.updateUser( it.AUTH_USER_NAME, new Tx<User>( ) {
        public void fire( User user ) throws Throwable {
          user.setQueryId( it.AUTH_USER_QUERY_ID );
          user.setSecretKey( it.AUTH_USER_SECRETKEY );
        }
      });
      userIdToUserName[it.ID] = it.AUTH_USER_NAME;
    }
    println "----- eucalyptus_auth.AUTH_USER_HAS_X509 -----"
    StandalonePersistence.getConnection("eucalyptus_auth").rows('SELECT * FROM AUTH_USER_HAS_X509').each{
      println "ROW: ${it}";
      userName = userIdToUserName[it.AUTH_USER_ID];
      if ( userName != null ) {
        certIdToUserName[it.AUTH_X509_ID] = userName;
      }
    }
    println "----- eucalyptus_auth.AUTH_X509 -----"
    StandalonePersistence.getConnection("eucalyptus_auth").rows('SELECT * FROM AUTH_X509').each{
      println "ROW: ${it}";
      def cert = new X509Cert( );
      cert.setAlias( it.AUTH_X509_ALIAS );
      cert.setPemCertificate( it.AUTH_X509_PEM_CERTIFICATE );
      if ( certIdToUserName[it.ID] != null ) {
        println "Update certificate for ${userName}";
        Users.updateUser( certIdToUserName[it.ID], new Tx<User>( ) {
          public void fire( User user ) throws Throwable {
            ( ( UserEntity ) user ).getCertificates( ).add( cert );
          }
        });
      } else if ( ncCertIdToCluster[it.ID] != null ) {
        println "Update node certificate for ${ncCertIdToCluster[it.ID]}";
        EntityWrapper<ClusterCredentials> db = Authentication.getEntityWrapper( );
        try {
          ClusterCredentials creds = db.getUnique( new ClusterCredentials( ncCertIdToCluster[it.ID] ) );
          creds.setNodeCertificate( cert );
          db.commit( );
        } catch ( EucalyptusCloudException e ) {
          db.rollback( );
          println "Failed to load node credential for cluster ${ncCertIdToCluster[it.ID]}";
          e.printStackTrace( );
        }
      } else if ( ccCertIdToCluster[it.ID] != null ) {
        println "Update cluster certificate for ${ccCertIdToCluster[it.ID]}";
        EntityWrapper<ClusterCredentials> db = Authentication.getEntityWrapper( );
        try {
          ClusterCredentials creds = db.getUnique( new ClusterCredentials( ccCertIdToCluster[it.ID] ) );
          creds.setClusterCertificate( cert );
          db.commit( );
        } catch ( EucalyptusCloudException e ) {
          db.rollback( );
          println "Failed to load cluster credential for cluster ${ccCertIdToCluster[it.ID]}";
          e.printStackTrace( );
        }
      }
    }
    println "----- eucalyptus_general.USERS -----"
    StandalonePersistence.getConnection("eucalyptus_general").rows('SELECT * FROM USERS').each{
      println "ROW: ${it}";
      Users.updateUser( it.USER_NAME , new Tx<User>( ) {
        public void fire( User user ) throws Throwable {
          user.setPassword( it.USER_B_CRYPTED_PASSWORD );
          user.setEnabled( it.USER_IS_ENABLED );
          ( ( UserEntity ) user ).setToken( it.USER_CERTIFICATE_CODE );
        }
      });
      UserInfoStore.updateUserInfo( it.USER_NAME, new Tx<UserInfo>( ) {
        public void fire( UserInfo userInfo ) throws Throwable {
          userInfo.setConfirmationCode( it. USER_CONFIRMATION_CODE );
          userInfo.setAffiliation( ifEmptyReplaceByBogus(it.USER_AFFILIATION ) );
          userInfo.setEmail( ifEmptyReplaceByBogus(it.USER_EMAIL ) );
          userInfo.setApproved( it.USER_IS_APPROVED );
          userInfo.setConfirmed( it.USER_IS_CONFIRMED );
          userInfo.setPasswordExpires( it.PASSWORD_EXPIRES );
          userInfo.setProjectDescription( ifEmptyReplaceByBogus(it.USER_PROJECT_DESCRIPTION ) );
          userInfo.setProjectPIName( ifEmptyReplaceByBogus(it.USER_PROJECT_PI_NAME ) );
          userInfo.setRealName( ifEmptyReplaceByBogus(it.USER_REAL_NAME ) );
          userInfo.setTelephoneNumber( ifEmptyReplaceByBogus(it.USER_TELEPHONE_NUMBER ) );
        }
      });
    }
  }
  
  private String ifEmptyReplaceByBogus( String value ) {
    if ( value == null || "".equals( value ) ) {
      return UserInfo.BOGUS_ENTRY;
    } else {
      return value;
    }
  }
}