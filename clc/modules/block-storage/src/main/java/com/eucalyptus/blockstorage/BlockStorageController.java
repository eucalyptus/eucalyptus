/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.entities.BlockStorageGlobalConfiguration;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.entities.VolumeExportRecord;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.entities.VolumeToken;
import com.eucalyptus.blockstorage.exceptions.AccessDeniedException;
import com.eucalyptus.blockstorage.exceptions.SnapshotNotFoundException;
import com.eucalyptus.blockstorage.exceptions.SnapshotTooLargeException;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeType;
import com.eucalyptus.blockstorage.msgs.ConvertVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.ConvertVolumesType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.msgs.StorageSnapshot;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationType;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.InvalidParameterValueException;
import edu.ucsb.eucalyptus.cloud.NoSuchVolumeException;
import edu.ucsb.eucalyptus.cloud.SnapshotInUseException;
import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.VolumeNotReadyException;
import edu.ucsb.eucalyptus.cloud.VolumeSizeExceededException;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

public class BlockStorageController {
    private static Logger LOG = Logger.getLogger(BlockStorageController.class);

    static LogicalStorageManager blockManager;
    static BlockStorageChecker checker;
    static VolumeService volumeService;
    static SnapshotService snapshotService;
    static StorageCheckerService checkerService;

    //TODO: zhill, this can be added later for snapshot abort capabilities
    //static ConcurrentHashMap<String,HttpTransfer> httpTransferMap; //To keep track of current transfers to support aborting

    public static Random randomGenerator = new Random();

    //Introduced for testing EUCA-9297 fix: allows artificial capacity changes of backend
    static boolean setUseTestingDelegateManager(boolean enableDelegate) {
        if(enableDelegate && !(blockManager instanceof StorageManagerTestingProxy)) {
            LOG.info("Switching to use delegating storage manager for testing");
            blockManager = new StorageManagerTestingProxy(blockManager);
        } else if(!enableDelegate && (blockManager instanceof StorageManagerTestingProxy)) {
            LOG.info("Switching to NOT use delegating storage manager anymore");
            blockManager = ((StorageManagerTestingProxy)blockManager).getDelegateStorageManager();
        }
        return enableDelegate;
    }

    public static void configure() throws EucalyptusCloudException {
    	BlockStorageGlobalConfiguration.getInstance();
        StorageProperties.updateWalrusUrl();
        StorageProperties.updateName();
        StorageProperties.updateStorageHost();

        try {
            blockManager = StorageManagers.getInstance();
            if(blockManager != null) {
                blockManager.initialize();
            }
            else {
                throw new EucalyptusCloudException("Got null block manager. Cannot configure.");
            }
        } catch (Exception e) {
            throw new EucalyptusCloudException(e);
        }

        volumeService = new VolumeService();
        snapshotService = new SnapshotService();
        checkerService = new StorageCheckerService();
        
        try {
        	BlockStorageUtil.checkAndConfigureBlockStorageAccount();
        } catch (Exception e) {
        	LOG.warn("Error checking and or configuring blockstorage account during bootstrap. Moving on with the SC bootstrap process");
        }
    }

    public BlockStorageController() {}

    // for unit testing with a mock implementation
    public BlockStorageController( LogicalStorageManager blockManager ) {
        this.blockManager = blockManager;
    }

    private static void startupChecks() throws EucalyptusCloudException {
        if(checker != null) {
            checker.startupChecks();
        }
    }

    public static void checkPending() {
        if(checker != null) {
            StorageProperties.updateWalrusUrl();
            try {
                checker.transferPendingSnapshots();
            } catch (Exception ex) {
                LOG.error("unable to transfer pending snapshots", ex);
            }
        }
    }

    public static void check() throws EucalyptusCloudException {
        blockManager.checkReady();
    }

    public static void stop() throws EucalyptusCloudException {
        if(blockManager != null) {
            LOG.info("Stopping blockmanager");
            blockManager.stop();
        }
        //clean all state.
        blockManager = null;
        checker = null;
        if(volumeService != null) {
            volumeService.shutdown();
        }
        if(snapshotService != null) {
            snapshotService.shutdown();
        }
        if(checkerService != null) {
            checkerService.shutdown();
        }
        StorageProperties.enableSnapshots = false;
    }

    public static void enable() throws EucalyptusCloudException {
        blockManager.configure();
        //blockManager.initialize();
        blockManager.enable();
        checkerService.add(new VolumeStateChecker(blockManager));
        //add any block manager checkers
        for(CheckerTask checker : blockManager.getCheckers()) {
            checkerService.add(checker);
        }
        checkerService.add(new VolumeDeleterTask());
        checkerService.add(new SnapshotDeleterTask());
        checkerService.add(new SnapshotUploadCheckerTask());
        // TODO ask neil what this means
        StorageProperties.enableSnapshots = StorageProperties.enableStorage = true;
        checker = new BlockStorageChecker(blockManager);
        try {
            startupChecks();
        } catch(EucalyptusCloudException ex) {
            LOG.error("Startup checks failed ", ex);
        }
    }

    public static void disable() throws EucalyptusCloudException {
        blockManager.disable();
    }

    public static void addChecker(CheckerTask checkerTask) {
        if(checkerService != null) {
            checkerService.add(checkerTask);
        }
    }
    
    public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) throws EucalyptusCloudException {
      UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
        if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
            throw new AccessDeniedException("Only admin can change walrus properties.");
        //test connection to ObjectStorage
        StorageProperties.updateWalrusUrl();
        try {
            blockManager.checkPreconditions();
            StorageProperties.enableStorage = true;
        } catch (Exception ex) {
            StorageProperties.enableStorage = false;
            LOG.error(ex);
        }
        if(request.getStorageParams() != null) {
            for(ComponentProperty param : request.getStorageParams()) {
                LOG.debug("Storage Param: " + param.getDisplayName() + " Qname: " + param.getQualifiedName() + " Value: " + param.getValue());
            }
            blockManager.setStorageProps(request.getStorageParams());
        }
        return reply;
    }

    public GetStorageConfigurationResponseType GetStorageConfiguration(GetStorageConfigurationType request) throws EucalyptusCloudException {
        GetStorageConfigurationResponseType reply = (GetStorageConfigurationResponseType) request.getReply();
        StorageProperties.updateName();
        if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
            throw new AccessDeniedException("Only admin can change walrus properties.");
        if(StorageProperties.NAME.equals(request.getName())) {
            reply.setName(StorageProperties.NAME);
            ArrayList<ComponentProperty> storageParams = blockManager.getStorageProps();
            reply.setStorageParams(storageParams);
        }
        return reply;
    }

    public GetVolumeTokenResponseType GetVolumeToken(GetVolumeTokenType request) throws EucalyptusCloudException {
        GetVolumeTokenResponseType reply = (GetVolumeTokenResponseType) request.getReply();
        String volumeId = request.getVolumeId();
        LOG.info("Processing GetVolumeToken request for volume " + volumeId);

        if(null == volumeId) {
            LOG.error("Cannot get token for a null-valued volumeId");
            throw new EucalyptusCloudException("No volumeId specified in token request");
        }

        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo vol = Entities.uniqueResult(new VolumeInfo(volumeId));
            VolumeToken token = vol.getOrCreateAttachmentToken();

            //Encrypt the token with the NC's private key
            String encryptedToken = BlockStorageUtil.encryptForNode(token.getToken(), BlockStorageUtil.getPartitionForLocalService(Storage.class));
            reply.setToken(encryptedToken);
            reply.setVolumeId(volumeId);
            tran.commit();    
            LOG.debug(reply.toSimpleString());
            return reply;
        } catch(NoSuchElementException e) {
            throw new EucalyptusCloudException("Volume " + request.getVolumeId() + " not found", e);
        } catch(Exception e) {
            LOG.error("Failed to get volume token: " + e.getMessage());
            throw new EucalyptusCloudException("Could not get volume token for volume " + request.getVolumeId(), e);
        }
    }

    /**
     * Removes connection authorization for the specified iqn/ip pair in the request using
     * the specified token. Only performs the operation if the token is valid for the specified volume.
     *
     * Invalidates the token upon successful de-authorization.
     * @param request
     * @return
     * @throws EucalyptusCloudException
     */
    public UnexportVolumeResponseType UnexportVolume(UnexportVolumeType request) throws EucalyptusCloudException {
        UnexportVolumeResponseType reply = request.getReply();
        final String token = request.getToken();
        final String volumeId = request.getVolumeId();
        final String nodeIqn = request.getIqn();
        final String nodeIp = request.getIp();

        LOG.info("Processing UnexportVolume request for volume " + volumeId + " from node " + nodeIp + " with iqn " + nodeIqn);

        VolumeInfo volumeEntity = null;
        VolumeToken validToken = null;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo foundVolume = Entities.uniqueResult(new VolumeInfo(volumeId));
            volumeEntity = Entities.merge(foundVolume);

            try {
                validToken = volumeEntity.getAttachmentTokenIfValid(token);
                //volumeEntity.invalidateExport(tokenValue, nodeIp, nodeIqn);
                //Entities.flush(volumeEntity); //Sync state -- not needed.... same transaction
            } catch(Exception e) {
                LOG.error("Invalid token in request for volume " + volumeId + ". Encrypted token: " + token);
                throw new EucalyptusCloudException(e);
            }


            if(validToken.hasOnlyExport(nodeIp, nodeIqn)) {
                //There are no active exports, so unexport all.
                blockManager.unexportVolumeFromAll(volumeId);
            } else {
                try {
                    blockManager.unexportVolume(volumeEntity.getVolumeId(), nodeIqn);
                } catch(UnsupportedOperationException e) {
                    //The backend doesn't support unexport to just one host... this is a noop.
                    LOG.info("Volume " + volumeId + ": UnexportVolume for single host not supported by backend. Treating as no-op and continuing normally.");
                } catch(Exception e) {
                    LOG.error("Could not detach volume: " + volumeId, e);
                    throw e;
                }
            }

            //Do the actual invalidation. Handle retries, but only on the DB part.
            if(!Entities.asTransaction(VolumeInfo.class, new Function<VolumeInfo, Boolean>() {
                @Override
                public Boolean apply(VolumeInfo vol) {
                    VolumeInfo entity = Entities.merge(vol);
                    try {
                        entity.invalidateExport(token, nodeIp, nodeIqn);
                        return true;
                    } catch(Exception e) {
                        LOG.error("Error invalidating export: " + e.getMessage());
                        return false;
                    }
                }
            }).apply(volumeEntity)) {
                //Transaction failed after retries...
                LOG.error("Error invalidating the export record in the DB for volume " + volumeId);
            }

            tran.commit();
            reply.set_return(true);
        } catch(NoSuchElementException e) {
            LOG.error("Volume " + volumeId + " not found in DB",e);
            throw new EucalyptusCloudException("Volume " + volumeId + " not found");
        } catch(Exception e) {
            LOG.error("Failed UnexportVolume due to: " + e.getMessage(), e);
            throw new EucalyptusCloudException(e);
        }
        return reply;
    }

    /**
     * Perform a volume export validated by the token presented in the request.
     * Upon completion of the Export operation, the identified host (by ip and iqn) will
     * have access to connect to the requested volume. No connection is made, just the authorization.
     *
     * If a valid export record exists for the given token and host information, then the connectionString
     * for that record is returned rather than creating a new record.
     * @param request
     * @return
     * @throws EucalyptusCloudException
     */
    public ExportVolumeResponseType ExportVolume(ExportVolumeType request) throws EucalyptusCloudException {
        final ExportVolumeResponseType reply = (ExportVolumeResponseType) request.getReply();
        final String volumeId = request.getVolumeId();
        final String token = request.getToken();
        final String ip = request.getIp();
        final String iqn = request.getIqn();
        reply.setVolumeId(volumeId);

        LOG.info("Processing ExportVolume request for volume " + volumeId);

        final Function<VolumeInfo, String> exportAndAttach = new Function<VolumeInfo, String>() {
            @Override
            public String apply(VolumeInfo volume) {
                int tokenRetry = 3;
                VolumeToken tokenInfo = null;
                VolumeInfo volEntity = Entities.merge(volume);
                for(int i = 0; i < tokenRetry ; i++ ) {
                    try {
                        tokenInfo = volEntity.getAttachmentTokenIfValid(token);
                        if(tokenInfo != null) {
                            break;
                        }
                    } catch(Exception e) {
                        LOG.warn("Could not check for valid token. Will retry. ", e);
                        tokenInfo = null;
                    }
                    try {
                        Thread.sleep(100); //sleep 100ms to make retry useful.
                    } catch(InterruptedException e) {
                        throw new RuntimeException("Token check backoff sleep interrupted", e);
                    }
                }

                if(tokenInfo == null) {
                    throw new RuntimeException("Cannot export, due to invalid token");
                }

                VolumeExportRecord export = null;
                try {
                    export = tokenInfo.getValidExport(ip, iqn);
                } catch (EucalyptusCloudException e2) {
                    LOG.error("Failed when checking/getting valid export for " + ip + " - " + iqn);
                    return null;
                }

                if(export == null) {
                    String connectionString = null;
                    try {
                        //attachVolume must be idempotent.
                        connectionString = blockManager.exportVolume(volumeId, iqn);
                    } catch(Exception e) {
                        LOG.error("Could not attach volume: " + e.getMessage());
                        LOG.trace("Failed volume attach",e);
                        return null;
                    }

                    try{
                        //addExport must be idempotent, if one exists a new is not added with same data
                        tokenInfo.addExport(ip, iqn, connectionString);
                        return connectionString;
                    } catch(Exception e) {
                        LOG.error("Could not export volume " + volumeId + " failed to add export record");
                        try {
                            LOG.info("Unexporting volume " + volumeId + " to " + iqn + " for export failure cleanup");
                            blockManager.unexportVolume(volumeId, iqn);
                        } catch (EucalyptusCloudException e1) {
                            LOG.error("Failed to detach volume during invalidation failure", e);
                        }
                        return null;
                    }
                } else {
                    LOG.debug("Found extant valid export for " + ip + " and " + iqn + " returning connection information for that export");
                    return export.getConnectionString();
                }
            }
        };

        VolumeInfo searchVol = new VolumeInfo(volumeId);
        VolumeInfo vol = null;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            vol = Entities.uniqueResult(searchVol);
            tran.commit();
        } catch(NoSuchElementException e) {
            LOG.error("No volume db record found for " + volumeId,e);
            throw new EucalyptusCloudException("Volume not found " + volumeId);
        } catch (TransactionException e) {
            LOG.error("Failed to Export due to db error",e);
            throw new EucalyptusCloudException("Could not export volume",e);
        }

        //Do the export
        try {
            String connectionString = Entities.asTransaction(VolumeInfo.class, exportAndAttach).apply(vol);
            if(connectionString != null) {
                reply.setConnectionString(connectionString);
            } else {
                throw new Exception("Got null record result. Cannot set connection string");
            }
        } catch(Exception e) {
            LOG.error("Failed ExportVolume transaction due to: " + e.getMessage(), e);
            throw new EucalyptusCloudException("Failed to add export",e);
        }
        return reply;
    }

    public GetStorageVolumeResponseType GetStorageVolume(GetStorageVolumeType request) throws EucalyptusCloudException {
        GetStorageVolumeResponseType reply = (GetStorageVolumeResponseType) request.getReply();
        if(!StorageProperties.enableStorage) {
            LOG.error("BlockStorage has been disabled. Please check your setup");
            return reply;
        }

        String volumeId = request.getVolumeId();
        LOG.info("Processing GetStorageVolume request for volume " + volumeId);

        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo volumeInfo = new VolumeInfo();
            volumeInfo.setVolumeId(volumeId);
            List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
            if (volumeInfos.size() > 0) {
                VolumeInfo foundVolumeInfo = volumeInfos.get(0);
                String deviceName = blockManager.getVolumeConnectionString(volumeId);
                reply.setVolumeId(foundVolumeInfo.getVolumeId());
                reply.setSize(foundVolumeInfo.getSize().toString());
                reply.setStatus(foundVolumeInfo.getStatus());
                reply.setSnapshotId(foundVolumeInfo.getSnapshotId());
                if (deviceName != null)
                    reply.setActualDeviceName(deviceName);
                else
                    reply.setActualDeviceName("invalid");
            } else {
                throw new NoSuchVolumeException(volumeId);
            }
            tran.commit();
        }
        return reply;
    }

    public DeleteStorageVolumeResponseType DeleteStorageVolume(DeleteStorageVolumeType request) throws EucalyptusCloudException {
        DeleteStorageVolumeResponseType reply = (DeleteStorageVolumeResponseType) request.getReply();
        if(!StorageProperties.enableStorage) {
            LOG.error("BlockStorage has been disabled. Please check your setup");
            return reply;
        }

        String volumeId = request.getVolumeId();
        LOG.info("Processing DeleteStorageVolume request for volume " + volumeId);

        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo foundVolume = Entities.uniqueResult(volumeInfo);
            //check its status
            String status = foundVolume.getStatus();
            if(status == null) {
                throw new EucalyptusCloudException("Invalid volume status: null");
            } else if(status.equals(StorageProperties.Status.available.toString()) ||
                    status.equals(StorageProperties.Status.failed.toString())) {
                //Set status, for cleanup thread to find.
                LOG.trace("Marking volume " + volumeId + " for deletion");
                foundVolume.setStatus(StorageProperties.Status.deleting.toString());
            } else if(status.equals(StorageProperties.Status.deleting.toString()) || status.equals(StorageProperties.Status.deleted.toString()) ) {
                LOG.debug("Volume " + volumeId + " already in deleting/deleted. No-op for delete request.");
            } else {
                throw new EucalyptusCloudException("Cannot delete volume in state: " +  status + ". Please retry later");
            }
            // Delete operation should be idempotent as multiple attempts can be made to delete the same volume
            // Set the response element to true if the volume entity is found. EUCA-6093
            reply.set_return(Boolean.TRUE);
            tran.commit();
        } catch(NoSuchElementException e) {
            // Set the response element to false if the volume entity does not exist in the SC database
            // if record is not found, delete is idempotent
            LOG.warn("Got delete request, but unable to find volume in SC database: " + volumeId);
            reply.set_return(Boolean.TRUE);
        } catch(EucalyptusCloudException e) {
            LOG.error("Error marking volume " + volumeId + " for deletion: " + e.getMessage());
            throw e;
        } catch(final Throwable e) {
            LOG.error("Exception looking up volume: " + volumeId, e);
            throw new EucalyptusCloudException(e);
        }
        return reply;
    }

    /**
     * Checks to see if a new snapshot of size volSize will exceed the quota
     * @param volSize
     * @param maxSize 
     * @return
     */
    private boolean totalSnapshotSizeLimitExceeded(String snapshotId, int volSize, int sizeLimitGB) throws EucalyptusCloudException {

        int totalSnapshotSize = 0;
        try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
            Criteria query = Entities.createCriteria(SnapshotInfo.class);
            query.setReadOnly(true);

            //Only look for snaps that are not failed and not error
            ImmutableSet<String> excludedStates = ImmutableSet.of(StorageProperties.Status.failed.toString(),
                    StorageProperties.Status.error.toString(), StorageProperties.Status.deleted.toString());

            query.add(Restrictions.not(Restrictions.in("status", excludedStates)));

            //The listing may include duplicates (for snapshots cached on multiple clusters), this set ensures each unique snap id is counted only once.
            HashSet<String> idSet = new HashSet<String>();
            List<SnapshotInfo> snapshots = (List<SnapshotInfo>)query.list();
            tran.commit();
            for (SnapshotInfo snap : snapshots) {
                totalSnapshotSize += (snap.getSizeGb() != null && idSet.add(snap.getSnapshotId()) ? snap.getSizeGb() : 0);
            }
            LOG.debug("Snapshot " + snapshotId + " checking snapshot total size of  " + totalSnapshotSize + " against limit of " + sizeLimitGB);
            return (totalSnapshotSize + volSize) > sizeLimitGB;
        } catch(final Throwable e) {
            LOG.error("Error finding total snapshot used size " + e.getMessage());
            throw new EucalyptusCloudException("Failed to check snapshot total size limit",e);
        }
    }

    public CreateStorageSnapshotResponseType CreateStorageSnapshot( CreateStorageSnapshotType request ) throws EucalyptusCloudException {
        CreateStorageSnapshotResponseType reply = ( CreateStorageSnapshotResponseType ) request.getReply();

        StorageProperties.updateWalrusUrl();
        if(!StorageProperties.enableSnapshots) {
            LOG.error("Snapshots have been disabled. Please check connection to ObjectStorage.");
            return reply;
        }

        String volumeId = request.getVolumeId();
        LOG.info("Processing CreateStorageSnapshot request for volume " + volumeId);

        String snapshotId = request.getSnapshotId();
        VolumeInfo sourceVolumeInfo = null;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo volumeInfo = new VolumeInfo(volumeId);
            sourceVolumeInfo = Entities.uniqueResult(volumeInfo);
            tran.commit();
        } catch(NoSuchElementException e) {
            LOG.debug("Volume " + volumeId + " not found in db");
            throw new NoSuchVolumeException(volumeId);
        } catch(final Throwable e) {
            LOG.warn("Volume " + volumeId + " error getting info from db. May not exist. " + e.getMessage());
            throw new EucalyptusCloudException("Could not get volume information for volume " + volumeId, e);
        }

        if(sourceVolumeInfo == null) {
            //Another check to be sure that we have the source volume
            throw new NoSuchVolumeException(volumeId);
        } else {
            //check status
            if(!sourceVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                throw new VolumeNotReadyException(volumeId);
            } else {
                //create snapshot
                if(StorageProperties.shouldEnforceUsageLimits) {
                    int maxSize = -1;
                    try {
                        maxSize = BlockStorageGlobalConfiguration.getInstance().getGlobal_total_snapshot_size_limit_gb();
                    } catch(Exception e) {
                        LOG.error("Could not determine max global snapshot limit. Aborting snapshot creation", e);
                        throw new EucalyptusCloudException("Total size limit not found.", e);
                    } 
                    if(maxSize <= 0) {
                    	LOG.warn("Total snapshot size limit is less than or equal to 0");
                    	throw new EucalyptusCloudException("Total snapshot size limit is less than or equal to 0");
                    }
                    if(totalSnapshotSizeLimitExceeded(snapshotId, sourceVolumeInfo.getSize(), maxSize)) {	
                    	LOG.info("Snapshot " + snapshotId + " exceeds total snapshot size limit of " + maxSize + "GB");
                    	throw new SnapshotTooLargeException(snapshotId, maxSize);
                    }
                }

                Snapshotter snapshotter = null;
                SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
                Date startTime = new Date();
                try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
                    snapshotInfo.setUserName(sourceVolumeInfo.getUserName());
                    snapshotInfo.setVolumeId(volumeId);
                    snapshotInfo.setStartTime(startTime);
                    snapshotInfo.setProgress("0");
                    snapshotInfo.setSizeGb(sourceVolumeInfo.getSize());
                    snapshotInfo.setStatus(StorageProperties.Status.creating.toString());

					/* Change to support sync snap consistency point set on CLC round-trip */
					/*
					 * Always do this operation. On backends that don't support it they will
					 * return null. In that case it is effectively a no-op and we continue normal
					 * async snapshot.
					 * 
					 * If the snap point is set, then we update the DB properly. 
					 */
                    String snapPointId = null;
                    try {
                        //This will be a no-op if the backend doesn't support it. Will return null.
                        snapPointId = blockManager.createSnapshotPoint(volumeId, snapshotId);
                        if(snapPointId == null) {
                            LOG.debug("Synchronous snap point not supported for this backend. Cleanly skipped.");
                        } else {
                            snapshotInfo.setSnapPointId(snapPointId);
                        }
                        //Do a commit here because the snapshotter expects to find db entry.
                        snapshotInfo.setStatus(StorageProperties.Status.creating.toString());

                        Context ctx = null;
                        try {
                            ctx = Contexts.lookup(request.getCorrelationId());
                            if(!ctx.getChannel().isOpen()) {
                                throw new NoSuchContextException("Channel is closed");
                            }
                        } catch(NoSuchContextException e) {
                            if(snapPointId != null) {
                                //Other end hung up, mark this as failed since this is a sync operation
                                throw new EucalyptusCloudException("Channel closed, aborting snapshot.");
                            }
                        }
                    } catch(EucalyptusCloudException e) {
                        //If the snapshot was done but took too long then delete the snap and fail the op.
                        try {
                            blockManager.deleteSnapshotPoint(volumeId, snapshotId, snapPointId);
                        } catch(Exception ex) {
                            LOG.error("Snapshot " + snapshotId + " exception on snap point cleanup after failure: " + e.getMessage());
                        }
                        LOG.error("Snapshot " + snapshotId + " failed to create snap point successfully: " + e.getMessage());
                        throw e;
                    } finally {
                        Entities.persist(snapshotInfo);
                        tran.commit();
                    }

					/* Resume old code path and finish the snapshot process if already started */
                    //snapshot asynchronously
                    snapshotter = new Snapshotter(volumeId, snapshotId, snapPointId);

                    reply.setSnapshotId(snapshotId);
                    reply.setVolumeId(volumeId);
                    reply.setStartTime(DateUtils.format(startTime.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                    reply.setProgress(snapshotInfo.getProgress());
                } catch(EucalyptusCloudException cloudEx) {
                    snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
                    LOG.error("Snapshot " + snapshotId + " creation failed with exception ", cloudEx);
                    throw cloudEx;
                } catch(final Throwable e) {
                    snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
                    LOG.error("Snapshot " + snapshotId + " Error committing state update to failed", e);
                    throw new EucalyptusCloudException("Snapshot " + snapshotId + " unexpected throwable exception caught", e);
                }
                reply.setStatus(snapshotInfo.getStatus());
				if (snapshotter != null) { // Kick off the snapshotter task after persisting snapshot to database
					snapshotService.add(snapshotter);
				}
            }
        }
        return reply;
    }

    //returns snapshots in progress or at the SC
    public DescribeStorageSnapshotsResponseType DescribeStorageSnapshots( DescribeStorageSnapshotsType request ) throws EucalyptusCloudException {
        DescribeStorageSnapshotsResponseType reply = ( DescribeStorageSnapshotsResponseType ) request.getReply();
        checker.transferPendingSnapshots();
        List<String> snapshotSet = request.getSnapshotSet();
        ArrayList<SnapshotInfo> snapshotInfos = new ArrayList<SnapshotInfo>();
        try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
            if ((snapshotSet != null) && !snapshotSet.isEmpty()) {
                for (String snapshotSetEntry : snapshotSet) {
                    SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotSetEntry);
                    List<SnapshotInfo> foundSnapshotInfos = Entities.query(snapshotInfo);
                    if (foundSnapshotInfos.size() > 0) {
                        snapshotInfos.add(foundSnapshotInfos.get(0));
                    }
                }
            } else {
                SnapshotInfo snapshotInfo = new SnapshotInfo();
                List<SnapshotInfo> foundSnapshotInfos = Entities.query(snapshotInfo);
                for (SnapshotInfo snapInfo : foundSnapshotInfos) {
                    snapshotInfos.add(snapInfo);
                }
            }

            ArrayList<StorageSnapshot> snapshots = reply.getSnapshotSet();
            for (SnapshotInfo snapshotInfo : snapshotInfos) {
                snapshots.add(convertSnapshotInfo(snapshotInfo));
                if (snapshotInfo.getStatus().equals(StorageProperties.Status.failed.toString()))
                    checker.cleanFailedSnapshot(snapshotInfo.getSnapshotId());
            }
            tran.commit();
        }
        return reply;
    }

    /**
     * Delete snapshot in idempotent way. Multiple requests for same snapshotId should
     * return true. Only return false if the snapsnot *cannot* be deleted but does exist
     * @param request
     * @return
     * @throws EucalyptusCloudException
     */
    public DeleteStorageSnapshotResponseType DeleteStorageSnapshot( DeleteStorageSnapshotType request ) throws EucalyptusCloudException {
        DeleteStorageSnapshotResponseType reply = ( DeleteStorageSnapshotResponseType ) request.getReply();

        StorageProperties.updateWalrusUrl();
        if(!StorageProperties.enableSnapshots) {
            LOG.error("Snapshots have been disabled. Please check connection to ObjectStorage.");
            return reply;
        }

        String snapshotId = request.getSnapshotId();
        LOG.info("Processing DeleteStorageSnapshot request for snapshot " + snapshotId);

        try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
            List<SnapshotInfo> snapshotInfos = Entities.query(snapshotInfo);

            reply.set_return(false);
            if (snapshotInfos.size() > 0) {
                SnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
                String status = foundSnapshotInfo.getStatus();
                if (status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
                    foundSnapshotInfo.setStatus(StorageProperties.Status.deleting.toString());
                    tran.commit();
                    reply.set_return(true);
                } else {
                    //snapshot is still in progress.
                    tran.rollback();
                    throw new SnapshotInUseException(snapshotId);
                }
            } else {
                //the SC knows nothing about this snapshot, either never existed or was deleted
                //For idempotent behavior, tell backend to delete and return true
                reply.set_return(true);
                tran.rollback();
            }
        }
        return reply;
    }

	/* TODO: zhill, removed this since it isn't necessary, but can be added-back later when we have time for full dev and testing
	 * public AbortStorageSnapshotResponseType AbortSnapshotPoint( AbortStorageSnapshotType request ) throws EucalyptusCloudException {
		AbortStorageSnapshotResponseType reply = ( AbortStorageSnapshotResponseType ) request.getReply();
		String snapshotId = request.getSnapshotId();
		reply.set_return(true);

		try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
    		SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(new SnapshotInfo(snapshotId));
    		String status = foundSnapshotInfo.getStatus();
    		if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
    			foundSnapshotInfo.setStatus(StorageProperties.Status.deleting.toString());
    			tr.commit();
			} else {
				//snapshot is still in progress.
				foundSnapshotInfo.setStatus(StorageProperties.Status.failed.toString());
				tr.commit();
				checker.cleanFailedSnapshot(snapshotId);
			}
    	} catch (NoSuchElementException e) {
    		//the SC knows nothing about this snapshot.
    		LOG.debug("Snapshot " + snapshotId + " not found");
		} catch (Exception e) {
			LOG.error("Failed to abort snapshot " + snapshotId, e);
			throw new EucalyptusCloudException("Failed to abort snapshot " + snapshotId, e)
		}
		
		return reply;
	}*/

    public CreateStorageVolumeResponseType CreateStorageVolume(CreateStorageVolumeType request) throws EucalyptusCloudException {
        CreateStorageVolumeResponseType reply = (CreateStorageVolumeResponseType) request.getReply();

        if(!StorageProperties.enableStorage) {
            LOG.error("BlockStorage has been disabled. Please check your setup");
            return reply;
        }

        String snapshotId = request.getSnapshotId();
        String parentVolumeId = request.getParentVolumeId();
        String userId = request.getUserId();
        String volumeId = request.getVolumeId();
        LOG.info("Processing CreateStorageVolume request for volume " + volumeId);

        //in GB
        String size = request.getSize();
        int sizeAsInt = (size != null) ? Integer.parseInt(size) : 0;
        if (size != null && sizeAsInt <= 0) {
            throw new InvalidParameterValueException("The parameter size (" + sizeAsInt + ") must be greater than zero.");
        }
        if(StorageProperties.shouldEnforceUsageLimits) {
            if(size != null) {
                int totalVolumeSize = 0;
                VolumeInfo volInfo = new VolumeInfo();
                try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class) ) {
                    List<VolumeInfo> volInfos = Entities.query(volInfo);
                    for (VolumeInfo vInfo : volInfos) {
                        if (!vInfo.getStatus().equals(StorageProperties.Status.failed.toString()) &&
                                !vInfo.getStatus().equals(StorageProperties.Status.error.toString()) &&
                                !vInfo.getStatus().equals(StorageProperties.Status.deleted.toString())) {
                            totalVolumeSize += vInfo.getSize();
                        }
                    }
                    tran.commit();
                }
                if(((totalVolumeSize + sizeAsInt) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb())) {
                    throw new VolumeSizeExceededException(volumeId, "Total Volume Size Limit Exceeded");
                }
                if(sizeAsInt > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB()) {
                    throw new VolumeSizeExceededException(volumeId, "Max Volume Size Limit Exceeded");
                }
            }
        }

        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo volumeInfo = new VolumeInfo(volumeId);
            List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
            if (volumeInfos.size() > 0) {
                throw new VolumeAlreadyExistsException(volumeId);
            }
            if (snapshotId != null) {
                SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
                snapInfo.setScName(null);
                snapInfo.setStatus(StorageProperties.Status.available.toString());
                List<SnapshotInfo> snapInfos = Entities.query(snapInfo);
                if (snapInfos.size() == 0) {
                    throw new SnapshotNotFoundException("Snapshot " + snapshotId + " does not exist or is unavailable");
                }
                volumeInfo.setSnapshotId(snapshotId);
                reply.setSnapshotId(snapshotId);
            }
            volumeInfo.setUserName(userId);
            volumeInfo.setSize(sizeAsInt);
            volumeInfo.setStatus(StorageProperties.Status.creating.toString());
            Date creationDate = new Date();
            volumeInfo.setCreateTime(creationDate);
            Entities.persist(volumeInfo);
            reply.setVolumeId(volumeId);
            reply.setCreateTime(DateUtils.format(creationDate.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
            reply.setSize(size);
            reply.setStatus(volumeInfo.getStatus());
            tran.commit();
        }
        //create volume asynchronously
        VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, parentVolumeId, sizeAsInt);
        volumeService.add(volumeCreator);

        return reply;
    }


    /*
     * Does a check of the snapshot's status as reflected in the DB.
     */
    private static boolean isSnapshotMarkedFailed(String snapshotId) {
        try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
            tran.setRollbackOnly();
            SnapshotInfo snap = Entities.uniqueResult(new SnapshotInfo(snapshotId));
            if(snap != null && StorageProperties.Status.failed.toString().equals(snap.getStatus())) {
                return true;
            }
        } catch(Exception e) {
            LOG.error("Error determining status of snapshot " + snapshotId);
        }
        return false;
    }

    public DescribeStorageVolumesResponseType DescribeStorageVolumes(DescribeStorageVolumesType request) throws EucalyptusCloudException {
        DescribeStorageVolumesResponseType reply = (DescribeStorageVolumesResponseType) request.getReply();

        List<String> volumeSet = request.getVolumeSet();
        ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            if ((volumeSet != null) && !volumeSet.isEmpty()) {
                for (String volumeSetEntry : volumeSet) {
                    VolumeInfo volumeInfo = new VolumeInfo(volumeSetEntry);
                    List<VolumeInfo> foundVolumeInfos = Entities.query(volumeInfo);
                    if (foundVolumeInfos.size() > 0) {
                        volumeInfos.add(foundVolumeInfos.get(0));
                    }
                }
            } else {
                VolumeInfo volumeInfo = new VolumeInfo();
                List<VolumeInfo> foundVolumeInfos = Entities.query(volumeInfo);
                for (VolumeInfo volInfo : foundVolumeInfos) {
                    volumeInfos.add(volInfo);
                }
            }

            ArrayList<StorageVolume> volumes = reply.getVolumeSet();
            for (VolumeInfo volumeInfo : volumeInfos) {
                volumes.add(convertVolumeInfo(volumeInfo));
                if(volumeInfo.getStatus().equals(StorageProperties.Status.failed.toString()) &&
                    (System.currentTimeMillis() - volumeInfo.getLastUpdateTimestamp().getTime() > StorageProperties.FAILED_STATE_CLEANUP_THRESHOLD_MS)) {
                    LOG.warn("Failed volume, cleaning it: " + volumeInfo.getVolumeId());
                    checker.cleanFailedVolume(volumeInfo.getVolumeId());
                }
            }
            tran.commit();
        }
        return reply;

    }

    public ConvertVolumesResponseType ConvertVolumes(ConvertVolumesType request) throws EucalyptusCloudException {
        ConvertVolumesResponseType reply = (ConvertVolumesResponseType) request.getReply();
        String provider = request.getOriginalProvider();
        provider = "com.eucalyptus.storage." + provider;
        if(!blockManager.getClass().getName().equals(provider)) {
            //different backend provider. Try upgrade
            try {
                LogicalStorageManager fromBlockManager = (LogicalStorageManager) ClassLoader.getSystemClassLoader().loadClass(provider).newInstance();
                fromBlockManager.checkPreconditions();
                //initialize fromBlockManager
                new VolumesConvertor(fromBlockManager).start();
            } catch(InstantiationException e) {
                LOG.error(e);
                throw new EucalyptusCloudException(e);
            } catch(ClassNotFoundException e) {
                LOG.error(e);
                throw new EucalyptusCloudException(e);
            } catch(IllegalAccessException e) {
                LOG.error(e);
                throw new EucalyptusCloudException(e);
            }
        }
        return reply;
    }

    /**
     * This should no longer be called/invoked directly...
     * @param request
     * @return
     * @throws EucalyptusCloudException
     */
    public AttachStorageVolumeResponseType attachVolume(AttachStorageVolumeType request) throws EucalyptusCloudException {
        throw new EucalyptusCloudException("Operation not supported");
    }

    public DetachStorageVolumeResponseType detachVolume(DetachStorageVolumeType request) throws EucalyptusCloudException {
        DetachStorageVolumeResponseType reply = request.getReply();
        String volumeId = request.getVolumeId();
        LOG.info("Processing DetachVolume request for volume " + volumeId);

        //Do the work.
        try {
            LOG.info("Detaching volume " + volumeId + " from all hosts");
            Entities.asTransaction(VolumeInfo.class, invalidateAndDetachAll()).apply(volumeId);
        } catch(final Exception e) {
            LOG.error("Failed to fully detach volume " + volumeId);
            reply.set_return(false);
        }
        return reply;
    }

    private static Function<String, VolumeInfo> invalidateAndDetachAll() {

        final Predicate<VolumeToken> invalidateExports = new Predicate<VolumeToken>() {
            @Override
            public boolean apply(VolumeToken volToken) {
                VolumeToken tokenEntity = Entities.merge(volToken);
                try {
                    tokenEntity.invalidateAllExportsAndToken();
                    return true;
                } catch(Exception e) {
                    LOG.error("Failed invalidating exports for token " + tokenEntity.getToken());
                    return false;
                }
            }
        };

        //Could save cycles by statically setting all of these functions that don't require closures so they are not
        //constructed for each request.
        return new Function<String, VolumeInfo>(){
            @Override
            public VolumeInfo apply(String volumeId) {
                try {
                    VolumeInfo volumeEntity = Entities.uniqueResult(new VolumeInfo(volumeId));
                    try {
                        LOG.debug("Invalidating all tokens and all exports for " + volumeId);
                        //Invalidate all tokens and exports and forcibly detach.
                        if(!Iterables.all(volumeEntity.getAttachmentTokens(), invalidateExports)){
                            //At least one failed.
                            LOG.error("Failed to invalidate all tokens and exports");
                        }
                    } catch(Exception e) {
                        LOG.error("Error invalidating tokens", e);
                    }

                    try {
                        LOG.debug("Unexporting volume " + volumeId + " from all hosts");
                        blockManager.unexportVolumeFromAll(volumeId);
                    } catch (EucalyptusCloudException ex) {
                        LOG.error("Detaching volume " + volumeId + " from all hosts failed",ex);
                    }
                } catch(NoSuchElementException e) {
                    LOG.error("Cannot force detach of volume " + volumeId + " because it is not found in database");
                    return null;
                } catch(TransactionException e) {
                    LOG.error("Failed to lookup volume " + volumeId);
                }

                return null;
            }
        };
    }

    private StorageVolume convertVolumeInfo(VolumeInfo volInfo) throws EucalyptusCloudException {
        StorageVolume volume = new StorageVolume();
        String volumeId = volInfo.getVolumeId();
        volume.setVolumeId(volumeId);
        volume.setStatus(volInfo.getStatus());
        volume.setCreateTime(DateUtils.format(volInfo.getCreateTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        volume.setSize(String.valueOf(volInfo.getSize()));
        volume.setSnapshotId(volInfo.getSnapshotId());
        VolumeToken tok = volInfo.getCurrentValidToken();
        if(tok != null) {
            volume.setActualDeviceName(BlockStorageUtil.encryptForNode(tok.getToken(), BlockStorageUtil.getPartitionForLocalService(Storage.class)));
        }else{
            //use 'invalid' to indicate no export? invalid seems okay since there is no valid device unless a token is valid
            volume.setActualDeviceName("invalid");
        }
        return volume;
    }

    private StorageSnapshot convertSnapshotInfo(SnapshotInfo snapInfo) {
        StorageSnapshot snapshot = new StorageSnapshot();
        snapshot.setVolumeId(snapInfo.getVolumeId());
        snapshot.setStatus(snapInfo.getStatus());
        snapshot.setSnapshotId(snapInfo.getSnapshotId());
        String progress = snapInfo.getProgress();
        progress = progress != null ? progress + "%" : progress;
        snapshot.setProgress(progress);
        snapshot.setStartTime(DateUtils.format(snapInfo.getStartTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        return snapshot;
    }

    public static abstract class SnapshotTask implements Runnable {}

    public static abstract class VolumeTask implements Runnable {}

	public static class Snapshotter extends SnapshotTask {
		private String volumeId;
		private String snapshotId;
		private String snapPointId;

		/**
		 * Initializes the Snapshotter task. snapPointId should be null if no snap point has been created yet.
		 * 
		 * @param volumeId
		 * @param snapshotId
		 * @param snapPointId
		 */
		public Snapshotter(String volumeId, String snapshotId, String snapPointId) {
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.snapPointId = snapPointId;
		}

		@Override
		public void run() {
			EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(volumeId);
			try {
				Boolean shouldTransferSnapshots = true;
				StorageResource snapshotResource = null;
				SnapshotTransfer snapshotTransfer = null;
				String bucket = null;

				// Check whether the snapshot needs to be uploaded
				shouldTransferSnapshots = StorageInfo.getStorageInfo().getShouldTransferSnapshots();

				if (shouldTransferSnapshots) {
					// Prepare for the snapshot upload (fetch credentials for snapshot upload to osg, create the bucket). Error out if this fails without
					// creating the snapshot on the blockstorage backend
					snapshotTransfer = new S3SnapshotTransfer(snapshotId, snapshotId);
					bucket = snapshotTransfer.prepareForUpload();

					if (snapshotTransfer == null || StringUtils.isBlank(bucket)) {
						throw new EucalyptusCloudException("Failed to initialize snapshot transfer mechanism for uploading " + snapshotId);
					}
				}

				// Acquire the semaphore here and release it here as well
				try {
					try {
						semaphore.acquire();
					} catch (InterruptedException ex) {
						throw new EucalyptusCloudException("Failed to create snapshot " + snapshotId + " as the semaphore could not be acquired");
					}

					// Check to ensure that a failed/cancellation has not be set
					if (!isSnapshotMarkedFailed(snapshotId)) {
						snapshotResource = blockManager.createSnapshot(this.volumeId, this.snapshotId, this.snapPointId, shouldTransferSnapshots);
					} else {
						throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
					}
				} finally {
					semaphore.release();
				}

				if (shouldTransferSnapshots) {
					if (snapshotResource == null) {
						throw new EucalyptusCloudException("Snapshot file unknown. Cannot transfer snapshot");
					}

					// Update snapshot location in database
					String snapshotLocation = SnapshotInfo.generateSnapshotLocationURI(SnapshotTransferConfiguration.OSG, bucket, snapshotId);
					SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
					SnapshotInfo snapshotInfo = null;
					try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
						snapshotInfo = Entities.uniqueResult(snapInfo);
						snapshotInfo.setSnapshotLocation(snapshotLocation);
						tran.commit();
					} catch (TransactionException | NoSuchElementException e) {
						LOG.debug("Failed to update upload location for snapshot " + snapshotId, e);
					}

					if (!isSnapshotMarkedFailed(snapshotId)) {
						try {
							snapshotTransfer.upload(snapshotResource);
						} catch (Exception e) {
							throw new EucalyptusCloudException("Failed to upload snapshot " + snapshotId + " to objectstorage", e);
						}
					} else {
						throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
					}

					try {
						LOG.debug("Finalizing snapshot " + snapshotId + " post upload");
						blockManager.finishVolume(snapshotId);
					} catch (EucalyptusCloudException ex) {
						LOG.error("Failed to finalize snapshot " + snapshotId, ex);
					}
				} else {
					// Snapshot does not have to be transferred. Mark it as available.
					Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {

						@Override
						public SnapshotInfo apply(String arg0) {
							SnapshotInfo snap;
							try {
								snap = Entities.uniqueResult(new SnapshotInfo(arg0));
								snap.setStatus(StorageProperties.Status.available.toString());
								snap.setProgress("100");
								snap.setSnapPointId(null);
								return snap;
							} catch (TransactionException | NoSuchElementException e) {
								LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
							}
							return null;
						}
					};

					Entities.asTransaction(SnapshotInfo.class, updateFunction).apply(snapshotId);
				}
			} catch (Exception ex) {
				LOG.error("Failed to create snapshot " + snapshotId, ex);
				try {
					LOG.debug("Disconnecting snapshot " + snapshotId + " on failed snapshot attempt");
					blockManager.finishVolume(snapshotId);
				} catch (EucalyptusCloudException e1) {
					LOG.debug("Deleting snapshot " + snapshotId + " on failed snapshot attempt", e1);
					blockManager.cleanSnapshot(snapshotId);
				}

				Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {

					@Override
					public SnapshotInfo apply(String arg0) {
						SnapshotInfo snap;
						try {
							snap = Entities.uniqueResult(new SnapshotInfo(arg0));
							snap.setStatus(StorageProperties.Status.failed.toString());
							snap.setProgress("0");
							return snap;
						} catch (TransactionException | NoSuchElementException e) {
							LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
						}
						return null;
					}
				};

				Entities.asTransaction(SnapshotInfo.class, updateFunction).apply(snapshotId);
			}
		}
	}


	public static class VolumeCreator extends VolumeTask {
		private String volumeId;
		private String snapshotId;
		private String parentVolumeId;
		private int size;

		public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, String parentVolumeId, int size) {
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.parentVolumeId = parentVolumeId;
			this.size = size;
		}

		@Override
		public void run() {
			boolean success = true;
			if (snapshotId != null) {
				try {
					SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
					List<SnapshotInfo> foundSnapshotInfos = Transactions.findAll(snapshotInfo);

					if (foundSnapshotInfos.size() == 0) {// SC *may not* have a database record for the snapshot and or the actual snapshot

						EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(snapshotId);
						try {
							semaphore.acquire(); // Get the semaphore to avoid concurrent access by multiple threads
							foundSnapshotInfos = Transactions.findAll(snapshotInfo); // Check if another thread setup the snapshot

							if (foundSnapshotInfos.size() == 0) { // SC does not have a database record for the snapshot
								SnapshotInfo firstSnap = null;

								// Search for the snapshots on other clusters in the ascending order of creation time stamp and get the first one
								snapshotInfo.setScName(null);
								try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
									Criteria snapCriteria = Entities.createCriteria(SnapshotInfo.class).setReadOnly(true)
											.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).setCacheable(true)
											.add(Example.create(snapshotInfo).enableLike(MatchMode.EXACT)).addOrder(Order.asc("creationTimestamp"));
									foundSnapshotInfos = (List<SnapshotInfo>) snapCriteria.list();
									tr.commit();
								}

								if (foundSnapshotInfos != null && foundSnapshotInfos.size() > 0) {
									firstSnap = foundSnapshotInfos.get(0);
								} else {
									throw new EucalyptusCloudException("No record of snapshot " + snapshotId + " on any SC");
								}

								// If size was not found in database, bail out. Can't create a snapshot without the size
								if (firstSnap.getSizeGb() == null || firstSnap.getSizeGb() <= 0) {
									throw new EucalyptusCloudException("Snapshot size for " + snapshotId
											+ " is unknown. Cannot prep snapshot holder on the storage backend");
								}

								// Check for the snpahsot on the storage backend. Clusters/zones/partitions may be connected to the same storage backend in
								// which case snapshot does not have to be downloaded from ObjectStorage.
								if (!blockManager.getFromBackend(snapshotId, firstSnap.getSizeGb())) { // Storage backend does not contain snapshot. Download snapshot from OSG
									LOG.debug(snapshotId + " not found on storage backend. Will attempt to download from objectstorage gateway");
									
									String bucket = null;
									String key = null;

									if (StringUtils.isBlank(firstSnap.getSnapshotLocation())) {
										throw new EucalyptusCloudException("Snapshot location (bucket, key) for " + snapshotId
												+ " is unknown. Cannot download snapshot from objectstorage.");
									}
									String[] names = SnapshotInfo.getSnapshotBucketKeyNames(firstSnap.getSnapshotLocation());
									bucket = names[0];
									key = names[1];
									if (StringUtils.isBlank(bucket) || StringUtils.isBlank(key)) {
										throw new EucalyptusCloudException("Failed to parse bucket and key information for downloading " + snapshotId
												+ ". Cannot download snapshot from objectstorage.");
									}
									
									// Try to fetch the snapshot size before preparing the snapshot holder on the backend. If size is unavailable, the snapshot
									// must be downloaded, unzipped and measured before creating the snapshot holder on the backend. Some SANs (Equallogic) add
									// arbitrary amount of writable space to the lun and hence the exact size of the snapshot is required for preparing the
									// holder on the backend
									SnapshotTransfer snapshotTransfer = new S3SnapshotTransfer(snapshotId, bucket, key);
									Long actualSizeInBytes = null;
									try {
										actualSizeInBytes = snapshotTransfer.getSizeInBytes();
									} catch (Exception e) {
										LOG.debug("Snapshot size not found", e);
									}
									
									if (actualSizeInBytes == null) { // Download the snapshot from OSG and find out the size

										String tmpSnapshotFileName = null;
										try {
											tmpSnapshotFileName = downloadSnapshotToTempFile(snapshotTransfer);

											File snapFile = new File(tmpSnapshotFileName);
											if (!snapFile.exists()) {
												throw new EucalyptusCloudException("Unable to find snapshot " + snapshotId + "on SC");
											}

											// TODO add snapshot size to osg object metadata

											long actualSnapSizeInMB = (long) Math.ceil((double) snapFile.length() / StorageProperties.MB);

											try {
												// Allocates the necessary resources on the backend
												StorageResource storageResource = blockManager.prepareSnapshot(snapshotId, firstSnap.getSizeGb(), actualSnapSizeInMB);

												if (storageResource != null) {
													// Check if the destination is a block device
													if (storageResource.getPath().startsWith("/dev/")) {
														CommandOutput output = SystemUtil
																.runWithRawOutput(new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "dd",
																		"if=" + tmpSnapshotFileName, "of=" + storageResource.getPath(),
																		"bs=" + StorageProperties.blockSize });
														LOG.debug("Output of dd command: " + output.error);
														if (output.returnValue != 0) {
															throw new EucalyptusCloudException("Failed to copy the snapshot to the right location due to: "
																	+ output.error);
														}
														cleanupFile(tmpSnapshotFileName);
													} else {
														// Rename file
														if (!snapFile.renameTo(new File(storageResource.getPath()))) {
															throw new EucalyptusCloudException("Failed to rename the snapshot");
														}
													}

													// Finish the snapshot
													blockManager.finishVolume(snapshotId);
												} else {
													LOG.warn("Block Manager replied that " + snapshotId
															+ " not on backend, but snapshot preparation indicated that the snapshot is already present");
												}
											} catch (Exception ex) {
												LOG.error("Failed to prepare the snapshot " + snapshotId
														+ " on storage backend. Cleaning up the snapshot on backend", ex);
												cleanFailedSnapshot(snapshotId);
												throw ex;
											}
										} catch (Exception ex) {
											LOG.error("Failed to prepare the snapshot " + snapshotId
													+ " on the storage backend. Cleaning up the snapshot on SC", ex);
											cleanupFile(tmpSnapshotFileName);
											throw ex;
										}
									} else { // Prepare the snapshot holder and download the snapshot directly to it

										long actualSnapSizeInMB = (long) Math.ceil((double) actualSizeInBytes / StorageProperties.MB);

										try {
											// Allocates the necessary resources on the backend
											StorageResource storageResource = blockManager.prepareSnapshot(snapshotId, firstSnap.getSizeGb(),
													actualSnapSizeInMB);

											if (storageResource != null) {
												// Download the snapshot to the destination
												snapshotTransfer.download(storageResource);

												// Finish the snapshot
												blockManager.finishVolume(snapshotId);
											} else {
												LOG.warn("Block Manager replied that " + snapshotId
														+ " not on backend, but snapshot preparation indicated that the snapshot is already present");
											}
										} catch (Exception ex) {
											LOG.error("Failed to prepare the snapshot " + snapshotId
													+ " on storage backend. Cleaning up the snapshot on backend", ex);
											cleanFailedSnapshot(snapshotId);
											throw ex;
										}
									}

								} else { // Storage backend contains snapshot 
									// Just create a record of it for this partition in the DB and get going!
									LOG.debug(snapshotId + " found on storage backend");
								}
								
								// Create a snapshot record for this SC
								try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
									snapshotInfo = copySnapshotInfo(firstSnap);
									snapshotInfo.setProgress("100");
									snapshotInfo.setStartTime(new Date());
									snapshotInfo.setStatus(StorageProperties.Status.available.toString());
									Entities.persist(snapshotInfo);
									tr.commit();
								}
							} else { // SC has a database record for the snapshot
								// This condition is hit when concurrent threads compete to create a volume from a snapshot that did not exist on this SC. One
								// of the concurrent threads may have finished the snapshot prep there by making it available to all other threads
								SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
								if (!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
									success = false;
									LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
								} else {
									// Do NOT create the volume here as this is synchronized block. Snapshot prepping has to be synchronized, volume
									// creation can be done in parallel
								}
							}
						} catch (InterruptedException ex) {
							throw new EucalyptusCloudException("semaphore could not be acquired");
						} finally {
							try {
								semaphore.release();
							} finally {
								EucaSemaphoreDirectory.removeSemaphore(snapshotId);
							}
						}

						// Create the volume from the snapshot, this can happen in parallel.
						if (success) {
							size = blockManager.createVolume(volumeId, snapshotId, size);
						}
					} else { // SC has a database record for the snapshot
						// Repeated logic, fix it!
						SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
						if (!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
							success = false;
							LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
						} else {
							size = blockManager.createVolume(volumeId, snapshotId, size);
						}
					}
				} catch (Exception ex) {
					success = false;
					LOG.error("Failed to create volume " + volumeId, ex);
				}
			} else { // Not a snapshot-based volume create.
				try {
					if (parentVolumeId != null) {
						// Clone the parent volume.
						blockManager.cloneVolume(volumeId, parentVolumeId);
					} else {
						// Create a regular empty volume
						blockManager.createVolume(volumeId, size);
					}
				} catch (Exception ex) {
					success = false;
					LOG.error("Failed to create volume " + volumeId, ex);
				}
			}
			
			// Update database record for the volume.
			VolumeInfo volumeInfo = new VolumeInfo(volumeId);
			try (TransactionResource tr = Entities.transactionFor(VolumeInfo.class)) {
				VolumeInfo foundVolumeInfo = Entities.uniqueResult(volumeInfo);
				if (foundVolumeInfo != null) {
					if (success) {
						foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
					} else {
						foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
					}
					if (snapshotId != null) {
						foundVolumeInfo.setSize(size);
					}
				} else {
					LOG.error("VolumeInfo entity for volume id " + volumeId + " was not found in the database");
				}
				tr.commit();
			} catch (Exception e) {
				LOG.error("Failed to update VolumeInfo entity for volume id " + volumeId + " in the database", e);
			} 
		}
        
        // DO NOT throw any exceptions from cleaning routines. Log the errors and move on
        private void cleanFailedSnapshot(String snapshotId) {
            if(snapshotId == null) return;
            LOG.debug("Disconnecting and cleaning local snapshot after failed snapshot transfer: " + snapshotId);
            try {
                blockManager.finishVolume(snapshotId);
            } catch(Exception e) {
                LOG.error("Error finishing failed snapshot " + snapshotId, e);
            } finally {
                try {
                    blockManager.cleanSnapshot(snapshotId);
                } catch (Exception e) {
                    LOG.error("Error deleting failed snapshot " + snapshotId, e);
                }
            }
        }
        
        private SnapshotInfo copySnapshotInfo (SnapshotInfo source) {
        	SnapshotInfo copy = new SnapshotInfo(source.getSnapshotId());
        	copy.setSizeGb(source.getSizeGb());
        	copy.setSnapshotLocation(source.getSnapshotLocation());
        	copy.setUserName(source.getUserName());
        	copy.setVolumeId(source.getVolumeId());
        	return copy;
        }
        
        private String downloadSnapshotToTempFile(SnapshotTransfer snapshotTransfer) throws EucalyptusCloudException {

            String tmpUncompressedFileName = null;
            File tmpUncompressedFile = null;
            int retry = 0;
            int maxRetry = 5;
            
            do{
                tmpUncompressedFileName = StorageProperties.storageRootDirectory + File.separator + snapshotId + "-" + String.valueOf(randomGenerator.nextInt());
                tmpUncompressedFile = new File(tmpUncompressedFileName);
            } while (tmpUncompressedFile.exists() && retry++ < maxRetry);

            // This should be *very* rare
            if (retry >= maxRetry) {
                // Nothing to clean up at this point
                throw new EucalyptusCloudException("Could not get a temporary file for snapshot " + snapshotId + " download after " + maxRetry + " attempts");
            }

            // Download the snapshot from OSG
            try {
            	snapshotTransfer.download(new FileResource(snapshotId, tmpUncompressedFileName));
            } catch (Exception ex) {
                // Cleanup
                cleanupFile(tmpUncompressedFile);
                throw new EucalyptusCloudException("Failed to download snapshot " + snapshotId + " from objectstorage", ex);
            }

            return tmpUncompressedFileName;
        }

        private void cleanupFile(String fileName) {
            try {
                cleanupFile(new File(fileName));
            } catch (Exception e) {
                LOG.error("Failed to delete file", e);
            }
        }

        private void cleanupFile(File file) {
            if (file != null && file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    LOG.error("Failed to delete file", e);
                }
            }
        }
    }

    public static class VolumesConvertor extends Thread {
        private LogicalStorageManager fromBlockManager;

        public VolumesConvertor(LogicalStorageManager fromBlockManager) {
            this.fromBlockManager = fromBlockManager;
        }

        @Override
        public void run() {
            //This is a heavy weight operation. It must execute atomically.
            //All other volume operations are forbidden when a conversion is in progress.
            synchronized (blockManager) {
                StorageProperties.enableStorage = StorageProperties.enableSnapshots = false;
                List<VolumeInfo> volumes = Lists.newArrayList();
                List<SnapshotInfo> snapshots = Lists.newArrayList();
                try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
                    VolumeInfo volumeInfo = new VolumeInfo();
                    volumeInfo.setStatus(StorageProperties.Status.available.toString());
                    List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
                    volumes.addAll(volumeInfos);

                    SnapshotInfo snapInfo = new SnapshotInfo();
                    snapInfo.setStatus(StorageProperties.Status.available.toString());
                    List<SnapshotInfo> snapshotInfos = Entities.query(snapInfo);
                    snapshots.addAll(snapshotInfos);

                    tran.commit();
                }
                for(VolumeInfo volume : volumes) {
                    String volumeId = volume.getVolumeId();
                    try {
                        LOG.info("Converting volume: " + volumeId + " please wait...");
                        String volumePath = fromBlockManager.getVolumePath(volumeId);
                        blockManager.importVolume(volumeId, volumePath, volume.getSize());
                        fromBlockManager.finishVolume(volumeId);
                        LOG.info("Done converting volume: " + volumeId);
                    } catch (Exception ex) {
                        LOG.error(ex);
                        try {
                            blockManager.deleteVolume(volumeId);
                        } catch (EucalyptusCloudException e1) {
                            LOG.error(e1);
                        }
                        //this one failed, continue processing the rest
                    }
                }

                for(SnapshotInfo snap : snapshots) {
                    String snapshotId = snap.getSnapshotId();
                    try {
                        LOG.info("Converting snapshot: " + snapshotId + " please wait...");
                        String snapPath = fromBlockManager.getSnapshotPath(snapshotId);
                        int size = fromBlockManager.getSnapshotSize(snapshotId);
                        blockManager.importSnapshot(snapshotId, snap.getVolumeId(), snapPath, size);
                        fromBlockManager.finishVolume(snapshotId);
                        LOG.info("Done converting snapshot: " + snapshotId);
                    } catch (Exception ex) {
                        LOG.error(ex);
                        try {
                            blockManager.deleteSnapshot(snapshotId);
                        } catch (EucalyptusCloudException e1) {
                            LOG.error(e1);
                        }
                        //this one failed, continue processing the rest
                    }
                }
                LOG.info("Conversion complete");
                StorageProperties.enableStorage = StorageProperties.enableSnapshots = true;
            }
        }
    }

    public CloneVolumeResponseType CloneVolume(CloneVolumeType request) throws EucalyptusCloudException {
        CloneVolumeResponseType reply = request.getReply();
        CreateStorageVolumeType createStorageVolume = new CreateStorageVolumeType();
        createStorageVolume.setParentVolumeId(request.getVolumeId());
        CreateStorageVolume(createStorageVolume);
        return reply;
    }

	public static class VolumeDeleterTask extends CheckerTask {

		public VolumeDeleterTask() {
			this.name = "VolumeDeleter";
		}

		@Override
		public void run() {
			VolumeInfo searchVolume = new VolumeInfo();
			searchVolume.setStatus(StorageProperties.Status.deleted.toString());

			// Check if deleted volumes have expired and remove them
			try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
				List<VolumeInfo> deletedVolumes = Entities.query(searchVolume);
				for (VolumeInfo deletedVolume : deletedVolumes) {
					if (deletedVolume.cleanupOnDeletion()) {
						LOG.info("Volume deletion time expired for: " + deletedVolume.getVolumeId() + " ...cleaning up.");
						Entities.delete(deletedVolume);
					}
				}
				tran.commit();
			} catch (Exception e) {
				LOG.error("Failed during clean up of expired volume records", e);
			}

			// Look for volumes marked for deltion and delete them
			searchVolume = new VolumeInfo();
			searchVolume.setStatus(StorageProperties.Status.deleting.toString());
			List<VolumeInfo> volumesToBeDeleted = null;
			try {
				volumesToBeDeleted = Transactions.findAll(searchVolume);
			} catch (Exception e) {
				LOG.error("Failed to lookup volumes marked for deletion", e);
				return;
			}

			if (volumesToBeDeleted != null && !volumesToBeDeleted.isEmpty()) {
				for (VolumeInfo vol : volumesToBeDeleted) {
					// Do separate transaction for each volume so we don't
					// keep the transaction open for a long time
					try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
						vol = Entities.uniqueResult(vol);
						final String volumeId = vol.getVolumeId();
						LOG.info("Volume: " + volumeId + " marked for deletion. Checking export status");
						if (Iterables.any(vol.getAttachmentTokens(), new Predicate<VolumeToken>() {
							@Override
							public boolean apply(VolumeToken token) {
								// Return true if attachment is valid or export exists.
								try {
									return token.hasActiveExports();
								} catch (EucalyptusCloudException e) {
									LOG.warn("Failure checking for active exports for volume " + volumeId);
									return false;
								}
							}
						})) {
							// Exports exists... try un exporting the volume before deleting.
							LOG.info("Volume: " + volumeId + " found to be exported. Detaching volume from all hosts");
							try {
								Entities.asTransaction(VolumeInfo.class, invalidateAndDetachAll()).apply(volumeId);
							} catch (Exception e) {
								LOG.error("Failed to fully detach volume " + volumeId, e);
							}
						}

						LOG.info("Volume: " + volumeId + " was marked for deletion. Cleaning up...");
						try {
							blockManager.deleteVolume(volumeId);
						} catch (EucalyptusCloudException e) {
							LOG.debug("Failed to delete " + volumeId, e);
							LOG.warn("Unable to delete " + volumeId + ". Will retry later");
							continue;
						}
						vol.setStatus(StorageProperties.Status.deleted.toString());
						vol.setDeletionTime(new Date());
						EucaSemaphoreDirectory.removeSemaphore(volumeId); // who put it there ?
						tran.commit();
					} catch (Exception e) {
						LOG.error("Error deleting volume " + vol.getVolumeId() + ": " + e.getMessage());
						LOG.debug("Exception during deleting volume " + vol.getVolumeId() + ".", e);
					}
				}
			} else {
				LOG.trace("No volumes marked for deletion");
			}
		}
	}

	public static class SnapshotDeleterTask extends CheckerTask {

		public SnapshotDeleterTask() {
			this.name = "SnapshotDeleter";
		}

		private S3SnapshotTransfer mock;

		public SnapshotDeleterTask(S3SnapshotTransfer mock) {
			this.mock = mock;
		}

		@Override
		public void run() {
			SnapshotInfo searchSnap = new SnapshotInfo();
			searchSnap.setStatus(StorageProperties.Status.deleting.toString());
			List<SnapshotInfo> snapshotsToBeDeleted = null;
			try {
				snapshotsToBeDeleted = Transactions.findAll(searchSnap);
			} catch (Exception e) {
				LOG.error("Failed to lookup snapshots marked for deletion", e);
				return;
			}
			if (snapshotsToBeDeleted != null && !snapshotsToBeDeleted.isEmpty()) {
				S3SnapshotTransfer snapshotTransfer = null;
				for (SnapshotInfo snap : snapshotsToBeDeleted) {
					String snapshotId = snap.getSnapshotId();
					LOG.info("Snapshot: " + snapshotId + " was marked for deletion. Cleaning up...");
					try {
						blockManager.deleteSnapshot(snapshotId);
					} catch (EucalyptusCloudException e1) {
						LOG.debug("Failed to delete " + snapshotId, e1);
						LOG.warn("Unable to delete " + snapshotId + ". Will retry later");
						continue;
					}
					SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);

					SnapshotInfo foundSnapshotInfo;
					try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
						foundSnapshotInfo = Entities.uniqueResult(snapInfo);
						foundSnapshotInfo.setStatus(StorageProperties.Status.deleted.toString());
						tran.commit();
					} catch (TransactionException | NoSuchElementException e) {
						LOG.error(e);
						continue;
					}

					if (StringUtils.isNotBlank(foundSnapshotInfo.getSnapshotLocation())) {
						try {
							String[] names = SnapshotInfo.getSnapshotBucketKeyNames(foundSnapshotInfo.getSnapshotLocation());
							if (snapshotTransfer == null) {
								if (mock == null) {
									snapshotTransfer = new S3SnapshotTransfer();
								} else {
									snapshotTransfer = mock;
								}
							}
							snapshotTransfer.setSnapshotId(snapshotId);
							snapshotTransfer.setBucketName(names[0]);
							snapshotTransfer.setKeyName(names[1]);
							snapshotTransfer.delete();
						} catch (Exception e) {
							LOG.warn("Failed to delete snapshot " + snapshotId + " from objectstorage", e);
						}
					} else {
						LOG.debug("Snapshot location missing for " + snapshotId + ". Skipping deletion from ObjectStorageGateway");
					}
				}
			} else {
				LOG.trace("No snapshots marked for deletion");
			}
		}
	}

}
