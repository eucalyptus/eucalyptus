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
package com.eucalyptus.images

import com.eucalyptus.compute.common.internal.images.BlockStorageDeviceMapping
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo
import com.eucalyptus.compute.common.internal.images.DeviceMapping
import com.eucalyptus.compute.common.internal.images.ImageInfo
import com.eucalyptus.compute.common.internal.images.KernelImageInfo
import com.eucalyptus.compute.common.internal.images.MachineImageInfo
import com.eucalyptus.compute.common.internal.images.RamdiskImageInfo
import org.junit.Test
import com.eucalyptus.compute.common.ImageMetadata.Architecture
import com.eucalyptus.compute.common.ImageMetadata.Type
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.compute.common.ImageMetadata

/**
 * Unit tests for image filter support
 */
class ImageInfoFilterSupportTest extends FilterSupportTest.InstanceTestSupport<ImageInfo> {
                                                                          
  @Test
  void testFilteringSupport() {
    assertValid( new Images.ImageInfoFilterSupport(), [ 
        (DeviceMapping.class) : [ BlockStorageDeviceMapping.class ],
        (ImageInfo.class) : [ KernelImageInfo.class, MachineImageInfo.class, RamdiskImageInfo.class ]
    ] )
  }
  
  @Test
  void testPredicateFilters() {
    assertMatch(true, "architecture", "i386", new ImageInfo(architecture: Architecture.i386))
    assertMatch(true, "architecture", "i*", new ImageInfo(architecture: Architecture.i386))
    assertMatch(false, "architecture", "i386", new ImageInfo(architecture: Architecture.x86_64))
    assertMatch(false, "architecture", "i386", new ImageInfo())

    assertMatch(true, "block-device-mapping.delete-on-termination", "true", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(delete: true) ] as List))
    assertMatch(true, "block-device-mapping.delete-on-termination", "true", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(delete: true), new BlockStorageDeviceMapping(delete: false) ] as List))
    assertMatch(true, "block-device-mapping.delete-on-termination", "false", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(delete: true), new BlockStorageDeviceMapping(delete: false) ] as List))
    assertMatch(false, "block-device-mapping.delete-on-termination", "true", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(delete: false) ] as List ))
    assertMatch(false, "block-device-mapping.delete-on-termination", "true", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping() ] as List ))
    assertMatch(false, "block-device-mapping.delete-on-termination", "true", new ImageInfo(deviceMappings: [] as List ))
    assertMatch(false, "block-device-mapping.delete-on-termination", "true", new ImageInfo())

    assertMatch(true, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(deviceName: "/dev/sdb1") ] as List))
    assertMatch(true, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(deviceName: "/dev/sdb1"), new BlockStorageDeviceMapping(deviceName: "/dev/sdb2") ] as List))
    assertMatch(true, "block-device-mapping.device-name", "/dev/sdb2", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(deviceName: "/dev/sdb1"), new BlockStorageDeviceMapping(deviceName: "/dev/sdb2") ] as List))
    assertMatch(false, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(deviceName: "/dev/sdb2") ] as List ))
    assertMatch(false, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping() ] as List ))
    assertMatch(false, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo(deviceMappings: [] as List ))
    assertMatch(false, "block-device-mapping.device-name", "/dev/sdb1", new ImageInfo())

    assertMatch(true, "block-device-mapping.snapshot-id", "snap-00000001", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(snapshotId:  "snap-00000001") ] as List))
    assertMatch(false, "block-device-mapping.snapshot-id", "snap-00000001", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(snapshotId: "snap-00000002") ] as List ))

    assertMatch(true, "block-device-mapping.volume-size", "1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(size: 1) ] as List))
    assertMatch(false, "block-device-mapping.volume-size", "1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping(size: 2) ] as List ))
    assertMatch(false, "block-device-mapping.volume-size", "1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping() ] as List ))

    assertMatch(true, "block-device-mapping.volume-type", "standard", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping() ] as List))
    assertMatch(false, "block-device-mapping.volume-type", "io1", new ImageInfo(deviceMappings: [ new BlockStorageDeviceMapping() ] as List ) )

    assertMatch(true, "description", "description1", new ImageInfo(description: "description1"))
    assertMatch(false, "description", "description1", new ImageInfo(description: "description2"))
    assertMatch(false, "description", "description1", new ImageInfo())

    assertMatch(true, "image-id", "id1", new ImageInfo(displayName: "id1"))
    assertMatch(false, "image-id", "id1", new ImageInfo(displayName: "id2"))
    assertMatch(false, "image-id", "id1", new ImageInfo())

    assertMatch(true, "image-type", "machine", new ImageInfo(imageType: Type.machine))
    assertMatch(false, "image-type", "machine", new ImageInfo(imageType: Type.kernel))
    assertMatch(false, "image-type", "machine", new ImageInfo())

    assertMatch(true, "is-public", "true", new ImageInfo(imagePublic: true))
    assertMatch(false, "is-public", "true", new ImageInfo(imagePublic: false))
    assertMatch(false, "is-public", "true", new ImageInfo())

    assertMatch(true, "kernel-id", "eki-00000001", new BlockStorageImageInfo(kernelId: "eki-00000001"))
    assertMatch(false, "kernel-id", "eki-00000001", new BlockStorageImageInfo(kernelId: "eki-00000002"))
    assertMatch(false, "kernel-id", "eki-00000001", new BlockStorageImageInfo())

    assertMatch(true, "manifest-location", "image/fedora-16-x86_64.manifest.xml", new MachineImageInfo(manifestLocation: "image/fedora-16-x86_64.manifest.xml"))
    assertMatch(false, "manifest-location", "image/fedora-16-x86_64.manifest.xml", new MachineImageInfo(manifestLocation: "image/fedora-16-x86_64.manifest2.xml"))
    assertMatch(false, "manifest-location", "image/fedora-16-x86_64.manifest.xml", new MachineImageInfo())

    assertMatch(true, "name", "name1", new ImageInfo(imageName: "name1"))
    assertMatch(false, "name", "name1", new ImageInfo(imageName: "name2"))
    assertMatch(false, "name", "name1", new ImageInfo())

    //assertMatch(true, "owner-alias", "owner", new ImageInfo(ownerAccountName: "owner")) //TODO:STEVE: Owner alias test
    //assertMatch(false, "owner-alias", "owner", new ImageInfo(ownerAccountName: "owner2"))
    //assertMatch(false, "owner-alias", "owner", new ImageInfo())

    assertMatch(true, "owner-id", "owner-id1", new ImageInfo(ownerAccountNumber: "owner-id1"))
    assertMatch(false, "owner-id", "owner-id1", new ImageInfo(ownerAccountNumber: "owner-id2"))
    assertMatch(false, "owner-id", "owner-id1", new ImageInfo())

    assertMatch(true, "platform", "", new ImageInfo(platform: ImageMetadata.Platform.linux))
    assertMatch(true, "platform", "windows", new ImageInfo(platform: ImageMetadata.Platform.windows))
    assertMatch(false, "platform", "linux", new ImageInfo(platform: ImageMetadata.Platform.windows))
    assertMatch(false, "platform", "linux", new ImageInfo())

    assertMatch(true, "product-code", "product1", new ImageInfo(productCodes: ["product1"] as Set))
    assertMatch(false, "product-code", "product2", new ImageInfo(productCodes: ["product1"] as Set))
    assertMatch(false, "product-code", "product2", new ImageInfo(productCodes: [] as Set))    
    assertMatch(false, "product-code", "product2", new ImageInfo())

    assertMatch(true, "ramdisk-id", "eri-00000001", new BlockStorageImageInfo(ramdiskId: "eri-00000001"))
    assertMatch(false, "ramdisk-id", "eri-00000001", new BlockStorageImageInfo(ramdiskId: "eri-00000002"))
    assertMatch(false, "ramdisk-id", "eri-00000001", new BlockStorageImageInfo())

    assertMatch(true, "root-device-name", "/dev/sda1", new MachineImageInfo())
    assertMatch(false, "root-device-name", "/dev/xda1", new MachineImageInfo())
    assertMatch(false, "root-device-name", "/dev/xda1", new KernelImageInfo())

    assertMatch(true, "root-device-type", "instance-store", new MachineImageInfo())
    assertMatch(false, "root-device-type", "instance-store", new BlockStorageImageInfo())
    assertMatch(false, "root-device-type", "instance-store", new KernelImageInfo())

    assertMatch(true, "state", "available", new MachineImageInfo(state: ImageMetadata.State.available))
    assertMatch(false, "state", "available", new MachineImageInfo(state: ImageMetadata.State.pending))
    assertMatch(false, "state", "available", new MachineImageInfo())
  }  

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final ImageInfo target) {
    super.assertMatch( new Images.ImageInfoFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

  
}
