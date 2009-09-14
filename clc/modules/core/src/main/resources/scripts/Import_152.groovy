
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.SshKeyPair;
import com.eucalyptus.keys.KeyPairUtil;
import com.eucalyptus.network.NetworkGroupUtil;

import org.bouncycastle.util.encoders.UrlBase64;
import groovy.sql.Sql;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.ProductCode;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.state.Snapshot;
import edu.ucsb.eucalyptus.cloud.state.Volume;
import edu.ucsb.eucalyptus.cloud.ws.SnapshotManager;
import edu.ucsb.eucalyptus.cloud.ws.VolumeManager;

import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserGroupInfo;

baseDir = "/home/decker/epc.db"
targetDir = "/home/decker/epc.db"
targetDbPrefix= "test"

def getSql() {
  source = new org.hsqldb.jdbc.jdbcDataSource();
  source.database = "jdbc:hsqldb:file:${baseDir}/eucalyptus";
  source.user = 'sa';
  source.password = '';
  return new Sql(source);
}

def getSqlVolumes() {
  source = new org.hsqldb.jdbc.jdbcDataSource();
  source.database = "jdbc:hsqldb:file:${baseDir}/eucalyptus_volumes";
  source.user = 'sa';
  source.password = '';
  return new Sql(source);
}

db = getSql();
dbVolumes = getSqlVolumes( );

System.setProperty("euca.log.dir", "${System.getProperty('euca.home')}/var/log/eucalyptus/")
System.setProperty("euca.db.host", "jdbc:hsqldb:file:${targetDir}/${targetDbPrefix}")
System.setProperty("euca.log.level", 'INFO')

UserGroupInfo userGroupInfo = new UserGroupInfo( "all" );
EntityWrapper<UserGroupInfo> db3 = new EntityWrapper<UserGroupInfo>( );
try {
  userGroupInfo = db3.getUnique( userGroupInfo );
  userGroupInfo.getUsers().add( u );
  db3.commit();
} catch ( Throwable t ) {
  db3.rollback();
}

db.rows('SELECT * FROM SYSTEM_INFO').each{ 
  SystemConfiguration config = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration();
  EntityWrapper<SystemConfiguration> confDb = new EntityWrapper<SystemConfiguration>();
  try {
    config.setDefaultKernel(it.SYSTEM_INFO_DEFAULT_KERNEL);
    config.setDefaultRamdisk(it.SYSTEM_INFO_DEFAULT_RAMDISK);
    config.setSystemReservedPublicAddresses(it.SYSTEM_SYSTEM_RESERVED_PUBLIC_ADDRESSES);
    config.setMaxUserPublicAddresses(it.SYSTEM_MAX_USER_PUBLIC_ADDRESSES);
    config.setDoDynamicPublicAddresses(it.SYSTEM_DO_DYNAMIC_PUBLIC_ADDRESSES);
    config.setRegistrationId(it.SYSTEM_REGISTRATION_ID);
    confDb.merge( config );
    confDb.commit( );
  } catch (Throwable t) {
    confDb.commit();
  }
}


db.rows('SELECT * FROM VM_TYPES').each{   
  EntityWrapper<VmType> dbVm = new EntityWrapper<VmType>( );
  try {
    dbVm.add( new VmType( it.VM_TYPE_NAME, it.VM_TYPE_CPU, it.VM_TYPE_DISK, it.VM_TYPE_MEMORY ) );
    dbVm.commit();
  } catch (Throwable t) {
    dbVm.rollback();
  }
}

db.rows('SELECT * FROM USERS').each{   
  println "Adding user: ${it.USER_NAME}";
  UserInfo user = new UserInfo( it.USER_NAME, 
      it.USER_EMAIL, 
      it.USER_REAL_NAME, 
      it.USER_RESERVATION_ID, 
      it.USER_B_CRYPTED_PASSWORD, 
      it.USER_TELEPHONE_NUMBER, 
      it.USER_AFFILIATION, 
      it.USER_PROJECT_DESCRIPTION, 
      it.USER_PROJECT_PI_NAME,
      it.USER_CONFIRMATION_CODE,
      it.USER_CERTIFICATE_CODE,
      it.USER_IS_APPROVED, 
      it.USER_IS_CONFIRMED, 
      it.USER_IS_ENABLED, 
      it.USER_IS_ADMIN, 
      it.PASSWORD_EXPIRES );
  EntityWrapper<UserInfo> dbUser = new EntityWrapper<UserInfo>();
  try { 
    dbUser.add( user ); 
    userGroupInfo = dbUser.getUnique( userGroupInfo );
    userGroupInfo.getUsers().add( u );
    dbUser.commit();
  } catch( Throwable t ) { 
    dbUser.rollback();
  }

  CredentialProvider.addUser(it.USER_NAME,it.USER_IS_ADMIN,it.USER_QUERY_ID,it.USER_SECRETKEY);
  db.rows("SELECT cert.* FROM user_has_certificates has_certs LEFT OUTER JOIN cert_info cert on cert.cert_info_id=has_certs.cert_info_id WHERE has_certs.user_id=${ it.USER_ID }").each{  cert_info ->
    println "-> certificate: ${cert_info.CERT_INFO_ALIAS}";
    CredentialProvider.addCertificate( it.USER_NAME, cert_info.CERT_INFO_ALIAS, Hashes.getPemCert( UrlBase64.decode( cert_info.CERT_INFO_VALUE.getBytes( ) ) ) );
  }
  db.rows("SELECT kp.* FROM user_has_sshkeys has_keys LEFT OUTER JOIN ssh_keypair kp on kp.ssh_keypair_id=has_keys.ssh_keypair_id WHERE has_keys.user_id=${ it.USER_ID }").each{  keypair ->
    println "-> keypair: ${keypair.SSH_KEYPAIR_NAME}";
    EntityWrapper<SshKeyPair> dbKp = KeyPairUtil.getEntityWrapper( );
    try {
      dbKp.add( new SshKeyPair( it.USER_ID, keypair.SSH_KEYPAIR_NAME, keypair.SSH_KEYPAIR_PUBLIC_KEY, keypair.SSH_KEYPAIR_FINGER_PRINT ) );
      dbKp.commit( );
    } catch ( Throwable e1 ) {
      dbKp.rollback( );
    }
  }
  db.rows("SELECT net.* FROM user_has_network_groups has_net LEFT OUTER JOIN network_group net on net.network_group_id=has_net.network_group_id WHERE has_net.user_id=${ it.USER_ID }").each{  net ->
    println "-> network: ${net.NETWORK_GROUP_NAME}"
    NetworkGroupUtil.createUserNetworkRulesGroup( it.USER_NAME, net.NETWORK_GROUP_NAME, net.NETWORK_GROUP_DESCRIPTION );
  }
};

db.rows("SELECT image.* FROM images image").each{  image ->
  println "Adding images: ${image.IMAGE_NAME}";
  ImageInfo imgInfo = new ImageInfo( image.IMAGE_ARCH, image.IMAGE_NAME, image.IMAGE_PATH, image.IMAGE_OWNER_ID, image.IMAGE_AVAILABILITY, image.IMAGE_TYPE, image.IMAGE_IS_PUBLIC, image.IMAGE_KERNEL_ID, image.IMAGE_RAMDISK_ID );
  EntityWrapper<ImageInfo> dbImg = new EntityWrapper<ImageInfo>( );
  try {
    dbImg.add( imgInfo );
    db.rows("SELECT pc.* FROM image_has_product_codes has_pc LEFT OUTER JOIN image_product_code pc on pc.image_product_code_id=has_pc.image_product_code_id WHERE has_pc.image_id=${ image.IMAGE_ID }").each{  
      imgInfo.getProductCodes( ).add( new ProductCode( it.IMAGE_PRODUCT_CODE_VALUE ) );
    }
    db.rows("SELECT p.* FROM image_has_perms has_p LEFT OUTER JOIN users p on p.user_id=has_p.user_id WHERE has_p.image_id=${ image.IMAGE_ID }").each{  
      UserInfo u = dbImg.getUnique( it.USER_ID );
      imgInfo.getPermissions( ).add( u );
    }
    userGroupInfo = dbImg.getUnique( userGroupInfo );
    imgInfo.getUserGroups().add(userGroupInfo);
    dbImg.commit();
  } catch( Throwable t ) {
    dbImg.rollback();
  }
}

dbVolumes.rows("SELECT * FROM VOLUME").each{
  println "Adding volume: ${it.DISPLAYNAME}"
  EntityWrapper<Volume> dbVol = VolumeManager.getEntityWrapper();
  try {
    Volume v = new Volume(it.USERNAME, it.DISPLAYNAME, it.SIZE, it.CLUSTER );
    v.setCluster(it.CLUSTER);
    v.setParentSnapshot(it.PARENTSNAPSHOT);
    v.setLocalDevice(it.LOCALDEVICE);
    v.setRemoteDevice(it.REMOTEDEVICE);
    dbVol.add( v );
    dbVol.commit();
  } catch (Throwable t) {
    dbVol.rollback();
  }
}

dbVolumes.rows("SELECT * FROM SNAPSHOT").each{
  println "Adding snapshot: ${it.DISPLAYNAME}"
  
  EntityWrapper<Snapshot> dbSnap = SnapshotManager.getEntityWrapper( );
  try {
    Snapshot s = new Snapshot(it.USERNAME,it.DISPLAYNAME);
    s.setBirthday(it.BIRTHDAY);
    s.setState(it.STATE);
    s.setParentVolume(it.PARENTVOLUME);
    dbSnap.add(s);
    dbSnap.commit();
  } catch (Throwable t) {
    dbSnap.rollback();
  }
}

db.rows('SELECT * FROM CLUSTERS').each{ 
  println "CLUSTER: name=${it.CLUSTER_NAME} host=${it.CLUSTER_HOST} port=${it.CLUSTER_PORT}"
}

