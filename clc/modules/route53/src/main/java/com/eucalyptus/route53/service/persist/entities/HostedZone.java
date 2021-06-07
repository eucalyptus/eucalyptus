/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.entities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.Route53Metadata.HostedZoneMetadata;
import com.eucalyptus.route53.service.dns.Route53DnsHelper;
import com.eucalyptus.route53.service.persist.entities.HostedZone.Status;
import com.eucalyptus.route53.service.persist.views.HostedZoneView;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
@Entity
@PersistenceContext(name = "eucalyptus_route53")
@Table(name = "r53_hosted_zone")
public class HostedZone extends UserMetadata<Status> implements HostedZoneMetadata, HostedZoneView {

  private static final long serialVersionUID = 1L;

  public enum Status {
    PENDING,
    INSYNC,
  }

  @Column( name = "r53_zone_name", nullable = false, updatable = false )
  private String zoneName;

  @Column( name = "r53_zone_comment", length = 256 )
  private String comment;

  @Column( name = "r53_zone_caller_ref", nullable = false, updatable = false )
  private String callerReference;

  @Column( name = "r53_zone_unique_caller_ref", nullable = false, updatable = false, unique = true)
  private String uniqueCallerReference;

  @Column( name = "r53_zone_private", nullable = false, updatable = false )
  private Boolean privateZone;

  @ElementCollection
  @CollectionTable( name = "r53_hosted_zone_vpc", joinColumns = @JoinColumn( name = "r53_hosted_zone_id" ) )
  @Column( name = "r53_hosted_zone_vpc_id" )
  @OrderColumn( name = "r53_hosted_zone_vpc_id_index")
  private List<String> vpcIds = Lists.newArrayList();

  @Column( name = "r53_rr_set_count", nullable = false )
  private Integer resourceRecordSetCount;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hostedZone" )
  private Collection<ResourceRecordSet> resourceRecordSets;

  @ElementCollection
  @CollectionTable( name = "r53_hosted_zone_tag", joinColumns = @JoinColumn( name = "r53_hosted_zone_id" ) )
  @MapKeyColumn( name = "r53_zone_tag_key" )
  @Column( name = "r53_zone_tag_value", length = 256 )
  private Map<String,String> tags = Maps.newHashMap( );

  protected HostedZone() {
  }

  protected HostedZone(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static HostedZone create(
      final OwnerFullName owner,
      final String displayName,
      final String callerReference,
      final String zoneName,
      final Boolean privateZone
  ) {
    final HostedZone zone = new HostedZone(owner, displayName);
    zone.setCallerReference(callerReference);
    zone.setUniqueCallerReference(owner.getAccountNumber() + ":" + callerReference);
    zone.setZoneName(Route53DnsHelper.absoluteName(zoneName));
    zone.setPrivateZone(privateZone);
    zone.setState(Status.INSYNC);
    zone.setResourceRecordSetCount(0);
    return zone;
  }

  public static HostedZone exampleWithOwner(final OwnerFullName owner) {
    return new HostedZone(owner, null);
  }

  public static HostedZone exampleWithName(final OwnerFullName owner, final String name) {
    return new HostedZone(owner, name);
  }

  public String getZoneName() {
    return zoneName;
  }

  public void setZoneName(final String zoneName) {
    this.zoneName = zoneName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public String getCallerReference() {
    return callerReference;
  }

  public void setCallerReference(final String callerReference) {
    this.callerReference = callerReference;
  }

  public String getUniqueCallerReference() {
    return uniqueCallerReference;
  }

  public void setUniqueCallerReference(final String uniqueCallerReference) {
    this.uniqueCallerReference = uniqueCallerReference;
  }

  public Boolean getPrivateZone() {
    return privateZone;
  }

  public void setPrivateZone(final Boolean privateZone) {
    this.privateZone = privateZone;
  }

  public List<String> getVpcIds() {
    return vpcIds;
  }

  public void setVpcIds(final List<String> vpcIds) {
    this.vpcIds = vpcIds;
  }

  public Integer getResourceRecordSetCount() {
    return resourceRecordSetCount;
  }

  public void setResourceRecordSetCount(final Integer resourceRecordSetCount) {
    this.resourceRecordSetCount = resourceRecordSetCount;
  }

  public Collection<ResourceRecordSet> getResourceRecordSets() {
    return resourceRecordSets;
  }

  public void setResourceRecordSets(final Collection<ResourceRecordSet> resourceRecordSets) {
    this.resourceRecordSets = resourceRecordSets;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  @Override
  public String getPartition() {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Route53.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId("hostedzone", getDisplayName());
  }

}
