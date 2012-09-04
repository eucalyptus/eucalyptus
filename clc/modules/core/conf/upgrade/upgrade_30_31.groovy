import groovy.lang.GroovyRuntimeException;
import java.io.File;
import java.io.BufferedInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

// EUARE classes
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.DatabaseAuthUtils;
import com.eucalyptus.auth.DatabaseAccountProxy;
import com.eucalyptus.auth.DatabaseGroupProxy;
import com.eucalyptus.auth.DatabaseUserProxy;

// Enums
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.cloud.util.Reference;

// Reporting classes
import com.eucalyptus.reporting.modules.storage.StorageSnapshotKey;
import com.eucalyptus.reporting.modules.storage.StorageUsageData;
import com.eucalyptus.reporting.modules.s3.S3SnapshotKey;
import com.eucalyptus.reporting.modules.s3.S3UsageData;

// Network classes
import com.eucalyptus.network.NetworkGroups;

// Images
import com.eucalyptus.images.KernelImageInfo;
import com.eucalyptus.images.MachineImageInfo;
import com.eucalyptus.images.RamdiskImageInfo;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.EphemeralDeviceMapping;
import com.eucalyptus.images.SuppressDeviceMappping;
import com.eucalyptus.images.BlockStorageDeviceMapping;

import com.eucalyptus.images.BlockStorageDeviceMapping;

import com.eucalyptus.util.BlockStorageUtil;

// Perform discovery for EUARE policy maps
import com.eucalyptus.auth.policy.condition.ConditionOpDiscovery;
import com.eucalyptus.auth.policy.key.KeyDiscovery;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

class upgrade_30_31 extends AbstractUpgradeScript {
    static final List<String> FROM_VERSION = ["3.0.0", "3.0.1", "3.0.2"];
    static final List<String> TO_VERSION   = ["3.1.0", "3.1.1", "3.1.2"];
    private static Logger LOG = Logger.getLogger( upgrade_30_31.class );
    private static List<Class> entities = new ArrayList<Class>();
    private static Map<String, Class> entityMap = new HashMap<String, Class>();

    // Map user ids to accounts / user
    private static Map<String, List> realUserMap = new HashMap<String, ArrayList<String>>();
    // Map user id to account / user
    private static Map<String, List> phantomUserMap = new HashMap<String, ArrayList<String>>();
    private static Map<String, String> phantomAcctMap = new HashMap<String, String>();
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
		ServiceJarDiscovery.runDiscovery(new ConditionOpDiscovery());
		ServiceJarDiscovery.runDiscovery(new KeyDiscovery());
		
        buildConnectionMap();

        // VMware
        entities.add(Class.forName("com.eucalyptus.cloud.ws.BrokerVolumeInfo"));
        entities.add(Class.forName("com.eucalyptus.cloud.ws.BrokerGroupInfo"));
        entities.add(Class.forName("com.eucalyptus.broker.vmware.VMwareBrokerInfo"));

        // SAN
        entities.add(Class.forName("edu.ucsb.eucalyptus.cloud.entities.SANInfo"));
        entities.add(Class.forName("edu.ucsb.eucalyptus.cloud.entities.DASInfo"));
        entities.add(Class.forName("edu.ucsb.eucalyptus.cloud.entities.IgroupInfo"));
        entities.add(Class.forName("edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo"));
        entities.add(Class.forName("edu.ucsb.eucalyptus.cloud.entities.NetappInfo"));

        // Do object upgrades which follow the entity map / setter map pattern
        buildEntityMap();

        Set<String> entityKeys = entityMap.keySet();
        upgradeAuth();
        entityKeys.remove("auth_account");
        entityKeys.remove("auth_group");
        entityKeys.remove("auth_user");
        entityKeys.remove("auth_access_key");
        entityKeys.remove("auth_cert");
        entityKeys.remove("auth_policy");
        entityKeys.remove("auth_statement");
        addPhantoms();

        upgradeNetwork();
        upgradeWalrus();
        upgradeISCSIVolumeInfo();
        entityKeys.remove("ISCSIVolumeInfo");
        upgradeMisc();
        entityKeys.remove("CHAPUserInfo");
        upgradeComponents();
        entityKeys.remove("config_partition");
        upgradeImages();
        entityKeys.remove("metadata_images");
        entityKeys.remove("kernel_images");
        entityKeys.remove("ramdisk_images");
        entityKeys.remove("machine_images");
        entityKeys.remove("blockstorage_images");
        entityKeys.remove("metadata_device_mappings");
        entityKeys.remove("blockstorage_mappings");
        entityKeys.remove("ephemeral_mappings");
        entityKeys.remove("suppress_mappings");

        upgradeSANVolumeInfo();
        entityKeys.remove("EquallogicVolumeInfo");

        // Hardcode some ordering here
        for (String entityKey : entityKeys) {
            upgradeEntity(entityKey);
        }

        deletePhantoms();

        return;
    }

    private void upgradeComponents() {
        def confConn = connMap['eucalyptus_config'];

        def partSetterMap = buildSetterMap(confConn, "config_partition");
        doUpgrade('eucalyptus_config', confConn, "config_partition", partSetterMap);

        def compSetterMap = buildSetterMap(confConn, "config_component_base");
        def componentMap = new HashMap<String, Class>();
        componentMap.put("ArbitratorConfiguration", Class.forName("com.eucalyptus.config.ArbitratorConfiguration"));
        componentMap.put("EucalyptusConfiguration", Class.forName("com.eucalyptus.cloud.EucalyptusConfiguration"));
        componentMap.put("ClusterConfiguration", Class.forName("com.eucalyptus.cluster.ClusterConfiguration"));
        componentMap.put("StorageControllerConfiguration", Class.forName("com.eucalyptus.config.StorageControllerConfiguration"));
        componentMap.put("WalrusConfiguration", Class.forName("com.eucalyptus.config.WalrusConfiguration"));
        componentMap.put("VMwareBrokerConfiguration", Class.forName("com.eucalyptus.broker.vmware.VMwareBrokerConfiguration"));

        confConn.rows("""select * from config_component_base""").each { row ->
            Class componentType = componentMap[row.DTYPE];
            EntityWrapper<ComponentConfiguration> db = EntityWrapper.get(componentType);
            db.recast(componentType);
            def comp= componentType.newInstance();
            initMetaClass(comp, componentType);
            comp = convertRowToObject(compSetterMap, row, comp);
            if (comp instanceof ClusterConfiguration) {
                comp.setNetworkMode( row.cluster_network_mode );
                comp.setUseNetworkTags( row.cluster_use_network_tags );
                comp.setMinNetworkTag( row.cluster_min_network_tag );
                comp.setMaxNetworkTag( row.cluster_max_network_tag );
                comp.setMinNetworkIndex( row.cluster_min_addr );
                comp.setMaxNetworkIndex( row.cluster_min_vlan );
                comp.setAddressesPerNetwork( row.cluster_addrs_per_net );
                comp.setVnetSubnet( row.cluster_vnet_subnet );
                comp.setVnetNetmask( row.cluster_vnet_netmask );
                comp.setVnetType( row.cluster_vnet_type );
                // comp.setPropertyPrefix( row.cluster_property_prefix );
                comp.setSourceHostName( row.cluster_alt_source_hostname );
            }

            db.add(comp);
            db.commit();
        }
    }


    private void upgradeAuth() {
        def authConn = connMap['eucalyptus_auth'];
        def acctSetterMap = buildSetterMap(authConn, "auth_account");
        def groupSetterMap = buildSetterMap(authConn, "auth_group");
        def userSetterMap = buildSetterMap(authConn, "auth_user");
        def akeySetterMap = buildSetterMap(authConn, "auth_access_key");
        def certSetterMap = buildSetterMap(authConn, "auth_cert");

        authConn.rows("""select * from auth_account""").each { row ->
            EntityWrapper<AccountEntity> db = EntityWrapper.get(AccountEntity.class);
            AccountEntity acct = AccountEntity.newInstanceWithAccountNumber(row.auth_account_number);
            acct = convertRowToObject(acctSetterMap, row, acct);
            initMetaClass(acct, AccountEntity.class);
            LOG.info("setting account number to " + row.auth_account_number);
            acct.setAccountNumber(row.auth_account_number);
            db.add(acct);
            db.commit();
            db = EntityWrapper.get(AccountEntity.class);
            AccountEntity acctEnt = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", acct.getName( ) );
            initMetaClass(acctEnt, AccountEntity.class);
            acctEnt.setAccountNumber(row.auth_account_number);
            db.commit()

            LOG.info("Getting groups for account " + acctEnt.getAccountNumber());
            authConn.rows("""select g.*
                               from auth_group g
                               join auth_account a on (g.auth_group_owning_account=a.id)
                              where a.auth_account_name=?""", acct.getName()).each { rowResult ->
                db = EntityWrapper.get(GroupEntity.class);
                GroupEntity group = GroupEntity.newInstanceWithGroupId(rowResult.auth_group_id_external);
                LOG.info("adding group " + rowResult.auth_group_id_external + " in " + acct.getName());
                group = convertRowToObject(groupSetterMap, rowResult, group);
                initMetaClass(group, GroupEntity.class);
                group.setAccount(acctEnt);
                db.add(group);
                db.commit()

                authConn.rows("""select * from auth_policy
                                  where auth_policy_owning_group=?""", rowResult.id).each { rowResult2 ->
                    DatabaseGroupProxy groupProxy = new DatabaseGroupProxy(group);
                    groupProxy.addPolicy(rowResult2.auth_policy_name, rowResult2.auth_policy_text);
                }
            }

            authConn.rows("""select u.*
                             from auth_user u
                             join auth_group_has_users gu on (u.id=gu.auth_user_id)
                             join auth_group g on (gu.auth_group_id=g.id)
                             join auth_account a on (g.auth_group_owning_account=a.id)
                            where g.auth_group_user_group = True
                              and a.auth_account_name=?""", acct.getName()).each { rowResult ->
                db = EntityWrapper.get(AccountEntity.class);
                UserEntity user = UserEntity.newInstanceWithUserId(rowResult.auth_user_id_external);
                try {
                  LOG.info("adding user " + rowResult.auth_user_id_external + " in " + acct.getName());
                  user = convertRowToObject(userSetterMap, rowResult, user);
                  initMetaClass(user, UserEntity.class);
                  user.setRegistrationStatus(RegistrationStatus.valueOf(rowResult.auth_user_reg_stat));
                  GroupEntity userGroup = DatabaseAuthUtils.getUniqueGroup(db, DatabaseAuthUtils.getUserGroupName( rowResult.auth_user_name ), acct.getName( ) );
                  user = db.recast( UserEntity.class ).merge( user );
                  userGroup = db.recast( GroupEntity.class ).merge( userGroup );
                  initMetaClass(user, UserEntity.class);
                  initMetaClass(userGroup, GroupEntity.class);
                  user.getGroups().add( userGroup );
                  userGroup.getUsers().add( user );
                } catch (org.hibernate.NonUniqueResultException e) {
                  LOG.warn("Skipping non-unique user ${rowResult.auth_user_name}");
                  db.rollback();
                  return;
                }

                authConn.rows("""select auth_group_id_external,auth_group_name
                                   from auth_group g
                                   join auth_group_has_users gu on (g.id=gu.auth_group_id)
                                   join auth_user u on (u.id=gu.auth_user_id)
                                  where u.auth_user_id_external=?
                                    and auth_group_user_group=False""", rowResult.auth_user_id_external).each { rowResult2 ->
                    GroupEntity extraGroup = DatabaseAuthUtils.getUniqueGroup(db, rowResult2.auth_group_name, acct.getName( ) );
                    LOG.debug("Adding user ${rowResult.auth_user_id_external} ( ${rowResult.auth_user_name} ) to group ${rowResult2.auth_group_name}");
                    initMetaClass(extraGroup, GroupEntity.class);
                    user.getGroups().add( extraGroup );
                    extraGroup.getUsers().add( user );
                }
                db.commit()

                realUserMap.put(rowResult.auth_user_id_external, [ rowResult.auth_user_name,
                                                                   row.auth_account_name,
                                                                   row.auth_account_number ])

                db = EntityWrapper.get(AccessKeyEntity.class);
                authConn.rows("""select * from auth_access_key k
                                  where k.auth_access_key_owning_user=?""", rowResult.id).each { rowResult2 ->
                    AccessKeyEntity accessKey = new AccessKeyEntity(user);
                    accessKey = convertRowToObject(akeySetterMap, rowResult2, accessKey);
                    initMetaClass(accessKey, AccessKeyEntity.class);
                    accessKey.setSecretKey(rowResult2.auth_access_key_key);
                    accessKey.setAccess(rowResult2.auth_access_key_query_id);
                    db.add(accessKey);
                }
                db.commit();

                def userDelegate = new DatabaseUserProxy(user);
                initMetaClass(userDelegate, userDelegate.class);
                Map<String, String> info = new HashMap<String, String>( );
                authConn.rows("""select * from auth_user_info_map
                                  where userentity_id=?""", rowResult.id).each { infoRow ->
                    info.put(infoRow.auth_user_info_key, infoRow.auth_user_info_value);
                }
                userDelegate.setInfo(info)

                db = EntityWrapper.get(CertificateEntity.class);
                authConn.rows("""select c.* from auth_cert c
                                   join auth_user u on (c.auth_certificate_owning_user=u.id)
                                  where u.auth_user_id_external=?""", rowResult.auth_user_id_external).each { rowResult3 ->
                    CertificateEntity cert = CertificateEntity.newInstanceWithId(rowResult3.auth_certificate_id);
                    cert = convertRowToObject(certSetterMap, rowResult3, cert);
                    initMetaClass(cert, CertificateEntity.class);
                    cert.setUser(user);
                    db.add(cert);
                }
                db.commit();
            }
        }
    }

    private void addPhantoms() {
        def walrusconn = connMap['eucalyptus_walrus'];

        // I would not expect some of these tables to have data during upgrade,
        // but better safe than sorry.
        for (String table : [ 'metadata_addresses',
                              'metadata_images',
                              'metadata_instances',
                              'metadata_keypairs',
                              'metadata_network_group',
                              'metadata_network_indices',
                              'metadata_snapshots',
                              'metadata_volumes']) {
            connMap['eucalyptus_cloud'].rows("""select distinct metadata_user_name,metadata_user_id,metadata_account_id,metadata_account_name from """ + table).each {
                if (!phantomUserMap.containsKey(it.metadata_user_id) &&
                    !realUserMap.containsKey(it.metadata_user_id) &&
                    (it.metadata_account_id != '000000000001')) {
                    addPhantom(it.metadata_user_id,
                               it.metadata_user_name,
                               it.metadata_account_id,
                               it.metadata_account_name);
                    phantomUserMap.put(it.metadata_user_id,
                                       [it.metadata_user_name,
                                        it.metadata_account_name,
                                        it.metadata_account_id])
                }
            }
        }
    }

    void addPhantom(userid, username, acctid, acctname) {
        // DatabaseAccountProxy.addUser won't work, because we need
        // to set the user id.  This code is otherwise equivalent
        // to that function.
        UserEntity newUser = UserEntity.newInstanceWithUserId(userid);
        initMetaClass(newUser, newUser.class);
        newUser.setPath("/");
        newUser.setName(username);
        newUser.setEnabled(true);
        newUser.setRegistrationStatus( User.RegistrationStatus.CONFIRMED );
        GroupEntity newGroup = new GroupEntity( DatabaseAuthUtils.getUserGroupName( username ) );
        initMetaClass(newGroup, newGroup.class);
        newGroup.setUserGroup( true );
        EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
        try {
            AccountEntity account = null;
            try {
                account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", acctname );
                initMetaClass(account, AccountEntity.class);
            } catch (java.util.NoSuchElementException e) {
                account = AccountEntity.newInstanceWithAccountNumber(acctid);
                initMetaClass(account, AccountEntity.class);
                phantomAcctMap.put(acctid, acctname);
                account.setName(acctname);
                db.add(account);
            }

            newGroup = db.recast( GroupEntity.class ).merge( newGroup );
            initMetaClass(newGroup, newGroup.class);
            newUser = db.recast( UserEntity.class ).merge( newUser );
            initMetaClass(newUser, newUser.class);
            newGroup.setAccount( account );
            newGroup.getUsers( ).add( newUser );
            newUser.getGroups( ).add( newGroup );
            db.commit( );
        } catch ( Exception e ) {
            LOG.error( "Failed to add user: " + username + " in " + acctname );
            db.rollback( );
            throw e;
        }
    }

    private void deletePhantoms() {
        for (String userId : phantomUserMap.keySet()) {
            // deleteUser
            EntityWrapper<AccountEntity> db = EntityWrapper.get(AccountEntity.class);
            AccountEntity acctEnt = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", phantomUserMap[userId][1] );
            initMetaClass(acctEnt, AccountEntity.class);
            Account acct = new DatabaseAccountProxy(acctEnt);
            acct.deleteUser(phantomUserMap[userId][0], true, true);
        }
        for (String acctId : phantomAcctMap.keySet()) {
            Accounts.deleteAccount(phantomAcctMap[acctId], false, true);
        }
    }

    public boolean upgradeWalrus() {
        connMap['eucalyptus_walrus'].rows('SELECT * FROM Buckets').each{
            LOG.debug("Adding bucket: ${it.bucket_name}");

            EntityWrapper<BucketInfo> dbBucket = EntityWrapper.get(BucketInfo.class);
            try {
                BucketInfo b = new BucketInfo(it.owner_id, it.user_id,it.bucket_name,it.bucket_creation_date);
                initMetaClass(b, b.class);
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
                connMap['eucalyptus_walrus'].rows("""SELECT g.* FROM bucket_has_grants has_thing
                                    LEFT OUTER JOIN Grants g on g.id=has_thing.grant_id
                                    WHERE has_thing.bucket_id=?""", [ it.id ]).each{  grant ->
                    LOG.debug("--> grant: ${it.id}/${grant.user_id}");
                    GrantInfo grantInfo = new GrantInfo();
                                        initMetaClass(grantInfo, grantInfo.class);

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
            } catch (Exception t) {
                t.printStackTrace();
                dbBucket.rollback();
                throw t;
            }
        }
        connMap['eucalyptus_walrus'].rows('SELECT * FROM Objects').each{
            LOG.debug("Adding object: ${it.bucket_name}/${it.object_name}");
            EntityWrapper<ObjectInfo> dbObject = EntityWrapper.get(ObjectInfo.class);
            try {
                ObjectInfo objectInfo = new ObjectInfo(it.bucket_name, it.object_key);
                                initMetaClass(objectInfo, objectInfo.class);
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
                connMap['eucalyptus_walrus'].rows("""SELECT g.* FROM object_has_grants has_thing
                                    LEFT OUTER JOIN Grants g on g.id=has_thing.grant_id
                                    WHERE has_thing.object_id=?""", [ it.id ]).each{  grant ->
                    LOG.debug("--> grant: ${it.object_name}/${grant.user_id}")
                    GrantInfo grantInfo = new GrantInfo();
                    initMetaClass(grantInfo, grantInfo.class);
                    grantInfo.setUserId(grant.user_id);
                    grantInfo.setGrantGroup(grant.grantGroup);
                    grantInfo.setCanWrite(grant.allow_write);
                    grantInfo.setCanRead(grant.allow_read);
                    grantInfo.setCanReadACP(grant.allow_read_acp);
                    grantInfo.setCanWriteACP(grant.allow_write_acp);
                    objectInfo.getGrants().add(grantInfo);
                }
                connMap['eucalyptus_walrus'].rows("""SELECT m.* FROM object_has_metadata has_thing
                                    LEFT OUTER JOIN MetaData m on m.id=has_thing.metadata_id
                                    WHERE has_thing.object_id=?""", [ it.id ]).each{  metadata ->
                    LOG.debug("--> metadata: ${it.object_name}/${metadata.name}")
                    MetaDataInfo mInfo = new MetaDataInfo();
                    initMetaClass(mInfo, mInfo.class);
                    mInfo.setObjectName(it.object_name);
                    mInfo.setName(metadata.name);
                    mInfo.setValue(metadata.value);
                    objectInfo.getMetaData().add(mInfo);
                }
                dbObject.add(objectInfo);
                dbObject.commit();
            } catch (Exception t) {
                t.printStackTrace();
                dbObject.rollback();
                throw t;
            }
        }
        return true;
    }

    public boolean upgradeNetwork() {
        // I wish I could handle CollectionTables in a more generic way, but this is the most expedient for now.
        def conn = connMap['eucalyptus_cloud'];
        conn.rows('SELECT * FROM metadata_network_group').each {
            EntityWrapper<NetworkGroup> dbGen = EntityWrapper.get(NetworkGroup.class);
            try {
                User user = Accounts.lookupUserById( it.metadata_user_id );
                UserFullName ufn = new UserFullName(user);
                def rulesGroup = new NetworkGroup(ufn, it.metadata_display_name, it.metadata_network_group_description);
                initMetaClass(rulesGroup, rulesGroup.class);
                rulesGroup.setDisplayName(it.metadata_display_name);
                LOG.debug("Adding network rules for ${ it.metadata_user_name }/${it.metadata_display_name}");
                conn.rows("""SELECT *
                             FROM metadata_network_rule
                             WHERE metadata_network_group_rule_fk=?""", [ it.id ]).each { rule ->
                    Collection<String> ipRanges = conn.rows("""SELECT *
                                     FROM metadata_network_rule_ip_ranges
                                     WHERE NetworkRule_id=?""", [ rule.id ]).collect { iprange -> iprange.ipRanges; }
                    def peers = ArrayListMultimap.create() as Multimap<String, String>;
                    conn.rows("""SELECT *
                                 FROM metadata_network_group_rule_peers
                                 WHERE metadata_network_group_rule_peers.NetworkRule_id=?""", [ rule.id ]).each { peer ->
                        peers.put(peer.network_rule_peer_network_user_query_key, peer.network_rule_peer_network_user_group);
                        // LOG.debug("Peer: " + networkPeer);
                    }
                    try {
                        NetworkRule networkRule = NetworkRule.create(rule.metadata_network_rule_protocol.toLowerCase(),
                                                                  rule.metadata_network_rule_low_port,
                                                                  [rule.metadata_network_rule_high_port, 65535].min(),
                                                                  peers as Multimap<String, String>,
                                                                  ipRanges as Collection<String>);
                        initMetaClass(networkRule, networkRule.class);
                        rulesGroup.getNetworkRules().add(networkRule);
                    } catch(IllegalArgumentException e) {
                        LOG.warn("Ignored invalid network rule: protocol ${rule.metadata_network_rule_protocol}, ports ${rule.metadata_network_rule_low_port} to ${rule.metadata_network_rule_high_port}");
                    }
                }

                LOG.debug("adding rules group: " + rulesGroup);
                dbGen.add(rulesGroup);
                dbGen.commit();
            } catch (Exception t) {
                t.printStackTrace();
                dbGen.rollback();
                throw t;
            }
        }
        return true;
    }

    private void upgradeImages() {
        def connCloud = connMap['eucalyptus_cloud'];
        def kimgSetterMap = buildSetterMap(connCloud, "kernel_images");
        def rimgSetterMap = buildSetterMap(connCloud, "ramdisk_images");
        def mimgSetterMap = buildSetterMap(connCloud, "machine_images");
        def bimgSetterMap = buildSetterMap(connCloud, "blockstorage_images");
        def bsdmSetterMap = buildSetterMap(connCloud, "blockstorage_mappings");
        def edmSetterMap = buildSetterMap(connCloud, "ephemeral_mappings");
        def sdmSetterMap = buildSetterMap(connCloud, "suppress_mappings");

        EntityWrapper<ImageInfo> dbGen = EntityWrapper.get(ImageInfo.class);
        connMap['eucalyptus_cloud'].rows("SELECT * FROM metadata_images").each { img ->
            User user = null;
            try {
                user = Accounts.lookupUserById( img.metadata_user_id );
            } catch (Exception e) {
                // leave user as null, which translates to "nobody"
            }
            UserFullName ufn = UserFullName.getInstance(user, "");
            def ii = null;
            switch (img.metadata_image_discriminator) {
                case "kernel":
                    ii = new KernelImageInfo();
                    initMetaClass(ii, ii.class);
                    ii = convertRowToObject(kimgSetterMap, img, ii);
                    break;
                case "ramdisk":
                    ii = new RamdiskImageInfo();
                    initMetaClass(ii, ii.class);
                    ii = convertRowToObject(rimgSetterMap, img, ii);
                    break;
                case "machine":
                    ii = new MachineImageInfo();
                    initMetaClass(ii, ii.class);
                    ii = convertRowToObject(mimgSetterMap, img, ii);
                    break;
                case "blockstorage":
                    ii = new BlockStorageImageInfo();
                    initMetaClass(ii, ii.class);
                    ii = convertRowToObject(bimgSetterMap, img, ii);
                    break;
            }
            dbGen.add(ii);
            LOG.debug("Adding image ${img.metadata_image_name}");

            dbGen.recast(DeviceMapping.class);
            Set<DeviceMapping> deviceMappings = new HashSet<DeviceMapping>( );
            connMap['eucalyptus_cloud'].rows("""SELECT m.* 
                                                  FROM metadata_device_mappings m
                                                  JOIN metadata_images i
                                                    ON (m.metadata_image_dev_map_fk=i.id)
                                                 WHERE i.id=?""", img.id).each { mapping ->
                def dm = null;
                switch (mapping.metadata_device_mapping_discriminator) {
                    case "blockstorage":
                        dm = new BlockStorageDeviceMapping();
                        initMetaClass(dm, dm.class);
                        dm = convertRowToObject(bsdmSetterMap, mapping, dm);
                        break;
                    case "ephemeral":
                        dm = new EphemeralDeviceMapping();
                        initMetaClass(dm, dm.class);
                        dm = convertRowToObject(edmSetterMap, mapping, dm);
                        break;
                    case "suppress":
                        dm = new SuppressDeviceMappping();
                        initMetaClass(dm, dm.class);
                        dm = convertRowToObject(sdmSetterMap, mapping, dm);
                        break;
                }
                dm.setParent(ii);
                deviceMappings.add(dm);
                dbGen.add(dm);
            }
            ii.setDeviceMappings(deviceMappings);
        }
        dbGen.commit();
    }

    private void upgradeMisc() {
        // StaticDatabaseProperty
        def conn = connMap["eucalyptus_config"];

        def db = EntityWrapper.get(StaticDatabasePropertyEntry.class);
        conn.rows("""select * from config_static_property""").each { prop ->
            // lowercase the last word of the field name
            def fieldName = prop.config_static_field_name.replaceAll(/(.\w*)$/) { whole, match -> match.toLowerCase() }
            StaticDatabasePropertyEntry sdbprop = new StaticDatabasePropertyEntry(fieldName, prop.config_static_prop_name, prop.config_static_field_value);
            db.add(sdbprop);
        }
        db.commit();

        db = EntityWrapper.get(InstanceUsageSnapshot.class);
        conn = connMap["eucalyptus_reporting"];
        conn.rows("""select * from instance_usage_snapshot""").each { row ->
            InstanceUsageSnapshot ius = new InstanceUsageSnapshot(row.uuid,
                                                                  row.timestamp_ms,
                                                                  row.total_network_io_megs,
                                                                  row.total_disk_io_megs);
            db.add(ius);
        }
        db.commit();

        db = EntityWrapper.get(S3UsageSnapshot.class);
        conn.rows("""select * from s3_usage_snapshot""").each { row ->
            S3UsageSnapshot s3us = new S3UsageSnapshot(new S3SnapshotKey(row.owner_id, row.account_id, row.timestamp_ms),
                                                       new S3UsageData(row.buckets_num, row.objects_num, row.objects_megs));
            db.add(s3us);
        }
        db.commit();

        db = EntityWrapper.get(StorageUsageSnapshot.class);
        conn.rows("""select * from storage_usage_snapshot""").each { row ->
            StorageUsageSnapshot sus = new StorageUsageSnapshot(new StorageSnapshotKey(row.owner_id, row.account_id,
                                                                                       row.cluster_name, row.availability_zone,
                                                                                       row.timestamp_ms),
                                                                new StorageUsageData(row.volumes_num, row.volumes_megs,
                                                                                     row.snapshot_num, row.snapshot_megs));
            db.add(sus);
        }
        db.commit();

        db = EntityWrapper.get(CHAPUserInfo.class);
        conn = connMap['eucalyptus_storage'];
        def cuiSetterMap = buildSetterMap(conn, "CHAPUserInfo");
        conn.rows("""select * from CHAPUserInfo;""").each { row ->
            CHAPUserInfo cui = new CHAPUserInfo();
            initMetaClass(cui, cui.class);
            cui = convertRowToObject(cuiSetterMap, row, cui);
            cui.setEncryptedPassword(row.encryptedPassword);
            db.add(cui);
        }
        db.commit();
    }

    private void upgradeISCSIVolumeInfo() {
        EntityWrapper<ISCSIVolumeInfo> db = EntityWrapper.get(ISCSIVolumeInfo.class);
        def conn = connMap['eucalyptus_storage'];
        def iviSetterMap = buildSetterMap(conn, "ISCSIVolumeInfo");
        conn.rows("""select * from ISCSIVolumeInfo;""").each { row ->
            ISCSIVolumeInfo ivi = new ISCSIVolumeInfo();
            initMetaClass(ivi, ivi.class);
            ivi = convertRowToObject(iviSetterMap, row, ivi);
            // No column decorators in this class
            ivi.setStoreName(row.storeName);
            ivi.setStoreUser(row.storeUser);
            ivi.setTid(row.tid);
            ivi.setLun(row.lun);
            ivi.setEncryptedPassword(row.encryptedPassword);
            db.add(ivi);
         }
         db.commit();
    }

    private void upgradeSANVolumeInfo() {
        def conn = connMap['eucalyptus_storage'];
        def sviSetterMap = buildSetterMap(conn, "EquallogicVolumeInfo");
        def sviClass = Class.forName("edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo");
        EntityWrapper<AbstractPersistent> db = EntityWrapper.get(sviClass);
        db.recast(sviClass);
        conn.rows("""select * from EquallogicVolumeInfo""").each { row ->
            def svi = sviClass.newInstance();
            initMetaClass(svi, sviClass);
            svi = convertRowToObject(sviSetterMap, row, svi);
            // Missing column decorators for this class
            svi.setScName(row.scName);
            svi.setIqn(row.iqn);
            svi.setStoreUser(row.storeUser);
            svi.setVolumeId(row.volumeId);
            db.add(svi);
        }
        db.commit();
    }

    private void upgradeEntity(entityKey) {
        String contextName = getContextName(entityKey);
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
            LOG.debug("Got " + rowResults.size().toString() + " results from " + entityKey);
            EntityWrapper db =  EntityWrapper.get(entityMap.get(entityKey));

            def dest = null;
            for (GroovyRowResult rowResult : rowResults) {
                try {
                    Class cls = ClassLoader.getSystemClassLoader().loadClass(entityMap.get(entityKey).getCanonicalName());
                    if (entityMap.get(entityKey) == "CertificateEntity") {
                        dest = cls.newInstanceWithId(rowResult.auth_certificate_id);
                    } else {
                        dest = cls.newInstance();
                    }
                    dest = convertRowToObject(setterMap, rowResult, dest);
                } catch (ClassNotFoundException e1) {
                    LOG.warn(e1);
                    break;
                } catch (InstantiationException e) {
                    LOG.warn(e);
                    break;
                } catch (IllegalAccessException e) {
                    LOG.warn(e);
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

    private Object convertRowToObject(Map<String, Method> setterMap, GroovyRowResult rowResult, Object dest) {
        Set<String> columns = rowResult.keySet();

        HashMap<String, Class> enumSetterMap = new HashMap<String, Class>();
        enumSetterMap.put("setState", State.class);
        enumSetterMap.put("setLastState", State.class);
        // enumSetterMap.put("setProtocol", NetworkRule.Protocol.class);
        enumSetterMap.put("setPlatform", ImageMetadata.Platform.class);
        enumSetterMap.put("setArchitecture", ImageMetadata.Architecture.class);
        enumSetterMap.put("setImageType", ImageMetadata.Type.class);
        enumSetterMap.put("setDeviceMappingType", ImageMetadata.DeviceMappingType.class);

        // columns.each{ c -> LOG.debug("column: " + c); }
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
                        setter.setAccessible(true);
                        Class enumClass = enumSetterMap.get(setter.getName(), null);
                        if (enumClass != null) {
                            if (setter.getName() in ['setState', 'setLastState']) {
                                if (dest instanceof NetworkGroup) {
                                    setter.invoke(dest, NetworkGroup.State.valueOf(o));
                                } else if (dest instanceof ImageInfo) {
                                    setter.invoke(dest, ImageMetadata.State.valueOf(o));
                                } else {
                                    setter.invoke(dest, enumClass.valueOf(o));
                                }
                            } else {
                                setter.invoke(dest, enumClass.valueOf(o));
                            }
                        } else if (setter.getName() == 'setSanPassword') {
                            // decrypt the password so that it can be re-encrypted
                            def decpass = BlockStorageUtil.decryptSCTargetPassword(o);
                            setter.invoke(dest, decpass);
                            // dest.sanPassword = o;
                        } else {
                            setter.invoke(dest, o);
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.warn(dest.getClass().getName()  + " " + column + " " + e);
                    } catch (IllegalAccessException e) {
                        LOG.warn(dest.getClass().getName()  + " " + column + " " + e);
                    } catch (InvocationTargetException e) {
                        LOG.warn(dest.getClass().getName()  + " " + column + " " + e);
                    }
                } else {
                    LOG.debug("Column " + column + " was NULL");
                }
            } else if (!(column in ["id", "auth_group_id_external", "auth_group_owning_account",
                                    "auth_account_number", "auth_account_name",
                                    "auth_user_reg_stat", "auth_user_id_external",
                                    "auth_group_id", "auth_user_id" ])) {
                // This should probably be a fatal exception
                // throw new GroovyRuntimeException("Setter for " + dest.getClass().getName() + "." + column + " was NULL");
                LOG.debug("Setter for " + dest.getClass().getName() + "." + column + " was NULL");
            }
        }

        return dest;
    }

    private Map<String, Method> buildSetterMap(Sql conn, String entityKeyAlias) {
        def entityKey = entityKeyAlias;
        if (entityKey in ["kernel_images", "ramdisk_images", 
                          "machine_images", "blockstorage_images"]) {
            entityKey = "metadata_images";
        } else if (entityKey in ["blockstorage_mappings",
                                 "ephemeral_mappings", 
                                 "suppress_mappings"]) {
            entityKey = "metadata_device_mappings";
        }
        Map<String, Method> setterMap = new HashMap<String, Method>();
        try {
            Object firstRow = conn.firstRow("SELECT * FROM " + entityKey);
            if(firstRow == null) {
                LOG.info("Unable to find anything in table: " + entityKey);
                return null;
            }
            if(firstRow instanceof Map) {
                Set<String> origColumns = ((Map) firstRow).keySet();
                LOG.info("Columns for ${entityKeyAlias}: " + origColumns.join(", "));
                Class definingClass = entityMap.get(entityKeyAlias);
                Field[] fields = definingClass.getDeclaredFields();
                //special case. Do this better.
                addToSetterMap(setterMap, origColumns, definingClass, fields);
                Class superClass = entityMap.get(entityKeyAlias).getSuperclass();

                // Checking for the Table annotation here allows us to handle
                // multiple subclasses stored in the same Table, such as
                // image metadata.
                while(superClass.isAnnotationPresent(MappedSuperclass.class) ||
                      superClass.isAnnotationPresent(Table.class)) {
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
                if(f.isAnnotationPresent(Column.class)) {
                  if (!f.isAnnotationPresent(Id.class) || definingClass in [ ReportingUser.class, ReportingAccount.class ]) {
                    Column annotClass = (Column)f.getAnnotation(Column.class);
                    if(((String)column).toLowerCase().equals(annotClass.name().toLowerCase())) {
                        String baseMethodName = f.getName( ).substring( 0, 1 ).toUpperCase( ) + f.getName( ).substring( 1 );
                        try {
                            Class[] classes = new Class[1];
                            classes[0] = f.getType();
                            if (baseMethodName == "VolumeSc") {
                                baseMethodName = "VolumeCluster";
                            } else if (baseMethodName == "IGroupName") {
                                baseMethodName = "iGroupName";
                            }
                            Method setMethod = definingClass.getDeclaredMethod( "set" + baseMethodName, classes );
                            setterMap.put(column, setMethod);
                        } catch (SecurityException e) {
                            LOG.warn(e);
                        } catch (NoSuchMethodException e) {
                            LOG.info(e);
                        }
                        break;
                    }
                  }
                }
            }
            if(setterMap.containsKey(column)) {
                LOG.debug(column + " is set by: " + setterMap.get(column).getName());
            }
        }
    }

    private void buildEntityMap() {
        for (Class entity : entities) {
            if (entity.isAnnotationPresent(Table.class)) {
                // This only handles tables whose names have not changed.
                Table annot = (Table)entity.getAnnotation(Table.class);
                entityMap.put(annot.name(), entity);
                LOG.info("Mapping " + entity + " to " + annot.name());
            }
        }

        // fake entities for images
        entityMap.put("kernel_images", KernelImageInfo.class);
        entityMap.put("ramdisk_images", RamdiskImageInfo.class);
        entityMap.put("machine_images", MachineImageInfo.class);
        entityMap.put("blockstorage_images", BlockStorageImageInfo.class);
        entityMap.put("blockstorage_mappings", BlockStorageDeviceMapping.class);
        entityMap.put("ephemeral_mappings", EphemeralDeviceMapping.class);
        entityMap.put("suppress_mappings", SuppressDeviceMappping.class);
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
        entities.add(Address.class)
        entities.add(AddressingConfiguration.class)
        entities.add(AOEMetaInfo.class)
        entities.add(AOEVolumeInfo.class)
        entities.add(BaseRecord.class)
        // entities.add(BucketInfo.class)
        entities.add(CertificateEntity.class)
        entities.add(CHAPUserInfo.class)
        entities.add(Clusters.class)
        entities.add(CNAMERecordInfo.class)
        entities.add(ComponentConfiguration.class)
        entities.add(ConditionEntity.class)
        entities.add(DeviceMapping.class)
        entities.add(DirectStorageInfo.class)
        entities.add(DRBDInfo.class)
        entities.add(Faults.class)
        // entities.add(GrantInfo.class)
        entities.add(GroupEntity.class)
        entities.add(ImageCacheInfo.class)
        entities.add(ImageConfiguration.class)
        entities.add(ImageInfo.class)
        entities.add(ISCSIMetaInfo.class)
        entities.add(ISCSIVolumeInfo.class)
        entities.add(LogFileRecord.class)
        entities.add(MetaDataInfo.class)
        // entities.add(NetworkGroup.class)
        // entities.add(NetworkRule.class)
        entities.add(NSRecordInfo.class)
        // entities.add(ObjectInfo.class)
        entities.add(Partition.class)
        entities.add(PolicyEntity.class)
        entities.add(ReportingAccount.class)
        entities.add(ReportingUser.class)
        entities.add(Snapshot.class)
        entities.add(SnapshotInfo.class)
        entities.add(SOARecordInfo.class)
        entities.add(SshKeyPair.class)
        entities.add(StatementEntity.class)
        entities.add(StorageInfo.class)
        entities.add(StorageStatsInfo.class)
        entities.add(SystemConfiguration.class)
        entities.add(TorrentInfo.class)
        entities.add(UniqueIds.class)
        entities.add(UserEntity.class)
        entities.add(VmType.class)
        entities.add(Volume.class)
        entities.add(VolumeInfo.class)
        entities.add(WalrusInfo.class)
        entities.add(WalrusSnapshotInfo.class)
        entities.add(WalrusStatsInfo.class)
        entities.add(ZoneInfo.class)

    }
}
