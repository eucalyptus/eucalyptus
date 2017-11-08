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
package com.eucalyptus.blockstorage

import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.tags.FilterSupportTest;


import org.junit.Test;

class VolumeFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Volume> {
	@Test
	void testFilteringSupport() {
	  assertValid( new Volumes.VolumeFilterSupport() );
	}
	
	@Test
	void testPredicateFilters() {
		//TODO:SPARK: Non trivial to test attachment-related filters
		 
		//attachment.attach-time
		//attachment.delete-on-termination
		//attachment.device
		//attachment.instance-id
		//attachment.status
		assertMatch(true, "availability-zone", "PARTI00", new Volume(partition: "PARTI00"));
		assertMatch(false, "availability-zone", "PARTI00", new Volume(partition: "PARTI01"));
		
		assertMatch(true, "create-time", "2013-01-09T23:54:39.524Z", new Volume(creationTimestamp: date("2013-01-09T23:54:39.524Z")));
		assertMatch(false, "create-time", "2013-01-10T23:54:39.524Z", new Volume(creationTimestamp: date("2013-01-09T23:54:39.524Z")));
		
		assertMatch(true, "size", "20", new Volume(size: 20));
		assertMatch(false, "size", "20", new Volume(size: 21));
		
		assertMatch(true,"snapshot-id", "snap-35AA423F", new Volume(parentSnapshot: "snap-35AA423F"));
		assertMatch(true,"volume-id", "vol-2A593FFC", new Volume(displayName: "vol-2A593FFC"));
		assertMatch(false,"volume-id", "vol-2A593FFC", new Volume(displayName: "vol-94473F40"));
		
		//assertMatch(true, "status", "available", new Volume(state: State.EXTANT ));
		assertMatch(true, "volume-type", "standard", new Volume());
		assertMatch(false, "volume-type", "io1", new Volume()); // we don't support IOPS volume
	}
  
	void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final Volume target) {
        super.assertMatch( new Volumes.VolumeFilterSupport(), expectedMatch, filterKey, filterValue, target )
	}
}