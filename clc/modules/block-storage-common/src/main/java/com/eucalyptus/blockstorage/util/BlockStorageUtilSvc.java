package com.eucalyptus.blockstorage.util;

import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partition;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Created by wesw on 6/18/14.
 */
public interface BlockStorageUtilSvc {

    public <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException;

    public String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException;

    public String encryptSCTargetPassword(String password) throws EucalyptusCloudException ;

    public String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException ;

    public String encryptForNode(String data, Partition partition) throws EucalyptusCloudException ;

    public String decryptForNode(String data, Partition partition) throws EucalyptusCloudException ;

    public String encryptForCloud(String data) throws EucalyptusCloudException ;

    public String decryptWithCloud(String data) throws EucalyptusCloudException ;

    public Role checkAndConfigureBlockStorageAccount() throws EucalyptusCloudException ;

}
