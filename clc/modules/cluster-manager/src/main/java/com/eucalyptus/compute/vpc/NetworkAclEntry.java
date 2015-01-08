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
package com.eucalyptus.compute.vpc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_acl_entries" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkAclEntry extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  public enum RuleAction {
    allow,
    deny,
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_network_acl_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private NetworkAcl networkAcl;

  @Column( name = "metadata_rule_number", nullable = false, updatable = false )
  private Integer ruleNumber;

  @Column( name = "metadata_protocol", nullable = false, updatable = false )
  private Integer protocol;

  @Column( name = "metadata_rule_action", nullable = false, updatable = false )
  @Enumerated( EnumType.STRING )
  private RuleAction ruleAction;

  @Column( name = "metadata_egress", nullable = false, updatable = false )
  private Boolean egress;

  @Column( name = "metadata_cidr", nullable = false, updatable = false )
  private String cidr;

  @Column( name = "metadata_icmp_code", updatable = false )
  private Integer icmpCode;

  @Column( name = "metadata_icmp_type", updatable = false )
  private Integer icmpType;

  @Column( name = "metadata_port_from", updatable = false )
  private Integer portRangeFrom;

  @Column( name = "metadata_port_to", updatable = false )
  private Integer portRangeTo;

  protected NetworkAclEntry() {
  }

  protected NetworkAclEntry( final NetworkAcl networkAcl,
                             final Integer ruleNumber,
                             final Integer protocol,
                             final RuleAction ruleAction,
                             final Boolean egress,
                             final String cidr,
                             final Integer icmpCode,
                             final Integer icmpType,
                             final Integer portRangeFrom,
                             final Integer portRangeTo ) {
    this.networkAcl = networkAcl;
    this.ruleNumber = ruleNumber;
    this.protocol = protocol;
    this.ruleAction = ruleAction;
    this.egress = egress;
    this.cidr = cidr;
    this.icmpCode = icmpCode;
    this.icmpType = icmpType;
    this.portRangeFrom = portRangeFrom;
    this.portRangeTo = portRangeTo;
  }

  public static NetworkAclEntry createIcmpEntry( final NetworkAcl networkAcl,
                                                 final Integer ruleNumber,
                                                 final RuleAction ruleAction,
                                                 final Boolean egress,
                                                 final String cidr,
                                                 final Integer icmpCode,
                                                 final Integer icmpType ) {
    return new NetworkAclEntry( networkAcl, ruleNumber, 1, ruleAction, egress, cidr, icmpCode, icmpType, null, null );
  }

  public static NetworkAclEntry createTcpUdpEntry( final NetworkAcl networkAcl,
                                                   final Integer ruleNumber,
                                                   final Integer protocol,
                                                   final RuleAction ruleAction,
                                                   final Boolean egress,
                                                   final String cidr,
                                                   final Integer portRangeFrom,
                                                   final Integer portRangeTo ) {
    return new NetworkAclEntry( networkAcl, ruleNumber, protocol, ruleAction, egress, cidr, null, null, portRangeFrom, portRangeTo );
  }

  public static NetworkAclEntry createEntry( final NetworkAcl networkAcl,
                                             final Integer ruleNumber,
                                             final Integer protocol,
                                             final RuleAction ruleAction,
                                             final Boolean egress,
                                             final String cidr ) {
    return new NetworkAclEntry( networkAcl, ruleNumber, protocol, ruleAction, egress, cidr, null, null, null, null );
  }

  public Integer getRuleNumber() {
    return ruleNumber;
  }

  public void setRuleNumber( final Integer ruleNumber ) {
    this.ruleNumber = ruleNumber;
  }

  public Integer getProtocol() {
    return protocol;
  }

  public void setProtocol( final Integer protocol ) {
    this.protocol = protocol;
  }

  public RuleAction getRuleAction() {
    return ruleAction;
  }

  public void setRuleAction( final RuleAction ruleAction ) {
    this.ruleAction = ruleAction;
  }

  public Boolean getEgress() {
    return egress;
  }

  public void setEgress( final Boolean egress ) {
    this.egress = egress;
  }

  public String getCidr() {
    return cidr;
  }

  public void setCidr( final String cidr ) {
    this.cidr = cidr;
  }

  public Integer getIcmpCode() {
    return icmpCode;
  }

  public void setIcmpCode( final Integer icmpCode ) {
    this.icmpCode = icmpCode;
  }

  public Integer getIcmpType() {
    return icmpType;
  }

  public void setIcmpType( final Integer icmpType ) {
    this.icmpType = icmpType;
  }

  public Integer getPortRangeFrom() {
    return portRangeFrom;
  }

  public void setPortRangeFrom( final Integer portRangeFrom ) {
    this.portRangeFrom = portRangeFrom;
  }

  public Integer getPortRangeTo() {
    return portRangeTo;
  }

  public void setPortRangeTo( final Integer portRangeTo ) {
    this.portRangeTo = portRangeTo;
  }
}
