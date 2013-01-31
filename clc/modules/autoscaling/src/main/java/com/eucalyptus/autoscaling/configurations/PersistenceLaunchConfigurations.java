/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.autoscaling.configurations;

import java.util.List;
import java.util.NoSuchElementException;
import com.eucalyptus.autoscaling.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.AutoScalingMetadataNotFoundException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceLaunchConfigurations extends LaunchConfigurations {

  @Override
  public List<LaunchConfiguration> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    try {
      return Transactions.findAll( LaunchConfiguration.withOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find launch configurations for " + ownerFullName, e );
    }
  }

  @Override
  public List<LaunchConfiguration> list( final OwnerFullName ownerFullName,
                                         final Predicate<? super LaunchConfiguration> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( LaunchConfiguration.withOwner( ownerFullName ), filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find launch configurations for " + ownerFullName, e );
    }
  }

  @Override
  public LaunchConfiguration lookup( final OwnerFullName ownerFullName,
                                     final String launchConfigurationName ) throws AutoScalingMetadataException {
    try {
      return Transactions.find( LaunchConfiguration.named( ownerFullName, launchConfigurationName ) );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Launch configuration not found '"+launchConfigurationName+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error finding launch configuration '"+launchConfigurationName+"' for " + ownerFullName, e );
    }
  }

  @Override
  public boolean delete( final LaunchConfiguration launchConfiguration ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( LaunchConfiguration.withId( launchConfiguration ) );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting launch configuration '"+launchConfiguration.getLaunchConfigurationName()+"'", e );
    }
  }

  @Override
  public LaunchConfiguration save( final LaunchConfiguration launchConfiguration ) throws AutoScalingMetadataException {
    try {
      return Transactions.saveDirect( launchConfiguration );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error creating launch configuration '"+launchConfiguration.getLaunchConfigurationName()+"'", e );
    }
  }

}
