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
package com.eucalyptus.images

import static org.junit.Assert.*
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.cloud.ImageMetadata.Architecture;
import com.eucalyptus.cloud.ImageMetadata.State;
import com.eucalyptus.cloud.ImageMetadata.Type;
import com.eucalyptus.images.Images.FilterBooleanFunctions;
import com.eucalyptus.images.Images.FilterBooleanSetFunctions;
import com.eucalyptus.images.Images.FilterIntegerSetFunctions;
import com.eucalyptus.images.Images.FilterStringFunctions;
import com.eucalyptus.images.Images.FilterStringSetFunctions;
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.util.TypeMappers;

import org.hibernate.type.ImageType;
import org.junit.Test

/**
 * Unit tests for image filter support
 */
class ImageInfoFilterSupportTest extends FilterSupportTest.InstanceTest<ImageInfo> {

  @Test
  void testFilteringSupport() {
    assertValid( new Images.ImageInfoFilterSupport() )
  }
  @Test
  void testPredicateFilters() {


    

    assertMatch(true, "architecture", "i386", new ImageInfo(architecture: Architecture.i386));
    assertMatch(false, "architecture", "i386", new ImageInfo(architecture: Architecture.x86_64));

    // Not sure how to test:
    //block-device-mapping.delete-on-termination
    //block-device-mapping.device-name
    //block-device-mapping.snapshot-id
    //block-device-mapping.volume-size
    //block-device-mapping.volume-type

    assertMatch(true, "description", "description1", new ImageInfo(description: "description1"));
    assertMatch(false, "description", "description1", new ImageInfo(description: "description2"));

    assertMatch(true, "image-id", "id1", new ImageInfo(displayName: "id1"));
    assertMatch(false, "image-id", "id1", new ImageInfo(displayName: "id2"));

    assertMatch(true, "image-type", "machine", new ImageInfo(imageType: Type.machine));
    assertMatch(false, "image-type", "machine", new ImageInfo(imageType: Type.kernel));

    // these fields do not have a getter on ImageInfo, they require 
    // using the TO_IMAGE_DETAILS.apply() method which needs TypeMappers to set up
    //kernel-id
    //manifest-location
    //owner-alias
    //platform
    //ramdisk-id
    //root-device-name
    //root-device-type

// Tried the below, got InitializationError    
//    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery();
//    assertTrue(discovery.processClass(Images.BlockStorageImageDetails.class));
//    assertTrue(discovery.processClass(Images.KernelImageDetails.class);
//    assertTrue(discovery.processClass(Images.RamdiskImageDetails.class));
//    assertTrue(discovery.processClass(Images.MachineImageDetails.class));
//    assertTrue(discovery.processClass(Images.DeviceMappingDetails.class));

    //    assertMatch(true, "kernel-id", "kernel-id-1", new BlockStorageImageInfo(kernelId: "kernel-id-1"));
//    assertMatch(false, "kernel-id", "kernel-id-1", new BlockStorageImageInfo(kernelId: "kernel-id-2"));
//    assertMatch(true, "kernel-id", "kernel-id-3", new MachineImageInfo(kernelId: "kernel-id-3"));
//    assertMatch(false, "kernel-id", "kernel-id-3", new MachineImageInfo(kernelId: "kernel-id-4"));

    assertMatch(true, "name", "name1", new ImageInfo(imageName: "name1"));
    assertMatch(false, "name", "name1", new ImageInfo(imageName: "name2"));

    assertMatch(true, "owner-id", "owner-id1", new ImageInfo(ownerAccountNumber: "owner-id1"));
    assertMatch(false, "owner-id", "owner-id1", new ImageInfo(ownerAccountNumber: "owner-id2"));
//    setting state causes InitializationErrors
//    assertMatch(true, "state", "pending", new ImageInfo(state: State.pending));
//    assertMatch(false, "state", "pending", new ImageInfo(state: State.deregistered));
    

   // Not sure how to test product codes (many to 1)
    
   }
  

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final ImageInfo target) {
    super.assertMatch( new Images.ImageInfoFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

  
}
