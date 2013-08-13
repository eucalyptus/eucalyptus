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
package com.eucalyptus.images;

import static com.eucalyptus.cloud.ImageMetadata.Platform;
import java.util.Collections;
import java.util.Set;

import com.eucalyptus.cloud.ImageMetadata;

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
  public String getRamdiskId() {
    return null;
  }

  @Override
  public boolean hasKernel() {
    return false;
  }

  @Override
  public boolean hasRamdisk() {
    return false;
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
    return "emi-00000000";
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
