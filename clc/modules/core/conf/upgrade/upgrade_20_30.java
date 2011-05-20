// DNS
import edu.ucsb.eucalyptus.cloud.entities.ARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.CNAMERecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.SOARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ZoneInfo;

// Records
import com.eucalyptus.records.LogFileRecord;
import com.eucalyptus.records.BaseRecord;

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
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.vm.VmType;

// Other
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

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

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.eucalyptus.address.Address;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.Counters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.upgrade.UpgradeScript;

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
		if(FROM_VERSION.equals(from) && TO_VERSION.equals(to))
			return true;
		return false;
	}

	@Override
	public void upgrade(File oldEucaHome, File newEucaHome) {
		buildEntityMap();
		def altEntityMap = [ metadata_keypair:'eucalyptus_general',
				metadata_network_group:'eucalyptus_general',
				metadata_network_group_has_rules:'eucalyptus_general',
				metadata_network_rule:'eucalyptus_general',
				metadata_network_rule_has_ip_range:'eucalyptus_general',
				metadata_network_rule_has_peer_network:'eucalyptus_general',
				metadata_network_rule_ip_range:'eucalyptus_general',
				network_rule_peer_network:'eucalyptus_general',
				vm_types:'eucalyptus_general' ];


		Set<String> entityKeys = entityMap.keySet();
		for (String entityKey : entityKeys) {
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
			if(setterMap.containsKey(column)) {
				LOG.debug(column + " is set by: " + setterMap.get(column).getName());
			} 
		}
	}

	private void buildEntityMap() {
                // Note that this maps new -> old
		def tableMap = [ metadata_network_rule_peer_network:'network_rule_peer_network',
				metadata_keypairs:'metadata_keypair',
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

	static {
		//this is added by hand because there are special cases/entities that other upgrade scripts will process.
		//In the future, this should be discovered.
		//entities.add(BucketInfo.class);
		//entities.add(ObjectInfo.class);
		//entities.add(GrantInfo.class);

                // eucalyptus_cloud (some from other DBs)
		entities.add(IpRange.class);
		entities.add(NetworkPeer.class);
		entities.add(NetworkRule.class);
		entities.add(NetworkRulesGroup.class);
		entities.add(SshKeyPair.class);
		entities.add(VmType.class);

                // eucalyptus_dns
		entities.add(ARecordInfo.class);
		entities.add(CNAMERecordInfo.class);
		entities.add(SOARecordInfo.class);
		entities.add(NSRecordInfo.class);
		entities.add(ZoneInfo.class);

		// eucalyptus_records
		entities.add(BaseRecord.class);
		entities.add(LogFileRecord.class);

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
		entities.add(BucketInfo.class);
		entities.add(GrantInfo.class);
		entities.add(ImageCacheInfo.class);
                entities.add(MetaDataInfo.class);
		entities.add(ObjectInfo.class);
		entities.add(TorrentInfo.class);
		entities.add(WalrusInfo.class);
		entities.add(WalrusSnapshotInfo.class);
		entities.add(WalrusStatsInfo.class);

	}
}
