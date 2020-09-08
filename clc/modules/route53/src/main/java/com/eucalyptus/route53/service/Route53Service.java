/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import org.xbill.DNS.Name;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.route53.common.Route53Metadatas;
import com.eucalyptus.route53.common.msgs.*;
import com.eucalyptus.route53.service.dns.Route53DnsHelper;
import com.eucalyptus.route53.service.persist.HostedZones;
import com.eucalyptus.route53.service.persist.Route53MetadataNotFoundException;
import com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.Type;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.net.InternetDomainName;
import com.google.common.primitives.Ints;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
@SuppressWarnings({"Guava", "unused", "UnstableApiUsage", "Convert2Lambda", "StaticPseudoFunctionalStyleMethod"})
@ComponentNamed
public class Route53Service {
  private static final Logger logger = Logger.getLogger( Route53Service.class );

  private static final Set<String> reservedTagPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  private final HostedZones hostedZones;

  @Inject
  public Route53Service(
      final HostedZones hostedZones
  ) {
    this.hostedZones = hostedZones;
  }

  public AssociateVPCWithHostedZoneResponseType associateVPCWithHostedZone(final AssociateVPCWithHostedZoneType request) throws Route53Exception {
    final AssociateVPCWithHostedZoneResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String id = request.getHostedZoneId();
      final String comment = request.getComment();
      final String vpcId = request.getVPC() != null ? request.getVPC().getVPCId() : null;

      if (vpcId == null) {
        throw new Route53ClientException("InvalidVPCId", "VPC identifier required" );
      }

      if (!vpcId.matches("vpc-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?")) {
        throw new Route53ClientException("InvalidVPCId", "Invalid vpc identifier: " + vpcId );
      }

      hostedZones.withRetries().updateByExample(
          com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithName(ownerFullName, id),
          ownerFullName, id,
          zone -> {
            if (zone != null && Route53Metadatas.filterPrivileged().apply(zone)) {
              if (!zone.getPrivateZone()) {
                throw Exceptions.toUndeclared(
                    new Route53ClientException("PublicZoneVPCAssociation", "Not a private zone"));
              }
              if (!zone.getVpcIds().contains(vpcId)) {
                zone.getVpcIds().add(vpcId);
                if (zone.getVpcIds().size() > Route53ServiceProperties.getHostedZoneVpcLimit()) {
                  throw Exceptions.toUndeclared(
                      new Route53ClientException("LimitsExceeded", "Too many vpcs"));
                }
              }
            } else {
              throw Exceptions.toUndeclared(new Route53AuthorizationException("AccessDeniedException", "Hosted zone access denied"));
            }
            return zone;
          });

      final ChangeInfo changeInfo = new ChangeInfo();
      changeInfo.setComment(comment);
      changeInfo.setId(Identifiers.generateIdentifier( "C" ).substring(0,14));
      changeInfo.setStatus("INSYNC");
      changeInfo.setSubmittedAt(new Date());
      reply.setChangeInfo(changeInfo);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ChangeResourceRecordSetsResponseType changeResourceRecordSets(
      final ChangeResourceRecordSetsType request
  ) throws Route53Exception {
    final ChangeResourceRecordSetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getHostedZoneId();
      final ChangeBatch changeBatch = request.getChangeBatch();
      final String comment = changeBatch == null ? null : changeBatch.getComment();

      if ( changeBatch != null ) {
        hostedZones.withRetries().updateByExample(
            com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithName(ownerFullName, id),
            ownerFullName, id,
            zone -> {
              if (zone != null && Route53Metadatas.filterPrivileged().apply(zone)) {
                final int originalRRSetCount = zone.getResourceRecordSetCount();
                changes:
                for( final Change change : changeBatch.getChanges().getMember() ) {
                  if (change.getResourceRecordSet()==null) continue;
                  final String rrSetName = Route53DnsHelper.absoluteName(change.getResourceRecordSet().getName());
                  if (!rrSetName.equals(zone.getZoneName()) && !rrSetName.endsWith("." + zone.getZoneName())) {
                    throw Exceptions.toUndeclared(new Route53ClientException("InvalidChangeBatch", "Invalid name for zone: " + rrSetName));
                  }

                  if ("DELETE".equals(change.getAction())) {
                    for (final com.eucalyptus.route53.service.persist.entities.ResourceRecordSet rrset : zone.getResourceRecordSets()) {
                      if (rrset.getName().equals(rrSetName) &&
                          rrset.getType().name().equals(change.getResourceRecordSet().getType())) {
                        zone.getResourceRecordSets().remove(rrset);
                        continue changes;
                      }
                    }
                  } else {
                    final Integer newTtl = change.getResourceRecordSet().getTTL() != null ?
                        change.getResourceRecordSet().getTTL().intValue() :
                        null;
                    final List<String> newValues = change.getResourceRecordSet().getResourceRecords() != null ?
                        Lists.transform(
                            change.getResourceRecordSet().getResourceRecords().getMember(),
                            ResourceRecord::getValue) :
                        null;
                    for (final com.eucalyptus.route53.service.persist.entities.ResourceRecordSet rrset : zone.getResourceRecordSets()) {
                      if (rrset.getName().equals(rrSetName) &&
                          rrset.getType().name().equals(change.getResourceRecordSet().getType())) {
                        if ("CREATE".equals(change.getAction())) {
                          throw Exceptions.toUndeclared(new Route53ClientException("InvalidChangeBatch", "Name exists: " + rrSetName));
                        }
                        if (newValues != null && !newValues.isEmpty()) {
                          rrset.setValues(newValues);
                          rrset.setAliasDnsName(null);
                          rrset.setAliasHostedZoneId(null);
                        } else if (change.getResourceRecordSet().getAliasTarget() != null) {
                          rrset.setTtl(0);
                          rrset.setValues(Lists.newArrayList());
                          rrset.setAliasDnsName(change.getResourceRecordSet().getAliasTarget().getDNSName());
                          rrset.setAliasHostedZoneId(change.getResourceRecordSet().getAliasTarget().getHostedZoneId());
                        }
                        if (!rrset.getValues().isEmpty()) {
                          if (newTtl != null) {
                            rrset.setTtl(newTtl);
                          }
                        }
                        continue changes;
                      }
                    }
                    if (newValues != null && !newValues.isEmpty()) {
                      zone.getResourceRecordSets().add(com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.createSimple(
                          zone,
                          ctx.getUserFullName(),
                          rrSetName,
                          Type.valueOf(change.getResourceRecordSet().getType()),
                          MoreObjects.firstNonNull(newTtl, 300),
                          newValues
                      ));
                    } else if (change.getResourceRecordSet().getAliasTarget() != null) {
                      zone.getResourceRecordSets().add(com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.createAlias(
                          zone,
                          ctx.getUserFullName(),
                          rrSetName,
                          Type.valueOf(change.getResourceRecordSet().getType()),
                          change.getResourceRecordSet().getAliasTarget().getDNSName(),
                          change.getResourceRecordSet().getAliasTarget().getHostedZoneId()
                      ));
                    } else {
                      throw Exceptions.toUndeclared(new Route53ClientException("InvalidChangeBatch", "Name requires values: " + rrSetName));
                    }
                  }
                }
                zone.setResourceRecordSetCount(zone.getResourceRecordSets().size());
                if (zone.getResourceRecordSetCount() > originalRRSetCount &&
                    zone.getResourceRecordSetCount() > Route53ServiceProperties.getHostedZoneRRSetLimit()) {
                  throw Exceptions.toUndeclared(new Route53ClientException("InvalidChangeBatch", "Too many resource record sets"));
                }
              } else {
                throw Exceptions.toUndeclared(new Route53AuthorizationException("AccessDeniedException", "Hosted zone access denied"));
              }
              return zone;
            });
      }
      final ChangeInfo changeInfo = new ChangeInfo();
      changeInfo.setComment(comment);
      changeInfo.setId(Identifiers.generateIdentifier( "C" ).substring(0,14));
      changeInfo.setStatus("INSYNC");
      changeInfo.setSubmittedAt(new Date());
      reply.setChangeInfo(changeInfo);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ChangeTagsForResourceResponseType changeTagsForResource(final ChangeTagsForResourceType request) throws Route53Exception {
    final ChangeTagsForResourceResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String id = request.getResourceId();
      final String type = request.getResourceType();
      final boolean privileged = ctx.isPrivileged();
      if ("hostedzone".equals(type)) {
        hostedZones.withRetries().updateByExample(
            com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithName(ownerFullName, id),
            ownerFullName, id,
            zone -> {
              if (zone == null) {
                throw Exceptions.toUndeclared(new Route53MetadataNotFoundException(id));
              }
              if (Route53Metadatas.filterPrivileged().apply(zone)) {
                final TagKeyList remove = request.getRemoveTagKeys();
                if (remove!=null) {
                  for ( final String key : remove.getMember( ) ) {
                    zone.getTags().remove(key);
                  }
                }
                final TagList tags = request.getAddTags();
                if (tags!=null) {
                  for ( final Tag tag : tags.getMember( ) ) {
                    final String key = tag.getKey();
                    final String value = tag.getValue();
                    if (key == null || value == null || key.length() >  128 || value.length() > 256) {
                      throw Exceptions.toUndeclared(new Route53ClientException("InvalidInput",
                          "Invalid tag key or value"));
                    }
                    if (!privileged && Iterables.any(reservedTagPrefixes, Strings.isPrefixOf(key))) {
                      throw Exceptions.toUndeclared(new Route53ClientException("InvalidInput",
                          "Invalid tag key, reserved prefixes "+reservedTagPrefixes));
                    }
                    zone.getTags().put( key, value );
                  }
                  if (zone.getTags().size() > Route53ServiceProperties.getMaxTags()) {
                    throw Exceptions.toUndeclared(new Route53ClientException("InvalidInput", "Tag limit exceeded"));
                  }
                }
              } else {
                throw Exceptions.toUndeclared(new Route53AuthorizationException("AccessDeniedException", "Hosted zone access denied"));
              }
              return zone;
            });
      }
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public CreateHealthCheckResponseType createHealthCheck(final CreateHealthCheckType request) {
    return request.getReply();
  }

  public CreateHostedZoneResponseType createHostedZone(
      final CreateHostedZoneType request
  ) throws Route53Exception {
    final CreateHostedZoneResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String callerReference = request.getCallerReference();
      final String zoneName = request.getName();
      final HostedZoneConfig config = request.getHostedZoneConfig();
      final boolean privateZone = (config != null && config.getPrivateZone() != null) ?
          config.getPrivateZone() :
          (request.getVPC() != null && request.getVPC().getVPCId() != null);
      final String comment = config != null ? config.getComment() : null;
      final String vpcId = request.getVPC() != null ? request.getVPC().getVPCId() : null;

      if ( zoneName == null || !InternetDomainName.isValid(zoneName) ) {
        throw new Route53ClientException("InvalidDomainName", "The specified domain name is not valid");
      }

      if ( !ctx.isAdministrator() && Route53DnsHelper.isSystemDomain(zoneName) ) {
        throw new Route53ClientException("InvalidDomainName", "Not authorized to use system domain");
      }

      if ( vpcId != null && !privateZone ) {
        throw new Route53ClientException("InvalidInput", "VPC given for public zone");
      }

      if ( vpcId != null && !vpcId.matches("vpc-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?")) {
        throw new Route53ClientException("InvalidVPCId", "Invalid vpc identifier: " + vpcId );
      }

      final Supplier<com.eucalyptus.route53.service.persist.entities.HostedZone> allocator =
          new Supplier<com.eucalyptus.route53.service.persist.entities.HostedZone>( ) {
        @Override
        public com.eucalyptus.route53.service.persist.entities.HostedZone get( ) {
          try {
            final String hostedZoneId = Identifiers.generateIdentifier( "Z" ).substring(0,14);
            final com.eucalyptus.route53.service.persist.entities.HostedZone hostedZone =
                com.eucalyptus.route53.service.persist.entities.HostedZone.create(
                    ownerFullName,
                    hostedZoneId,
                    callerReference,
                    zoneName,
                    privateZone
                );
            hostedZone.setComment(comment);
            hostedZone.setVpcIds(vpcId == null ? null : Lists.newArrayList(vpcId));
            if (!privateZone && !Route53DnsHelper.isSystemDomain(zoneName)) {
              // add default SOA and NS resource record sets.
              final List<String> nameservers = buildDefaultNameservers();
              final String soaValue = buildSOADefaultValue(nameservers);
              if (soaValue != null && !nameservers.isEmpty()) {
                final com.eucalyptus.route53.service.persist.entities.ResourceRecordSet soaRRset =
                    com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.createSimple(
                        hostedZone,
                        ownerFullName,
                        zoneName,
                        Type.SOA,
                        Route53ServiceProperties.getHostedZoneSoaTtlDefault(),
                        Lists.newArrayList(soaValue)
                    );
                final com.eucalyptus.route53.service.persist.entities.ResourceRecordSet nsRRset =
                    com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.createSimple(
                        hostedZone,
                        ownerFullName,
                        zoneName,
                        Type.NS,
                        Route53ServiceProperties.getHostedZoneNsTtlDefault(),
                        nameservers
                    );
                hostedZone.setResourceRecordSets(Lists.newArrayList( soaRRset, nsRRset ));
                hostedZone.setResourceRecordSetCount(hostedZone.getResourceRecordSets().size());
              }
            }
            return hostedZones.save(hostedZone);
          } catch ( Exception ex ) {
            throw new RuntimeException( ex );
          }
        }
      };

      final com.eucalyptus.route53.service.persist.entities.HostedZone hostedZone =
          Route53Metadatas.allocateUnitlessResource( allocator );
      reply.setLocation("/2013-04-01/hostedzone/" + hostedZone.getDisplayName());
      reply.setHostedZone(
          TypeMappers.transform(hostedZone, HostedZone.class));
      reply.setVPC(
          TypeMappers.transform(hostedZone, VPC.class));
    } catch ( final Exception e ) {
      handleException( e, __ -> new Route53ConflictClientException(
          "HostedZoneAlreadyExists", "Hosted zone caller reference or name exists") );
    }
    return reply;
  }

  public CreateQueryLoggingConfigResponseType createQueryLoggingConfig(final CreateQueryLoggingConfigType request) {
    return request.getReply();
  }

  public CreateReusableDelegationSetResponseType createReusableDelegationSet(final CreateReusableDelegationSetType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyResponseType createTrafficPolicy(final CreateTrafficPolicyType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyInstanceResponseType createTrafficPolicyInstance(final CreateTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyVersionResponseType createTrafficPolicyVersion(final CreateTrafficPolicyVersionType request) {
    return request.getReply();
  }

  public CreateVPCAssociationAuthorizationResponseType createVPCAssociationAuthorization(final CreateVPCAssociationAuthorizationType request) {
    return request.getReply();
  }

  public DeleteHealthCheckResponseType deleteHealthCheck(final DeleteHealthCheckType request) {
    return request.getReply();
  }

  public DeleteHostedZoneResponseType deleteHostedZone(final DeleteHostedZoneType request) throws Route53Exception {
    final DeleteHostedZoneResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.isAdministrator( ) ? null : ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getId();

      final com.eucalyptus.route53.service.persist.entities.HostedZone hostedZone =
          hostedZones.lookupByName( ownerFullName, id, Route53Metadatas.filterPrivileged(), zone -> {
            if (zone.getResourceRecordSetCount() > 0) {
              boolean defaultsOnly = zone.getResourceRecordSetCount()==2;
              if (defaultsOnly) {
                for (final com.eucalyptus.route53.service.persist.entities.ResourceRecordSet rrSet : zone.getResourceRecordSets()) {
                  if (!EnumSet.of(Type.NS, Type.SOA).contains(rrSet.getType()) ||
                      !zone.getZoneName().equals(rrSet.getName())) {
                    defaultsOnly = false;
                    break;
                  }
                }
              }
              if (!defaultsOnly && ctx.isAdministrator() && !ctx.getAccountNumber().equals(zone.getOwnerAccountNumber())) {
                defaultsOnly = true; // allow administrator to delete from other accounts with non-default values
              }
              if (!defaultsOnly) {
                throw Exceptions.toUndeclared(new Route53ClientException("HostedZoneNotEmpty", "Hosted zone not empty"));
              }
            }
            return zone;
          });

      hostedZones.deleteByExample( hostedZone );
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e, __ -> new Route53ClientException("HostedZoneNotEmpty", "Hosted zone conflict") );
    }
    return reply;
  }

  public DeleteQueryLoggingConfigResponseType deleteQueryLoggingConfig(final DeleteQueryLoggingConfigType request) {
    return request.getReply();
  }

  public DeleteReusableDelegationSetResponseType deleteReusableDelegationSet(final DeleteReusableDelegationSetType request) {
    return request.getReply();
  }

  public DeleteTrafficPolicyResponseType deleteTrafficPolicy(final DeleteTrafficPolicyType request) {
    return request.getReply();
  }

  public DeleteTrafficPolicyInstanceResponseType deleteTrafficPolicyInstance(final DeleteTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public DeleteVPCAssociationAuthorizationResponseType deleteVPCAssociationAuthorization(final DeleteVPCAssociationAuthorizationType request) {
    return request.getReply();
  }

  public DisassociateVPCFromHostedZoneResponseType disassociateVPCFromHostedZone(final DisassociateVPCFromHostedZoneType request) throws Route53Exception {
    final DisassociateVPCFromHostedZoneResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String id = request.getHostedZoneId();
      final String comment = request.getComment();
      final String vpcId = request.getVPC() != null ? request.getVPC().getVPCId() : null;

      if (vpcId == null) {
        throw new Route53ClientException("InvalidVPCId", "VPC identifier required" );
      }

      if (!vpcId.matches("vpc-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?")) {
        throw new Route53ClientException("InvalidVPCId", "Invalid vpc identifier: " + vpcId );
      }

      hostedZones.withRetries().updateByExample(
          com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithName(ownerFullName, id),
          ownerFullName, id,
          zone -> {
            if (zone != null && Route53Metadatas.filterPrivileged().apply(zone)) {
              if (zone.getVpcIds().contains(vpcId)) {
                zone.getVpcIds().remove(vpcId);
              } else {
                throw Exceptions.toUndeclared(
                    new Route53NotFoundClientException("VPCAssociationNotFound", "VPC not associated"));
              }
              if (zone.getVpcIds().isEmpty()) {
                throw Exceptions.toUndeclared(
                    new Route53ClientException("LastVPCAssociation", "At least one vpc association required"));
              }
            } else {
              throw Exceptions.toUndeclared(new Route53AuthorizationException("AccessDeniedException", "Hosted zone access denied"));
            }
            return zone;
          });

      final ChangeInfo changeInfo = new ChangeInfo();
      changeInfo.setComment(comment);
      changeInfo.setId(Identifiers.generateIdentifier( "C" ).substring(0,14));
      changeInfo.setStatus("INSYNC");
      changeInfo.setSubmittedAt(new Date());
      reply.setChangeInfo(changeInfo);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public GetAccountLimitResponseType getAccountLimit(final GetAccountLimitType request) {
    return request.getReply();
  }

  public GetChangeResponseType getChange(final GetChangeType request) {
    final GetChangeResponseType reply = request.getReply();
    final ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.setComment("");
    changeInfo.setId(request.getId());
    changeInfo.setStatus("INSYNC");
    changeInfo.setSubmittedAt(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1)));
    reply.setChangeInfo(changeInfo);
    return reply;
  }

  public GetCheckerIpRangesResponseType getCheckerIpRanges(final GetCheckerIpRangesType request) {
    return request.getReply();
  }

  public GetGeoLocationResponseType getGeoLocation(final GetGeoLocationType request) {
    return request.getReply();
  }

  public GetHealthCheckResponseType getHealthCheck(final GetHealthCheckType request) {
    return request.getReply();
  }

  public GetHealthCheckCountResponseType getHealthCheckCount(final GetHealthCheckCountType request) {
    return request.getReply();
  }

  public GetHealthCheckLastFailureReasonResponseType getHealthCheckLastFailureReason(final GetHealthCheckLastFailureReasonType request) {
    return request.getReply();
  }

  public GetHealthCheckStatusResponseType getHealthCheckStatus(final GetHealthCheckStatusType request) {
    return request.getReply();
  }

  public GetHostedZoneResponseType getHostedZone(final GetHostedZoneType request) throws Route53Exception {
    final GetHostedZoneResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getId();

      final com.eucalyptus.route53.service.persist.entities.HostedZone hostedZone =
          hostedZones.lookupByName( ownerFullName, id, Route53Metadatas.filterPrivileged(), zone -> {
            if (zone != null) {
              Entities.initialize(zone.getVpcIds());
            }
            return zone;
          });

      reply.setHostedZone(TypeMappers.transform(hostedZone, HostedZone.class));
      reply.setVPCs(TypeMappers.transform(hostedZone, VPCs.class));
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public GetHostedZoneCountResponseType getHostedZoneCount(final GetHostedZoneCountType request) throws Route53Exception {
    final GetHostedZoneCountResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final long hostedZoneCount =
          hostedZones.list(ownerFullName, Predicates.alwaysTrue(), Functions.identity()).size();
      reply.setHostedZoneCount(hostedZoneCount);
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public GetHostedZoneLimitResponseType getHostedZoneLimit(final GetHostedZoneLimitType request) throws Route53Exception {
    final GetHostedZoneLimitResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getHostedZoneId();
      final String type = request.getType();
      final Tuple2<Integer,Integer> countAndLimit =
          hostedZones.lookupByName( ownerFullName, id, Route53Metadatas.filterPrivileged(), zone -> {
            Tuple2<Integer,Integer> result = Tuple.of(0, 0);
            if (zone != null) {
              switch (type) {
                case "MAX_RRSETS_BY_ZONE":
                  result = Tuple.of(
                      zone.getResourceRecordSetCount(),
                      Route53ServiceProperties.getHostedZoneRRSetLimit());
                  break;
                case "MAX_VPCS_ASSOCIATED_BY_ZONE":
                  result = Tuple.of(
                      zone.getVpcIds().size(),
                      Route53ServiceProperties.getHostedZoneVpcLimit());
                  break;
              }
            }
            return result;
          });

      reply.setCount(countAndLimit._1().longValue());
      final HostedZoneLimit limit = new HostedZoneLimit();
      limit.setType(type);
      limit.setValue(countAndLimit._2().longValue());
      reply.setLimit(limit);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public GetQueryLoggingConfigResponseType getQueryLoggingConfig(final GetQueryLoggingConfigType request) {
    return request.getReply();
  }

  public GetReusableDelegationSetResponseType getReusableDelegationSet(final GetReusableDelegationSetType request) {
    return request.getReply();
  }

  public GetReusableDelegationSetLimitResponseType getReusableDelegationSetLimit(final GetReusableDelegationSetLimitType request) {
    return request.getReply();
  }

  public GetTrafficPolicyResponseType getTrafficPolicy(final GetTrafficPolicyType request) {
    return request.getReply();
  }

  public GetTrafficPolicyInstanceResponseType getTrafficPolicyInstance(final GetTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public GetTrafficPolicyInstanceCountResponseType getTrafficPolicyInstanceCount(final GetTrafficPolicyInstanceCountType request) {
    return request.getReply();
  }

  public ListGeoLocationsResponseType listGeoLocations(final ListGeoLocationsType request) {
    return request.getReply();
  }

  public ListHealthChecksResponseType listHealthChecks(final ListHealthChecksType request) {
    return request.getReply();
  }

  public ListHostedZonesResponseType listHostedZones(final ListHostedZonesType request) throws Route53Exception {
    final ListHostedZonesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final boolean showAll = "verbose".equals( request.getDelegationSetId() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final com.eucalyptus.route53.common.msgs.HostedZones resultHostedZones =
          new com.eucalyptus.route53.common.msgs.HostedZones();
      resultHostedZones.getMember().addAll( hostedZones.list(
          ownerFullName,
          Route53Metadatas.filterPrivileged( ),
          TypeMappers.lookup(
              com.eucalyptus.route53.service.persist.entities.HostedZone.class,
              HostedZone.class ) ) );
      reply.setHostedZones(resultHostedZones);
    } catch ( Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ListHostedZonesByNameResponseType listHostedZonesByName(final ListHostedZonesByNameType request) throws Route53Exception {
    final ListHostedZonesByNameResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final boolean showAll = "verbose".equals( request.getHostedZoneId() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final String canonicalRequestName = Route53DnsHelper.absoluteName(request.getDNSName());
      final String hostedZoneMatch = request.getHostedZoneId() == null ?
          null :
          "/hostedzone/" + request.getHostedZoneId();
      final Predicate<HostedZone> firstZonePredicate = zone -> {
        boolean matches = zone != null;
        if ( matches && canonicalRequestName!=null && !canonicalRequestName.equals(zone.getName()) ) {
          matches = false;
        }
        if ( matches && hostedZoneMatch != null && !hostedZoneMatch.equals( zone.getId() ) ) {
          matches = false;
        }
        return matches;
      };
      final com.eucalyptus.route53.service.persist.entities.HostedZone example =
          com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithOwner(ownerFullName);
      example.setZoneName(Route53DnsHelper.absoluteName(request.getDNSName()));
      List<HostedZone> hostedZoneList = Lists.newArrayList( hostedZones.list(
          ownerFullName,
          Route53Metadatas.filterPrivileged( ),
          TypeMappers.lookup(
              com.eucalyptus.route53.service.persist.entities.HostedZone.class,
              HostedZone.class ) ) );
      final Function<HostedZone,String> zoneName = HostedZone::getName;
      final Function<HostedZone,String> zoneId = HostedZone::getId;
      hostedZoneList.sort(Ordering.natural().onResultOf(zoneName).compound(Ordering.natural().onResultOf(zoneId)));
      final Iterator<HostedZone> zoneIterator = hostedZoneList.iterator();
      while (zoneIterator.hasNext()) {
        final HostedZone zone = zoneIterator.next();
        if (!firstZonePredicate.apply(zone)) {
          zoneIterator.remove();
        } else {
          break;
        }
      }
      reply.setDNSName(request.getDNSName());
      reply.setHostedZoneId(request.getHostedZoneId());
      reply.setMaxItems(request.getMaxItems());
      if (request.getMaxItems() != null) {
        final Integer maxItems = Ints.tryParse(request.getMaxItems());
        if (maxItems != null && maxItems < hostedZoneList.size()) {
          final HostedZone next = hostedZoneList.get(maxItems);
          hostedZoneList = hostedZoneList.subList(0, maxItems);
          reply.setNextDNSName(next.getName());
          reply.setNextHostedZoneId(Strings.substringAfter("/hostedzone/",next.getId()));
        }
      }
      final com.eucalyptus.route53.common.msgs.HostedZones resultHostedZones =
          new com.eucalyptus.route53.common.msgs.HostedZones();
      resultHostedZones.getMember().addAll(hostedZoneList);
      reply.setHostedZones(resultHostedZones);
    } catch ( Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ListQueryLoggingConfigsResponseType listQueryLoggingConfigs(final ListQueryLoggingConfigsType request) {
    return request.getReply();
  }

  public ListResourceRecordSetsResponseType listResourceRecordSets(final ListResourceRecordSetsType request) throws Route53Exception {
    final ListResourceRecordSetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getHostedZoneId();

      final List<com.eucalyptus.route53.service.persist.entities.ResourceRecordSet> rrSets =
          hostedZones.lookupByName( ownerFullName, id,
              Route53Metadatas.filterPrivileged(),
              CompatFunction.of(FUtils.chain(
                  com.eucalyptus.route53.service.persist.entities.HostedZone::getResourceRecordSets,
                  Lists::newArrayList)));
      final Function<com.eucalyptus.route53.service.persist.entities.ResourceRecordSet,String> rrSetName =
          com.eucalyptus.route53.service.persist.entities.ResourceRecordSet::getName;
      final Function<com.eucalyptus.route53.service.persist.entities.ResourceRecordSet,Type> rrSetType =
          com.eucalyptus.route53.service.persist.entities.ResourceRecordSet::getType;
      rrSets.sort(Ordering.natural().onResultOf(rrSetName).compound(Ordering.natural().onResultOf(rrSetType)));
      final ResourceRecordSets resultRrSets = new ResourceRecordSets();
      resultRrSets.getMember().addAll(
          Lists.transform(
              rrSets,
              TypeMappers.lookup(
                  com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.class,
                  ResourceRecordSet.class)));
      reply.setResourceRecordSets(resultRrSets);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ListReusableDelegationSetsResponseType listReusableDelegationSets(final ListReusableDelegationSetsType request) {
    return request.getReply();
  }

  public ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request) throws Route53Exception {
    final ListTagsForResourceResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String id = request.getResourceId();
      final String type = request.getResourceType();
      if ("hostedzone".equals(type)) {
        final ResourceTagSet resourceTagSet =
            hostedZones.lookupByName(ownerFullName, id, Route53Metadatas.filterPrivileged(),
                TypeMappers.lookup(
                    com.eucalyptus.route53.service.persist.entities.HostedZone.class,
                    ResourceTagSet.class));
        reply.setResourceTagSet(resourceTagSet);
      }
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ListTagsForResourcesResponseType listTagsForResources(final ListTagsForResourcesType request) throws Route53Exception {
    final ListTagsForResourcesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final List<String> ids = request.getResourceIds().getMember();
      final String type = request.getResourceType();
      final ResourceTagSetList tagSetList = new ResourceTagSetList();
      if ("hostedzone".equals(type)) {
        final List<ResourceTagSet> resourceTagSets = hostedZones.list(
            ownerFullName,
            Predicates.and(
                Route53Metadatas.filterById(ids),
                Route53Metadatas.filterPrivileged() ),
            TypeMappers.lookup(
                    com.eucalyptus.route53.service.persist.entities.HostedZone.class,
                    ResourceTagSet.class));
        tagSetList.getMember().addAll(resourceTagSets);
        reply.setResourceTagSets(tagSetList);
      }
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public ListTrafficPoliciesResponseType listTrafficPolicies(final ListTrafficPoliciesType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesResponseType listTrafficPolicyInstances(final ListTrafficPolicyInstancesType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesByHostedZoneResponseType listTrafficPolicyInstancesByHostedZone(final ListTrafficPolicyInstancesByHostedZoneType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesByPolicyResponseType listTrafficPolicyInstancesByPolicy(final ListTrafficPolicyInstancesByPolicyType request) {
    return request.getReply();
  }

  public ListTrafficPolicyVersionsResponseType listTrafficPolicyVersions(final ListTrafficPolicyVersionsType request) {
    return request.getReply();
  }

  public ListVPCAssociationAuthorizationsResponseType listVPCAssociationAuthorizations(final ListVPCAssociationAuthorizationsType request) {
    return request.getReply();
  }

  public TestDNSAnswerResponseType testDNSAnswer(final TestDNSAnswerType request) {
    return request.getReply();
  }

  public UpdateHealthCheckResponseType updateHealthCheck(final UpdateHealthCheckType request) {
    return request.getReply();
  }

  public UpdateHostedZoneCommentResponseType updateHostedZoneComment(final UpdateHostedZoneCommentType request) throws Route53Exception {
    final UpdateHostedZoneCommentResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String id = request.getId();
      final String comment = request.getComment();
      final HostedZone hostedZone = hostedZones.withRetries().updateByExample(
          com.eucalyptus.route53.service.persist.entities.HostedZone.exampleWithName(ownerFullName, id),
          ownerFullName, id,
          zone -> {
            if (zone == null) {
              throw Exceptions.toUndeclared(new Route53MetadataNotFoundException(id));
            }
            if (Route53Metadatas.filterPrivileged().apply(zone)) {
              zone.setComment(comment);
            } else {
              throw Exceptions.toUndeclared(new Route53AuthorizationException("AccessDeniedException", "Hosted zone access denied"));
            }
            return TypeMappers.transform(zone, HostedZone.class);
          });
      reply.setHostedZone(hostedZone);
    } catch ( final Route53MetadataNotFoundException e ) {
      throw new Route53NotFoundClientException( "NoSuchHostedZone", "Hosted zone not found" );
    } catch ( final Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public UpdateTrafficPolicyCommentResponseType updateTrafficPolicyComment(final UpdateTrafficPolicyCommentType request) {
    return request.getReply();
  }

  public UpdateTrafficPolicyInstanceResponseType updateTrafficPolicyInstance(final UpdateTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  @Nullable
  private static String buildSOADefaultValue(final List<String> nameservers) {
    String soaValue = null;
    if (!nameservers.isEmpty()) try {
      final Name externalSubdomain = DomainNames.externalSubdomain();
      final String ns = nameservers.get(0);
      final String email = Route53ServiceProperties.getHostedZoneSoaEmailDefault();
      final String dnsEmail = email.contains("@") ?
          Route53DnsHelper.absoluteName(email.replace('@', '.')) :
          DomainNames.absolute(Name.fromString(email.toLowerCase()), externalSubdomain ).toString();
      final String value = Route53ServiceProperties.getHostedZoneSoaValueDefault();
      final StringBuilder builder = new StringBuilder();
      builder.append(ns);
      builder.append(" ");
      builder.append(dnsEmail);
      builder.append(" ");
      builder.append(value);
      soaValue = builder.toString();
    } catch (Exception e) {
      // skip default soa
    }
    return soaValue;
  }

  private static List<String> buildDefaultNameservers() {
    final List<String> nameservers = Lists.newArrayList();
    nameservers.addAll(Route53ServiceProperties.getHostedZoneNsNamesDefault().stream()
        .map(Route53DnsHelper::absoluteName)
        .collect(Collectors.toList()));
    if (nameservers.isEmpty()) {
      nameservers.addAll(Route53DnsHelper.getSystemNameservers().stream()
          .map(Tuple2::_1)
          .collect(Collectors.toList()));
    }
    return nameservers;
  }

  private static void handleException( final Exception e ) throws Route53Exception {
    handleException( e,  null );
  }

  private static void handleException(
      final Exception e,
      final NonNullFunction<ConstraintViolationException, Route53Exception> constraintExceptionBuilder
  ) throws Route53Exception {
    final Route53Exception cause = Exceptions.findCause( e, Route53Exception.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new Route53ClientException( "TooManyHostedZones", "Request would exceed limit" );
    }

    final ConstraintViolationException constraintViolationException =
        Exceptions.findCause( e, ConstraintViolationException.class );
    if ( constraintViolationException != null && constraintExceptionBuilder != null ) {
      throw constraintExceptionBuilder.apply( constraintViolationException );
    }

    logger.error( e, e );

    final Route53ServiceException exception =
        new Route53ServiceException( "InternalFailure", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
