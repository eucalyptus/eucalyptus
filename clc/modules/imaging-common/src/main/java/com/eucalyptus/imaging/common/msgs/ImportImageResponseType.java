/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.imaging.common.msgs;

public class ImportImageResponseType extends ImagingMessage {

  private DiskImageConversionTask conversionTask;

  public void ImportImageResponse( ) {
  }

  public DiskImageConversionTask getConversionTask( ) {
    return conversionTask;
  }

  public void setConversionTask( DiskImageConversionTask conversionTask ) {
    this.conversionTask = conversionTask;
  }
}
