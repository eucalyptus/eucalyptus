import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
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

// For cluster / node keypairs
import java.security.KeyPair;
import java.security.Security;
import org.bouncycastle.openssl.PEMReader;

// Auth
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.UserFullName;
import java.security.cert.X509Certificate;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.DatabaseAccountProxy;
import com.eucalyptus.auth.DatabaseAuthUtils;

// Config
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.cluster.ClusterBuilder;
import com.eucalyptus.component.Partition;

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

// SAN
import edu.ucsb.eucalyptus.cloud.entities.NetappInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.IgroupInfo;
import edu.ucsb.eucalyptus.cloud.entities.DASInfo;

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
import groovy.lang.GroovySystem;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import com.eucalyptus.address.Address;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.util.Internets;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.util.Counters;

class upgrade_20_30 extends AbstractUpgradeScript {
    static final List<String> FROM_VERSION = ["eee-2.0.2", "eee-2.0.1", "2.0.2", "2.0.3"];
    static final String TO_VERSION = "eee-3.0.0";
    private static Logger LOG = Logger.getLogger( upgrade_20_30.class );
    private static List<Class> entities = new ArrayList<Class>();
    private static Map<String, Class> entityMap = new HashMap<String, Class>();

    // Map users to accounts, possible substituting characters to make them "safe"
    private static Map<String, String> safeUserMap = new HashMap<String, String>();
    // Map users to userId of "admin" in their mapped account
    private static Map<String, String> userIdMap = new HashMap<String, String>();
    // Map account names to account ids
    private static Map<String, String> accountIdMap = new HashMap<String, String>();
    private static Map<String, Sql> connMap = new HashMap<String, Sql>();
    private static List<String> unmappedColumns = [ "walrus_stats_info.walrus_stats_info_id",
                                "ISCSIMetadata.id",
                                "direct_storage_info.storage_direct_id",
                                "vm_types.id",
                                "storage_info.storage_id",
                                "storage_stats_info.storage_stats_info_id",
                                "ARecords.arecord_id",
                                "SOARecords.soarecord_id",
                                "Snapshots.snapshot_id",
                                "walrus_info.walrus_info_id",
                                "Zones.zones_id",
                                "NSRecords.nsrecord_id",
                                "WalrusSnapshots.walrus_snapshot_id",
                                "ImageCache.image_cache_id",
                                "Volumes.volume_id",
                              ];

    public upgrade_20_30() {
        super(1);        
    }

    @Override
    public Boolean accepts( String from, String to ) {
        // We should support multiple from versions, but need
        // to decide which ones. 2.0.[1-9](eee)? for example
        if(TO_VERSION.equals(to) && FROM_VERSION.contains(from))
            return true;
        return false;
    }

    @Override
    public void setLogger( Logger log ) {
        LOG = log;
    }

    @Override
    public void upgrade(File oldEucaHome, File newEucaHome) {
        // Do this in stages and bail out if something goes seriously wrong.
        def parts = [ 'Cluster', 'Auth', 'KeyPairs', 'Network', 'Walrus', 'Storage', 'SAN' ]
        buildConnectionMap();
        parts.each { this."upgrade${it}"(); }
                
        // Do object upgrades which follow the entity map / setter map pattern
        buildEntityMap();
        def altEntityMap = [ vm_types:'eucalyptus_general' ];
        def optionalTables = [ 'netapp_info', 'das_info' ]

        Set<String> entityKeys = entityMap.keySet();
        for (String entityKey : entityKeys) {
            if (entityKey.equals("metadata_network_group")) {
                continue
            }
            String contextName = getContextName(entityKey);
            if (optionalTables.contains(entityKey) && !has_table(contextName, entityKey)) {
                LOG.info("table ${entityKey} does not exist; skipping.");
                continue;
            }
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

    private void buildConnectionMap() {
        def dbList = [ 'eucalyptus_auth', 'eucalyptus_general', 'eucalyptus_storage',
                       'eucalyptus_walrus', 'eucalyptus_images', 'eucalyptus_config',
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
        /* This is a generic function for handling "easy" upgrades, where
         * simply initializing an object and setting the attributes from
         * the old version of the object is sufficient.  In the 3.0 migration,
         * this is often insufficient due to user -> account migration, among
         * other things.
         */

        List<GroovyRowResult> rowResults;
        try {
            rowResults = conn.rows("SELECT * FROM " + entityKey);
            LOG.debug("Got " + rowResults.size().toString() + " results from " + entityKey);
            EntityWrapper db =  EntityWrapper.get(entityMap.get(entityKey));
                        
            for (GroovyRowResult rowResult : rowResults) {
                Set<String> columns = rowResult.keySet();
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
            throw e;
        }        
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
                // switching columns here makes the setterMap easier
                // to construct.  We flip them back inside the setterMap 
                // at the end of the function.
                def columnMap = [ vm_type_cpu:'metadata_vm_type_cpu',
                    vm_type_disk:'metadata_vm_type_disk',
                    vm_type_memory:'metadata_vm_type_memory',
                    vm_type_name:'metadata_vm_type_name' ];
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
                    if(!setterMap.containsKey(column) && !unmappedColumns.contains("${entityKey}.${column}".toString()) ) {
                        print unmappedColumns;
                        LOG.warn("No corresponding field for column: ${entityKey}.${column} found");
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
            /* if(setterMap.containsKey(column)) {
                LOG.debug(column + " is set by: " + setterMap.get(column).getName());
            } */
        }
    }

    private void buildEntityMap() {
                // Note that this maps new -> old
        def tableMap = [ cloud_vm_types:'vm_types' ];
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
        /* This function upgrades images, snapshots, and volumes as well as
         * users, because it was initially easier to map owners and users of
         * those objects.  Now that there are HashMaps in the class, this
         * could be split into smaller functions.  If this is done, it would
         * be worthwhile to check for unmapped users, as there were situations
         * in 2.0.x where user deletions left unowned objects under HSQLDB.
         */

        def userInfoFields = [ "user_real_name":"Full Name",
                           "user_email":"Email",
                           "user_telephone_number":"Telephone",
                           "user_affiliation":"Affiliation",
                           "user_project_description":"ProjectDescription",
                           "user_project_pi_name":"ProjectPI" ];

        connMap['eucalyptus_auth'].rows('SELECT * FROM auth_users').each {
            def account = null;
            def accountProxy = null;
            def accountName = (it.auth_user_name == "admin") ? "eucalyptus" : makeSafeUserName(it.auth_user_name);
            /* As of now, every user becomes "admin" in a different account.
             * There are certainly issues with this approach, but there's
             * not another obvious mapping into the new account hierarchy.
             */
            def userName = "admin"
            while (account == null) {
                try { 
                    LOG.debug("adding account ${ accountName }");
                    EntityWrapper<AccountEntity> dbAcct = EntityWrapper.get(AccountEntity.class);
                    account = new AccountEntity(accountName);
                    dbAcct.add(account);
                    dbAcct.commit()
                    accountProxy = new DatabaseAccountProxy(account);
                    safeUserMap.put(it.auth_user_name, accountName);
                    accountIdMap.put(accountName, accountProxy.getAccountNumber());
                } catch (AuthException e) {
                    // The account already existed
                    userName = userName + "-";
                    accountName = userName;
                }
            }
            
            def user = accountProxy.addUser( userName, "/", true/* skipRegistration */, true/* enabled */, null);
            user.setPassword( it.auth_user_password );
            user.setToken(it.auth_user_token );
            userIdMap.put(it.auth_user_name, user.getUserId());
            LOG.debug("added " + account);
            connMap['eucalyptus_auth'].rows("""SELECT c.* FROM auth_users
                         JOIN auth_user_has_x509 on auth_users.id=auth_user_has_x509.auth_user_id
                         JOIN auth_x509 c on auth_user_has_x509.auth_x509_id=c.id
                         WHERE auth_users.auth_user_name=?""", [it.auth_user_name]).each { certificate ->
                X509Certificate x509cert = X509CertHelper.toCertificate(certificate.auth_x509_pem_certificate);
                def cert = user.addCertificate(x509cert);
                // cert.setRevoked(certificate.auth_x509_revoked);
            }
            // The test data I have includes duplicate rows here.  Why?
            def uInfo = connMap['eucalyptus_general'].firstRow("SELECT * FROM Users WHERE Users.user_name=?", [it.auth_user_name]);
            if (uInfo != null) {
                Map<String, String> info = new HashMap<String, String>( );
                userInfoFields.each { k,v ->
                    if (uInfo[k] != null) { info.put(v, uInfo[k]); }
                }
                user.setInfo( info );
            }

            EntityWrapper<UserEntity> dbUE = EntityWrapper.get(UserEntity.class);
            def ue = DatabaseAuthUtils.getUniqueUser(dbUE, userName, accountName);
            EntityWrapper<AccessKeyEntity> dbAuth = EntityWrapper.get(AccessKeyEntity.class);
            AccessKeyEntity accessKey = new AccessKeyEntity();
            initMetaClass(accessKey, accessKey.class);
            accessKey.setAccess(it.auth_user_query_id);
            accessKey.setSecretKey(it.auth_user_secretkey);
            accessKey.setUser(ue);
            accessKey.setActive(true);
            dbAuth.add(accessKey);
            dbAuth.commit();
            
            def ufn = UserFullName.getInstance(user);
            connMap['eucalyptus_general'].rows("SELECT * FROM Images WHERE image_owner_id=?", [it.auth_user_name]).each { img ->
                EntityWrapper<ImageInfo> dbGen = EntityWrapper.get(ImageInfo.class);
                LOG.debug("Adding image ${img.image_name}");
                def path = img.image_path.split("/");
                // We cannot get the checksum or bundle size without reading
                // the manifest, so leave them as null.
                def imgSize = 1;
                def bundleSize = null;
                def ckSum = null;
                def ckSumType = null;
                def platform = Image.Platform.valueOf("linux");
                def cachedImg = connMap['eucalyptus_walrus'].firstRow("""SELECT manifest_name,size sz FROM ImageCache 
                                                      WHERE bucket_name=? AND manifest_name=?""", path);
                if (cachedImg != null)
                    imgSize = cachedImg.sz.toInteger();
                if (img.image_platform != null)
                    platform = Image.Platform.valueOf(img.image_platform);
                def ii = null;
                switch ( img.image_type ) {
                    case "kernel":
                        ii = new KernelImageInfo( ufn, img.image_name, img.image_name, 
                                                 "No Description", imgSize, 
                                                 Image.Architecture.valueOf(img.image_arch), platform,
                                                 img.image_path, bundleSize, ckSum, ckSumType );
                        break;
                    case "ramdisk":
                        ii = new RamdiskImageInfo( ufn, img.image_name, img.image_name,
                                                  "No Description", imgSize, 
                                                  Image.Architecture.valueOf(img.image_arch), platform,
                                                  img.image_path, bundleSize, ckSum, ckSumType );
                        break;
                    case "machine":
                        ii = new MachineImageInfo( ufn, img.image_name, img.image_name,
                                                  "No Description", imgSize, 
                                                  Image.Architecture.valueOf(img.image_arch), platform,
                                                  img.image_path, bundleSize, ckSum, ckSumType,
                                                  img.image_kernel_id, img.image_ramdisk_id );
                        break;
                }
                initMetaClass(ii, ii.class);
                ii.setImagePublic(img.image_is_public);
                ii.setImageType(Image.Type.valueOf(img.image_type));
                ii.setSignature(img.image_signature);
                ii.setState( Image.State.valueOf(img.image_availability));
                dbGen.add(ii);
                dbGen.commit();
                connMap['eucalyptus_general'].rows("""SELECT image_product_code_value FROM image_product_code
                                 JOIN image_has_product_codes 
                                 ON image_product_code.image_product_code_id=image_has_product_codes.image_product_code_id
                                 WHERE image_id=?""", [ img.image_id ]).each { prodCode ->
                    EntityWrapper<ProductCode> dbPC = EntityWrapper.get(ProductCode.class);
                    dbPC.add(new ProductCode(ii, prodCode.image_product_code_value));
                    dbPC.commit();
                }
                connMap['eucalyptus_general'].rows("""SELECT * FROM image_authorization
                                 JOIN image_has_user_auth
                                 ON image_authorization.image_auth_id=image_has_user_auth.image_auth_id
                                 WHERE image_id=?""", [ img.image_id ]).each { imgAuth ->
                    EntityWrapper<LaunchPermission> dbLP = EntityWrapper.get(LaunchPermission.class);
                    dbLP.add(new LaunchPermission(ii, imgAuth.image_auth_name));
                    dbLP.commit();
                }
            }

            connMap['eucalyptus_images'].rows("SELECT * FROM Volume WHERE username=?", [ it.auth_user_name ]).each { vol ->
                EntityWrapper<Volume> dbVol = EntityWrapper.get(Volume.class);
                def vol_meta = connMap['eucalyptus_storage'].firstRow("SELECT * FROM Volumes WHERE volume_name=?", 
                                                                      [ vol.displayname ]);
                if (vol.cluster == "default") {
                    vol.cluster = System.getProperty("euca.storage.name");
                }
                // Second "vol.cluster" is partition name
                Volume v = new Volume( ufn, vol.displayname, vol.size, vol.cluster, vol.cluster, vol.parentsnapshot );
                initMetaClass(v, v.class);
                v.setState(State.valueOf(vol.state));
                v.setLocalDevice(vol.localdevice);
                v.setRemoteDevice(vol.remotedevice);
                v.setSize(vol.size);
                LOG.debug("Adding volume ${ vol.displayname } for ${ it.auth_user_name }");
                dbVol.add(v);
                dbVol.commit();
            }
            connMap['eucalyptus_images'].rows("SELECT * FROM Snapshot WHERE username=?", [ it.auth_user_name ]).each { snap ->
                EntityWrapper<Snapshot> dbSnap = EntityWrapper.get(Snapshot.class);
                def snap_meta = connMap['eucalyptus_storage'].firstRow("SELECT * FROM Snapshots WHERE snapshot_name=?", [ snap.displayname ]);
                def scName = (snap_meta == null) ? null :  snap_meta.sc_name;
                // Second scName is partition
                Snapshot s = new Snapshot( ufn, snap.displayname, snap.parentvolume, scName, scName);
                initMetaClass(s, s.class);
                s.setState(State.valueOf(snap.state));
                LOG.debug("Adding snapshot ${ snap.displayname } for ${ it.auth_user_name }");
                dbSnap.add(s);
                dbSnap.commit();
            }
        }
        return true;
    }

    public boolean upgradeKeyPairs() {
        connMap['eucalyptus_general'].rows('SELECT * FROM metadata_keypair').each{
            EntityWrapper<SshKeyPair> dbkp = EntityWrapper.get(SshKeyPair.class);
            if (!userIdMap.containsKey(it.metadata_user_name)) {
                return;
            }
            UserFullName ufn = UserFullName.getInstance(userIdMap.get(it.metadata_user_name));
            SshKeyPair kp = new SshKeyPair( ufn, 
                                            it.metadata_keypair_user_keyname,
                                            it.metadata_keypair_public_key,
                                            it.metadata_keypair_finger_print );
            dbkp.add(kp);
            dbkp.commit();
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

    public String makeSafeUserName(String user_name) {
        /* See http://docs.amazonwebservices.com/IAM/latest/UserGuide/LimitationsOnEntities.html
         * This was originally for making safe usernames, but now it is used
         * only for account names, so perhaps it should be relaxed.
         * Duplicate and trailing hyphens must also be removed.
         */
        return (user_name =~ /([^a-zA-Z0-9+=.,@-])/).replaceAll("-")
    }

    public boolean upgradeWalrus() {
        connMap['eucalyptus_config'].rows('SELECT * FROM config_walrus').each{
            EntityWrapper<WalrusConfiguration> dbcfg = EntityWrapper.get(WalrusConfiguration.class);
            WalrusConfiguration walrus = new WalrusConfiguration(it.config_component_name, it.config_component_hostname, it.config_component_port);
            dbcfg.add(walrus);
            dbcfg.commit();
        }    

        connMap['eucalyptus_walrus'].rows('SELECT * FROM Buckets').each{
            LOG.debug("Adding bucket: ${it.bucket_name}");
            
            EntityWrapper<BucketInfo> dbBucket = EntityWrapper.get(BucketInfo.class);
            try {
                BucketInfo b = new BucketInfo(accountIdMap.get(safeUserMap.get(it.owner_id)),accountIdMap.get(safeUserMap.get(it.owner_id)),it.bucket_name,it.bucket_creation_date);
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
                                    LEFT OUTER JOIN Grants g on g.grant_id=has_thing.grant_id
                                    WHERE has_thing.bucket_id=?""", [ it.bucket_id ]).each{  grant ->
                    LOG.debug("--> grant: ${it.bucket_id}/${grant.user_id}");
                    GrantInfo grantInfo = new GrantInfo();
                                        initMetaClass(grantInfo, grantInfo.class);
                    
                    grantInfo.setUserId(accountIdMap.get(safeUserMap.get(grant.user_id)));
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
                objectInfo.setOwnerId(accountIdMap.get(safeUserMap.get(it.owner_id)));
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
                                    LEFT OUTER JOIN Grants g on g.grant_id=has_thing.grant_id 
                                    WHERE has_thing.object_id=?""", [ it.object_id ]).each{  grant ->
                    LOG.debug("--> grant: ${it.object_name}/${grant.user_id}")
                    GrantInfo grantInfo = new GrantInfo();
                    initMetaClass(grantInfo, grantInfo.class);
                    grantInfo.setUserId(accountIdMap.get(safeUserMap.get(grant.user_id)));
                    grantInfo.setGrantGroup(grant.grantGroup);
                    grantInfo.setCanWrite(grant.allow_write);
                    grantInfo.setCanRead(grant.allow_read);
                    grantInfo.setCanReadACP(grant.allow_read_acp);
                    grantInfo.setCanWriteACP(grant.allow_write_acp);
                    objectInfo.getGrants().add(grantInfo);
                }
                connMap['eucalyptus_walrus'].rows("""SELECT m.* FROM object_has_metadata has_thing 
                                    LEFT OUTER JOIN MetaData m on m.metadata_id=has_thing.metadata_id 
                                    WHERE has_thing.object_id=?""", [ it.object_id ]).each{  metadata ->
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
            } catch (Throwable t) {
                t.printStackTrace();
                dbObject.rollback();
                throw t;
            }
        }
        return true;
    }

    public boolean upgradeNetwork() {
        /* TODO: The system admin may not have a default network rules group.
         * This means that no images can be launched from this account.
         * Should a "default" one be created?
         */ 

        connMap['eucalyptus_general'].rows('SELECT * FROM metadata_network_group').each {
            EntityWrapper<NetworkRulesGroup> dbGen = EntityWrapper.get(NetworkRulesGroup.class);
            try {
                if (!userIdMap.containsKey(it.metadata_user_name)) {
                    return;
                }
                UserFullName ufn = UserFullName.getInstance(userIdMap.get(it.metadata_user_name));
                def uniqueName = "${ it.metadata_user_name }_${it.metadata_display_name}";
                if ( it.metadata_user_name == 'admin' && it.metadata_display_name == 'default' ) {
                    uniqueName = it.metadata_display_name;
                }
                def rulesGroup = new NetworkRulesGroup(ufn, uniqueName, it.metadata_network_group_description);
                initMetaClass(rulesGroup, rulesGroup.class);
                rulesGroup.setDisplayName(uniqueName);
                LOG.debug("Adding network rules for ${ it.metadata_user_name }/${it.metadata_display_name}");
                connMap['eucalyptus_general'].rows("""SELECT r.* 
                                 FROM metadata_network_group_has_rules 
                                 LEFT OUTER JOIN metadata_network_rule r 
                                 ON r.metadata_network_rule_id=metadata_network_group_has_rules.metadata_network_rule_id 
                                 WHERE metadata_network_group_has_rules.id=?""", [ it.id ]).each { rule ->
                    NetworkRule networkRule = new NetworkRule(rule.metadata_network_rule_protocol, 
                                                              rule.metadata_network_rule_low_port, 
                                                              rule.metadata_network_rule_high_port);
                    initMetaClass(networkRule, networkRule.class);
                    connMap['eucalyptus_general'].rows("""SELECT ip.* 
                                     FROM metadata_network_rule_has_ip_range 
                                     LEFT OUTER JOIN metadata_network_rule_ip_range ip
                                     ON ip.metadata_network_rule_ip_range_id=metadata_network_rule_has_ip_range.metadata_network_rule_ip_range_id 
                                     WHERE metadata_network_rule_has_ip_range.metadata_network_rule_id=?""", [ rule.metadata_network_rule_id ]).each { iprange ->
                        IpRange ipRange = new IpRange(iprange.metadata_network_rule_ip_range_value);
                        initMetaClass(ipRange, ipRange.class);
                        networkRule.getIpRanges().add(ipRange);
                        LOG.debug("IP Range: ${iprange.metadata_network_rule_ip_range_value}");
                    }
                    connMap['eucalyptus_general'].rows("""SELECT peer.* 
                                     FROM metadata_network_rule_has_peer_network 
                                     LEFT OUTER JOIN network_rule_peer_network peer
                                     ON peer.network_rule_peer_network_id=metadata_network_rule_has_peer_network.metadata_network_rule_peer_network_id 
                                     WHERE metadata_network_rule_has_peer_network.metadata_network_rule_id=?""", [ rule.metadata_network_rule_id ]).each { peer ->
                        NetworkPeer networkPeer = new NetworkPeer(peer.network_rule_peer_network_user_query_key, peer.network_rule_peer_network_user_group);
                        initMetaClass(networkPeer, networkPeer.class);
                        networkRule.getNetworkPeers().add(networkPeer);

                        LOG.debug("Peer: " + networkPeer);
                    }
                    rulesGroup.getNetworkRules().add(networkRule);
                } 

                LOG.debug("adding rules group: " + rulesGroup);
                dbGen.add(rulesGroup);
                dbGen.commit();
            } catch (Throwable t) {
                t.printStackTrace();
                dbGen.rollback();
                throw t;
            }
        }
        return true;
    }

    public boolean upgradeStorage() {
        EntityWrapper<CHAPUserInfo> dbchap = EntityWrapper.get(CHAPUserInfo.class);
        connMap['eucalyptus_storage'].rows('SELECT * FROM CHAPUserInfo').each{
            CHAPUserInfo cui = new CHAPUserInfo(it.user, it.encryptedPassword);
            dbchap.add(cui);
        }
        dbchap.commit()

        EntityWrapper<ISCSIVolumeInfo> dbIvi = EntityWrapper.get(ISCSIVolumeInfo.class);
        connMap['eucalyptus_storage'].rows('SELECT * FROM ISCSIVolumeInfo').each{
            ISCSIVolumeInfo ivi = new ISCSIVolumeInfo();
            initMetaClass(ivi, ivi.class);
            ivi.setLoDevName(it.lodev_name);
            ivi.setLoFileName(it.lofile_name);
            ivi.setLvName(it.lv_name);
            ivi.setPvName(it.pv_name);
            ivi.setSize(it.size);
            // I think "SC Name" is actually partition here
            ivi.setScName(it.sc_name);
            ivi.setSnapshotOf(it.snapshot_of);
            ivi.setStatus(it.status);
            ivi.setVgName(it.vg_name);
            ivi.setVolumeId(it.volume_name);
            ivi.setEncryptedPassword(it.encryptedPassword);
            ivi.setLun(it.lun);
            ivi.setStoreName(it.storeName);
            ivi.setStoreUser(it.storeUser);
            ivi.setTid(it.tid);
            dbIvi.add(ivi);
        }
        dbIvi.commit();
        return true;
    }

    def has_table(dbName, tableName) {
        try {
            // Gets the database metadata
            DatabaseMetaData dbmd = connMap[dbName].getConnection().getMetaData();

            // Specify the type of object; in this case we want tables
            String[] types = {"TABLE"};
            dbmd.getTables(null, null, "%", types).each { table ->
                // Get the table name
                if (table.getString(3) == tableName) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LOG.warn('During check for table ${tableName}: ${e}');
        }
        return false;
    }

    public boolean upgradeSAN() {
        // Only eee
        EntityWrapper<SANVolumeInfo> dbsvi = EntityWrapper.get(SANVolumeInfo.class);
        if (has_table('eucalyptus_storage', "EquallogicVolumeInfo")) {
            connMap['eucalyptus_storage'].rows('SELECT * FROM EquallogicVolumeInfo').each{
                SANVolumeInfo sanvol = new SANVolumeInfo(it.volumeId, it.iqn, it.size);
                initMetaClass(sanvol, sanvol.class);
                sanvol.setStoreUser(it.storeUser);
                sanvol.setScName(it.scName);
                sanvol.setEncryptedPassword(it.encryptedPassword);
                sanvol.setStatus(it.status);
                sanvol.setSnapshotOf(it.snapshot_of);
                dbsvi.add(sanvol);
            }
            dbsvi.commit()
        } 

        if (has_table('eucalyptus_storage', "Igroups")) {
            EntityWrapper<IgroupInfo> dbigroup = EntityWrapper.get(IgroupInfo.class);
            connMap['eucalyptus_storage'].rows('SELECT * FROM Igroups').each{
                IgroupInfo igroup = new IgroupInfo(it.igroup_name, it.volume_name, it.iqn);
                dbigroup.add(igroup);
            }
            dbigroup.commit();
        }

        if (has_table('eucalyptus_storage', "san_info")) {
            EntityWrapper<SANInfo> dbsaninfo = EntityWrapper.get(SANInfo.class);
            connMap['eucalyptus_storage'].rows('SELECT * FROM san_info').each{
                SANInfo s = new SANInfo(it.storage_name, it.san_host, it.san_user, it.san_password);
                dbsaninfo.add(s);
            }
            dbsaninfo.commit();
        }
        return true;
    }
    
    public boolean upgradeCluster() {
        def oldHome = System.getProperty( "euca.upgrade.old.dir" );
        connMap['eucalyptus_config'].rows('SELECT * FROM config_clusters').each{
            EntityWrapper<ClusterConfiguration> dbCluster = EntityWrapper.get(ClusterConfiguration.class);
            EntityWrapper<Partition> dbPart = EntityWrapper.get(Partition.class);
            try {
                 def clCert = connMap['eucalyptus_auth'].firstRow("SELECT * from auth_x509 x join auth_clusters ac ON ac.auth_cluster_x509_certificate=x.id WHERE ac.auth_cluster_name=?", [it.config_component_name]);
                 def nodeCert = connMap['eucalyptus_auth'].firstRow("SELECT * from auth_x509 x join auth_clusters ac ON ac.auth_cluster_node_x509_certificate=x.id WHERE ac.auth_cluster_name=?", [it.config_component_name]);

                 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                 PEMReader pem = new PEMReader(new FileReader("${ oldHome }/var/lib/eucalyptus/keys/${ it.config_component_name }/node-pk.pem"));
                 KeyPair nodeKeyPair = (KeyPair) pem.readObject();
                 pem.close();
                 pem = new PEMReader(new FileReader("${ oldHome }/var/lib/eucalyptus/keys/${ it.config_component_name }/cluster-pk.pem"));
                 KeyPair clusterKeyPair = (KeyPair) pem.readObject();
                 pem.close();
                 LOG.debug("Adding Partition ${ it.config_component_name }");
                 Partition p = new Partition(it.config_component_name, 
                                             clusterKeyPair, 
                                             X509CertHelper.toCertificate(clCert.auth_x509_pem_certificate), 
                                             nodeKeyPair, 
                                             X509CertHelper.toCertificate(nodeCert.auth_x509_pem_certificate));
                 dbPart.add(p);
                 dbPart.commit();
                 LOG.debug("Adding Cluster ${ it.config_component_name }");
                 // First argument is Partition name
                 ClusterConfiguration clcfg = new ClusterConfiguration(it.config_component_name, it.config_component_name, it.config_component_hostname, it.config_component_port);
                 dbCluster.add(clcfg);
                 dbCluster.commit();
            } finally {
                 // NOOP -- should be doing catch / rollback here
            }
        }
        connMap['eucalyptus_config'].rows('SELECT * FROM config_sc').each{
            EntityWrapper<StorageControllerConfiguration> dbSC = EntityWrapper.get(StorageControllerConfiguration.class);
            // First argument is partition name
            StorageControllerConfiguration sc = new StorageControllerConfiguration(it.config_component_name, it.config_component_name, it.config_component_hostname, it.config_component_port);
            if (it.config_component_port == -1 || Internets.testLocal(it.config_component_hostname)) {
                System.setProperty('euca.storage.name', it.config_component_name);
            }
            dbSC.add(sc);
            dbSC.commit();
        }
        return true;
    }   

    static {
        // This is the list of entities which do not need special handling.

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
        entities.add(ISCSIMetaInfo.class);
        entities.add(SnapshotInfo.class);
        entities.add(VolumeInfo.class);
        entities.add(DirectStorageInfo.class);
        entities.add(StorageInfo.class);
        entities.add(StorageStatsInfo.class);
        // Below are for enterprise only
        entities.add(NetappInfo.class);
        entities.add(DASInfo.class);

        // eucalyptus_walrus
        entities.add(ImageCacheInfo.class);
        entities.add(TorrentInfo.class);
        entities.add(WalrusInfo.class);
        entities.add(WalrusSnapshotInfo.class);
        entities.add(WalrusStatsInfo.class);
    }
}
