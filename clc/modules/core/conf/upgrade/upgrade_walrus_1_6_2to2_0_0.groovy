import javax.persistence.Column;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import groovy.sql.Sql;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;

class upgrade_walrus_162to2_0_0 extends AbstractUpgradeScript {
	static final String FROM_VERSION = "1.6.2";
	static final String TO_VERSION = "2.0.0";
	
	public upgrade_walrus_162to2_0_0() {
		super(2);
	}
	
	@Override
	public Boolean accepts( String from, String to ) {
		if(FROM_VERSION.equals(from) && TO_VERSION.equals(to))
			return true;
		return false;
	}
	
	@Override
	public void upgrade(File oldEucaHome, File newEucaHome) {
		def walrus_conn = StandalonePersistence.getConnection("eucalyptus_walrus");
		walrus_conn.rows('SELECT * FROM BUCKETS').each{
			println "Adding bucket: ${it.BUCKET_NAME}"
			
			EntityWrapper<BucketInfo> dbBucket = WalrusControl.getEntityWrapper();
			try {
				BucketInfo b = new BucketInfo(it.OWNER_ID,it.BUCKET_NAME,it.BUCKET_CREATION_DATE);
				b.setLocation(it.BUCKET_LOCATION);
				b.setGlobalRead(it.GLOBAL_READ);
				b.setGlobalWrite(it.GLOBAL_WRITE);
				b.setGlobalReadACP(it.GLOBAL_READ_ACP);
				b.setGlobalWriteACP(it.GLOBAL_WRITE_ACP);
				b.setBucketSize(it.BUCKET_SIZE);
				b.setHidden(false);
				b.setLoggingEnabled(it.LOGGING_ENABLED);
				b.setTargetBucket(it.TARGET_BUCKET);
				b.setTargetPrefix(it.TARGET_PREFIX);
				walrus_conn.rows("SELECT g.* FROM bucket_has_grants has_thing LEFT OUTER JOIN grants g on g.grant_id=has_thing.grant_id WHERE has_thing.bucket_id=${ it.BUCKET_ID }").each{  grant ->
					println "--> grant: ${it.BUCKET_NAME}/${grant.USER_ID}"
					GrantInfo grantInfo = new GrantInfo();
					grantInfo.setUserId(grant.USER_ID);
					grantInfo.setGrantGroup(grant.GRANTGROUP);
					grantInfo.setCanWrite(grant.ALLOW_WRITE);
					grantInfo.setCanRead(grant.ALLOW_READ);
					grantInfo.setCanReadACP(grant.ALLOW_READ_ACP);
					grantInfo.setCanWriteACP(grant.ALLOW_WRITE_ACP);
					b.getGrants().add(grantInfo);
				}
				dbBucket.add(b);
				dbBucket.commit();
			} catch (Throwable t) {
				t.printStackTrace();
				dbBucket.rollback();
			}
		}
		walrus_conn.rows('SELECT * FROM OBJECTS').each{
			println "Adding object: ${it.BUCKET_NAME}/${it.OBJECT_NAME}"			
			EntityWrapper<ObjectInfo> dbObject = WalrusControl.getEntityWrapper();
			try {
				ObjectInfo objectInfo = new ObjectInfo(it.BUCKET_NAME, it.OBJECT_KEY);
				objectInfo.setObjectName(it.OBJECT_NAME);
				objectInfo.setOwnerId(it.OWNER_ID);
				objectInfo.setGlobalRead(it.GLOBAL_READ);
				objectInfo.setGlobalWrite(it.GLOBAL_WRITE);
				objectInfo.setGlobalReadACP(it.GLOBAL_READ_ACP);
				objectInfo.setGlobalWriteACP(it.GLOBAL_WRITE_ACP);
				objectInfo.setSize(it.SIZE);
				objectInfo.setEtag(it.ETAG);
				objectInfo.setLastModified(it.LAST_MODIFIED);
				objectInfo.setStorageClass(it.STORAGE_CLASS);
				objectInfo.setContentType(it.CONTENT_TYPE);
				objectInfo.setContentDisposition(it.CONTENT_DISPOSITION);
				objectInfo.setDeleted(false);
				objectInfo.setVersionId("null");
				objectInfo.setLast(true);
				walrus_conn.rows("SELECT g.* FROM object_has_grants has_thing LEFT OUTER JOIN grants g on g.grant_id=has_thing.grant_id WHERE has_thing.object_id=${ it.OBJECT_ID }").each{  grant ->
					println "--> grant: ${it.OBJECT_NAME}/${grant.USER_ID}"
					GrantInfo grantInfo = new GrantInfo();
					grantInfo.setUserId(grant.USER_ID);
					grantInfo.setGrantGroup(grant.GRANTGROUP);
					grantInfo.setCanWrite(grant.ALLOW_WRITE);
					grantInfo.setCanRead(grant.ALLOW_READ);
					grantInfo.setCanReadACP(grant.ALLOW_READ_ACP);
					grantInfo.setCanWriteACP(grant.ALLOW_WRITE_ACP);
					objectInfo.getGrants().add(grantInfo);
				}
				walrus_conn.rows("SELECT m.* FROM object_has_metadata has_thing LEFT OUTER JOIN metadata m on m.metadata_id=has_thing.metadata_id WHERE has_thing.object_id=${ it.OBJECT_ID }").each{  metadata ->
					println "--> metadata: ${it.OBJECT_NAME}/${metadata.NAME}"
					MetaDataInfo mInfo = new MetaDataInfo();
					mInfo.setObjectName(it.OBJECT_NAME);
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
}
