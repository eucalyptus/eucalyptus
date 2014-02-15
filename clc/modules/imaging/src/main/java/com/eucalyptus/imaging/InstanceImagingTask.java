/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.util.OwnerFullName;

import edu.ucsb.eucalyptus.msgs.ConversionTask;

/**
 * @author Sang-Min Park
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_imaging" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "instance-imaging-task" )
public class InstanceImagingTask extends VolumeImagingTask {

  protected InstanceImagingTask(OwnerFullName ownerFullName,
      ConversionTask conversionTask, Integer volumeSize,
      String availabilityZone, String format, Long bytes,
      String importManifestUrl, String description) {
    super(ownerFullName, conversionTask, volumeSize, availabilityZone, format,
        bytes, importManifestUrl, description);
    // TODO Auto-generated constructor stub
  }
  
}
