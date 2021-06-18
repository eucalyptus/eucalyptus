/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.util.CompatFunction;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.common.RdsMetadatas;
import com.eucalyptus.rds.common.msgs.AvailabilityZone;
import com.eucalyptus.rds.common.msgs.Subnet;
import com.eucalyptus.rds.common.msgs.SubnetList;
import com.eucalyptus.rds.service.persist.entities.DBSubnet;
import com.eucalyptus.rds.service.persist.entities.DBSubnetGroup;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;

/**
 *
 */
public interface DBSubnetGroups {

  <T> T lookupByName( final OwnerFullName ownerFullName,
                      final String name,
                      final Predicate<? super DBSubnetGroup> filter,
                      final Function<? super DBSubnetGroup,T> transform ) throws RdsMetadataException;

  <T> List<T> list(OwnerFullName ownerFullName,
                   Predicate<? super DBSubnetGroup> filter,
                   Function<? super DBSubnetGroup,T> transform ) throws RdsMetadataException;

  <T> List<T> listByExample(DBSubnetGroup example,
                            Predicate<? super DBSubnetGroup> filter,
                            Function<? super DBSubnetGroup,T> transform ) throws RdsMetadataException;

  <T> T updateByExample( DBSubnetGroup example,
                         OwnerFullName ownerFullName,
                         String activityId,
                         Function<? super DBSubnetGroup,T> updateTransform ) throws RdsMetadataException;


  DBSubnetGroup save( DBSubnetGroup dbSubnetGroup ) throws RdsMetadataException;

  List<DBSubnetGroup> deleteByExample( DBSubnetGroup example ) throws RdsMetadataException;

  AbstractPersistentSupport<RdsMetadata.DBSubnetGroupMetadata,DBSubnetGroup,RdsMetadataException> withRetries( );

  @TypeMapper
  enum DBSubnetTransform implements CompatFunction<DBSubnet, com.eucalyptus.rds.common.msgs.Subnet> {
    INSTANCE;

    @Override
    public Subnet apply(final DBSubnet subnet) {
      final Subnet result = new Subnet();
      final AvailabilityZone zone = new AvailabilityZone();
      zone.setName( subnet.getAvailabilityZone() );
      result.setSubnetAvailabilityZone( zone );
      result.setSubnetIdentifier( subnet.getSubnetId() );
      result.setSubnetStatus( subnet.getStatus().name() );
      return result;
    }
  }

  @TypeMapper
  enum DBSubnetGroupTransform implements CompatFunction<DBSubnetGroup, com.eucalyptus.rds.common.msgs.DBSubnetGroup> {
    INSTANCE;

    @Override
    public com.eucalyptus.rds.common.msgs.DBSubnetGroup apply(final DBSubnetGroup group) {
      final com.eucalyptus.rds.common.msgs.DBSubnetGroup result = new com.eucalyptus.rds.common.msgs.DBSubnetGroup();
      result.setDBSubnetGroupName( group.getDisplayName() );
      result.setDBSubnetGroupDescription( group.getDescription() );
      result.setSubnetGroupStatus( group.getState().name() );
      result.setVpcId( group.getVpcId() );
      result.setDBSubnetGroupArn(RdsMetadatas.toArn(group));
      final SubnetList subnetList = new SubnetList();
      subnetList.getMember().addAll(
          group.getSubnets().stream()
              .map(TypeMappers.lookupF(DBSubnet.class, Subnet.class))
              .collect(Collectors.toList()) );
      result.setSubnets(subnetList);
      return result;
    }
  }
}
