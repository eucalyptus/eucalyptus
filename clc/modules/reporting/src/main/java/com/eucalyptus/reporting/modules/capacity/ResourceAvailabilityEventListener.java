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
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

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

    final ModelComputeUpdater<ReportingComputeZoneDomainModel> zoneSetter = zoneSetters.get( event.getType() );
    final ModelComputeUpdater<ReportingComputeDomainModel> globalSetter = globalSetters.get( event.getType() );
    for ( final Availability availability : event.getAvailability() ) {
      if ( zoneSetter != null ) {
        final ReportingComputeZoneDomainModel zoneModel = getZoneModelForTags( availability.getTags() );
        if ( zoneModel == null ) continue;
        zoneSetter.update( zoneModel, availability.getTags(), availability.getAvailable(), availability.getTotal() );
      }
      if ( globalSetter != null ) {
        final ReportingComputeDomainModel globalModel = getReportingComputeDomainModel();
        globalSetter.update( globalModel, availability.getTags(), availability.getAvailable(), availability.getTotal() );
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

  private static interface ModelComputeUpdater<T> {
    void update( T model, Set<Tag> tags, Long available, Long total );
  }

  private static final Map<ResourceType,ModelComputeUpdater<ReportingComputeZoneDomainModel>> zoneSetters =
      ImmutableMap.<ResourceType,ModelComputeUpdater<ReportingComputeZoneDomainModel>>builder()
          .put( ResourceType.Instance,
              new ModelComputeUpdater<ReportingComputeZoneDomainModel>() {
                @Override
                public void update( final ReportingComputeZoneDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  final Tag vmTypeTag = Iterables.find( tags, Predicates.compose( Predicates.equalTo( "vm-type" ), ResourceAvailabilityEvent.tagType() ), null );
                  if ( vmTypeTag != null ) {
                    model.setInstancesAvailableForType( vmTypeTag.getValue(), available.intValue() );
                    model.setInstancesTotalForType( vmTypeTag.getValue(), total.intValue() );
                  }
                }
              } )
          .put( ResourceType.Core,
              new ModelComputeUpdater<ReportingComputeZoneDomainModel>(){
                @Override
                public void update( final ReportingComputeZoneDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setEc2ComputeUnitsAvailable( available.intValue() );
                  model.setEc2ComputeUnitsTotal( total.intValue() );
                }
              } )
          .put( ResourceType.Memory,
              new ModelComputeUpdater<ReportingComputeZoneDomainModel>(){
                @Override
                public void update( final ReportingComputeZoneDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setEc2MemoryUnitsAvailable( available.intValue() );
                  model.setEc2MemoryUnitsTotal( total.intValue() );
                }
              } )
          .put( ResourceType.Disk,
              new ModelComputeUpdater<ReportingComputeZoneDomainModel>(){
                @Override
                public void update( final ReportingComputeZoneDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setEc2DiskUnitsAvailable( available.intValue() );
                  model.setEc2DiskUnitsTotal( total.intValue() );
                }
              } )
          .put( ResourceType.StorageEBS,
              new ModelComputeUpdater<ReportingComputeZoneDomainModel>(){
                @Override
                public void update( final ReportingComputeZoneDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setSizeEbsAvailableGB( available );
                  model.setSizeEbsTotalGB( total );
                }
              } )
          .build();

  private static final Map<ResourceType,ModelComputeUpdater<ReportingComputeDomainModel>> globalSetters =
      ImmutableMap.<ResourceType,ModelComputeUpdater<ReportingComputeDomainModel>>builder()
          .put( ResourceType.Address,
              new ModelComputeUpdater<ReportingComputeDomainModel>(){
                @Override
                public void update( final ReportingComputeDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setNumPublicIpsAvailable( available.intValue() );
                  model.setNumPublicIpsTotal( total.intValue() );
                }
              } )
          .put( ResourceType.StorageWalrus,
              new ModelComputeUpdater<ReportingComputeDomainModel>(){
                @Override
                public void update( final ReportingComputeDomainModel model, final Set<Tag> tags, final Long available, final Long total ) {
                  model.setSizeS3ObjectAvailableGB( available );
                  model.setSizeS3ObjectTotalGB( total );
                }
              } )
          .build();
}
