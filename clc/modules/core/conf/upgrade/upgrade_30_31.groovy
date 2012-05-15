import java.io.File;
import java.io.BufferedInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.RuntimeException;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;
import org.hibernate.exception.ConstraintViolationException;

/* 
 * for x in $( grep -r @Table * | sed -r 's!.*main/java/(.*).(java|groovy):.*!\1!' | sed -r 's!/!.!g' ); 
 * do echo import $x\; ; done | sort
 */
import com.eucalyptus.address.Address;
import com.eucalyptus.address.AddressingConfiguration;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.cloud.EucalyptusConfiguration;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.Partition;
import com.eucalyptus.config.ArbitratorConfiguration;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.configurable.StaticDatabasePropertyEntry;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.images.DeviceMapping;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkRule;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.BaseRecord;
import com.eucalyptus.records.LogFileRecord;
import com.eucalyptus.reporting.modules.instance.InstanceAttributes;
import com.eucalyptus.reporting.modules.instance.InstanceUsageSnapshot;
import com.eucalyptus.reporting.modules.s3.S3UsageSnapshot;
import com.eucalyptus.reporting.modules.storage.StorageUsageSnapshot;
import com.eucalyptus.reporting.user.ReportingAccount;
import com.eucalyptus.reporting.user.ReportingUser;
import com.eucalyptus.util.UniqueIds;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.ws.StackConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.AOEMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.ARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.CNAMERecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.DRBDInfo;
import edu.ucsb.eucalyptus.cloud.entities.GrantInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.MetaDataInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.SOARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageStatsInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.TorrentInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusStatsInfo;
import edu.ucsb.eucalyptus.cloud.entities.ZoneInfo;

// Other
import groovy.lang.GroovySystem;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.DatabaseAuthUtils;
import com.eucalyptus.auth.principal.User.RegistrationStatus;

class upgrade_30_31 extends AbstractUpgradeScript {
    static final List<String> FROM_VERSION = ["3.0.0", "3.0.1", "3.0.2"];
    static final List<String> TO_VERSION   = ["3.1.0"];
    private static Logger LOG = Logger.getLogger( upgrade_30_31.class );
    private static List<Class> entities = new ArrayList<Class>();
    private static Map<String, Class> entityMap = new HashMap<String, Class>();

    // Map users to accounts, possible substituting characters to make them "safe"
    private static Map<String, String> safeUserMap = new HashMap<String, String>();
    // Map users to userId of "admin" in their mapped account
    private static Map<String, String> userIdMap = new HashMap<String, String>();
    // Map account names to account ids
    private static Map<String, String> accountIdMap = new HashMap<String, String>();
    private static Map<String, Sql> connMap = new HashMap<String, Sql>();
    private static List<String> unmappedColumns = [ ];

    public upgrade_30_31() {
        super(1);        
    }

    @Override
    public Boolean accepts( String from, String to ) {
        // We should support multiple from versions, but need
        // to decide which ones. 2.0.[1-9](eee)? for example
        if(TO_VERSION.contains(to) && FROM_VERSION.contains(from))
            return true;
        return false;
    }

    @Override
    public void setLogger( Logger log ) {
        LOG = log;
    }

    @Override
    public void upgrade(File oldEucaHome, File newEucaHome) {
        buildConnectionMap();
         
        // Do object upgrades which follow the entity map / setter map pattern
        buildEntityMap();

        Set<String> entityKeys = entityMap.keySet();
        upgradeAuth();
        entityKeys.remove("auth_account"); 
        entityKeys.remove("auth_group");
        entityKeys.remove("auth_user");
        entityKeys.remove("auth_access_key");

        // Hardcode some ordering here
        for (String entityKey : entityKeys) {
            upgradeEntity(entityKey);
        }
        return;
    } 

    private void upgradeAuth() {
        def acctSetterMap = buildSetterMap(connMap['eucalyptus_auth'], "auth_account");
        def groupSetterMap = buildSetterMap(connMap['eucalyptus_auth'], "auth_group");
        def userSetterMap = buildSetterMap(connMap['eucalyptus_auth'], "auth_user");
        def akeySetterMap = buildSetterMap(connMap['eucalyptus_auth'], "auth_access_key");
        def accountProxy = null;
        def authConn = connMap['eucalyptus_auth'];

        authConn.rows("""select * from auth_account""").each { row ->
            EntityWrapper<AccountEntity> db = EntityWrapper.get(AccountEntity.class); 
            AccountEntity acct = AccountEntity.newInstanceWithAccountNumber(row.auth_account_number);
            acct = convertRowToObject('eucalyptus_auth', connMap['eucalyptus_auth'], "auth_account", acctSetterMap, row, acct);
            initMetaClass(acct, AccountEntity.class); 
            LOG.error("setting account number to " + row.auth_account_number);
            acct.setAccountNumber(row.auth_account_number);
            db.add(acct);
            db.commit();
            db = EntityWrapper.get(AccountEntity.class);
            AccountEntity acctEnt = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", acct.getName( ) );
            initMetaClass(acctEnt, AccountEntity.class); 
            acctEnt.setAccountNumber(row.auth_account_number); 
            db.commit()
            
            LOG.error("Getting groups for account " + acctEnt.getAccountNumber());
            db = EntityWrapper.get(AccountEntity.class);
            authConn.rows("""select * 
                                         from auth_group g
                                         join auth_account a on (g.auth_group_owning_account=a.id)
                                        where a.auth_account_name=?""", acct.getName()).each { rowResult ->
                GroupEntity group = GroupEntity.newInstanceWithGroupId(rowResult.auth_group_id_external);
                LOG.error("adding group " + rowResult.auth_group_id_external + " in " + acct.getName());
                group = convertRowToObject('eucalyptus_auth', connMap['eucalyptus_auth'], "auth_group",  groupSetterMap, rowResult, group);
                initMetaClass(group, GroupEntity.class);
                group.setAccount(acctEnt);
                db.recast( GroupEntity.class ).add(group);
            }
            db.commit()

            authConn.rows("""select * 
                             from auth_user u
                             join auth_group_has_users gu on (u.id=gu.auth_user_id) 
                             join auth_group g on (gu.auth_group_id=g.id) 
                             join auth_account a on (g.auth_group_owning_account=a.id)
                            where g.auth_group_user_group = True
                              and a.auth_account_name=?""", acct.getName()).each { rowResult ->
                db = EntityWrapper.get(AccountEntity.class);
                UserEntity user = UserEntity.newInstanceWithUserId(rowResult.auth_user_id_external);
                LOG.error("adding user " + rowResult.auth_user_id_external + " in " + acct.getName());
                user = convertRowToObject('eucalyptus_auth', connMap['eucalyptus_auth'], "auth_user", userSetterMap, rowResult, user);
                initMetaClass(user, UserEntity.class);
                user.setRegistrationStatus(RegistrationStatus.valueOf(rowResult.auth_user_reg_stat));
                GroupEntity userGroup = DatabaseAuthUtils.getUniqueGroup(db, DatabaseAuthUtils.getUserGroupName( rowResult.auth_user_name ), acct.getName( ) );
                user = db.recast( UserEntity.class ).merge( user );
                userGroup = db.recast( GroupEntity.class ).merge( userGroup );
                initMetaClass(user, UserEntity.class);
                initMetaClass(userGroup, GroupEntity.class);
                user.getGroups().add( userGroup );
                userGroup.getUsers().add( user );
                db.commit()

                authConn.rows("""select * from auth_access_key k
                                   join auth_user u on (k.auth_access_key_owning_user=u.id)
                                  where u.auth_user_id_external=?""", rowResult.auth_user_id_external).each { rowResult2 ->
                    db = EntityWrapper.get(AccessKeyEntity.class);
                    AccessKeyEntity accessKey = new AccessKeyEntity(user);
                    accessKey = convertRowToObject('eucalyptus_auth', authConn, "auth_access_key", akeySetterMap, rowResult2, accessKey);
                    initMetaClass(accessKey, AccessKeyEntity.class);
                    accessKey.setSecretKey(rowResult2.auth_access_key_key);
                    accessKey.setAccess(rowResult2.auth_access_key_query_id);
                    db.add(accessKey);
                    db.commit();
                }
            }
        } 
        LOG.error("sleeping");
        // sleep 3600000;
        LOG.error("done sleeping");
    }

    private void upgradeEntity(entityKey) {
        def optionalTables = [ 'das_info' ]
        String contextName = getContextName(entityKey);
        if (optionalTables.contains(entityKey) && !has_table(contextName, entityKey)) {
            LOG.info("table ${entityKey} does not exist; skipping.");
            return;
        }
        Sql conn = connMap[contextName];
        if (conn != null) {
            Map<String, Method> setterMap = buildSetterMap(conn, entityKey);
            if(setterMap != null)
                 doUpgrade(contextName, conn, entityKey, setterMap);
        } else {
            LOG.error("Failed to get connection to " + contextName);
        }
        return;
    }

    private Sql getConnection(String contextName) {
        try {
            Sql conn = StandalonePersistence.getConnection(contextName);
            if (conn == null) {
                LOG.warn("Connection for ${contextName} is null; this could cause errors.");
                sleep(60);
            }
            return conn;
        } catch (SQLException e) {
            LOG.error(e);
            // return null;
            throw(e);
        }
    }

    private void buildConnectionMap() {
        def dbList = [ "eucalyptus_auth", "eucalyptus_dns", "eucalyptus_records",
                       "eucalyptus_vmwarebroker", "eucalyptus_cloud",
                       "eucalyptus_faults", "eucalyptus_reporting",
                       "eucalyptus_walrus", "eucalyptus_config",
                       "eucalyptus_general", "eucalyptus_storage"                   
                     ];
        for (String db : dbList) {
            connMap.put(db, getConnection(db));
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
            LOG.error("Got " + rowResults.size().toString() + " results from " + entityKey);
            EntityWrapper db =  EntityWrapper.get(entityMap.get(entityKey));
                        
            def dest = null;
            for (GroovyRowResult rowResult : rowResults) {
                try {
                    dest = ClassLoader.getSystemClassLoader().loadClass(entityMap.get(entityKey).getCanonicalName()).newInstance();
                    dest = convertRowToObject(contextName, conn, entityKey, setterMap, rowResult, dest);
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
                db.add(dest);
            }
            LOG.debug("Upgraded: " + entityKey);
            db.commit();
        } catch (SQLException e) {
            LOG.error(e); 
            throw e;
        }        
    }

    private Object convertRowToObject(String contextName, Sql conn, String entityKey, Map<String, Method> setterMap, GroovyRowResult rowResult, Object dest) {
        Set<String> columns = rowResult.keySet();
        columns.each{ c -> LOG.debug("column: " + c); }
        for (String column : columns) {
            Method setter = setterMap.get(column);
            if(setter == null) {
                def loweredColumn = column.toLowerCase();
                LOG.debug("No setter for " + column + ", trying " + loweredColumn);
                setter = setterMap.get(loweredColumn);
            }
            if(setter != null) {
                Object o = rowResult.get(column);
                if(o != null) {
                    try {
                        if(dest instanceof Volume && (setter.getName().equals("setRemoteDevice"))) {
                            ((Volume)dest).setRemoteDevice(null);
                        } else {
                            setter.setAccessible(true);
                            setter.invoke(dest, o);
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.error(dest.getClass().getName()  + " " + column + " " + e);
                    } catch (IllegalAccessException e) {
                        LOG.error(dest.getClass().getName()  + " " + column + " " + e);
                    } catch (InvocationTargetException e) {
                        LOG.error(dest.getClass().getName()  + " " + column + " " + e);
                    }
                } else {
                    LOG.debug("Column " + column + " was NULL");
                }
            } else {
                LOG.debug("Setter for " + column + " was NULL");
            }
        }

        return dest;
    }

    private Map<String, Method> buildSetterMap(Sql conn, String entityKey) {
        Map<String, Method> setterMap = new HashMap<String, Method>();
        try {
            Object firstRow = conn.firstRow("SELECT * FROM " + entityKey);
            if(firstRow == null) {
                LOG.info("Unable to find anything in table: " + entityKey);
                return null;
            }
            if(firstRow instanceof Map) {
                Set<String> origColumns = ((Map) firstRow).keySet();
                Class definingClass = entityMap.get(entityKey);
                Field[] fields = definingClass.getDeclaredFields();
                //special case. Do this better.
                addToSetterMap(setterMap, origColumns, definingClass, fields);                
                Class superClass = entityMap.get(entityKey).getSuperclass();                
                while(superClass.isAnnotationPresent(MappedSuperclass.class)) {
                    Field[] superFields = superClass.getDeclaredFields();
                    addToSetterMap(setterMap, origColumns, superClass, superFields);
                    //nothing to see here (otherwise we loop forever).
                    if(superClass.equals(AbstractPersistent.class))
                        break;
                    superClass = superClass.getSuperclass();
                }
                for (String column : origColumns) {
                    if(!setterMap.containsKey(column) && !unmappedColumns.contains("${entityKey}.${column}".toString()) ) {
                        LOG.info("No corresponding field for column: ${entityKey}.${column} found");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error(e);
            throw e;
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
        for (Class entity : entities) {
            if (entity.isAnnotationPresent(Table.class)) {
                // This only handles tables whose names have not changed.
                Table annot = (Table)entity.getAnnotation(Table.class);
                entityMap.put(annot.name(), entity);
            }
        }
    }

    public void initMetaClass(obj, theClass) {
        /* This is the "magic" which ensures that objects are not incorrectly
         * mapped to the LogFileRecord metaClass.  We believe this to be a
         * bug in Groovy when used with JPA.
         */
        def emc = new ExpandoMetaClass( theClass, false );
        emc.initialize();
        obj.metaClass = emc;
    }

    static {
        // This is the list of entities which do not need special handling.

        entities.add(AccessKeyEntity.class)
        entities.add(AccountEntity.class)
        // entities.add(Address.class)
        entities.add(AddressingConfiguration.class)
        entities.add(AOEMetaInfo.class)
        entities.add(AOEVolumeInfo.class)
        entities.add(BaseRecord.class)
        entities.add(BucketInfo.class)
        entities.add(CertificateEntity.class)
        entities.add(CHAPUserInfo.class)
        // entities.add(ClusterConfiguration.class)
        entities.add(Clusters.class)
        entities.add(CNAMERecordInfo.class)
        entities.add(ComponentConfiguration.class)
        entities.add(ConditionEntity.class)
        entities.add(DeviceMapping.class)
        entities.add(DirectStorageInfo.class)
        entities.add(DRBDInfo.class)
        // entities.add(EucalyptusConfiguration.class)
        entities.add(ExtantNetwork.class)
        entities.add(Faults.class)
        entities.add(GrantInfo.class)
        entities.add(GroupEntity.class)
        entities.add(ImageCacheInfo.class)
        entities.add(ImageConfiguration.class)
        // entities.add(ImageInfo.class)
        // entities.add(InstanceAttributes.class)
        // entities.add(InstanceUsageSnapshot.class)
        entities.add(ISCSIMetaInfo.class)
        entities.add(ISCSIVolumeInfo.class)
        entities.add(LogFileRecord.class)
        entities.add(MetaDataInfo.class)
        entities.add(NetworkGroup.class)
        entities.add(NetworkRule.class)
        entities.add(NSRecordInfo.class)
        entities.add(ObjectInfo.class)
        // Caused by: java.lang.NullPointerException
        // entities.add(Partition.class)
        entities.add(PolicyEntity.class)
        entities.add(PrivateNetworkIndex.class)
        // entities.add(ReportingAccount.class)
        // entities.add(ReportingUser.class)
        // entities.add(S3UsageSnapshot.class)
        // entities.add(Snapshot.class)
        entities.add(SnapshotInfo.class)
        entities.add(SOARecordInfo.class)
        // entities.add(SshKeyPair.class)
        entities.add(StackConfiguration.class)
        entities.add(StatementEntity.class)
        // Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.eucalyptus.configurable.StaticDatabasePropertyEntry.fieldName
        // entities.add(StaticDatabasePropertyEntry.class)
        // com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Table 'eucalyptus_config.config_sc' doesn't exist
        // entities.add(StorageControllerConfiguration.class)
        entities.add(StorageInfo.class)
        entities.add(StorageStatsInfo.class)
        // Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.eucalyptus.reporting.modules.storage.StorageUsageSnapshot.key
        // entities.add(StorageUsageSnapshot.class)
        entities.add(SystemConfiguration.class)
        entities.add(TorrentInfo.class)
        entities.add(UniqueIds.class)
        entities.add(UserEntity.class)
        entities.add(VmInstance.class)
        entities.add(VmType.class)
        // Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.eucalyptus.blockstorage.Volume.uniqueName
        // entities.add(Volume.class)
        entities.add(VolumeInfo.class)
        // com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Table 'eucalyptus_config.config_walrus' doesn't exist
        // entities.add(WalrusConfiguration.class)
        entities.add(WalrusInfo.class)
        entities.add(WalrusSnapshotInfo.class)
        entities.add(WalrusStatsInfo.class)
        entities.add(ZoneInfo.class)

    }
}
