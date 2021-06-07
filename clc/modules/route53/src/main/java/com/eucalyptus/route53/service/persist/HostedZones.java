/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist;

import static com.eucalyptus.auth.euare.identity.region.RegionConfigurations.getRegionNameOrDefault;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.route53.common.Route53Metadata.HostedZoneMetadata;
import com.eucalyptus.route53.common.msgs.HostedZoneConfig;
import com.eucalyptus.route53.common.msgs.ResourceTagSet;
import com.eucalyptus.route53.common.msgs.Tag;
import com.eucalyptus.route53.common.msgs.TagList;
import com.eucalyptus.route53.service.persist.entities.HostedZone;
import com.eucalyptus.route53.service.persist.views.HostedZoneComposite;
import com.eucalyptus.route53.service.persist.views.ImmutableHostedZoneComposite;
import com.eucalyptus.route53.service.persist.views.ImmutableHostedZoneView;
import com.eucalyptus.route53.service.persist.views.ImmutableResourceRecordSetView;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 *
 */
public interface HostedZones {

  Function<HostedZone, HostedZoneComposite> COMPOSITE_FULL =
            hostedZone -> ImmutableHostedZoneComposite.builder()
                .hostedZone(ImmutableHostedZoneView.copyOf(hostedZone))
                .resourceRecordSets(Iterables.transform(
                    hostedZone.getResourceRecordSets(),
                    ImmutableResourceRecordSetView::copyOf))
                .build();

  <T> T lookupByName(final OwnerFullName ownerFullName,
                     final String name,
                     final Predicate<? super HostedZone> filter,
                     final Function<? super HostedZone, T> transform) throws Route53MetadataException;

  <T> List<T> list(OwnerFullName ownerFullName,
                   Predicate<? super HostedZone> filter,
                   Function<? super HostedZone, T> transform) throws Route53MetadataException;

  <T> List<T> listByExample(HostedZone example,
                            Predicate<? super HostedZone> filter,
                            Function<? super HostedZone, T> transform) throws Route53MetadataException;

  <T> T updateByExample(HostedZone example,
                        OwnerFullName ownerFullName,
                        String hostedZoneId,
                        Function<? super HostedZone, T> updateTransform) throws Route53MetadataException;


  HostedZone save(HostedZone hostedZone) throws Route53MetadataException;

  List<HostedZone> deleteByExample(HostedZone example) throws Route53MetadataException;

  AbstractPersistentSupport<HostedZoneMetadata, HostedZone, Route53MetadataException> withRetries();

  @TypeMapper
  enum HostedZoneTransform implements Function<HostedZone, com.eucalyptus.route53.common.msgs.HostedZone> {
    INSTANCE;

    @Override
    public com.eucalyptus.route53.common.msgs.HostedZone apply(final HostedZone hostedZone) {
      final com.eucalyptus.route53.common.msgs.HostedZone resultZone = new com.eucalyptus.route53.common.msgs.HostedZone();
      resultZone.setCallerReference(hostedZone.getCallerReference());
      resultZone.setId("/hostedzone/" + hostedZone.getDisplayName());
      resultZone.setName(hostedZone.getZoneName());
      final HostedZoneConfig config = new HostedZoneConfig();
      config.setComment(hostedZone.getComment());
      config.setPrivateZone(hostedZone.getPrivateZone());
      resultZone.setConfig(config);
      resultZone.setResourceRecordSetCount(Long.valueOf(hostedZone.getResourceRecordSetCount()));
      return resultZone;
    }
  }

  @TypeMapper
  enum HostedZoneVpcTransform implements Function<HostedZone, com.eucalyptus.route53.common.msgs.VPC> {
    INSTANCE;

    @Override
    public com.eucalyptus.route53.common.msgs.VPC apply(final HostedZone hostedZone) {
      if (hostedZone.getVpcIds() == null || hostedZone.getVpcIds().isEmpty()) {
        return null;
      }
      final com.eucalyptus.route53.common.msgs.VPC vpc = new com.eucalyptus.route53.common.msgs.VPC();
      vpc.setVPCId(hostedZone.getVpcIds().get(0));
      vpc.setVPCRegion(getRegionNameOrDefault());
      return vpc;
    }
  }

  @TypeMapper
  enum HostedZoneVpcsTransform implements Function<HostedZone, com.eucalyptus.route53.common.msgs.VPCs> {
    INSTANCE;

    @Override
    public com.eucalyptus.route53.common.msgs.VPCs apply(final HostedZone hostedZone) {
      if (hostedZone.getVpcIds() == null || hostedZone.getVpcIds().isEmpty()) {
        return null;
      }
      final String region = RegionConfigurations.getRegionNameOrDefault();
      final com.eucalyptus.route53.common.msgs.VPCs vpcs = new com.eucalyptus.route53.common.msgs.VPCs();
      for ( final String vpcId : hostedZone.getVpcIds() ) {
        final com.eucalyptus.route53.common.msgs.VPC vpc = new com.eucalyptus.route53.common.msgs.VPC();
        vpc.setVPCId(vpcId);
        vpc.setVPCRegion(region);
        vpcs.getMember().add(vpc);
      }
      return vpcs;
    }
  }

  @TypeMapper
  enum HostedZoneResourceTagSetTransform implements Function<HostedZone, ResourceTagSet> {
    INSTANCE;

    @Override
    public ResourceTagSet apply(final HostedZone hostedZone) {
      final ResourceTagSet tagSet = new ResourceTagSet();
      tagSet.setResourceId(hostedZone.getDisplayName());
      tagSet.setResourceType("hostedzone");
      final TagList tagList = new TagList();
      final Map<String,String> tags = Maps.newTreeMap();
      tags.putAll(hostedZone.getTags());
      for (final Map.Entry<String,String> tagEntry : tags.entrySet()) {
        final Tag tag = new Tag();
        tag.setKey(tagEntry.getKey());
        tag.setValue(tagEntry.getValue());
        tagList.getMember().add(tag);
      }
      tagSet.setTags(tagList);
      return tagSet;
    }
  }

  @RestrictedTypes.QuantityMetricFunction( HostedZoneMetadata.class )
  enum CountHostedZones implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor(HostedZone.class) ){
        return Entities.count( HostedZone.exampleWithOwner( input ) );
      }
    }
  }
}
