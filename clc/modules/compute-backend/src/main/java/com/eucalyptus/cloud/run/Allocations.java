/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.run;

import com.eucalyptus.auth.AuthContextSupplier;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.VmInstanceToken;
import com.eucalyptus.compute.common.RequestLaunchTemplateData;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.images.Emis;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.images.Emis.LookupMachine;
import com.eucalyptus.compute.common.internal.keys.KeyPairs;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;
import com.eucalyptus.util.UniqueIds;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.eucalyptus.cluster.common.msgs.VmInfo;
import edu.ucsb.eucalyptus.msgs.HasRequest;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import io.vavr.control.Option;

public class Allocations {
  private static Logger LOG = Logger.getLogger(Allocations.class);

  private static ThreadLocal<Allocation> allocationThreadLocal = new ThreadLocal<>();

  public enum AllocationType {
    Run, Start, Restore, Verify;

    public boolean matches(final Allocation allocation) {
      return allocation != null && this==allocation.getAllocationType();
    }
  }

  public static class Allocation implements HasRequest {
    /** to be eliminated **/
    private final Context context;
    private final RunInstancesType request;
    /** values determined by the request **/
    private final AllocationType allocationType;
    private final UserFullName ownerFullName;
    private RequestLaunchTemplateData launchTemplateData = new RequestLaunchTemplateData();
    private boolean verifyLaunchTemplateData;
    private byte[] userData;
    private String credential;
    private String rootDirective;
    private final int minCount;
    private final int maxCount;
    private boolean usePrivateAddressing;
    private boolean disableApiTermination;
    private boolean monitoring;
    @Nullable
    private final String clientToken;

    /** verified references determined by the request **/
    private Partition partition;
    private SshKeyPair sshKeyPair;
    private BootableSet bootSet;
    private VmType vmType;
    private Subnet subnet;
    private NetworkGroup primaryNetwork;
    private Map<String, NetworkGroup> networkGroups = Maps.newHashMap( );
    private String iamInstanceProfileArn;
    private String iamInstanceProfileId;
    private String iamRoleArn;
    private String launchTemplateArn;
    private Boolean launchTemplateResource;

    /** intermediate allocation state **/
    private final String reservationId;
    private final List<VmInstanceToken> allocationTokens = Lists.newArrayList();
    private final Long reservationIndex;
    private final Map<Integer, String> instanceIds;
    private final Map<Integer, String> instanceUuids;
    private Date expiration;
    private Function<String,List<ResourceTag>> tagsForResourceFunction = __ -> Collections.emptyList();

    /** */
    private final TypedContext allocationContext = TypedContext.newTypedContext( );
    private final AtomicBoolean committed = new AtomicBoolean( false );

    private Allocation(final RunInstancesType request) {
      this.allocationType = AllocationType.Run;
      this.context = Contexts.lookup();
      this.instanceIds = Maps.newHashMap();
      this.instanceUuids = Maps.newHashMap();
      this.request = request;
      this.minCount = request.getMinCount();
      this.maxCount = request.getMaxCount();
      this.usePrivateAddressing = "private".equals(request.getAddressingType());
      this.disableApiTermination = false;
      this.monitoring = false;
      this.clientToken = Strings.emptyToNull(request.getClientToken());

      this.ownerFullName = this.context.getUserFullName();
      this.reservationIndex = UniqueIds.nextIndex(VmInstance.class,
          (long) request.getMaxCount());
      this.reservationId = ResourceIdentifiers.generateString( "r" );
      // GRZE:FIXME: moved all this encode/decode junk into util.UserDatas
      if (this.request.getUserData() != null) {
        try {
          this.userData = Base64.decode(this.request.getUserData());
          this.request.setUserData(new String(Base64.encode(this.userData)));
        } catch (Exception e) {
        }
      } else {
        try {
          this.request.setUserData(new String(Base64.encode(new byte[0])));
        } catch (Exception ex) {
          LOG.error(ex, ex);
        }
      }
      this.credential = null;
    }

    /**
     * Constructor for restore case.
     */
    private Allocation( final String reservationId,
                        final String instanceId,
                        final String instanceUuid,
                        final int launchIndex,
                        final boolean usePrivateAddressing,
                        final VmType vmType,
                        final BootableSet bootSet,
                        final Partition partition,
                        final SshKeyPair sshKeyPair,
                        final byte[] userData,
                        final UserFullName ownerFullName ) {
      this.allocationType = AllocationType.Restore;
      this.minCount = 1;
      this.maxCount = 1;
      this.vmType = vmType;
      this.bootSet = bootSet;
      this.partition = partition;
      this.sshKeyPair = sshKeyPair;
      this.userData = userData;
      this.ownerFullName = ownerFullName;
      this.reservationId = reservationId;
      this.reservationIndex = -1l;
      this.instanceIds = Maps.newHashMap();
      this.instanceIds.put(launchIndex, instanceId);
      this.instanceUuids = Maps.newHashMap();
      this.instanceUuids.put(launchIndex, instanceUuid);
      this.allocationTokens.add( new VmInstanceToken( this, launchIndex ) );
      this.context = null;
      this.monitoring = false;
      this.usePrivateAddressing = usePrivateAddressing;
      this.clientToken = null;
      this.request = inferRequest();
    }

    /**
     * Constructor for start case.
     */
    private Allocation(final String reservationId, final String instanceId,
        final String instanceUuid, final byte[] userData,
        final Date expiration, final Partition partition,
        final SshKeyPair sshKeyPair, final BootableSet bootSet,
        final VmType vmType, final Set<NetworkGroup> networkGroups,
        final boolean isUsePrivateAddressing, final boolean monitoring,
        final String clientToken, final String iamInstanceProfileArn,
        final String iamInstanceProfileId, final String iamRoleArn) {
      this.allocationType = AllocationType.Start;
      this.context = Contexts.lookup();
      this.minCount = 1;
      this.maxCount = 1;
      this.usePrivateAddressing = isUsePrivateAddressing;
      this.ownerFullName = context.getUserFullName();
      this.reservationId = reservationId;
      this.reservationIndex = UniqueIds.nextIndex(VmInstance.class,
          (long) this.maxCount);
      this.instanceIds = Maps.newHashMap();
      this.instanceIds.put(0, instanceId);
      this.instanceUuids = Maps.newHashMap();
      this.instanceUuids.put(0, instanceUuid);
      this.userData = userData;
      this.partition = partition;
      this.sshKeyPair = (sshKeyPair != null ? sshKeyPair : KeyPairs.noKey());
      this.bootSet = bootSet;
      this.expiration = expiration;
      this.vmType = vmType;
      this.monitoring = monitoring;
      this.clientToken = clientToken;
      this.iamInstanceProfileArn = iamInstanceProfileArn;
      this.iamInstanceProfileId = iamInstanceProfileId;
      this.iamRoleArn = iamRoleArn;
      this.credential = null;

      this.networkGroups = new HashMap<String, NetworkGroup>() {
        {
          for (NetworkGroup g : networkGroups) {
            if (Allocation.this.primaryNetwork == null) {
              Allocation.this.primaryNetwork = g;
            }
            put(g.getDisplayName(), g);
          }
        }
      };
      this.request = inferRequest();
    }

    /**
     * Constructor for launch template metadata verification
     */
    private Allocation(final RequestLaunchTemplateData launchTemplateData) {
      this.allocationType = AllocationType.Verify;
      this.context = Contexts.lookup();
      this.minCount = 1;
      this.maxCount = 1;
      this.ownerFullName = context.getUserFullName();
      this.clientToken = null;
      this.reservationId = ResourceIdentifiers.generateString( "r" );
      this.reservationIndex = -1L;
      this.instanceIds = Maps.newHashMap();
      this.instanceUuids = Maps.newHashMap();
      this.disableApiTermination = false;
      this.monitoring = false;
      this.launchTemplateData = launchTemplateData;
      this.verifyLaunchTemplateData = true;
      this.request = inferRequest();
    }

    private RunInstancesType inferRequest( ) {
      final RunInstancesType runInstances = new RunInstancesType( );
      runInstances.setMinCount(1);
      runInstances.setMaxCount(1);
      if (bootSet != null) {
        runInstances.setImageId(bootSet.getMachine().getDisplayName());
      }
      if (partition != null) {
        runInstances.setAvailabilityZone(partition.getName());
      }
      if (vmType != null) {
        runInstances.setInstanceType(vmType.getName());
      }
      return runInstances;
    }

    public AllocationType getAllocationType( ) {
      return allocationType;
    }

    @Override
    public RunInstancesType getRequest() {
      return getRunInstancesRequest();
    }

    public RunInstancesType getRunInstancesRequest() {
      return this.request;
    }

    public VmInstance exampleInstanceResource( boolean includeId ) {
      return VmInstance.exampleResource(
          includeId && getInstanceIds( ).size( )==1 ? getInstanceId( 0 ) : "",
          getOwnerFullName( ),
          getPartition( ).getName( ),
          getIamInstanceProfileArn( ),
          getVmType( ).getName( ),
          getBootSet().isBlockStorage( )
      );
    }

    public NetworkGroup getPrimaryNetwork() {
      return this.primaryNetwork;
    }

    public boolean apply( final Predicate<Allocation> predicate ) {
      allocationThreadLocal.set( this );
      try {
        return predicate.apply( this );
      } finally {
        allocationThreadLocal.set( null );
      }
    }

    public static Option<Allocation> current( ) {
      return Option.of( allocationThreadLocal.get( ) );
    }

    public void commit() throws Exception {
      if ( !committed.get( ) ) try {
        for (final VmInstanceToken t : this.getAllocationTokens()) {
          VmInstances.Create.INSTANCE.apply(t);
        }
        committed.set( true );
      } catch (final Exception ex) {
        this.abort();
        throw ex;
      }
    }

    public boolean isCommitted() {
      return committed.get( );
    }

    public void abort() {
      for (final VmInstanceToken token : this.allocationTokens) {
        LOG.warn("Aborting resource token: " + token);
        Logs.exhaust().error( "Aborting resource token", new RuntimeException( ) );
        final EntityTransaction db = Entities.get(VmInstance.class);
        try {
          token.abort();
          db.commit();
        } catch (final Exception ex) {
          LOG.warn(ex.getMessage());
          Logs.exhaust().error(ex, ex);
          db.rollback();
        }
      }
    }

    @Nullable
    public RequestLaunchTemplateData getLaunchTemplateData() {
      return launchTemplateData;
    }

    public void setLaunchTemplateData(final RequestLaunchTemplateData launchTemplateData) {
      this.launchTemplateData = launchTemplateData;
    }

    public boolean isVerifyLaunchTemplateData() {
      return verifyLaunchTemplateData;
    }

    public void setVerifyLaunchTemplateData(final boolean verifyLaunchTemplateData) {
      this.verifyLaunchTemplateData = verifyLaunchTemplateData;
    }

    public List<NetworkGroup> getNetworkGroups() {
      return Lists.newArrayList(this.networkGroups.values());
    }

    public VmType getVmType() {
      return this.vmType;
    }

    public Partition getPartition() {
      return this.partition;
    }

    public void setBootableSet(final BootableSet bootSet) {
      this.bootSet = bootSet;
    }

    public void setVmType(final VmType vmType) {
      this.vmType = vmType;
    }

    public Subnet getSubnet() {
      return subnet;
    }

    public void setSubnet( final Subnet subnet ) {
      this.subnet = subnet;
    }

    public UserFullName getOwnerFullName() {
      return this.ownerFullName;
    }

    public String getAuthenticatedArn( ) {
      return Accounts.getAuthenticatedArn( context.getUser( ) );
    }

    public List<VmInstanceToken> getAllocationTokens() {
      return this.allocationTokens;
    }

    public void setUserData( final byte[] userData ) {
      this.userData = userData;
    }

    public byte[] getUserData() {
      return this.userData;
    }
    
    public void setCredential(final String credential) {
      this.credential = credential;
    }

    public String getCredential(){
      return this.credential;
    }

    public void setRootDirective() {
      if ( ! bootSet.isBlockStorage( ) ) {
        try {
          final MachineImageInfo mi = LookupMachine.INSTANCE.apply( this.getRequest( ).getImageId());
          rootDirective = mi.getRootDirective();
        } catch (Exception ex) {}
      }
    }

    public String getRootDirective(){
      return this.rootDirective;
    }

    public Long getReservationIndex() {
      return this.reservationIndex;
    }

    public String getReservationId() {
      return this.reservationId;
    }

    public BootableSet getBootSet() {
      return this.bootSet;
    }

    @Deprecated
    public Context getContext() {
      return this.context;
    }

    public void setPartition(final Partition partition2) {
      this.partition = partition2;
    }

    public SshKeyPair getSshKeyPair() {
      return this.sshKeyPair;
    }

    public void setSshKeyPair(final SshKeyPair sshKeyPair) {
      this.sshKeyPair = sshKeyPair;
    }

    public void setNetworkRules(
        final Map<String, NetworkGroup> networkRuleGroups) {
      final Entry<String, NetworkGroup> ent = networkRuleGroups.entrySet()
          .iterator().next();
      this.primaryNetwork = ent.getValue();
      this.networkGroups = networkRuleGroups;
    }

    public VmTypeInfo getVmTypeInfo(final Partition partition, final String reservationId) throws MetadataException {
      return this.bootSet.populateVirtualBootRecord(this.vmType, partition, reservationId);
    }

    @Nullable
    public String getIamInstanceProfileArn() {
      return iamInstanceProfileArn;
    }

    public void setInstanceProfileArn(final String instanceProfileArn) {
      this.iamInstanceProfileArn = instanceProfileArn;
    }

    public String getIamInstanceProfileId() {
      return iamInstanceProfileId;
    }

    public void setIamInstanceProfileId(final String iamInstanceProfileId) {
      this.iamInstanceProfileId = iamInstanceProfileId;
    }

    public String getIamRoleArn() {
      return iamRoleArn;
    }

    public void setIamRoleArn(final String iamRoleArn) {
      this.iamRoleArn = iamRoleArn;
    }

    public String getLaunchTemplateArn() {
      return launchTemplateArn;
    }

    public void setLaunchTemplateArn(final String launchTemplateArn) {
      this.launchTemplateArn = launchTemplateArn;
    }

    public Boolean isLaunchTemplateResource() {
      return launchTemplateResource;
    }

    public void setLaunchTemplateResource(final Boolean launchTemplateResource) {
      this.launchTemplateResource = launchTemplateResource;
    }

    public int getMinCount() {
      return this.minCount;
    }

    public int getMaxCount() {
      return this.maxCount;
    }

    public boolean isUsePrivateAddressing() {
      return usePrivateAddressing;
    }

    public void setUsePrivateAddressing( final boolean usePrivateAddressing ) {
      this.usePrivateAddressing = usePrivateAddressing;
    }

    public boolean isDisableApiTermination( ) {
      return disableApiTermination;
    }

    public void setDisableApiTermination( final boolean disableApiTermination ) {
      this.disableApiTermination = disableApiTermination;
    }

    public boolean isMonitoring() {
      return monitoring;
    }

    public void setMonitoring(final boolean monitoring) {
      this.monitoring = monitoring;
    }

    @Nullable
    public String getClientToken() {
      return clientToken;
    }

    @Nullable
    public String getUniqueClientToken( @Nonnull final Integer launchIndex ) {
      return clientToken == null ?
          null :
          String.format( "%s:%d:%s", getOwnerFullName( ).getAccountNumber( ), launchIndex, clientToken );
    }

    public Set<String> getInstanceIds( ) {
      return Sets.newHashSet( this.instanceIds.values( ) );
    }

    public String getInstanceId( int index ) {
      if ( !this.instanceIds.containsKey( index ) ) {
        this.instanceIds.put( index, VmInstances.getId( ) );
      }
      return this.instanceIds.get(index);
    }

    public String getInstanceUuid(int index) {
      if ( !this.instanceUuids.containsKey( index ) ) {
        this.instanceUuids.put( index, UUID.randomUUID().toString() );
      }
      return this.instanceUuids.get(index);
    }

    public Date getExpiration() {
      return this.expiration;
    }

    public void setExpiration(final Date expiration) {
      this.expiration = expiration;
    }

    public List<ResourceTag> getTagsForResource( final String resource ) {
      return getTagsForResourceFunction().apply( resource );
    }

    public Function<String, List<ResourceTag>> getTagsForResourceFunction() {
      return tagsForResourceFunction;
    }

    public void setTagsForResourceFunction(final Function<String, List<ResourceTag>> tagsForResourceFunction) {
      this.tagsForResourceFunction = tagsForResourceFunction;
    }

    public <T> T getAttribute(final TypedKey<T> key ) {
      return allocationContext.get( key );
    }

    public <T> T setAttribute( final TypedKey<T> key, final T value ) {
      return allocationContext.put( key, value );
    }

    public <T> T removeAttribute( final TypedKey<T> key ) {
      return allocationContext.remove( key );
    }

    public AuthContextSupplier getAuthContext( ) throws AuthException {
      return context != null ?
          context.getAuthContext( ) :
          Permissions.createAuthContextSupplier(
              Accounts.lookupPrincipalByUserId( getOwnerFullName( ).getUserId( ) ),
              Collections.<String,String>emptyMap( ) );
    }
  }

  public static Allocation run(final RunInstancesType request) {
    return new Allocation(request);
  }

  public static Allocation verify(final RequestLaunchTemplateData launchTemplateData) {
    return new Allocation(launchTemplateData);
  }

  public static Allocation start(final VmInstance vm) {
    BootableSet bootSet = Emis.recreateBootableSet(vm);
    return new Allocation(vm.getReservationId(), vm.getInstanceId(),
        vm.getInstanceUuid(), vm.getUserData(), vm.getExpiration(),
        vm.lookupPartition(), vm.getKeyPair(), bootSet, vm.getVmType(),
        vm.getNetworkGroups(), vm.isUsePrivateAddressing(), vm.getMonitoring(),
        vm.getClientToken(), vm.getIamInstanceProfileArn(),
        vm.getIamInstanceProfileId(), vm.getIamRoleArn());
  }

  public static Allocation restore( final VmInfo input,
                                    final int launchIndex,
                                    final VmType vmType,
                                    final BootableSet bootSet,
                                    final Partition partition,
                                    final SshKeyPair sshKeyPair,
                                    final byte[] userData,
                                    final UserFullName ownerFullName ) {
    final Allocation allocation = new Allocation(
        input.getReservationId( ),
        input.getInstanceId( ),
        input.getUuid( ),
        launchIndex,
        VmNetworkConfig.DEFAULT_IP.equals(input.getNetParams().getIgnoredPublicIp()) || Strings.isNullOrEmpty( input.getNetParams().getIgnoredPublicIp() ) ,
        vmType,
        bootSet,
        partition,
        sshKeyPair,
        userData,
        ownerFullName );

    final VmInstanceToken token = Iterables.getOnlyElement( allocation.getAllocationTokens( ) );
    token.setZombie( true );

    return allocation;
  }
}
