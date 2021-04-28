/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.blockstorage;

import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_volume_modifications", indexes = {
    @Index( name = "metadata_volumes_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_volumes_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_volumes_display_name_idx", columnList = "metadata_display_name" ),
} )
public class VolumeModification extends UserMetadata<VolumeModification.ModificationState> implements RestrictedType {

  private static final long serialVersionUID = 1L;

  public enum ModificationState {
    modifying(true),
    optimizing(true),
    completed,
    failed,
    ;

    private final boolean active;

    ModificationState(final boolean active) {
      this.active = active;
    }

    ModificationState() {
      this(false);
    }

    public boolean isActive() {
      return active;
    }
  }

  @Column(name = "metadata_progress")
  private Integer progress;

  @Column(name = "metadata_status_message")
  private String statusMessage;

  @Column(name = "metadata_start_time")
  private Date startTime;

  @Column(name = "metadata_end_time")
  private Date endTime;

  @Column(name = "metadata_iops_original")
  private Integer originalIops;

  @Column(name = "metadata_iops_target")
  private Integer targetIops;

  @Column(name = "metadata_size_original")
  private Integer originalSize;

  @Column(name = "metadata_size_target")
  private Integer targetSize;

  @Column(name = "metadata_type_original")
  private String originalVolumeType;

  @Column(name = "metadata_type_target")
  private String targetVolumeType;

  @Column( name = "metadata_volume_partition", updatable = false)
  private String partition;

  @JoinColumn(name = "metadata_volume_refid", unique = true, updatable = false, nullable = false)
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  private Volume volume;

  protected VolumeModification() {
  }

  protected VolumeModification(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static VolumeModification create(
      final OwnerFullName owner,
      final Volume volume
  ) {
    final VolumeModification volumeModification = new VolumeModification(owner, volume.getDisplayName());
    volumeModification.setVolume(volume);
    volumeModification.setPartition(volume.getPartition());
    volumeModification.markStart();
    return volumeModification;
  }

  public static VolumeModification named(final OwnerFullName owner, final String volumeId) {
    return new VolumeModification(owner, volumeId);
  }

  public static VolumeModification exampleWithOwner(final OwnerFullName owner) {
    final VolumeModification example = new VolumeModification();
    example.setOwner(owner);
    return example;
  }

  public void markStart() {
    setState(ModificationState.modifying);
    setStatusMessage(null);
    setStartTime(new Date());
    setEndTime(null);
    setProgress(0);
    setOriginalIops(null);
    setOriginalSize(null);
    setOriginalVolumeType(null);
    setTargetIops(null);
    setTargetSize(null);
    setTargetVolumeType(null);
  }

  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public Integer getOriginalIops() {
    return originalIops;
  }

  public void setOriginalIops(Integer originalIops) {
    this.originalIops = originalIops;
  }

  public Integer getTargetIops() {
    return targetIops;
  }

  public void setTargetIops(Integer targetIops) {
    this.targetIops = targetIops;
  }

  public Integer getTargetSize() {
    return targetSize;
  }

  public void setTargetSize(Integer targetSize) {
    this.targetSize = targetSize;
  }

  public Integer getOriginalSize() {
    return originalSize;
  }

  public void setOriginalSize(Integer originalSize) {
    this.originalSize = originalSize;
  }

  public String getOriginalVolumeType() {
    return originalVolumeType;
  }

  public void setOriginalVolumeType(String originalVolumeType) {
    this.originalVolumeType = originalVolumeType;
  }

  public String getTargetVolumeType() {
    return targetVolumeType;
  }

  public void setTargetVolumeType(String targetVolumeType) {
    this.targetVolumeType = targetVolumeType;
  }

  @Override
  public String getPartition() {
    return partition;
  }

  public void setPartition(String partition) {
    this.partition = partition;
  }

  public String getVolumeId() {
    return getDisplayName();
  }

  public Volume getVolume() {
    return volume;
  }

  public void setVolume(Volume volume) {
    this.volume = volume;
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( this.getOwnerAccountNumber( ) )
        .relativeId( "volume", this.getVolumeId(), "modification" );
  }
}
