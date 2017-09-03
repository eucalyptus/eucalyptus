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
package com.eucalyptus.images;

import static com.eucalyptus.compute.common.ImageMetadata.Platform;
import java.util.Collections;
import java.util.Set;

import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.images.BootableImageInfo;

/**
 * BootableImageInfo for use when original info is unavailable
 */
class UnavailableImageInfo implements BootableImageInfo {

  private final Platform platform;

  UnavailableImageInfo( final Platform platform ) {
    this.platform = platform;
  }

  @Override
  public String getKernelId() {
    return null;
  }

  @Override
  public ImageMetadata.Architecture getArchitecture() {
    return null;
  }

  @Override
  public String getRamdiskId() {
    return null;
  }

  @Override
  public Platform getPlatform() {
    return platform;
  }

  @Override
  public Long getImageSizeBytes() {
    return 0L;
  }

  @Override
  public String getDisplayName() {
    return ResourceIdentifiers.tryNormalize( ).apply( "emi-00000000" );
  }

  @Override
  public Set<String> getProductCodes() {
    return Collections.emptySet();
  }

  @Override
  public String getRootDeviceName() {
    return "";
  }

  @Override
  public String getRootDeviceType() {
    return "";
  }

  @Override
  public ImageMetadata.VirtualizationType getVirtualizationType(){
	  return ImageMetadata.VirtualizationType.paravirtualized;
  }
  
}
