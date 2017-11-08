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
package com.eucalyptus.reporting.modules.capacity

import com.eucalyptus.reporting.service.ReportingService
import org.junit.BeforeClass
import org.junit.Test
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Dimension
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Type
import com.eucalyptus.reporting.domain.ReportingComputeDomainModel
import com.eucalyptus.reporting.domain.ReportingComputeDomainModel.ReportingComputeZoneDomainModel;

import static org.junit.Assert.*
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.*
import javax.annotation.Nonnull
import com.google.common.collect.Sets

/**
 * Unit test for ResourceAvailabilityEventListener
 */
class ResourceAvailabilityEventListenerTest {

  @BeforeClass
  static void beforeClass( ) {
    ReportingService.DATA_COLLECTION_ENABLED = true
  }

  @Test
  void testInstantiable() {
    new ResourceAvailabilityEventListener()
  }

  @Test
  void testInstanceAvailability() {
    def ( _, ReportingComputeZoneDomainModel zone1Model,
    ReportingComputeZoneDomainModel zone2Model ) = testEvent( resourceEvent( Instance, 50, 27 , 'zone1', 'vm-type', 'm1.small' ) )

    assertEquals( 'zone1 availability', 27, zone1Model.getInstancesAvailableForType('m1.small') )
    assertNull( 'zone2 availability', zone2Model.getEc2ComputeUnitsAvailable() )
  }

  @Test
  void testComputeUnitAvailability() {
    def ( _, ReportingComputeZoneDomainModel zone1Model,
             ReportingComputeZoneDomainModel zone2Model ) = testEvent( resourceEvent( Core, 50, 27 , 'zone1' ) )

    assertEquals( 'zone1 availability', 27, zone1Model.getEc2ComputeUnitsAvailable() )
    assertNull( 'zone2 availability', zone2Model.getEc2ComputeUnitsAvailable() )
  }

  @Test
  void testMemoryAvailability() {
    def ( _, ReportingComputeZoneDomainModel zone1Model,
             ReportingComputeZoneDomainModel zone2Model ) = testEvent( resourceEvent( Memory, 40, 0 , 'zone2' ) )

    assertEquals( 'zone2 availability', 0, zone2Model.getEc2MemoryUnitsAvailable() )
    assertNull( 'zone1 availability', zone1Model.getEc2MemoryUnitsAvailable() )
  }

  @Test
  void testDiskAvailability() {
    def ( _, ReportingComputeZoneDomainModel zone1Model,
             ReportingComputeZoneDomainModel zone2Model ) = testEvent( resourceEvent( Disk, Integer.MAX_VALUE, Integer.MAX_VALUE , 'zone2' ) )

    assertEquals( 'zone2 availability', Integer.MAX_VALUE, zone2Model.getEc2DiskUnitsAvailable() )
    assertNull( 'zone1 availability', zone1Model.getEc2DiskUnitsAvailable() )
  }

  @Test
  void testEbsStorageAvailability() {
    def ( _, ReportingComputeZoneDomainModel zone1Model,
             ReportingComputeZoneDomainModel zone2Model ) = testEvent( resourceEvent( StorageEBS, 10000, 6666 , 'zone1' ) )

    assertEquals( 'zone1 availability', 6666, zone1Model.getSizeEbsAvailableGB() )
    assertNull( 'zone2 availability', zone2Model.getSizeEbsAvailableGB() )
  }

  @Test
  void testAddressAvailability() {
    def ( ReportingComputeDomainModel globalModel ) = testEvent( resourceEvent( Address, 6, 6 ) )

    assertEquals( 'availability', 6, globalModel.getNumPublicIpsAvailable() )
  }

  @Test
  void testWalrusStorageAvailability() {
    def ( ReportingComputeDomainModel globalModel ) = testEvent( resourceEvent( StorageWalrus, Long.MAX_VALUE, Long.MAX_VALUE ) )

    assertEquals( 'availability', Long.MAX_VALUE, globalModel.getSizeS3ObjectAvailableGB() )
  }

  private ResourceAvailabilityEvent resourceEvent( ResourceType type,
                                                   long total,
                                                   long available,
                                                   String availabilityZone=null,
                                                   String extraTagName=null,
                                                   String extraTagValue=null ) {
    Set<Tag> tags = Sets.newHashSet();
    if ( availabilityZone != null ) {
      tags.add( new Dimension( "availabilityZone", availabilityZone ) )
    }
    if ( extraTagName != null && extraTagValue != null ) {
      tags.add( new Type( extraTagName, extraTagValue ) )
    }

    new ResourceAvailabilityEvent( type, new Availability( total, available, tags ) )
  }

  private List testEvent( ResourceAvailabilityEvent event ) {
    ReportingComputeDomainModel globalModel = new ReportingComputeDomainModel()
    ReportingComputeZoneDomainModel zone1Model = new ReportingComputeZoneDomainModel()
    ReportingComputeZoneDomainModel zone2Model = new ReportingComputeZoneDomainModel()
    Map<String,ReportingComputeZoneDomainModel> zones = [
      'zone1': zone1Model,
      'zone2': zone2Model
    ]
    ResourceAvailabilityEventListener listener = new ResourceAvailabilityEventListener( ) {
      @Override @Nonnull protected ReportingComputeDomainModel getReportingComputeDomainModel() { globalModel }
      @Override @Nonnull protected ReportingComputeZoneDomainModel getReportingComputeDomainModelForZone( @Nonnull final String availabilityZone ) { zones.get(availabilityZone) }
    }

    listener.fireEvent( event )

    [ globalModel, zone1Model, zone2Model ]
  }
}
