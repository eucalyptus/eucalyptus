/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.entities;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.route53.service.dns.Route53DnsHelper;
import com.eucalyptus.route53.service.persist.views.ResourceRecordSetView;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext(name = "eucalyptus_route53")
@Table(name = "r53_resource_record_set")
public class ResourceRecordSet extends AbstractOwnedPersistent implements ResourceRecordSetView {
  private static final long serialVersionUID = 1L;

  public enum Type {
    A(org.xbill.DNS.Type.A),
    AAAA(org.xbill.DNS.Type.AAAA),
    CNAME(org.xbill.DNS.Type.CNAME),
    NS(org.xbill.DNS.Type.NS),
    SOA(org.xbill.DNS.Type.SOA),
    ;

    private final int code;

    Type(final int code) {
      this.code = code;
    }

    public int code() {
      return code;
    }
  }

  @Column( name = "r53_rrset_name", nullable = false, updatable = false )
  private String name;

  @Enumerated( EnumType.STRING )
  @Column( name = "r53_rrset_type", nullable = false, updatable = false )
  private Type type;

  @Column( name = "r53_rrset_ttl", nullable = false )
  private Integer ttl;

  @Column( name = "r53_rrset_alias_zone" )
  private String aliasHostedZoneId;

  @Column( name = "r53_rrset_alias_name" )
  private String aliasDnsName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable( name = "r53_resource_record_set_value", joinColumns = @JoinColumn( name = "r53_rrset_id" ) )
  @Column( name = "r53_rrset_value" )
  @OrderColumn( name = "r53_rrset_value_index")
  private List<String> values = Lists.newArrayList();

  @ManyToOne( optional = false )
  @JoinColumn( name = "r53_rrset_hosted_zone_id", nullable = false, updatable = false)
  private HostedZone hostedZone;

  protected ResourceRecordSet() {
  }

  protected ResourceRecordSet(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static ResourceRecordSet createSimple(
      final HostedZone zone,
      final OwnerFullName owner,
      final String name,
      final Type type,
      final int ttl,
      final List<String> values
  ) {
    final String canonicalName = Route53DnsHelper.absoluteName(name);
    final String displayName = type.name() + ":" + canonicalName;
    final ResourceRecordSet rrset = new ResourceRecordSet(owner, displayName);
    rrset.setHostedZone(zone);
    rrset.setName(canonicalName);
    rrset.setType(type);
    rrset.setTtl(ttl);
    rrset.setValues(values);
    return rrset;
  }

  public static ResourceRecordSet createAlias(
      final HostedZone zone,
      final OwnerFullName owner,
      final String name,
      final Type type,
      final String aliasDnsName,
      final String aliasHostedZoneId
  ) {
    final String canonicalName = name.endsWith(".") ? name : (name + ".");
    final String displayName = type.name() + ":" + canonicalName;
    final ResourceRecordSet rrset = new ResourceRecordSet(owner, displayName);
    rrset.setHostedZone(zone);
    rrset.setName(canonicalName);
    rrset.setType(type);
    rrset.setTtl(0);
    rrset.setAliasDnsName(Route53DnsHelper.absoluteName(aliasDnsName));
    rrset.setAliasHostedZoneId(aliasHostedZoneId);
    return rrset;
  }

  public HostedZone getHostedZone() {
    return hostedZone;
  }

  public void setHostedZone(final HostedZone hostedZone) {
    this.hostedZone = hostedZone;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public Integer getTtl() {
    return ttl;
  }

  public void setTtl(final Integer ttl) {
    this.ttl = ttl;
  }

  public String getAliasHostedZoneId() {
    return aliasHostedZoneId;
  }

  public void setAliasHostedZoneId(final String aliasHostedZoneId) {
    this.aliasHostedZoneId = aliasHostedZoneId;
  }

  public String getAliasDnsName() {
    return aliasDnsName;
  }

  public void setAliasDnsName(final String aliasDnsName) {
    this.aliasDnsName = aliasDnsName;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(final List<String> values) {
    this.values = values;
  }
}
