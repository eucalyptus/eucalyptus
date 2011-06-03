import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

// Auth
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import java.security.cert.X509Certificate;
import com.eucalyptus.auth.util.X509CertHelper;

// DNS
import edu.ucsb.eucalyptus.cloud.entities.ARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.CNAMERecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.SOARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ZoneInfo;

// Images
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.LaunchPermission;
import com.eucalyptus.images.ProductCode;
import com.eucalyptus.images.ImageUtil;
import com.eucalyptus.images.KernelImageInfo;
import com.eucalyptus.images.MachineImageInfo;
import com.eucalyptus.images.RamdiskImageInfo;
import com.eucalyptus.cloud.Image;

// Storage
import edu.ucsb.eucalyptus.cloud.entities.AOEMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageStatsInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;

// Walrus
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.GrantInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.MetaDataInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.cloud.entities.TorrentInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusStatsInfo;

// General -> Cloud
import com.eucalyptus.network.IpRange;
import com.eucalyptus.network.NetworkPeer;
import com.eucalyptus.network.NetworkRule;
import com.eucalyptus.network.NetworkRulesGroup;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.vm.VmType;

// Other
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import com.eucalyptus.address.Address;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.util.Counters;

class upgrade_20_30 extends AbstractUpgradeScript {
	static final String FROM_VERSION = "2.0.3";
	static final String TO_VERSION = "eee-3.0.0";
	private static Logger LOG = Logger.getLogger( upgrade_20_30.class );
	private static List<Class> entities = new ArrayList<Class>();
	private static Map<String, Class> entityMap = new HashMap<String, Class>();

	public upgrade_20_30() {
		super(1);		
	}

	@Override
	public Boolean accepts( String from, String to ) {
		if(TO_VERSION.equals(to))
			return true;
		return false;
	}

	@Override
	public void upgrade(File oldEucaHome, File newEucaHome) {
		if (!upgradeAuth()) {
			return;
		} else if (!upgradeNetwork()) {
			return;
		} else if (!upgradeWalrus()) {
			return;
		}
		
		buildEntityMap();
		def altEntityMap = [ metadata_keypair:'eucalyptus_general',
				vm_types:'eucalyptus_general' ];


		Set<String> entityKeys = entityMap.keySet();
		for (String entityKey : entityKeys) {
            if (entityKey.equals("metadata_network_group")) {
				continue
			}
			String contextName = getContextName(entityKey);
			Sql conn;
			if (altEntityMap.containsKey(entityKey)) {
				conn = getConnection(altEntityMap.get(entityKey));
			} else {
				conn = getConnection(contextName);
			}
			if (conn != null) {
				Map<String, Method> setterMap = buildSetterMap(conn, entityKey);
				if(setterMap != null)
					doUpgrade(contextName, conn, entityKey, setterMap);
			} else {
				LOG.error("Failed to get connection to " + contextName);
			}
		}
	}

	private Sql getConnection(String contextName) {
		try {
			Sql conn = StandalonePersistence.getConnection(contextName);
			return conn;
		} catch (SQLException e) {
			LOG.error(e);
			return null;
		}
	}

	private String getContextName(String entityKey) {
		Class entity = entityMap.get(entityKey);
		if (entity != null) {
			if(entity.isAnnotationPresent(PersistenceContext.class)) {
				PersistenceContext annot = (PersistenceContext) entity.getAnnotation(PersistenceContext.class);
				return annot.name();
			}
		}
		return null;		
	}

	private void doUpgrade(String contextName, Sql conn, String entityKey, Map<String, Method> setterMap) {
		List<GroovyRowResult> rowResults;
		try {
			rowResults = conn.rows("SELECT * FROM " + entityKey);
                        LOG.info("Got " + rowResults.size().toString() + " results from " + entityKey);
			EntityWrapper db =  EntityWrapper.get(entityMap.get(entityKey));
                        
			// def columnsShown = 0;
			for (GroovyRowResult rowResult : rowResults) {
				Set<String> columns = rowResult.keySet();
				/* if (columnsShown != 1){
					LOG.info("Columns: " + columns);
					columnsShown = 1;
				} */
				Object dest;
				try {
					dest = ClassLoader.getSystemClassLoader().loadClass(entityMap.get(entityKey).getCanonicalName()).newInstance();
				} catch (ClassNotFoundException e1) {
					LOG.error(e1);
					break;
				} catch (InstantiationException e) {
					LOG.error(e);
					break;
				} catch (IllegalAccessException e) {
					LOG.error(e);
					break;
				}
				for (String column : columns) {
					Method setter = setterMap.get(column);
					if(setter != null) {
						Object o = rowResult.get(column);
						if(o != null) {
							try {
								if(dest instanceof Volume && (setter.getName().equals("setRemoteDevice"))) {
									((Volume)dest).setRemoteDevice(null);
								} else {
									setter.invoke(dest, o);
								}
							} catch (IllegalArgumentException e) {
								LOG.error(dest.getClass().getName()  + " " + column + " " + e);
							} catch (IllegalAccessException e) {
								LOG.error(dest.getClass().getName()  + " " + column + " " + e);
							} catch (InvocationTargetException e) {
								LOG.error(dest.getClass().getName()  + " " + column + " " + e);
							}
						}
					}
				}
				db.add(dest);
			}
			LOG.debug("Upgraded: " + entityKey);
			db.commit();
		} catch (SQLException e) {
			LOG.error(e);
			return;
		}		
	}

	private Map<String, Method> buildSetterMap(Sql conn, String entityKey) {
		Map<String, Method> setterMap = new HashMap<String, Method>();
		try {
			Object firstRow = conn.firstRow("SELECT * FROM " + entityKey);
			if(firstRow == null) {
				LOG.warn("Unable to find anything in table: " + entityKey);
				return null;
			}
			if(firstRow instanceof Map) {
				// switching columns here makes the setterMap easier
				// to construct.  We flip them back inside the setterMap 
				// at the end of the function.
				def columnMap = [ VM_TYPE_CPU:'metadata_vm_type_cpu',
						VM_TYPE_DISK:'metadata_vm_type_disk',
						VM_TYPE_MEMORY:'metadata_vm_type_memory',
						VM_TYPE_NAME:'metadata_vm_type_name' ];
				Set<String> origColumns = ((Map) firstRow).keySet();
				Set<String> columnNames = origColumns.findAll{ columnMap.containsKey(it) }.collect{ columnMap[it] } +
							  origColumns.findAll{ !columnMap.containsKey(it) }.collect{ it };
				Class definingClass = entityMap.get(entityKey);
				Field[] fields = definingClass.getDeclaredFields();
				//special case. Do this better.
				addToSetterMap(setterMap, columnNames, definingClass, fields);				
				Class superClass = entityMap.get(entityKey).getSuperclass();				
				while(superClass.isAnnotationPresent(MappedSuperclass.class)) {
					Field[] superFields = superClass.getDeclaredFields();
					addToSetterMap(setterMap, columnNames, superClass, superFields);
					//nothing to see here (otherwise we loop forever).
					if(superClass.equals(AbstractPersistent.class))
						break;
					superClass = superClass.getSuperclass();
				}
				for (String column : columnNames) {
					if(!setterMap.containsKey(column)) {
						LOG.warn("No corresponding field for column: " + column + " found");
					}
				}
				if (entityKey.equals('vm_types')) {
					for (c in columnMap) {
						LOG.info("remove setterMap for " + c.value + ", replace with " + c.key);
						setterMap.put(c.key, setterMap.get(c.value));
						setterMap.remove(c.value);
					}
				}
			}
		} catch (SQLException e) {
			LOG.error(e);
		}
		return setterMap;
	}

	private void addToSetterMap(Map<String, Method> setterMap,
			Set<String> columnNames, Class definingClass, Field[] fields) {
		for(String column : columnNames) {
			for(Field f : fields) {
				if(f.isAnnotationPresent(Column.class) && !f.isAnnotationPresent(Id.class)) {
					Column annotClass = (Column)f.getAnnotation(Column.class);
					if(((String)column).toLowerCase().equals(annotClass.name().toLowerCase())) {
						String baseMethodName = f.getName( ).substring( 0, 1 ).toUpperCase( ) + f.getName( ).substring( 1 );
						try {
							Class[] classes = new Class[1];
							classes[0] = f.getType();
							Method setMethod = definingClass.getDeclaredMethod( "set" + baseMethodName, classes );
							setterMap.put(column, setMethod);
						} catch (SecurityException e) {
							LOG.error(e);
						} catch (NoSuchMethodException e) {
							LOG.error(e);
						}
						break;
					}
				}
			}
			/* if(setterMap.containsKey(column)) {
				LOG.debug(column + " is set by: " + setterMap.get(column).getName());
			} */
		}
	}

	private void buildEntityMap() {
                // Note that this maps new -> old
		def tableMap = [ metadata_keypairs:'metadata_keypair',
				cloud_vm_types:'vm_types' ];
		for (Class entity : entities) {
			if (entity.isAnnotationPresent(Table.class)) {
				// This only handles tables whose names have not changed.
				Table annot = (Table)entity.getAnnotation(Table.class);
				if (tableMap.containsKey(annot.name())) {
					entityMap.put(tableMap[annot.name()], entity);
				} else {
					entityMap.put(annot.name(), entity);
				}
			}
		}
	}

	public boolean upgradeAuth() {
		def conn = StandalonePersistence.getConnection("eucalyptus_auth");
		def gen_conn = StandalonePersistence.getConnection("eucalyptus_general");
        def image_conn = StandalonePersistence.getConnection("eucalyptus_images");
        def stor_conn = StandalonePersistence.getConnection("eucalyptus_storage");
		def walrus_conn = StandalonePersistence.getConnection("eucalyptus_walrus");
		conn.rows('SELECT * FROM auth_users').each {
			def account = Accounts.addAccount( it.auth_user_name );
			def user = account.addUser( it.auth_user_name, "/", true/* skipRegistration */, true/* enabled */, null);
			user.setPassword( it.auth_user_password );
			user.setToken(it.auth_user_token );
			println "added " + account;
			conn.rows("""SELECT c.* FROM auth_users
				JOIN auth_user_has_x509 on auth_users.id=auth_user_has_x509.auth_user_id
				JOIN auth_x509 c on auth_user_has_x509.auth_x509_id=c.id
				WHERE auth_users.auth_user_name=${ it.auth_user_name }""").each { certificate ->
                                X509Certificate x509cert = X509CertHelper.toCertificate(certificate.auth_x509_pem_certificate);
				def cert = user.addCertificate(x509cert);
				// cert.setRevoked(certificate.auth_x509_revoked);
			}
			// The test data I have includes duplicate rows here.  Why?
			def uInfo = gen_conn.firstRow("""SELECT * FROM Users WHERE Users.user_name=${ it.auth_user_name }""");
			Map<String, String> info = new HashMap<String, String>( );
			// Might want to drop NULLs here
			info.put("Full Name", uInfo.user_real_name);
			info.put("Email", uInfo.user_email);
			info.put("Telephone", uInfo.user_telephone_number);
			info.put("Affiliation", uInfo.user_affiliation);
			info.put("ProjectDescription", uInfo.user_project_description);
			info.put("ProjectPI", uInfo.user_project_pi_name);
			user.setInfo( info );
            def ufn = UserFullName.getInstance(user);
            
			gen_conn.rows("""SELECT * FROM Images WHERE image_owner_id=${ it.auth_user_name }""").each { img ->
				EntityWrapper<ImageInfo> dbGen = EntityWrapper.get(ImageInfo.class);
				println "Adding image ${img.image_name}"
				def path = img.image_path.split("/");
				// TODO:AGRIMM Need to make sure I get size / bundle size correctly here.
                // Do I have to actually fetch and read the manifest?
				def imgSize = walrus_conn.firstRow("""SELECT size sz FROM ImageCache 
								WHERE bucket_name=${ path[0] }
								AND manifest_name=${ path[1] }""").sz.toInteger();
				def ii = null;
				switch ( img.image_type ) {
					case "kernel":
					  ii = new KernelImageInfo( ufn, img.image_name, img.image_name, 
												 "No Description", img.image_path, imgSize, 1,
												 Image.Architecture.valueOf(img.image_arch), Image.Platform.valueOf(img.image_platform) );
					  break;
					case "ramdisk":
					  ii = new RamdiskImageInfo( ufn, img.image_name, img.image_name,
												  "No Description", img.image_path, imgSize, 1,
												  Image.Architecture.valueOf(img.image_arch), Image.Platform.valueOf(img.image_platform) );
					  break;
					case "machine":
					  ii = new MachineImageInfo( ufn, img.image_name, img.image_name,
												  "No Description", img.image_path, imgSize, 1,
												  Image.Architecture.valueOf(img.image_arch), Image.Platform.valueOf(img.image_platform),
												  img.image_kernel_id, img.image_ramdisk_id );
					  break;
				}
				ii.setImagePublic(img.image_is_public);
				ii.setImageType(Image.Type.valueOf(img.image_type));
				ii.setSignature(img.image_signature);
                dbGen.add(ii);
                dbGen.commit();
                gen_conn.rows("""SELECT image_product_code_value FROM image_product_code
                                 JOIN image_has_product_codes USING (image_product_code_id)
                                 WHERE image_id=${ img.image_id }""").each { prodCode ->
                    EntityWrapper<ProductCode> dbPC = EntityWrapper.get(ProductCode.class);
                    dbPC.add(new ProductCode(ii, prodCode.image_product_code_value));
                    dbPC.commit();
                }
                gen_conn.rows("""SELECT * FROM image_authorization
                                 JOIN image_has_user_auth USING (image_auth_id)
                                 WHERE image_id=${ img.image_id }""").each { imgAuth ->
                    EntityWrapper<LaunchPermission> dbLP = EntityWrapper.get(LaunchPermission.class);
                    dbLP.add(new LaunchPermission(ii, imgAuth.image_auth_name));
                    dbLP.commit();
                }
			}

            image_conn.rows("""SELECT * FROM Volume WHERE username=${ it.auth_user_name }""").each { vol ->
                EntityWrapper<Volume> dbVol = EntityWrapper.get(Volume.class);
                def vol_meta = stor_conn.firstRow("""SELECT * FROM Volumes WHERE volume_name=${ vol.displayname }""");
                Volume v = new Volume( ufn, vol.displayname, vol.size, vol_meta.sc_name, vol.cluster, vol.parentsnapshot );
                v.setMappedState(vol.state);
                v.setLocalDevice(vol.localdevice);
                v.setRemoteDevice(vol.remotedevice);
                dbVol.add(v);
                dbVol.commit();
            }
            image_conn.rows("""SELECT * FROM Snapshot WHERE username=${ it.auth_user_name }""").each { snap ->
                EntityWrapper<Snapshot> dbSnap = EntityWrapper.get(Snapshot.class);
                def snap_meta = stor_conn.firstRow("""SELECT * FROM Snapshots WHERE snapshot_name=${ snap.displayname }""");
                def scName = (snap_meta == null) ? null :  snap_meta.sc_name;
                Snapshot s = new Snapshot( ufn, snap.displayname, snap.parentvolume, scName, snap.cluster);
                dbSnap.add(s);
                dbSnap.commit();
            }
		}
        return true;
	}

	public boolean upgradeWalrus() {
		def walrus_conn = StandalonePersistence.getConnection("eucalyptus_walrus");
		walrus_conn.rows('SELECT * FROM Buckets').each{
			println "Adding bucket: ${it.bucket_name}"
			
			EntityWrapper<BucketInfo> dbBucket = EntityWrapper.get(BucketInfo.class);
			try {
				BucketInfo b = new BucketInfo(it.owner_id,it.owner_id,it.bucket_name,it.bucket_creation_date);
				b.setLocation(it.bucket_location);
				b.setGlobalRead(it.global_read);
				b.setGlobalWrite(it.global_write);
				b.setGlobalReadACP(it.global_read_acp);
				b.setGlobalWriteACP(it.global_write_acp);
				b.setBucketSize(it.bucket_size);
				b.setHidden(it.hidden);
				b.setLoggingEnabled(it.logging_enabled);
				b.setTargetBucket(it.target_bucket);
				b.setTargetPrefix(it.target_prefix);
				walrus_conn.rows("""SELECT g.* FROM bucket_has_grants has_thing 
				                    LEFT OUTER JOIN Grants g on g.grant_id=has_thing.grant_id
				                    WHERE has_thing.bucket_id=${ it.bucket_id }""").each{  grant ->
					println "--> grant: ${it.bucket_id}/${grant.user_id}"
					GrantInfo grantInfo = new GrantInfo();
					grantInfo.setUserId(grant.user_id);
					grantInfo.setGrantGroup(grant.grantGroup);
					grantInfo.setCanWrite(grant.allow_write);
					grantInfo.setCanRead(grant.allow_read);
					grantInfo.setCanReadACP(grant.allow_read_acp);
					grantInfo.setCanWriteACP(grant.allow_write_acp);
					b.getGrants().add(grantInfo);
				}
				dbBucket.add(b);
				dbBucket.commit();
			} catch (Throwable t) {
				t.printStackTrace();
				dbBucket.rollback();
				return false;
			}
		}
		walrus_conn.rows('SELECT * FROM Objects').each{
			println "Adding object: ${it.bucket_name}/${it.object_name}"
			EntityWrapper<ObjectInfo> dbObject = EntityWrapper.get(ObjectInfo.class);
			try {
				ObjectInfo objectInfo = new ObjectInfo(it.bucket_name, it.object_key);
				objectInfo.setObjectName(it.object_name);
				objectInfo.setOwnerId(it.owner_id);
				objectInfo.setGlobalRead(it.global_read);
				objectInfo.setGlobalWrite(it.global_write);
				objectInfo.setGlobalReadACP(it.global_read_acp);
				objectInfo.setGlobalWriteACP(it.global_write_acp);
				objectInfo.setSize(it.size);
				objectInfo.setEtag(it.etag);
				objectInfo.setLastModified(it.last_modified);
				objectInfo.setStorageClass(it.storage_class);
				objectInfo.setContentType(it.content_type);
				objectInfo.setContentDisposition(it.content_disposition);
				objectInfo.setDeleted(it.is_deleted);
				objectInfo.setVersionId(it.version_id);
				objectInfo.setLast(it.is_last);
				walrus_conn.rows("""SELECT g.* FROM object_has_grants has_thing 
				                  LEFT OUTER JOIN Grants g on g.grant_id=has_thing.grant_id 
								  WHERE has_thing.object_id=${ it.object_id }""").each{  grant ->
					println "--> grant: ${it.object_name}/${grant.user_id}"
					GrantInfo grantInfo = new GrantInfo();
					grantInfo.setUserId(grant.user_id);
					grantInfo.setGrantGroup(grant.grantGroup);
					grantInfo.setCanWrite(grant.allow_write);
					grantInfo.setCanRead(grant.allow_read);
					grantInfo.setCanReadACP(grant.allow_read_acp);
					grantInfo.setCanWriteACP(grant.allow_write_acp);
					objectInfo.getGrants().add(grantInfo);
				}
				walrus_conn.rows("""SELECT m.* FROM object_has_metadata has_thing 
				                    LEFT OUTER JOIN MetaData m on m.metadata_id=has_thing.metadata_id 
				                    WHERE has_thing.object_id=${ it.object_id }""").each{  metadata ->
					println "--> metadata: ${it.object_name}/${metadata.name}"
					MetaDataInfo mInfo = new MetaDataInfo();
					mInfo.setObjectName(it.object_name);
					mInfo.setName(metadata.name);
					mInfo.setValue(metadata.value);
					objectInfo.getMetaData().add(mInfo);
				}
				dbObject.add(objectInfo);
				dbObject.commit();
			} catch (Throwable t) {
				t.printStackTrace();
				dbObject.rollback();
				return false;
			}
		}
		return true;
	}

	public boolean upgradeNetwork() {
		//Network rules

		def gen_conn = StandalonePersistence.getConnection("eucalyptus_general");
                gen_conn.rows('SELECT * FROM metadata_network_group').each {
                        EntityWrapper<NetworkRulesGroup> dbGen = EntityWrapper.get(NetworkRulesGroup.class);
                        try {
				def account = Accounts.lookupAccountByName(it.metadata_user_name);
                                AccountFullName eucaAfn = new AccountFullName(account);
				def rulesGroup = new NetworkRulesGroup(eucaAfn, it.metadata_user_name + "_" + it.metadata_display_name, it.metadata_network_group_description);
                                println "Adding network rules for ${it.metadata_user_name}/${it.metadata_display_name}";
                                gen_conn.rows("""SELECT r.* 
                                                 FROM metadata_network_group_has_rules 
                                      LEFT OUTER JOIN metadata_network_rule r 
                                                   ON r.metadata_network_rule_id=metadata_network_group_has_rules.metadata_network_rule_id 
                                                WHERE metadata_network_group_has_rules.id=${ it.id }""").each { rule ->
                                        NetworkRule networkRule = new NetworkRule(rule.metadata_network_rule_protocol, 
                                                                                     rule.metadata_network_rule_low_port, 
                                                                                     rule.metadata_network_rule_high_port);
                                        gen_conn.rows("""SELECT ip.* 
                                                         FROM metadata_network_rule_has_ip_range 
                                              LEFT OUTER JOIN metadata_network_rule_ip_range ip
                                                           ON ip.metadata_network_rule_ip_range_id=metadata_network_rule_has_ip_range.metadata_network_rule_ip_range_id 
                                                        WHERE metadata_network_rule_has_ip_range.metadata_network_rule_id=${ rule.metadata_network_rule_id }""").each { iprange ->
                                                IpRange ipRange = new IpRange(iprange.metadata_network_rule_ip_range_value);
						networkRule.getIpRanges().add(ipRange);
                                                println "IP Range: ${iprange.metadata_network_rule_ip_range_value}";
                                        }
                                        gen_conn.rows("""SELECT peer.* 
                                                           FROM metadata_network_rule_has_peer_network 
                                                LEFT OUTER JOIN network_rule_peer_network peer
                                                             ON peer.network_rule_peer_network_id=metadata_network_rule_has_peer_network.metadata_network_rule_peer_network_id 
                                                          WHERE metadata_network_rule_has_peer_network.metadata_network_rule_id=${ rule.metadata_network_rule_id }""").each { peer ->
                                                NetworkPeer networkPeer = new NetworkPeer(peer.network_rule_peer_network_user_query_key, peer.network_rule_peer_network_user_group);
                                                networkRule.getNetworkPeers().add(networkPeer);

                                                println "Peer: " + networkPeer;
                                        }
                    println "Network rule has ip ranges: " + networkRule.getIpRanges();
					rulesGroup.getNetworkRules().add(networkRule);
				} 

                println "adding rules group: " + rulesGroup;
				dbGen.add(rulesGroup);
				dbGen.commit();
			} catch (Throwable t) {
				t.printStackTrace();
				dbGen.rollback();
				return false;
			}
		}
		return true;
	}

	static {
		//this is added by hand because there are special cases/entities that other upgrade scripts will process.
		//In the future, this should be discovered.
		//entities.add(BucketInfo.class);
		//entities.add(ObjectInfo.class);
		//entities.add(GrantInfo.class);

		entities.add(SshKeyPair.class);
		entities.add(VmType.class);

        // eucalyptus_dns
		entities.add(ARecordInfo.class);
		entities.add(CNAMERecordInfo.class);
		entities.add(SOARecordInfo.class);
		entities.add(NSRecordInfo.class);
		entities.add(ZoneInfo.class);

		// eucalyptus_storage
		entities.add(AOEMetaInfo.class);
		entities.add(AOEVolumeInfo.class);
		entities.add(CHAPUserInfo.class);
        entities.add(ISCSIMetaInfo.class);
		entities.add(ISCSIVolumeInfo.class);
		entities.add(SnapshotInfo.class);
		entities.add(VolumeInfo.class);
		entities.add(DirectStorageInfo.class);
		entities.add(StorageInfo.class);
		entities.add(StorageStatsInfo.class);

		// eucalyptus_walrus
		// entities.add(BucketInfo.class);
		// entities.add(GrantInfo.class);
		entities.add(ImageCacheInfo.class);
        // entities.add(MetaDataInfo.class);
		// entities.add(ObjectInfo.class);
		entities.add(TorrentInfo.class);
		entities.add(WalrusInfo.class);
		entities.add(WalrusSnapshotInfo.class);
		entities.add(WalrusStatsInfo.class);

	}
}
