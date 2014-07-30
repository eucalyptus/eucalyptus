package com.eucalyptus.blockstorage;

import com.eucalyptus.util.EucalyptusCloudException;

import javax.annotation.Nonnull;

/**
 * Created by wesw on 6/18/14.
 */
public class TGTServiceUsingTGTWrapper implements TGTService {

    @Override
    public void start() {
        TGTWrapper.start();
    }

    @Override
    public void stop() {
        TGTWrapper.stop();
    }

    @Override
    public void precheckService(Long timeout) throws EucalyptusCloudException {
        TGTWrapper.precheckService(timeout);
    }

    @Override
    public void checkService(Long timeout) throws EucalyptusCloudException {
        TGTWrapper.checkService(timeout);
    }

    @Override
    public void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.createTarget(volumeId, tid, name, timeout);
    }

    @Override
    public void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force) throws EucalyptusCloudException {
        TGTWrapper.deleteTarget(volumeId, tid, timeout, force);
    }

    @Override
    public void createLun(@Nonnull String volumeId, int tid, int lun, @Nonnull String resourcePath, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.createLun(volumeId, tid, lun, resourcePath, timeout);
    }

    @Override
    public void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.deleteLun(volumeId, tid, lun, timeout);
    }

    @Override
    public void bindUser(@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.bindUser(volumeId, user, tid, timeout);
    }

    @Override
    public void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.bindTarget(volumeId, tid, timeout);
    }

    @Override
    public void unbindTarget(String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.unbindTarget(volumeId, tid, timeout);
    }

    @Override
    public boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout) throws EucalyptusCloudException {
        return TGTWrapper.targetExists(volumeId, tid, resource, timeout);
    }

    @Override
    public boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException {
        return TGTWrapper.targetHasLun(volumeId, tid, lun, timeout);
    }

    @Override
    public void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.addUser(username, password, timeout);
    }

    @Override
    public void deleteUser(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException {
        TGTWrapper.deleteUser(username, timeout);
    }

    @Override
    public boolean userExists(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException {
        return TGTWrapper.userExists(username, timeout);
    }
}
