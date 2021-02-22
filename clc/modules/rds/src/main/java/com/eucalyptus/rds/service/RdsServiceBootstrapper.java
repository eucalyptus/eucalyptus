/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.service.activities.RdsActivityTasks;
import com.google.common.collect.Lists;

/**
 *
 */
@Provides(Rds.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(Rds.class)
public class RdsServiceBootstrapper extends Bootstrapper.Simple {
  private static Logger LOG = Logger.getLogger(RdsServiceBootstrapper.class);

  private static RdsServiceBootstrapper singleton;

  public static Bootstrapper getInstance() {
    synchronized (RdsServiceBootstrapper.class) {
      if (singleton == null) {
        singleton = new RdsServiceBootstrapper();
      }
    }
    return singleton;
  }

  private static int rdsImageCheckCounter = 0;
  private static boolean rdsImageCheckResult = true;

  @Override
  public boolean check() throws Exception {
    if (!super.check()) {
      return false;
    }

    if (!isImageConfigured()) {
      return false;
    }

    if (!ensureSystemVpc()) {
      return false;
    }

    return true;
  }

  private boolean isImageConfigured() {
    if ( CloudMetadatas.isMachineImageIdentifier( RdsWorkerProperties.IMAGE ) ) {
      if( rdsImageCheckCounter >= 3 && Topology.isEnabled( Compute.class ) ){
        try{
          final List<ImageDetails> emis =
              RdsActivityTasks.getInstance().describeImagesWithVerbose(
                  Lists.newArrayList(RdsWorkerProperties.IMAGE));
          rdsImageCheckResult = RdsWorkerProperties.IMAGE.equals( emis.get( 0 ).getImageId() );
          rdsImageCheckResult = "available".equals(emis.get(0).getImageState());
        }catch(final Exception ex){
          rdsImageCheckResult =false;
        }
        rdsImageCheckCounter = 0;
      }else
        rdsImageCheckCounter++;

      if (!rdsImageCheckResult)
        return rdsImageCheckResult;

      return true;
    } else {
      return false;
    }
  }

  private boolean ensureSystemVpc() {
    try{
      if (RdsSystemVpcs.isCloudVpc().isPresent() ) {
        if (RdsSystemVpcs.isCloudVpc().get() &&
            !RdsSystemVpcs.prepareSystemVpc()) {
          return false;
        }
      } else {
        return false;
      }
    } catch (final Exception ex) {
      LOG.error("Failed to prepare system VPC for rds service", ex);
      return false;
    }
    return true;
  }
}