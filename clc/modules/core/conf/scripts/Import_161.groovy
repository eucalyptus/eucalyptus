
import java.security.*;

import javax.crypto.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.SubDirectory;
import com.eucalyptus.util.WalrusProperties;
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
import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.AOEMetaInfo;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import edu.ucsb.eucalyptus.ic.StorageController;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;

import java.io.File;
import java.net.URI;
import com.eucalyptus.util.DatabaseUtil;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.Hashes.Digest;
import java.security.cert.X509Certificate;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.ClusterCredentials;

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

def getSqlStorage() {
	source = new org.hsqldb.jdbc.jdbcDataSource();
	source.database = "jdbc:hsqldb:file:${baseDir}/eucalyptus_storage";
	source.user = 'sa';
	source.password = '';
	return new Sql(source);
}

def getSqlWalrus() {
	source = new org.hsqldb.jdbc.jdbcDataSource();
	source.database = "jdbc:hsqldb:file:${baseDir}/eucalyptus_walrus";
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
dbStorage = getSqlStorage();
dbWalrus = getSqlWalrus();

System.setProperty("euca.home",System.getenv("EUCALYPTUS"))
System.setProperty("euca.var.dir","${System.getenv('EUCALYPTUS')}/var/lib/eucalyptus/")
System.setProperty("euca.log.dir", "${System.getenv('EUCALYPTUS')}/var/log/eucalyptus/")
Component c = Component.db
System.setProperty("euca.db.host", "jdbc:hsqldb:file:${targetDir}/${targetDbPrefix}")

System.setProperty("euca.db.password", "${System.getenv('EUCALYPTUS_DB')}");
System.setProperty("euca.log.level", 'INFO');

dbWalrus.rows('SELECT * FROM BUCKETS').each{ 
	EntityWrapper<BucketInfo> dbBucket = WalrusControl.getEntityManager();
	try {
		
		dbBucket.merge(bucket);
		dbBucket.commit( );
	} catch (Throwable t) {
		t.printStackTrace();
		dbBucket.rollback();
	}
}

dbStorage.rows('SELECT * FROM LVMMETADATA').each {
	EntityWrapper<AOEMetaInfo> dbStorage = StorageController.getEntityWrapper();
	try {
		AOEMetaInfo metaInfo = new AOEMetaInfo(it.HOSTNAME);
		metaInfo.setMajorNumber(it.MAJOR_NUMBER);
		metaInfo.setMinorNumber(it.MINOR_NUMBER);
		dbStorage.add(metaInfo);
		dbStorage.commit();
	} catch(Throwable t) {
		t.printStackTrace();
		dbStorage.rollback();
	}	
}

dbStorage.rows('SELECT * FROM LVMVOLUMES').each {
	EntityWrapper<AOEVolumeInfo> dbStorage = StorageController.getEntityWrapper();
	try {
		AOEVolumeInfo volumeInfo = new AOEVolumeInfo(it.VOLUME_NAME);
		volumeInfo.setMajorNumber(it.MAJOR_NUMBER);
		volumeInfo.setMinorNumber(it.MINOR_NUMBER);
		volumeInfo.setLoDevName(it.LODEV_NAME);
		volumeInfo.setLoFileName(it.LOFILE_NAME);
		volumeInfo.setVbladePid(it.VBLADE_PID);
		volumeInfo.setScName(it.SC_NAME);
		volumeInfo.setPvName(it.PV_NAME);
		volumeInfo.setLvName(it.LV_NAME);
		volumeInfo.setVgName(it.VG_NAME);
		volumeInfo.setSize(it.SIZE);
		volumeInfo.setStatus(it.STATUS);
		volumeInfo.setSnapshotOf(it.SNAPSHOT_OF);
		dbStorage.add(volumeInfo);
		dbStorage.commit();
	} catch(Throwable t) {
		t.printStackTrace();
		dbStorage.rollback();
	}	
}

//flush
DatabaseUtil.closeAllEMFs();
//the db will not sync to disk even after a close in some cases. Wait a bit.
Thread.sleep(5000);
