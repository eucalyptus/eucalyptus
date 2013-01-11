package com.eucalyptus.vm

import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for instance filter support
 */
class VmInstanceFilterSupport extends FilterSupportTest.InstanceTest<VmInstance> {

  @Test
  void testFilteringSupport() {
    assertValid( new VmInstances.VmInstanceFilterSupport() )
  }
}
