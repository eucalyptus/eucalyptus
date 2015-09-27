/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.internal.vm;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.upgrade.Upgrades;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances_persistent_volumes" )
@org.hibernate.annotations.Table(
    appliesTo = "metadata_instances_persistent_volumes",
    indexes =  @Index( name = "metadata_instances_persistent_volumes_vmid_idx", columnNames = "vminstance_id" )
)
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VmBootVolumeAttachment extends VmVolumeAttachment {
  private static final long serialVersionUID = 1L;

  VmBootVolumeAttachment( ) {
    super( );
  }

  public VmBootVolumeAttachment(
      final VmInstance vmInstance,
      final String volumeId,
      final String device,
      final String remoteDevice,
      final String status,
      final Date attachTime,
      final Boolean deleteOnTerminate,
      final Boolean rootDevice,
      final Boolean attachedAtStartup
  ) {
    super(
        vmInstance,
        volumeId,
        device,
        remoteDevice,
        status,
        attachTime,
        deleteOnTerminate,
        rootDevice,
        attachedAtStartup
    );
  }

  @Upgrades.PreUpgrade( since = v4_2_0, value = Compute.class )
  public static class SchemaUpgradeForEntity extends SchemaUpgradeForEntitySupport  {

    public SchemaUpgradeForEntity( ) {
      super(
          "metadata_instances_persistent_volumes",
          "metadata_instances_persistent_volumes_pkey",
          "uk_muacb67gpe742w0bfxdyk64c5"
      );
    }
  }
}
