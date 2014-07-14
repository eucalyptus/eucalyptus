package com.eucalyptus.blockstorage.util;

import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partition;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Created by wesw on 6/18/14.
 */
public class BlockStorageUtilSvcImpl implements BlockStorageUtilSvc {

    @Override
    public <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException {
        return BlockStorageUtil.getPartitionForLocalService(compClass);
    }

    @Override
    public String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException {
        return BlockStorageUtil.encryptNodeTargetPassword(password, partition);
    }

    @Override
    public String encryptSCTargetPassword(String password) throws EucalyptusCloudException {
        return BlockStorageUtil.encryptSCTargetPassword(password);
    }

    @Override
    public String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException {
        return BlockStorageUtil.decryptSCTargetPassword(encryptedPassword);
    }

    @Override
    public String encryptForNode(String data, Partition partition) throws EucalyptusCloudException {
        return BlockStorageUtil.encryptForNode(data, partition);
    }

    @Override
    public String decryptForNode(String data, Partition partition) throws EucalyptusCloudException {
        return BlockStorageUtil.decryptForNode(data, partition);
    }

    @Override
    public String encryptForCloud(String data) throws EucalyptusCloudException {
        return BlockStorageUtil.encryptForCloud(data);
    }

    @Override
    public String decryptWithCloud(String data) throws EucalyptusCloudException {
        return BlockStorageUtil.decryptWithCloud(data);
    }

    @Override
    public Role checkAndConfigureBlockStorageAccount() throws EucalyptusCloudException {
        return BlockStorageUtil.checkAndConfigureBlockStorageAccount();
    }
}
