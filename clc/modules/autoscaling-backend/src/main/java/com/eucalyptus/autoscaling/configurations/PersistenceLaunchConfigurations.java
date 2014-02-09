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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceLaunchConfigurations extends LaunchConfigurations {

  private PersistenceSupport persistenceSupport = new PersistenceSupport();
  
  @Override
  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Predicate<? super LaunchConfiguration> filter,
                           final Function<? super LaunchConfiguration, T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter, transform );
  }

  @Override
  public <T> T lookup( final OwnerFullName ownerFullName,
                       final String launchConfigurationNameOrArn,
                       final Function<? super LaunchConfiguration, T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.lookup( ownerFullName, launchConfigurationNameOrArn, transform );
  }

  @Override
  public boolean delete( final LaunchConfigurationMetadata launchConfiguration ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( launchConfiguration );
  }

  @Override
  public LaunchConfiguration save( final LaunchConfiguration launchConfiguration ) throws AutoScalingMetadataException {
    return persistenceSupport.save( launchConfiguration );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistentsWithResourceNameSupport<LaunchConfiguration> {
    private PersistenceSupport() {
      super( AutoScalingResourceName.Type.launchConfiguration );
    }

    @Override
    protected LaunchConfiguration exampleWithOwner( final OwnerFullName ownerFullName ) {
      return LaunchConfiguration.withOwner( ownerFullName );
    }

    @Override
    protected LaunchConfiguration exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return LaunchConfiguration.named( ownerFullName, name );
    }

    @Override
    protected LaunchConfiguration exampleWithUuid( final String uuid ) {
      return LaunchConfiguration.withUuid( uuid );
    }
  }
}
