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
package com.eucalyptus.blockstorage;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.tags.FilterSupportTest;
import com.eucalyptus.vm.VmVolumeAttachment;
import com.eucalyptus.vm.VmVolumeState;
import org.junit.Test;

class VolumeFilterSupportTest extends FilterSupportTest.InstanceTest<Volume> {
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