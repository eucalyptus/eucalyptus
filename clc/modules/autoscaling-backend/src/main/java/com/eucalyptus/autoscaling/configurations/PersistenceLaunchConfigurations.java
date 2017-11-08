/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.autoscaling.configurations;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
@ComponentNamed
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
