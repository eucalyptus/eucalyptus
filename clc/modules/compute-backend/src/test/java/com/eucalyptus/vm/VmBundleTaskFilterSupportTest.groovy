/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import com.eucalyptus.compute.common.internal.vm.VmBundleTask
import com.eucalyptus.compute.common.internal.vm.VmInstance
import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.compute.common.internal.tags.FilterSupport

/**
 * Unit tests for bundle task filter support
 */
class VmBundleTaskFilterSupportTest extends FilterSupportTest.InstanceTestSupport<VmBundleTask> {

  @Test
  void testFilteringSupport() {
    FilterSupport<VmBundleTask> filterSupport = new VmInstances.VmBundleTaskFilterSupport( )
    assertValidAliases( filterSupport )
    assertValidFilters( filterSupport, VmInstance, [:] )
    assertValidKeys( filterSupport )
    assertValidTagConfig( filterSupport )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "error-code", "test", new VmBundleTask( errorCode: "test" ) )
    assertMatch( false, "error-code", "test", new VmBundleTask( errorCode: "not test" ) )
    assertMatch( false, "error-code", "test", new VmBundleTask( ) )

    assertMatch( true, "error-message", "test", new VmBundleTask( errorMessage: "test" ) )
    assertMatch( false, "error-message", "test", new VmBundleTask( errorMessage: "not test" ) )
    assertMatch( false, "error-message", "test", new VmBundleTask( ) )

    assertMatch( true, "progress", "10%", new VmBundleTask( progress: 10 ) )
    assertMatch( false, "progress", "10%", new VmBundleTask( progress: 11 ) )
    assertMatch( false, "progress", "10%", new VmBundleTask( ) )

    assertMatch( true, "s3-bucket", "test", new VmBundleTask( bucket: "test" ) )
    assertMatch( false, "s3-bucket", "test", new VmBundleTask( bucket: "not test" ) )
    assertMatch( false, "s3-bucket", "test", new VmBundleTask( ) )

    assertMatch( true, "s3-prefix", "test", new VmBundleTask( prefix: "test" ) )
    assertMatch( false, "s3-prefix", "test", new VmBundleTask( prefix: "not test" ) )
    assertMatch( false, "s3-prefix", "test", new VmBundleTask( ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "error-code", "te*", new VmBundleTask( errorCode: "test" ) )
    assertMatch( false, "error-code", "te*", new VmBundleTask( errorCode: "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final VmBundleTask target ) {
    super.assertMatch( new VmInstances.VmBundleTaskFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
