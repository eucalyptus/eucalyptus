
import java.security.*;
import javax.crypto.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.SshKeyPair;
import com.eucalyptus.keys.KeyPairUtil;
import com.eucalyptus.entities.NetworkRule;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.entities.IpRange;
import com.eucalyptus.entities.NetworkPeer;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.bootstrap.Component;
import org.bouncycastle.util.encoders.UrlBase64;
import groovy.sql.Sql;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.ProductCode;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.state.Snapshot;
import edu.ucsb.eucalyptus.cloud.state.Volume;
import edu.ucsb.eucalyptus.cloud.state.State;
import edu.ucsb.eucalyptus.cloud.ws.SnapshotManager;
import edu.ucsb.eucalyptus.cloud.ws.VolumeManager;

import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserGroupInfo;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.GrantInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.MetaDataInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMMetaInfo;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import edu.ucsb.eucalyptus.ic.StorageController;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import java.net.URI;

//baseDir = "/disk1/import"
//targetDir = "/disk1/import"
baseDir = "${System.getenv('EUCALYPTUS')}/var/lib/eucalyptus/db";
targetDir = baseDir;
targetDbPrefix= "eucalyptus"

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


System.setProperty("euca.home",System.getenv("EUCALYPTUS"))
System.setProperty("euca.var.dir","${System.getenv('EUCALYPTUS')}/var/lib/eucalyptus/")
System.setProperty("euca.log.dir", "${System.getenv('EUCALYPTUS')}/var/log/eucalyptus/")
System.setProperty("euca.db.host", "jdbc:hsqldb:file:${targetDir}/${targetDbPrefix}")
System.setProperty("euca.db.password", "${System.getenv('EUCALYPTUS_DB')}");
System.setProperty("euca.log.level", 'INFO');

["${baseDir}/eucalyptus_general.script","${baseDir}/eucalyptus_images.script","${baseDir}/eucalyptus_auth.script","${baseDir}/eucalyptus_config.script","${baseDir}/eucalyptus_walrus.script","${baseDir}/eucalyptus_storage.script","${baseDir}/eucalyptus_dns.script"].each{
new File(it).write("CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
          "CREATE USER SA PASSWORD \"" + System.getProperty( "euca.db.password" ) + "\"\n" +
          "GRANT DBA TO SA\n" + 
          "SET WRITE_DELAY 100 MILLIS\n" +
          "SET SCHEMA PUBLIC\n");
}

UserGroupInfo userGroupInfo = new UserGroupInfo( "all" );
EntityWrapper<UserGroupInfo> db3 = new EntityWrapper<UserGroupInfo>( );
try {
  db3.add( userGroupInfo );
  db3.commit();
} catch ( Throwable t ) {
  t.printStackTrace();
  db3.rollback();
}


db.rows('SELECT * FROM SYSTEM_INFO').each{ 
  SystemConfiguration config = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration();
  EntityWrapper<SystemConfiguration> confDb = new EntityWrapper<SystemConfiguration>();
  try {
    config.setDefaultKernel(it.SYSTEM_INFO_DEFAULT_KERNEL);
    config.setDefaultRamdisk(it.SYSTEM_INFO_DEFAULT_RAMDISK);
    config.setRegistrationId(it.SYSTEM_REGISTRATION_ID);
    confDb.merge( config );
    confDb.commit( );
  } catch (Throwable t) {
    t.printStackTrace();
    confDb.commit();
  }
}


db.rows('SELECT * FROM VM_TYPES').each{   
  EntityWrapper<VmType> dbVm = new EntityWrapper<VmType>( );
  try {
    dbVm.add( new VmType( it.VM_TYPE_NAME, it.VM_TYPE_CPU, it.VM_TYPE_DISK, it.VM_TYPE_MEMORY ) );
    dbVm.commit();
  } catch (Throwable t) {
    t.printStackTrace();
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
    userGroupInfo = dbUser.getUnique( new UserGroupInfo("all") );
    userGroupInfo.getUsers().add( user );
    dbUser.commit();
  } catch( Throwable t ) { 
    t.printStackTrace();
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
      dbKp.add( new SshKeyPair( it.USER_NAME, keypair.SSH_KEYPAIR_NAME, keypair.SSH_KEYPAIR_PUBLIC_KEY, keypair.SSH_KEYPAIR_FINGER_PRINT ) );
      dbKp.commit( );
    } catch ( Throwable t ) {
      t.printStackTrace();
      dbKp.rollback( );
    }
  }
  db.rows("SELECT net.* FROM user_has_network_groups has_net LEFT OUTER JOIN network_group net on net.network_group_id=has_net.network_group_id WHERE has_net.user_id=${ it.USER_ID }").each{  net ->
    println "-> network: ${net.NETWORK_GROUP_NAME}"
    EntityWrapper<NetworkRulesGroup> dbNet = NetworkGroupUtil.getEntityWrapper();
    try {
      NetworkRulesGroup group = new NetworkRulesGroup( it.USER_NAME,net.NETWORK_GROUP_NAME, net.NETWORK_GROUP_DESCRIPTION );
      dbNet.add( group );
      db.rows("SELECT rule.* FROM network_group_has_rules has_thing LEFT OUTER JOIN network_rule rule on rule.network_rule_id=has_thing.network_rule_id WHERE has_thing.network_group_id=${ net.NETWORK_GROUP_ID }").each{  rule ->
      println "--> rule: ${rule.NETWORK_RULE_PROTOCOL}, ${rule.NETWORK_RULE_LOW_PORT}, ${rule.NETWORK_RULE_HIGH_PORT}"
        NetworkRule netRule = new NetworkRule(rule.NETWORK_RULE_PROTOCOL,rule.NETWORK_RULE_LOW_PORT, rule.NETWORK_RULE_HIGH_PORT);
        group.getNetworkRules().add( netRule );
        db.rows("SELECT o.* FROM network_rule_has_peer_network has_thing LEFT OUTER JOIN network_rule_peer_network o on o.network_rule_peer_network_id=has_thing.network_rule_peer_network_id WHERE has_thing.network_rule_id=${ rule.NETWORK_RULE_ID }").each{  peer ->
          println "---> peer: ${peer.NETWORK_RULE_PEER_NETWORK_USER_QUERY_KEY}, ${peer.NETWORK_RULE_PEER_NETWORK_USER_GROUP}" 
          netRule.getNetworkPeers().add( new NetworkPeer( peer.NETWORK_RULE_PEER_NETWORK_USER_QUERY_KEY, peer.NETWORK_RULE_PEER_NETWORK_USER_GROUP ) );
        }
        db.rows("SELECT o.* FROM network_rule_has_ip_range has_thing LEFT OUTER JOIN network_rule_ip_range o on o.network_rule_ip_range_id=has_thing.network_rule_ip_range_id WHERE has_thing.network_rule_id=${ rule.NETWORK_RULE_ID }").each{  ip ->
          println "---> ip-range: ${ip.NETWORK_RULE_IP_RANGE_VALUE}" 
          netRule.getIpRanges().add( new IpRange( ip.NETWORK_RULE_IP_RANGE_VALUE ) )
        }
      }      
      dbNet.commit();
    } catch (Throwable t) {
      t.printStackTrace();
      dbNet.rollback();
    }
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
    /*
    db.rows("SELECT p.* FROM image_has_perms has_p LEFT OUTER JOIN users p on p.user_id=has_p.user_id WHERE has_p.image_id=${ image.IMAGE_ID }").each{  
      UserInfo u = dbImg.getUnique( new UserInfo( db.rows("SELECT u.user_name FROM users u WHERE u.user_name=${it.USER_NAME}")[0] ) );
      imgInfo.getPermissions( ).add( u );
    }
    */
    userGroupInfo = dbImg.getUnique( userGroupInfo );
    imgInfo.getUserGroups().add(userGroupInfo);
    dbImg.commit();
  } catch( Throwable t ) {
    t.printStackTrace();
    dbImg.rollback();
  }
}

dbVolumes.rows("SELECT * FROM VOLUME").each{
  println "Adding volume: ${it.DISPLAYNAME}"
  EntityWrapper<Volume> dbVol = VolumeManager.getEntityWrapper();
  try {
    Volume v = new Volume(it.USERNAME, it.DISPLAYNAME, it.SIZE, it.CLUSTER, it.PARENTSNAPSHOT );
//    v.setLocalDevice(it.LOCALDEVICE);
//    v.setRemoteDevice(it.REMOTEDEVICE);
    dbVol.add( v );
    dbVol.commit();
  } catch (Throwable t) {
    t.printStackTrace();
    dbVol.rollback();
  }
}

dbVolumes.rows("SELECT * FROM SNAPSHOT").each{
  println "Adding snapshot: ${it.DISPLAYNAME}"
  
  EntityWrapper<Snapshot> dbSnap = SnapshotManager.getEntityWrapper( );
  try {
    Snapshot s = new Snapshot(it.USERNAME,it.DISPLAYNAME);
    s.setBirthday(it.BIRTHDAY);
    s.setState(State.valueOf(it.STATE));
    s.setParentVolume(it.PARENTVOLUME);
    dbSnap.add(s);
    dbSnap.commit();
  } catch (Throwable t) {
    t.printStackTrace();
    dbSnap.rollback();
  }
}

db.rows('SELECT * FROM BUCKETS').each{
  println "Adding bucket: ${it.BUCKET_NAME}"

  EntityWrapper<BucketInfo> dbBucket = WalrusControl.getEntityWrapper();
  try {
  	BucketInfo b = new BucketInfo(it.OWNER_ID,it.BUCKET_NAME,it.BUCKET_CREATION_DATE);
  	b.setOwnerId(it.OWNER_ID);
  	b.setLocation(it.BUCKET_LOCATION);
  	b.setGlobalRead(it.GLOBAL_READ);
  	b.setGlobalWrite(it.GLOBAL_WRITE);
  	b.setGlobalReadACP(it.GLOBAL_READ_ACP);
  	b.setGlobalWriteACP(it.GLOBAL_WRITE_ACP);
  	b.setBucketSize(it.BUCKET_SIZE);
  	b.setHidden(false);
  	/**
  	 * CREATE MEMORY TABLE BUCKET_HAS_OBJECTS(
  	 * BUCKET_ID BIGINT NOT NULL,
  	 * OBJECT_ID BIGINT NOT NULL)
  	 * 
  	 * CREATE MEMORY TABLE OBJECTS(OBJECT_ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY
  	 * ,OWNER_ID VARCHAR(255)
  	 * ,OBJECT_NAME VARCHAR(255)
  	 * ,GLOBAL_READ BOOLEAN,GLOBAL_WRITE BOOLEAN,GLOBAL_READ_ACP BOOLEAN,GLOBAL_WRITE_ACP BOOLEAN,ETAG VARCHAR(255)
  	 * ,LAST_MODIFIED TIMESTAMP,SIZE BIGINT,STORAGE_CLASS VARCHAR(255)
  	 * ,OBJECT_KEY VARCHAR(255))
  	 */
    db.rows("SELECT g.* FROM bucket_has_grants has_thing LEFT OUTER JOIN grants g on g.grant_id=has_thing.grant_id WHERE has_thing.bucket_id=${ it.BUCKET_ID }").each{  grant ->
    println "--> grant: ${it.BUCKET_NAME}/${grant.USER_ID}"
      GrantInfo grantInfo = new GrantInfo();
      grantInfo.setUserId(grant.USER_ID);
      grantInfo.setGrantGroup(grant.GRANT_GROUP);
      grantInfo.setCanWrite(grant.WRITE);
      grantInfo.setCanRead(grant.READ);
      grantInfo.setCanReadACP(grant.READ_ACP);
      grantInfo.setCanWriteACP(grant.WRITE_ACP);
      b.getGrants().add(grantInfo);
    }
 	dbBucket.add(b);
  	dbBucket.commit();
  } catch (Throwable t) {
	    t.printStackTrace();
		  dbBucket.rollback();
  }
    db.rows("SELECT o.* FROM bucket_has_objects has_thing LEFT OUTER JOIN objects o on o.object_id=has_thing.object_id WHERE has_thing.bucket_id=${ it.BUCKET_ID }").each{  obj ->
      println "--> object: ${it.BUCKET_NAME}/${obj.OBJECT_NAME}"
      //Do bucket object stuff here.
      EntityWrapper<ObjectInfo> dbObject = WalrusControl.getEntityWrapper();
      try {
    	ObjectInfo objectInfo = new ObjectInfo(it.BUCKET_NAME, obj.OBJECT_NAME);
    	objectInfo.setObjectName(obj.OBJECT_NAME);
    	objectInfo.setOwnerId(obj.OWNER_ID);
    	objectInfo.setGlobalRead(obj.GLOBAL_READ);
    	objectInfo.setGlobalWrite(obj.GLOBAL_WRITE);
    	objectInfo.setGlobalReadACP(obj.GLOBAL_READ_ACP);
    	objectInfo.setGlobalWriteACP(obj.GLOBAL_WRITE_ACP);
    	objectInfo.setEtag(obj.ETAG);
    	objectInfo.setLastModified(new Date());
    	objectInfo.setStorageClass(obj.STORAGE_CLASS);
        db.rows("SELECT g.* FROM object_has_grants has_thing LEFT OUTER JOIN grants g on g.grant_id=has_thing.grant_id WHERE has_thing.object_id=${ obj.OBJECT_ID }").each{  grant ->
        println "--> grant: ${obj.OBJECT_NAME}/${grant.USER_ID}"
          GrantInfo grantInfo = new GrantInfo();
          grantInfo.setUserId(grant.USER_ID);
          grantInfo.setGrantGroup(grant.GRANT_GROUP);
          grantInfo.setCanWrite(grant.WRITE);
          grantInfo.setCanRead(grant.READ);
          grantInfo.setCanReadACP(grant.READ_ACP);
          grantInfo.setCanWriteACP(grant.WRITE_ACP);
          objectInfo.getGrants().add(grantInfo);
        }
        db.rows("SELECT m.* FROM object_has_metadata has_thing LEFT OUTER JOIN metadata m on m.metadata_id=has_thing.metadata_id WHERE has_thing.object_id=${ obj.OBJECT_ID }").each{  metadata ->
        println "--> metadata: ${obj.OBJECT_NAME}/${metadata.NAME}"
          MetaDataInfo mInfo = new MetaDataInfo();
          mInfo.setObjectName(obj.OBJECT_NAME);
          mInfo.setName(metadata.NAME);
          mInfo.setValue(metadata.VALUE);
          objectInfo.getMetaData().add(mInfo);
        }
        dbObject.add(objectInfo);
    	dbObject.commit();
      } catch (Throwable t) {
    	t.printStackTrace();
    	dbObject.rollback();
      }
    }
}

db.rows('SELECT * FROM IMAGECACHE').each{ 
	  println "Adding IMAGECACHE: ${it.MANIFEST_NAME}"

	  EntityWrapper<ImageCacheInfo> dbImg = WalrusControl.getEntityWrapper(); 
	  try {
		ImageCacheInfo img = new ImageCacheInfo(it.BUCKET_NAME, it.MANIFEST_NAME);
		img.setImageName(it.IMAGE_NAME);
		img.setInCache(it.IN_CACHE);
		img.setSize(it.SIZE);
		img.setUseCount(it.USE_COUNT);
		dbImg.add(img);
	    dbImg.commit();
	  } catch (Throwable t) {
		t.printStackTrace();
		dbImg.rollback();
	  }
	}


db.rows('SELECT * FROM VOLUMES').each{ 
  println "Adding VOLUME: ${it.VOLUME_NAME}"

  EntityWrapper<VolumeInfo> dbVol = StorageController.getEntityWrapper(); 
  try {
	VolumeInfo v = new VolumeInfo(it.VOLUME_NAME);
	v.setScName(StorageProperties.SC_LOCAL_NAME);
	v.setUserName(it.VOLUME_USER_NAME);
	v.setSize(it.SIZE);
	v.setStatus(it.STATUS);
	v.setCreateTime(new Date());
	v.setZone(it.ZONE);
	v.setSnapshotId(it.SNAPSHOT_ID);
    dbVol.add(v);
    dbVol.commit();
  } catch (Throwable t) {
	t.printStackTrace();
	dbVol.rollback();
  }
}

db.rows('SELECT * FROM SNAPSHOTS').each{ 
	  println "Adding snapshit: ${it.SNAPSHOT_NAME}"

	  EntityWrapper<VolumeInfo> dbSnap = StorageController.getEntityWrapper(); 
	  try {
		SnapshotInfo s = new SnapshotInfo(it.SNAPSHOT_NAME);
		s.setShouldTransfer(true);
		s.setScName(StorageProperties.SC_LOCAL_NAME);
		s.setUserName(it.SNAPSHOT_USER_NAME);
		s.setVolumeId(it.VOLUME_NAME);
		s.setStatus(it.STATUS);
		s.setStartTime(new Date());
		dbSnap.add(s);
		dbSnap.commit();
	  } catch (Throwable t) {
		t.printStackTrace();
		dbSnap.rollback();
	  }
	}


db.rows('SELECT * FROM LVMVOLUMES').each{

  println "Adding LVMVOLUME: ${it.VOLUME_NAME}"
  EntityWrapper<LVMVolumeInfo> dbVol = StorageController.getEntityWrapper();
  try {
	LVMVolumeInfo l = new LVMVolumeInfo(it.VOLUME_NAME);
	l.setScName(StorageProperties.SC_LOCAL_NAME);
	l.setLoDevName(it.LODEV_NAME);
	l.setLoFileName(it.LOFILE_NAME);
	l.setPvName(it.PV_NAME);
	l.setVgName(it.VG_NAME);
	l.setLvName(it.LV_NAME);
	l.setSize(it.SIZE);
	l.setStatus(it.STATUS);
	l.setVbladePid(it.VBLADE_PID);
	l.setSnapshotOf(it.SNAPSHOT_OF);
	l.setMajorNumber(it.MAJOR_NUMBER);
	l.setMinorNumber(it.MINOR_NUMBER);
	dbVol.add(l);
	dbVol.commit();
  } catch (Throwable t) {
	t.printStackTrace();
	dbVol.rollback();
  }
}

db.rows('SELECT * FROM LVMMETADATA').each{
  println "Adding LVMMETADATA: ${it.HOSTNAME}"
  EntityWrapper<LVMMetaInfo> dbVol = StorageController.getEntityWrapper(); 
  try {
	  LVMMetaInfo lvmmeta = new LVMMetaInfo(it.HOSTNAME);
	  lvmmeta.setMajorNumber(it.MAJOR_NUMBER);
	  lvmmeta.setMinorNumber(it.MINOR_NUMBER);
	  dbVol.add(lvmmeta);
	  dbVol.commit();
  } catch (Throwable t) {
	t.printStackTrace();
	dbVol.rollback();
  }
}

db.rows('SELECT * FROM CLUSTERS').each{ 
  println "Adding CLUSTER: name=${it.CLUSTER_NAME} host=${it.CLUSTER_HOST} port=${it.CLUSTER_PORT}"
  EntityWrapper<ClusterConfiguration> dbClusterConfig = Configuration.getEntityWrapper()
  ClusterConfiguration clusterConfig = new ClusterConfiguration(it.CLUSTER_NAME, it.CLUSTER_HOST, it.CLUSTER_PORT)
  dbClusterConfig.add(clusterConfig)
  dbClusterConfig.commit();
}


db.rows('SELECT * FROM SYSTEM_INFO').each{
  URI uri = new URI(it.SYSTEM_INFO_STORAGE_URL)
  println "Adding Walrus: name=walrus host=${uri.getHost()} port=${uri.getPort()}"
  EntityWrapper<WalrusConfiguration> dbWalrusConfig = Configuration.getEntityWrapper()
  WalrusConfiguration walrusConfig = new WalrusConfiguration("walrus", uri.getHost(), uri.getPort())
  dbWalrusConfig.add(walrusConfig)
  dbWalrusConfig.commit()
  println "Adding SC: name=StorageController-local host=${uri.getHost()} port=${uri.getPort()}"
  EntityWrapper<StorageControllerConfiguration> dbSCConfig = Configuration.getEntityWrapper()
  StorageControllerConfiguration storageConfig = new StorageControllerConfiguration("StorageController-local", uri.getHost(), uri.getPort())
  dbSCConfig.add(storageConfig)
  dbSCConfig.commit()
}

