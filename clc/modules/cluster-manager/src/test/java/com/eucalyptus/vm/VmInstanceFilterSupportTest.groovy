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
package com.eucalyptus.vm

import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.images.ImageInfo
import com.eucalyptus.images.KernelImageInfo
import com.eucalyptus.images.MachineImageInfo
import com.eucalyptus.images.RamdiskImageInfo

/**
 * Unit tests for instance filter support
 */
@SuppressWarnings("GroovyAccessibility")
class VmInstanceFilterSupportTest extends FilterSupportTest.InstanceTest<VmInstance> {

  @Test
  void testFilteringSupport() {
    assertValid( new VmInstances.VmInstanceFilterSupport(), [ (ImageInfo.class) : [ KernelImageInfo.class, MachineImageInfo.class, RamdiskImageInfo.class ] ] )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "instance-id", "i-00000001", new VmInstance(displayName: "i-00000001") )
    assertMatch( true, "instance-id", "i*", new VmInstance(displayName: "i-00000001") )
    assertMatch( false, "instance-id", "i-00000001", new VmInstance(displayName: "i-00000002") )
    assertMatch( false, "instance-id", "i-00000001", new VmInstance( ) )

    assertMatch( true, "owner-id", "1234567890", new VmInstance(ownerAccountNumber: "1234567890") )
    assertMatch( true, "owner-id", "123*", new VmInstance(ownerAccountNumber: "1234567890") )
    assertMatch( false, "owner-id", "1234567890", new VmInstance(ownerAccountNumber: "1234567891") )
    assertMatch( false, "owner-id", "1234567890", new VmInstance( ) )
    
    //TODO:STEVE: add remaining filters when mock library available
  }

  private void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final VmInstance target) {
    super.assertMatch( new VmInstances.VmInstanceFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
