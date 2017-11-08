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
package com.eucalyptus.vm

import com.eucalyptus.compute.common.internal.vm.VmInstance
import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.compute.common.internal.images.ImageInfo
import com.eucalyptus.compute.common.internal.images.KernelImageInfo
import com.eucalyptus.compute.common.internal.images.MachineImageInfo
import com.eucalyptus.compute.common.internal.images.RamdiskImageInfo

/**
 * Unit tests for instance filter support
 */
@SuppressWarnings("GroovyAccessibility")
class VmInstanceFilterSupportTest extends FilterSupportTest.InstanceTestSupport<VmInstance> {

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
