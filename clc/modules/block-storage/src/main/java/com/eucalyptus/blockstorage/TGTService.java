package com.eucalyptus.blockstorage;

import com.eucalyptus.util.EucalyptusCloudException;

import javax.annotation.Nonnull;

/**
 * Created by wesw on 6/18/14.
 */
public interface TGTService {

    public void start( );

    public void stop( );

    public void precheckService(Long timeout) throws EucalyptusCloudException ;

    public void checkService(Long timeout) throws EucalyptusCloudException ;

    public void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force)
            throws EucalyptusCloudException ;

    public void createLun(@Nonnull String volumeId, int tid, int lun, @Nonnull String resourcePath, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void bindUser (@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void unbindTarget(String volumeId, int tid, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout)
            throws EucalyptusCloudException ;

    public void deleteUser(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException ;

    public boolean userExists(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException ;

}
