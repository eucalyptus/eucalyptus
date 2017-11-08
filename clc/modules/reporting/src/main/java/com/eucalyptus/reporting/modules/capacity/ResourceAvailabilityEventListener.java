/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
import com.eucalyptus.reporting.service.ReportingService;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

/**
 * Resource availability listener that updates the compute capacity domain model.
 */
public class ResourceAvailabilityEventListener implements EventListener<ResourceAvailabilityEvent> {

  private static final Logger LOG = Logger.getLogger(ResourceAvailabilityEventListener.class);
  public static void register( ) {
    Listeners.register( ResourceAvailabilityEvent.class, new ResourceAvailabilityEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final ResourceAvailabilityEvent event ) {
    if (!ReportingService.DATA_COLLECTION_ENABLED) {
      ReportingService.faultDisableReportingServiceIfNecessary();
      LOG.trace("Reporting service data collection disabled....ResourceAvailabilityEvent discarded");
      return;
    }
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
      } else if ( tag instanceof Type && !tag.getType().equals("vm-type")) {
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
