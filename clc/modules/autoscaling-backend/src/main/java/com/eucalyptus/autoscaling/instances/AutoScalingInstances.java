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
package com.eucalyptus.autoscaling.instances;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingInstanceDetails;
import com.eucalyptus.autoscaling.common.backend.msgs.Instance;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class AutoScalingInstances {
  public abstract <T> List<T> list( OwnerFullName ownerFullName,
                                    Predicate<? super AutoScalingInstance> filter,
                                    Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T> listByGroup( OwnerFullName ownerFullName,
                                           String groupName,
                                           Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T>  listByGroup( AutoScalingGroupMetadata group,
                                            Predicate<? super AutoScalingInstance> filter,
                                            Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T>  listByState( LifecycleState lifecycleState,
                                            ConfigurationState configurationState,
                                            Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T>  listUnhealthyByGroup( AutoScalingGroupMetadata group,
                                                     Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> T lookup( OwnerFullName ownerFullName,
                                String instanceId,
                                Function<? super AutoScalingInstance,T> transform ) throws AutoScalingMetadataException;

  public abstract void update( OwnerFullName ownerFullName,
                               String instanceId,
                               Callback<AutoScalingInstance> instanceUpdateCallback ) throws AutoScalingMetadataException;

  public abstract void markMissingInstancesUnhealthy( AutoScalingGroupMetadata group, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract void markExpiredPendingUnhealthy( AutoScalingGroupMetadata group, Collection<String> instanceIds, long maxAge ) throws AutoScalingMetadataException;

  public abstract Set<String> verifyInstanceIds( String accountNumber, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract void transitionState( AutoScalingGroupMetadata group, LifecycleState from, LifecycleState to, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract void transitionConfigurationState( AutoScalingGroupMetadata group, ConfigurationState from, ConfigurationState to, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract int registrationFailure( AutoScalingGroupMetadata group, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract boolean delete( OwnerFullName ownerFullName, String instanceId ) throws AutoScalingMetadataException;

  public abstract boolean deleteByGroup( final AutoScalingGroupMetadata group ) throws AutoScalingMetadataException;

  public abstract AutoScalingInstance save( AutoScalingInstance autoScalingInstance ) throws AutoScalingMetadataException;

  public static Function<AutoScalingInstance,String> instanceId() {
    return AutoScalingMetadatas.toDisplayName();
  }

  public static Function<AutoScalingInstanceCoreView,String> launchConfigurationName() {
    return AutoScalingInstanceProperties.LAUNCH_CONFIGURATION_NAME;
  }

  public static Function<AutoScalingInstanceCoreView,String> availabilityZone() {
    return AutoScalingInstanceProperties.AVAILABILITY_ZONE; 
  }

  public static Function<AutoScalingInstanceGroupView,String> groupArn() {
    return AutoScalingInstanceGroupProperties.GROUP_ARN;
  }

  @TypeMapper
  public enum AutoScalingInstanceTransform implements Function<AutoScalingInstance, AutoScalingInstanceDetails> {
    INSTANCE;

    @Override
    public AutoScalingInstanceDetails apply( final AutoScalingInstance autoScalingInstance ) {
      final AutoScalingInstanceDetails details = new AutoScalingInstanceDetails();      
      details.setAutoScalingGroupName( autoScalingInstance.getAutoScalingGroupName() );
      details.setAvailabilityZone( autoScalingInstance.getAvailabilityZone() );
      details.setHealthStatus( Strings.toString( autoScalingInstance.getHealthStatus() ) );
      details.setInstanceId( autoScalingInstance.getInstanceId() );
      details.setLaunchConfigurationName( autoScalingInstance.getLaunchConfigurationName() );
      details.setLifecycleState( Strings.toString( autoScalingInstance.getLifecycleState() ) );      
      return details; 
    }
  }

  @TypeMapper
  public enum AutoScalingInstanceCoreViewTransform implements Function<AutoScalingInstance, AutoScalingInstanceCoreView> {
    INSTANCE;

    @Override
    public AutoScalingInstanceCoreView apply( final AutoScalingInstance autoScalingInstance ) {
      return new AutoScalingInstanceCoreView( autoScalingInstance );
    }
  }

  @TypeMapper
  public enum AutoScalingInstanceGroupViewTransform implements Function<AutoScalingInstance, AutoScalingInstanceGroupView> {
    INSTANCE;

    @Override
    public AutoScalingInstanceGroupView apply( final AutoScalingInstance autoScalingInstance ) {
      return new AutoScalingInstanceGroupView( autoScalingInstance );
    }
  }

  @TypeMapper
  public enum AutoScalingInstanceSummaryTransform implements Function<AutoScalingInstance, Instance> {
    INSTANCE;

    @Override
    public Instance apply( final AutoScalingInstance autoScalingInstance ) {
      final Instance details = new Instance();
      details.setAvailabilityZone( autoScalingInstance.getAvailabilityZone() );
      details.setHealthStatus( Strings.toString( autoScalingInstance.getHealthStatus() ) );
      details.setInstanceId( autoScalingInstance.getInstanceId() );
      details.setLaunchConfigurationName( autoScalingInstance.getLaunchConfigurationName() );
      details.setLifecycleState( Strings.toString( autoScalingInstance.getLifecycleState() ) );
      return details;
    }
  }

  private enum AutoScalingInstanceGroupProperties implements Function<AutoScalingInstanceGroupView,String> {
    GROUP_ARN {
      @Override
      public String apply( final AutoScalingInstanceGroupView autoScalingInstance ) {
        return AutoScalingMetadatas.toArn().apply( autoScalingInstance.getAutoScalingGroup() );
      }
    },
  }

  private enum AutoScalingInstanceProperties implements Function<AutoScalingInstanceCoreView,String> {
    AVAILABILITY_ZONE {
      @Override
      public String apply( final AutoScalingInstanceCoreView autoScalingInstance ) {
        return autoScalingInstance.getAvailabilityZone();
      }
    },
    LAUNCH_CONFIGURATION_NAME {
      @Override
      public String apply( final AutoScalingInstanceCoreView autoScalingInstance ) {
        return autoScalingInstance.getLaunchConfigurationName();
      }
    },
  }
}
