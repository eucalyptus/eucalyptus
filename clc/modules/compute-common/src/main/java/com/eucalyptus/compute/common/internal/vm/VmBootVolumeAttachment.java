/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.vm;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.upgrade.Upgrades;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances_persistent_volumes", indexes = {
    @Index( name = "metadata_instances_persistent_volumes_vmid_idx", columnList = "vminstance_id" ),
} )
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

  public static VmBootVolumeAttachment example( ) {
    return new VmBootVolumeAttachment( );
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
