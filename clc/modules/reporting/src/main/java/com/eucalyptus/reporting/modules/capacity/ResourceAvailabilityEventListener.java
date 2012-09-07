/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.modules.capacity;

import static com.eucalyptus.reporting.domain.ReportingComputeDomainModel.ReportingComputeZoneDomainModel;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Dimension;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Type;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.domain.ReportingComputeDomainModel;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Resource availability listener that updates the compute capacity domain model.
 */
public class ResourceAvailabilityEventListener implements EventListener<ResourceAvailabilityEvent> {

  public static void register( ) {
    Listeners.register( ResourceAvailabilityEvent.class, new ResourceAvailabilityEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final ResourceAvailabilityEvent event ) {
    Preconditions.checkNotNull(event, "Event is required");

    final Function<ReportingComputeZoneDomainModel,Function<Long,Void>> zoneSetter = zoneSetters.get( event.getType() );
    final Function<ReportingComputeDomainModel,Function<Long,Void>> globalSetter = globalSetters.get( event.getType() );
    for ( final Availability availability : event.getAvailability() ) {
      if ( zoneSetter != null ) {
        final ReportingComputeZoneDomainModel zoneModel = getZoneModelForTags( availability.getTags() );
        if ( zoneModel == null ) continue;
        zoneSetter.apply( zoneModel ).apply( availability.getAvailable() );
      }
      if ( globalSetter != null ) {
        final ReportingComputeDomainModel globalModel = getReportingComputeDomainModel();
        globalSetter.apply( globalModel ).apply( availability.getAvailable() );
      }
    }
  }

  @Nullable
  private ReportingComputeZoneDomainModel getZoneModelForTags( @Nonnull final Set<Tag> tags ) {
    ReportingComputeZoneDomainModel model = null;
    for ( final Tag tag : tags ) {
      if ( tag instanceof Dimension && "availabilityZone".equals(tag.getType()) ) {
        model = getReportingComputeDomainModelForZone( tag.getValue() );
      } else if ( tag instanceof Type ) {
        return null;
      }
    }
    return model;
  }

  @Nonnull
  protected ReportingComputeDomainModel getReportingComputeDomainModel() {
    return ReportingComputeDomainModel.getGlobalComputeDomainModel();
  }

  @Nonnull
  protected ReportingComputeZoneDomainModel getReportingComputeDomainModelForZone( @Nonnull final String availabilityZone ) {
    return ReportingComputeDomainModel.getZoneComputeDomainModel( availabilityZone );
  }

  private static final Map<ResourceType,Function<ReportingComputeZoneDomainModel,Function<Long,Void>>> zoneSetters =
      ImmutableMap.<ResourceType,Function<ReportingComputeZoneDomainModel,Function<Long,Void>>>builder()
          .put( ResourceType.Core,
              new Function<ReportingComputeZoneDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeZoneDomainModel reportingComputeZoneDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeZoneDomainModel.setEc2ComputeUnitsAvailable( available.intValue() );
                      return null;
                    }
                  };
                }
              } )
          .put( ResourceType.Memory,
              new Function<ReportingComputeZoneDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeZoneDomainModel reportingComputeZoneDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeZoneDomainModel.setEc2MemoryUnitsAvailable( available.intValue() );
                      return null;
                    }
                  };
                }
              } )
          .put( ResourceType.Disk,
              new Function<ReportingComputeZoneDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeZoneDomainModel reportingComputeZoneDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeZoneDomainModel.setEc2DiskUnitsAvailable( available.intValue() );
                      return null;
                    }
                  };
                }
              } )
          .put( ResourceType.StorageEBS,
              new Function<ReportingComputeZoneDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeZoneDomainModel reportingComputeZoneDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeZoneDomainModel.setSizeEbsAvailableGB( available );
                      return null;
                    }
                  };
                }
              } )
          .build();

  private static final Map<ResourceType,Function<ReportingComputeDomainModel,Function<Long,Void>>> globalSetters =
      ImmutableMap.<ResourceType,Function<ReportingComputeDomainModel,Function<Long,Void>>>builder()
          .put( ResourceType.Address,
              new Function<ReportingComputeDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeDomainModel reportingComputeDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeDomainModel.setNumPublicIpsAvailable( available.intValue() );
                      return null;
                    }
                  };
                }
              } )
          .put( ResourceType.StorageWalrus,
              new Function<ReportingComputeDomainModel,Function<Long,Void>>(){
                @Override
                public Function<Long, Void> apply( final ReportingComputeDomainModel reportingComputeDomainModel ) {
                  return new Function<Long,Void>() {
                    @Override
                    public Void apply( final Long available ) {
                      reportingComputeDomainModel.setSizeS3ObjectAvailableGB( available );
                      return null;
                    }
                  };
                }
              } )
          .build();
}
