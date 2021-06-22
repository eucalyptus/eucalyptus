/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.util.CompatFunction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.hibernate.criterion.Criterion;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.common.RdsMetadata.DBInstanceMetadata;
import com.eucalyptus.rds.common.RdsMetadatas;
import com.eucalyptus.rds.common.msgs.Endpoint;
import com.eucalyptus.rds.common.msgs.VpcSecurityGroupMembership;
import com.eucalyptus.rds.common.msgs.VpcSecurityGroupMembershipList;
import com.eucalyptus.rds.service.dns.RdsDnsHelper;
import com.eucalyptus.rds.service.persist.entities.DBInstance;
import com.eucalyptus.rds.service.persist.entities.DBInstanceRuntime;
import com.eucalyptus.rds.service.persist.views.DBInstanceComposite;
import com.eucalyptus.rds.service.persist.views.DBInstanceRuntimeComposite;
import com.eucalyptus.rds.service.persist.views.DBInstanceView;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceComposite;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceRuntimeComposite;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceView;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.dns.DomainNames;


/**
 *
 */
public interface DBInstances {

  long EXPIRY_AGE = TimeUnit.HOURS.toMillis( 1 );

  Function<DBInstance, DBInstanceRuntimeComposite> COMPOSITE_RUNTIME =
      instance -> ImmutableDBInstanceRuntimeComposite.builder()
          .instance(ImmutableDBInstanceView.copyOf(instance))
          .runtime(instance.getDbInstanceRuntime())
          .build();

  Function<DBInstance, DBInstanceComposite> COMPOSITE_FULL =
      instance -> ImmutableDBInstanceComposite.builder()
          .instance(ImmutableDBInstanceView.copyOf(instance))
          .runtime(instance.getDbInstanceRuntime())
          .subnetGroup(instance.getDbSubnetGroup())
          .subnets(instance.getDbSubnetGroup().getSubnets())
          .build();

  List<DBInstance> deleteByExample(DBInstance example) throws RdsMetadataException;

  <T> List<T> list(OwnerFullName ownerFullName,
                   Predicate<? super DBInstance> filter,
                   Function<? super DBInstance, T> transform) throws RdsMetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super  DBInstance> filter,
                    Function<? super  DBInstance,T> transform ) throws RdsMetadataException;

  <T> List<T> listByExample(DBInstance example,
                            Predicate<? super DBInstance> filter,
                            Function<? super DBInstance, T> transform) throws RdsMetadataException;

  <T> T lookupByName(OwnerFullName ownerFullName,
                     String name,
                     Predicate<? super DBInstance> filter,
                     Function<? super DBInstance, T> transform) throws RdsMetadataException;

  <T> T lookupByExample(DBInstance example,
                        OwnerFullName ownerFullName,
                        String name,
                        Predicate<? super DBInstance> filter,
                        Function<? super DBInstance, T> transform) throws RdsMetadataException;

  DBInstance save(DBInstance dbSubnetGroup) throws RdsMetadataException;

  <T> T updateByExample(DBInstance example,
                        OwnerFullName ownerFullName,
                        String desc,
                        Function<? super DBInstance, T> updateTransform) throws RdsMetadataException;

  default <T> T updateByView(
      final DBInstanceView view,
      final Function<? super DBInstance, T> updateTransform
  ) throws RdsMetadataException {
    final AccountFullName accountFullName = AccountFullName.getInstance(view.getOwnerAccountNumber());
    return updateByExample(
        DBInstance.exampleWithName( accountFullName, view.getDisplayName() ),
        accountFullName,
        view.getDisplayName(),
        updateTransform
    );
  }

  AbstractPersistentSupport<DBInstanceMetadata, DBInstance, RdsMetadataException> withRetries();

  @TypeMapper
  enum DBInstanceTransform implements CompatFunction<DBInstance, com.eucalyptus.rds.common.msgs.DBInstance> {
    INSTANCE;

    @Override
    public com.eucalyptus.rds.common.msgs.DBInstance apply(final DBInstance instance) {
      final com.eucalyptus.rds.common.msgs.DBInstance result = new com.eucalyptus.rds.common.msgs.DBInstance();
      result.setDBInstanceIdentifier(instance.getDisplayName());
      result.setDBInstanceArn(RdsMetadatas.toArn(instance));
      result.setDBInstanceStatus(instance.getState().toString());
      result.setInstanceCreateTime(instance.getCreationTimestamp());
      result.setAllocatedStorage(instance.getAllocatedStorage());
      result.setAvailabilityZone(instance.getAvailabilityZone());
      result.setCopyTagsToSnapshot(instance.getCopyTagsToSnapshot());
      result.setDBInstanceClass(instance.getInstanceClass());
      result.setMasterUsername(instance.getMasterUsername());
      result.setEngine(instance.getEngine());
      result.setEngineVersion(instance.getEngineVersion());
      result.setDBName(instance.getDbName());
      result.setPubliclyAccessible(instance.getPubliclyAccessible());

      if ( Entities.isReadable( instance.getDbSubnetGroup( ) ) ) {
        result.setDBSubnetGroup(TypeMappers.transform(
            instance.getDbSubnetGroup( ),
            com.eucalyptus.rds.common.msgs.DBSubnetGroup.class ) );
      }

      if ( Entities.isReadable( instance.getVpcSecurityGroups( ) ) ) {
        final VpcSecurityGroupMembershipList list = new VpcSecurityGroupMembershipList();
        for ( final String securityGroupId : instance.getVpcSecurityGroups( ) ) {
          final VpcSecurityGroupMembership item = new VpcSecurityGroupMembership();
          item.setStatus("active");
          item.setVpcSecurityGroupId(securityGroupId);
          list.getMember().add(item);
        }
        result.setVpcSecurityGroups(list);
      }

      if ( Entities.isReadable( instance.getDbInstanceRuntime( ) ) ) {
        final DBInstanceRuntime runtime = instance.getDbInstanceRuntime();
        if ( runtime.getPrivateIp( ) != null ) try {
          final String endpointAddress = DomainNames.relativize(
              Name.concatenate(
                  RdsDnsHelper.getRelativeName(instance.getDisplayName(),instance.getOwnerAccountNumber()),
                  DomainNames.externalSubdomain(Rds.class)),
              DomainNames.root()).toString();

          final Endpoint endpoint = new Endpoint();
          endpoint.setAddress(endpointAddress);
          endpoint.setPort(instance.getDbPort());
          result.setEndpoint(endpoint);
        } catch (final NameTooLongException e) {
          // omit endpoint
        }
      }

      // hard-code some items we do not support
      result.setAutoMinorVersionUpgrade(false);
      result.setBackupRetentionPeriod(0);
      result.setDeletionProtection(false);
      result.setIAMDatabaseAuthenticationEnabled(false);
      result.setMultiAZ(false);
      result.setPerformanceInsightsEnabled(false);
      result.setStorageEncrypted(false);

      return result;
    }
  }
}
