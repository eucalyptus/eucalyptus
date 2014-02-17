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
import com.eucalyptus.util.OwnerFullName;
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

  public abstract boolean delete( AutoScalingInstanceMetadata autoScalingInstance ) throws AutoScalingMetadataException;

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
