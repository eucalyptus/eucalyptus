/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.loadbalancing;

import static com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer.Scheme;
import static com.eucalyptus.util.Strings.nonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.loadbalancing.activities.LoadBalancerVersionException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import com.eucalyptus.auth.euare.common.msgs.ServerCertificateType;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataException;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataNotFoundException;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingPersistence;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerListener;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroup.STATE;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroupRef;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerZone;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstance;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstances;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescription;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.loadbalancing.service.CertificateNotFoundException;
import com.eucalyptus.loadbalancing.service.DuplicateAccessPointName;
import com.eucalyptus.loadbalancing.service.DuplicateListenerException;
import com.eucalyptus.loadbalancing.service.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.service.InternalFailureException;
import com.eucalyptus.loadbalancing.service.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.service.ListenerNotFoundException;
import com.eucalyptus.loadbalancing.service.LoadBalancingException;
import com.eucalyptus.loadbalancing.service.UnsupportedParameterException;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendServerDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendServerDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerServoInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZoneFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZoneView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZonesView;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingActivitiesImpl;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

/**
 * @author Sang-Min Park
 */
@SuppressWarnings("Guava")
public class LoadBalancerHelper {
  private static Logger LOG = Logger.getLogger(LoadBalancerHelper.class);

  public static <T> List<T> listLoadbalancers(
      final LoadBalancingPersistence persistence,
      final Function<? super LoadBalancer, T> transform
  ) {
    return listLoadbalancers(persistence, LoadBalancer.named(), transform);
  }

  public static <T> List<T> listLoadbalancers(
      final LoadBalancingPersistence persistence,
      final Function<? super LoadBalancer, T> transform,
      final String accountNumber
  ) {
    return listLoadbalancers(persistence, LoadBalancer.ownedByAccount(accountNumber), transform);
  }

  public static <T> List<T> listLoadbalancers(
      final LoadBalancingPersistence persistence,
      final LoadBalancer example,
      final Function<? super LoadBalancer, T> transform
  ) {
    try {
      return persistence.balancers().listByExample(example, Predicates.alwaysTrue(), transform);
    } catch (final LoadBalancingMetadataException ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  // a loadbalancer is per-account resource; per-user access is governed by IAM policy
  public static <T> T getLoadbalancer(
      final LoadBalancingPersistence persistence,
      final Predicate<? super LoadBalancer> filter,
      final Function<? super LoadBalancer, T> transform,
      final OwnerFullName ownerFullName,
      final String lbName
  ) {
    return getLoadbalancer(persistence, filter, transform, ownerFullName.getAccountNumber(),
        lbName);
  }

  public static LoadBalancer getLoadbalancer(
      final LoadBalancingPersistence persistence,
      final String accountNumber,
      final String lbName
  ) throws NoSuchElementException {
    return getLoadbalancer(persistence, Predicates.alwaysTrue(), Functions.identity(),
        accountNumber, lbName);
  }

  public static <T> T getLoadbalancer(
      final LoadBalancingPersistence persistence,
      final Predicate<? super LoadBalancer> filter,
      final Function<? super LoadBalancer, T> transform,
      final String accountNumber,
      final String lbName
  ) throws NoSuchElementException {
    try {
      return persistence.balancers()
          .lookupByName(AccountFullName.getInstance(accountNumber), lbName, filter, transform);
    } catch (LoadBalancingMetadataNotFoundException ex) {
      throw new NoSuchElementException();
    } catch (LoadBalancingMetadataException ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public static LoadBalancer getLoadbalancerCaseInsensitive(
      final LoadBalancingPersistence persistence,
      final String accountNumber,
      final String lbName
  ) {
    for (final LoadBalancer lb : listLoadbalancers(persistence, Functions.identity(),
        accountNumber)) {
      if (lb.getDisplayName().toLowerCase().equals(lbName.toLowerCase())) {
        return lb;
      }
    }
    throw new NoSuchElementException();
  }

  public static String getLoadBalancerDnsName(final LoadBalancerView loadBalancer) {
    return getLoadBalancerDnsName(
        loadBalancer.getScheme(),
        loadBalancer.getDisplayName(),
        loadBalancer.getOwnerAccountNumber()
    );
  }

  private static String getLoadBalancerDnsName(
      @Nullable final Scheme scheme,
      @Nonnull final String displayName,
      @Nonnull final String accountNumber
  ) {
    return LoadBalancerDomainName.forScheme(scheme).generate(displayName, accountNumber);
  }

  public static <T> T getLoadBalancerByDnsName(
      final LoadBalancingPersistence persistence,
      final Predicate<? super LoadBalancer> filter,
      final Function<? super LoadBalancer, T> transform,
      final String dnsName
  ) throws NoSuchElementException {
    try {
      final Name hostName = Name.fromString(dnsName, Name.root)
          .relativize(LoadBalancerDomainName.getLoadBalancerSubdomain());
      final Optional<LoadBalancerDomainName> domainName =
          LoadBalancerDomainName.findMatching(hostName);
      if (domainName.isPresent()) {
        final Pair<String, String> accountNamePair =
            domainName.get().toScopedLoadBalancerName(hostName);
        try {
          return LoadBalancerHelper.getLoadbalancer(persistence, filter, transform,
              accountNamePair.getLeft(), accountNamePair.getRight());
        } catch (NoSuchElementException e) {
          if (domainName.get()
              == LoadBalancerDomainName.INTERNAL) { // perhaps it was an external balancer named "internal-..."
            final Pair<String, String> externalAccountNamePair =
                LoadBalancerDomainName.EXTERNAL.toScopedLoadBalancerName(hostName);
            return LoadBalancerHelper.getLoadbalancer(persistence, filter, transform,
                externalAccountNamePair.getLeft(), externalAccountNamePair.getRight());
          } else {
            throw e;
          }
        }
      } else {
        throw new NoSuchElementException();
      }
    } catch (TextParseException e) {
      throw new NoSuchElementException();
    }
  }

  public static void checkVersion(
      final LoadBalancerView lb,
      final LoadBalancerDeploymentVersion minVersion
  ) throws LoadBalancerVersionException {
    if (lb.getLoadbalancerDeploymentVersion() == null ||
        !LoadBalancerDeploymentVersion.getVersion(
            lb.getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(minVersion)) {
      throw new LoadBalancerVersionException(minVersion);
    }
  }

  public static Predicate<LoadBalancerView> v4_2_0 = (lb) -> {
    return versionOnOrLater(lb, LoadBalancerDeploymentVersion.v4_2_0);
  };

  public static Predicate<LoadBalancerView> v4_3_0 = (lb) -> {
    return versionOnOrLater(lb, LoadBalancerDeploymentVersion.v4_3_0);
  };

  public static Predicate<LoadBalancerView> v4_4_0 = (lb) -> {
    return versionOnOrLater(lb, LoadBalancerDeploymentVersion.v4_4_0);
  };

  private static boolean versionOnOrLater(final LoadBalancerView lb,
      final LoadBalancerDeploymentVersion version) {
    if (lb.getLoadbalancerDeploymentVersion() == null) {
      return false;
    } else {
      return LoadBalancerDeploymentVersion.getVersion(
          lb.getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(version);
    }
  }

  public static LoadBalancer addLoadbalancer(
      final LoadBalancingPersistence persistence,
      final UserFullName user,
      final String lbName,
      final String vpcId,
      final Scheme scheme,
      final Map<String, String> securityGroupIdsToNames,
      final Map<String, String> tags) throws LoadBalancingException {

    final List<LoadBalancer> accountLbs =
        LoadBalancerHelper.listLoadbalancers(persistence, Functions.identity(),
            user.getAccountNumber());
    for (final LoadBalancer lb : accountLbs) {
      if (lbName.toLowerCase().equals(lb.getDisplayName().toLowerCase())) {
        throw new DuplicateAccessPointName();
      }
    }

    /// EC2 classic
    if (vpcId == null) {
      ///FIXME: not a sane reference
      final String securityGroupName =
          LoadBalancingActivitiesImpl.getSecurityGroupName(user.getAccountNumber(), lbName);
      try {
        if (persistence.balancerSecurityGroups().listByExample(
            LoadBalancerSecurityGroup.withState(STATE.OutOfService),
            Predicates.alwaysTrue(),
            LoadBalancerSecurityGroup::getName).contains(securityGroupName)) {
          throw new InternalFailureException(
              "Cleaning up the previous ELB with the same name. Retry in a few minutes.");
        }
      } catch (LoadBalancingMetadataException e) {
        LOG.error("Error checking for previous ELB with the same name", e);
        throw new InternalFailureException("Error checking for previous ELB with the same name");
      }
    }
    try {
      try {
        persistence.balancers()
            .lookupByName(user.asAccountFullName(), lbName, Predicates.alwaysTrue(),
                ImmutableLoadBalancerView::copyOf);
        throw new DuplicateAccessPointName();
      } catch (final LoadBalancingMetadataNotFoundException e) {
      }
      final List<LoadBalancerSecurityGroupRef> refs = Lists.newArrayList();
      for (final Map.Entry<String, String> groupIdToNameEntry : securityGroupIdsToNames.entrySet()) {
        refs.add(new LoadBalancerSecurityGroupRef(groupIdToNameEntry.getKey(),
            groupIdToNameEntry.getValue()));
      }
      Collections.sort(refs, Ordering.natural().onResultOf(LoadBalancerSecurityGroupRef.groupId()));

      final LoadBalancer lb = LoadBalancer.newInstance(user, lbName);
      lb.setVpcId(vpcId);
      lb.setScheme(scheme);
      lb.setSecurityGroupRefs(refs);
      lb.setTags(tags);
      lb.setLoadbalancerDeploymentVersion(LoadBalancerDeploymentVersion.Latest.toVersionString());
      persistence.balancers().save(lb);
      return lb;
    } catch (LoadBalancingException ex) {
      throw ex;
    } catch (Exception ex) {
      LOG.error("Failed to persist a new loadbalancer", ex);
      throw new LoadBalancingException(
          "Failed to persist a new load-balancer because of: " + ex.getMessage(), ex);
    }
  }

  public static void deleteLoadbalancer(
      final LoadBalancingPersistence persistence,
      final OwnerFullName ownerFullName,
      final String lbName
  ) throws LoadBalancingException {
    try {
      final LoadBalancer loadBalancer =
          getLoadbalancer(persistence, Predicates.alwaysTrue(), Functions.identity(), ownerFullName,
              lbName);
      persistence.balancers().delete(loadBalancer);
    } catch (Exception e) {
      throw new LoadBalancingException("Error deleting load balancer " + lbName, e);
    }
  }

  public static void validateListener(final List<Listener> listeners)
      throws LoadBalancingException, EucalyptusCloudException {
    for (Listener listener : listeners) {
      if (!LoadBalancerListener.protocolSupported(listener)) {
        throw new UnsupportedParameterException("The requested protocol is not supported");
      }
      if (!LoadBalancerListener.acceptable(listener)) {
        throw new InvalidConfigurationRequestException("Invalid listener format");
      }
      if (!LoadBalancerListener.validRange(listener)) {
        throw new InvalidConfigurationRequestException("Invalid port range");
      }
      if (!LoadBalancerListener.portAvailable(listener)) {
        throw new EucalyptusCloudException("The specified port(s) "
            + LoadBalancerListener.RESTRICTED_PORTS
            + ", are restricted for use as a loadbalancer port.");
      }
      final PROTOCOL protocol = PROTOCOL.from(listener.getProtocol());
      if (protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
        final String sslId = listener.getSSLCertificateId();
        if (sslId == null || sslId.length() <= 0) {
          throw new InvalidConfigurationRequestException(
              "SSLCertificateId is required for HTTPS or SSL protocol");
        }
      }
    }
  }

  public static void validateListener(
      final LoadBalancerView lb,
      final List<LoadBalancerListenerView> existingListeners,
      final List<Listener> listeners
  ) throws LoadBalancingException, EucalyptusCloudException {
    validateListener(listeners);
    // check the listener
    for (Listener listener : listeners) {
      final LoadBalancerListenerView existing = lb == null ?
          null :
          LoadBalancerHelper.findListener(existingListeners, listener.getLoadBalancerPort());
      if (existing != null) {
        if (existing.getInstancePort() != listener.getInstancePort() ||
            !existing.getProtocol()
                .name()
                .toLowerCase()
                .equals(listener.getProtocol().toLowerCase()) ||
            ((existing.getCertificateId() == null || !existing.getCertificateId()
                .equals(listener.getSSLCertificateId())))) {
          throw new DuplicateListenerException();
        }
      }
    }
  }

  public static void createLoadbalancerListener(final LoadBalancingPersistence persistence,
      final String lbName, final Context ctx, final List<Listener> listeners)
      throws LoadBalancingException {
    try {
      persistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getAccount(),
          lbName,
          Predicates.alwaysTrue(),
          lb -> {
            try {
              validateListener(lb, Lists.newArrayList(lb.getListeners()), listeners);
            } catch (EucalyptusCloudException e) {
              throw Exceptions.toUndeclared(e);
            }
            for (Listener listener : listeners) {
              // check the listener
              try {
                if (LoadBalancerHelper.findListener(lb, listener.getLoadBalancerPort()) == null) {
                  LoadBalancerListener.Builder builder =
                      new LoadBalancerListener.Builder(lb, listener.getInstancePort(),
                          listener.getLoadBalancerPort(),
                          LoadBalancerListener.PROTOCOL.from(listener.getProtocol()));
                  if (!Strings.isNullOrEmpty(listener.getInstanceProtocol())) {
                    builder.instanceProtocol(PROTOCOL.from(listener.getInstanceProtocol()));
                  }

                  if (!Strings.isNullOrEmpty(listener.getSSLCertificateId())) {
                    builder.withSSLCerntificate(listener.getSSLCertificateId());
                  }
                  Entities.persist(builder.build());
                }
              } catch (Exception ex) {
                LOG.warn("failed to create the listener object", ex);
              }
            }
            return lb;
          });
    } catch (Exception ex) {
      throw new InternalFailure400Exception("unable to update the loadbalancer");
    }
  }

  public static void removeZone(
      final LoadBalancingPersistence persistence,
      final UserFullName user,
      final String loadbalancer,
      final Collection<String> zones
  ) {
    for (final String zoneName : zones) {
      try {
        persistence.balancers().updateByExample(
            LoadBalancer.namedByAccountId(user.getAccountNumber(), loadbalancer),
            user.asAccountFullName(),
            loadbalancer,
            Predicates.alwaysTrue(),
            lb -> {
              for (final LoadBalancerZone zone : lb.getZones()) {
                if (zoneName.equals(zone.getName())) {
                  Entities.delete(zone);
                }
              }
              return lb;
            }
        );
      } catch (Exception ex) {
        LOG.error("failed to delete the zone " + zoneName, ex);
      }
    }
  }

  public static LoadBalancerZone findZone(final LoadBalancer lb, final String zoneName) {
    try (final TransactionResource db = Entities.transactionFor(LoadBalancerZone.class)) {
      final LoadBalancerZone exist = Entities.uniqueResult(LoadBalancerZone.named(lb, zoneName));
      db.commit();
      return exist;
    } catch (NoSuchElementException ex) {
      throw ex;
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  /**
   * Caller must have tx for lb
   */
  public static List<LoadBalancerZoneView> findZonesInService(final LoadBalancer lb) {
    final List<LoadBalancerZoneView> inService = Lists.newArrayList();
    for (final LoadBalancerZoneView zone : lb.getZones()) {
      if (zone.getState().equals(LoadBalancerZone.STATE.InService)) {
        inService.add(zone);
      }
    }
    return inService;
  }

  public static LoadBalancerListenerView findListener(
      final List<LoadBalancerListenerView> listeners, final int portNum) {
    LoadBalancerListenerView listener = null;
    for (final LoadBalancerListenerView l : listeners) {
      if (l.getLoadbalancerPort() == portNum) {
        listener = l;
        break;
      }
    }
    return listener;
  }

  /**
   * Caller must have tx
   */
  public static LoadBalancerListener findListener(final LoadBalancer lb, final int portNum) {
    LoadBalancerListener listener = null;
    for (final LoadBalancerListener l : lb.getListeners()) {
      if (l.getLoadbalancerPort() == portNum) {
        listener = l;
        break;
      }
    }
    return listener;
  }

  /**
   * Caller must have tx
   */
  @Nullable
  public static LoadBalancerBackendInstance findBackendInstance(final LoadBalancer lb,
      final String instanceId) {
    if (lb.getBackendInstances() != null) {
      try {
        return Iterables.find(lb.getBackendInstances(),
            input -> input.getInstanceId().equals(instanceId));
      } catch (NoSuchElementException ex) {
        return null;
      }
    }
    return null;
  }

  /**
   * Caller must have tx
   */
  public static Map<String, String> getSecurityGroupIdsToNames(final LoadBalancer lb) {
    return ImmutableMap.copyOf(CollectionUtils.putAll(
        lb.getSecurityGroupRefs(),
        Maps.newLinkedHashMap(),
        LoadBalancerSecurityGroupRef.groupId(),
        LoadBalancerSecurityGroupRef.groupName()));
  }

  public static LoadBalancerServoInstance lookupServoInstance(final String instanceId)
      throws LoadBalancingException {
    try (final TransactionResource db = Entities.transactionFor(LoadBalancerServoInstance.class)) {
      LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
      final LoadBalancerServoInstance exist = Entities.uniqueResult(sample);
      return exist;
    } catch (NoSuchElementException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new LoadBalancingException("failed to query servo instances", ex);
    }
  }

  public static void unsetForeignKeys(
      final LoadBalancingPersistence persistence,
      final UserFullName user,
      final String loadbalancer
  ) {
    try {
      persistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(user.getAccountNumber(), loadbalancer),
          user.asAccountFullName(),
          loadbalancer,
          Predicates.alwaysTrue(),
          lb -> {
            for (final LoadBalancerZone zone : lb.getZones()) {
              for (final LoadBalancerServoInstance servoInstance : zone.getServoInstances()) {
                servoInstance.setAvailabilityZone(null);
                servoInstance.setAutoScalingGroup(null);
              }
            }
            return lb;
          }
      );
    } catch (Exception ex) {
    }
  }

  public static void setLoadBalancerListenerSSLCertificate(
      final LoadBalancingPersistence persistence,
      final LoadBalancerView lb,
      final List<LoadBalancerListenerView> listeners,
      final int lbPort,
      final String certArn
  ) throws LoadBalancingException {
    LoadBalancerListenerView listener = null;
    for (final LoadBalancerListenerView l : listeners) {
      if (l.getLoadbalancerPort() == lbPort) {
        listener = l;
        break;
      }
    }

    final ThrowingFunction<LoadBalancerListenerView, LoadBalancerListenerView, LoadBalancingException>
        listenerChecks = lbListener -> {
      if (lbListener == null) {
        throw new ListenerNotFoundException();
      }
      if (!(PROTOCOL.HTTPS.equals(lbListener.getProtocol()) || PROTOCOL.SSL.equals(
          lbListener.getProtocol()))) {
        throw new InvalidConfigurationRequestException("Listener's protocol is not HTTPS or SSL");
      }
      return lbListener;
    };

    listenerChecks.apply(listener);
    checkSSLCertificate(lb.getOwnerAccountNumber(), certArn);
    updateIAMRolePolicy(lb, listener.getCertificateId(), certArn);
    try {
      persistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(lb.getOwnerAccountNumber(), lb.getDisplayName()),
          AccountFullName.getInstance(lb.getOwnerAccountNumber()),
          lb.getDisplayName(),
          Predicates.alwaysTrue(),
          update -> {
            final LoadBalancerListener updateListener =
                LoadBalancerHelper.findListener(update, lbPort);
            try {
              listenerChecks.apply(updateListener);
            } catch (LoadBalancingException ex) {
              throw Exceptions.toUndeclared(ex);
            }
            updateListener.setSSLCertificateId(certArn);
            return update;
          }
      );
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new ListenerNotFoundException();
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  private static void updateIAMRolePolicy(final LoadBalancerView lb,
      final String oldCertArn, final String newCertArn) throws LoadBalancingException {
    final String accountId = lb.getOwnerAccountNumber();
    final String lbName = lb.getDisplayName();
    final String prefix =
        String.format("arn:aws:iam::%s:server-certificate", accountId);
    final String oldCertName = oldCertArn.replace(prefix, "")
        .substring(oldCertArn.replace(prefix, "").lastIndexOf("/") + 1);
    final String newCertName = newCertArn.replace(prefix, "")
        .substring(newCertArn.replace(prefix, "").lastIndexOf("/") + 1);

    ////FIXME: not a sound reference
    final String roleName = String.format("%s-%s-%s", LoadBalancingActivitiesImpl.ROLE_NAME_PREFIX,
        accountId, lbName);
    final String oldPolicyName = String.format("%s-%s-%s-%s",
        LoadBalancingActivitiesImpl.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
        accountId, lbName, oldCertName);

    try {
      EucalyptusActivityTasks.getInstance()
          .deleteRolePolicy(roleName, oldPolicyName, lb.useSystemAccount());
    } catch (final Exception ex) {
      throw new LoadBalancingException("Failed to delete old role policy " + oldPolicyName, ex);
    }
    final String newPolicyName = String.format("%s-%s-%s-%s",
        LoadBalancingActivitiesImpl.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
        accountId, lbName, newCertName);
    final String newPolicyDoc = LoadBalancingActivitiesImpl.ROLE_SERVER_CERT_POLICY_DOCUMENT
        .replace("CERT_ARN_PLACEHOLDER", newCertArn);
    try {
      EucalyptusActivityTasks.getInstance()
          .putRolePolicy(roleName, newPolicyName, newPolicyDoc, lb.useSystemAccount());
    } catch (final Exception ex) {
      throw new LoadBalancingException("Failed to add new role policy " + newPolicyName, ex);
    }
  }

  public static void checkSSLCertificate(final String accountNumber, final String certArn)
      throws LoadBalancingException {
    try {
      final String prefix = String.format("arn:aws:iam::%s:server-certificate", accountNumber);
      if (!certArn.startsWith(prefix)) {
        throw new CertificateNotFoundException();
      }

      final String pathAndName = certArn.replace(prefix, "");
      final String certName = pathAndName.substring(pathAndName.lastIndexOf("/") + 1);
      final ServerCertificateType cert =
          EucalyptusActivityTasks.getInstance().getServerCertificate(accountNumber, certName);
      if (cert == null) {
        throw new CertificateNotFoundException();
      }
      if (!certArn.equals(cert.getServerCertificateMetadata().getArn())) {
        throw new CertificateNotFoundException();
      }
    } catch (final Exception ex) {
      throw new CertificateNotFoundException();
    }
  }

  //// WARNING: this method is database intensive call
  //// Do not invoke too frequently!
  public static LoadBalancerServoDescription getServoDescription(
      final LoadBalancingPersistence persistence,
      final String accountId,
      final String lbName,
      final String zone
  ) throws LoadBalancingException {
    final Tuple2<LoadBalancerFullView, LoadBalancerZonesView> lbData;
    try {
      lbData = LoadBalancerHelper.getLoadbalancer(
          persistence,
          Predicates.alwaysTrue(),
          lbEntity -> Tuple.of(
              LoadBalancers.FULL_VIEW.apply(lbEntity),
              LoadBalancers.ZONES_VIEW.apply(lbEntity)
          ),
          AccountFullName.getInstance(accountId),
          lbName);
    } catch (final Exception ex) {
      throw new LoadBalancingException("Unexpected error while preparing loadbalancer description",
          ex);
    }

    final LoadBalancerZoneFullView lbZone = Stream.ofAll(lbData._2().getZones())
        .find(lb -> zone.equals(lb.getZone().getName()))
        .getOrElseThrow(
            () -> new LoadBalancingException("No such availability zone is found in database"));
    return getServoDescription(lbData._1(), lbZone);
  }

  public static LoadBalancerServoDescription getServoDescription(
      final LoadBalancerFullView lbFull,
      final LoadBalancerZoneFullView zone) {

    final LoadBalancerView lb = lbFull.getLoadBalancer();
    final List<LoadBalancerListenerFullView> lbListeners = lbFull.getListeners();
    final List<LoadBalancerBackendServerDescriptionFullView> backendServers =
        lbFull.getBackendServers();
    final List<LoadBalancerPolicyDescriptionFullView> lbPolicies = lbFull.getPolicies();
    final String lbName = lb.getDisplayName();

    final LoadBalancerServoDescription desc = new LoadBalancerServoDescription();
    desc.setLoadBalancerName(lbName); /// loadbalancer name
    desc.setCreatedTime(lb.getCreationTimestamp());/// createdtime

    /// dns name
    desc.setDnsName(LoadBalancerHelper.getLoadBalancerDnsName(lb));

    // attributes
    desc.setLoadBalancerAttributes(TypeMappers.transform(lb, LoadBalancerAttributes.class));

    /// backend instances in the same zone
    Collection<LoadBalancerBackendInstanceView> backendInstancesInSameZone =
        Collections2.filter(zone.getBackendInstances(),
            new Predicate<LoadBalancerBackendInstanceView>() {
              @Override
              public boolean apply(LoadBalancerBackendInstanceView arg0) {
                return !LoadBalancerBackendInstance.STATE.Error.equals(arg0.getBackendState()) &&
                    !(arg0.getIpAddress() == null || arg0.getIpAddress().length() <= 0);
              }
            });

    final boolean zoneHasAvailableInstance =
        backendInstancesInSameZone.stream().anyMatch(inst ->
            LoadBalancerBackendInstance.STATE.InService.equals(inst.getBackendState()) &&
                !(inst.getIpAddress() == null || inst.getIpAddress().length() <= 0)
        );

    // backend instances in cross-zone
    Collection<LoadBalancerBackendInstanceView> crossZoneBackendInstances =
        Lists.newArrayList();
    if (!zoneHasAvailableInstance
        || desc.getLoadBalancerAttributes().getCrossZoneLoadBalancing().getEnabled()) {
      // EUCA-13233: when a zone contains no available instance, cross-zone instances are always included
      crossZoneBackendInstances = Collections2.filter(lbFull.getBackendInstances(),
          new Predicate<LoadBalancerBackendInstanceView>() {
            @Override
            public boolean apply(LoadBalancerBackendInstanceView arg0) {
              // Instance's service state can only be determined in the same zone. Cross-zone instances are included only when InService
              final boolean inService =
                  LoadBalancerBackendInstance.STATE.InService.equals(arg0.getBackendState()) &&
                      !(arg0.getIpAddress() == null || arg0.getIpAddress().length() <= 0);
              return inService &&
                  !zone.getZone().getName().equals(arg0.getPartition()); // different zone
            }
          });
    }

    if (!backendInstancesInSameZone.isEmpty()) {
      desc.setBackendInstances(new BackendInstances());
      desc.getBackendInstances().getMember().addAll(
          Collections2.transform(backendInstancesInSameZone,
              new Function<LoadBalancerBackendInstanceView, BackendInstance>() {
                @Override
                public BackendInstance apply(final LoadBalancerBackendInstanceView be) {
                  final BackendInstance instance = new BackendInstance();
                  instance.setInstanceId(be.getInstanceId());
                  instance.setInstanceIpAddress(be.getIpAddress());
                  instance.setReportHealthCheck(true);
                  return instance;
                }
              }));
    }

    if (!crossZoneBackendInstances.isEmpty()) {
      if (desc.getBackendInstances() == null) {
        desc.setBackendInstances(new BackendInstances());
      }
      desc.getBackendInstances().getMember().addAll(
          Collections2.transform(crossZoneBackendInstances,
              new Function<LoadBalancerBackendInstanceView, BackendInstance>() {
                @Override
                public BackendInstance apply(final LoadBalancerBackendInstanceView be) {
                  final BackendInstance instance = new BackendInstance();
                  instance.setInstanceId(be.getInstanceId());
                  instance.setInstanceIpAddress(be.getIpAddress());
                  // if the servo's zone != backend instance's, it does not report health check
                  // only the servo in the same zone will change the instance's state
                  instance.setReportHealthCheck(false);
                  return instance;
                }
              }));
    }
    if (desc.getBackendInstances() != null) {
      desc.getBackendInstances()
          .getMember()
          .sort(Ordering.natural().onResultOf(nonNull(BackendInstance::getInstanceId)));
    }

    /// availability zones
    desc.setAvailabilityZones(new AvailabilityZones());
    desc.getAvailabilityZones().getMember().add(zone.getZone().getName());

    final Set<String> policiesOfListener = Sets.newHashSet();
    final Set<String> policiesForBackendServer = Sets.newHashSet();

    /// listeners
    if (lbListeners.size() > 0) {
      desc.setListenerDescriptions(new ListenerDescriptions());
      desc.getListenerDescriptions().setMember(new ArrayList<>(
          Collections2.transform(lbListeners,
              new Function<LoadBalancerListenerFullView, ListenerDescription>() {
                @Override
                public ListenerDescription apply(
                    final LoadBalancerListenerFullView lbListenerFull) {
                  final LoadBalancerListenerView lbListener = lbListenerFull.getListener();
                  ListenerDescription desc = new ListenerDescription();
                  Listener listener = new Listener();
                  listener.setLoadBalancerPort(lbListener.getLoadbalancerPort());
                  listener.setInstancePort(lbListener.getInstancePort());
                  if (lbListener.getInstanceProtocol() != PROTOCOL.NONE) {
                    listener.setInstanceProtocol(lbListener.getInstanceProtocol().name());
                  }
                  listener.setProtocol(lbListener.getProtocol().name());
                  if (lbListener.getCertificateId() != null) {
                    listener.setSSLCertificateId(lbListener.getCertificateId());
                  }
                  desc.setListener(listener);
                  final PolicyNames pnames = new PolicyNames();
                  pnames.setMember(Lists.newArrayList(
                      Stream.ofAll(lbListenerFull.getPolicyDescriptions()).map(arg0 -> {
                        try {
                          return arg0.getPolicyName(); // No other policy types are supported
                        } catch (final Exception ex) {
                          return ""; // No other policy types are supported
                        }
                      })));
                  pnames.getMember().sort(Ordering.natural());
                  policiesOfListener.addAll(pnames.getMember());
                  desc.setPolicyNames(pnames);
                  return desc;
                }
              })));
    }

    /// backend server descriptions
    try {
      if (backendServers.size() > 0) {
        desc.setBackendServerDescriptions(new BackendServerDescriptions());
        desc.getBackendServerDescriptions().setMember(new ArrayList<>(
            Collections2.transform(backendServers,
                new Function<LoadBalancerBackendServerDescriptionFullView, BackendServerDescription>() {
                  @Override
                  public BackendServerDescription apply(
                      LoadBalancerBackendServerDescriptionFullView backendFull) {
                    final LoadBalancerBackendServerDescriptionView backend =
                        backendFull.getBackendServer();
                    final BackendServerDescription desc = new BackendServerDescription();
                    desc.setInstancePort(backend.getInstancePort());
                    desc.setPolicyNames(new PolicyNames());
                    desc.getPolicyNames().setMember(new ArrayList<>(
                        Collections2.transform(backendFull.getPolicyDescriptions(),
                            new Function<LoadBalancerPolicyDescriptionView, String>() {
                              @Override
                              public String apply(
                                  LoadBalancerPolicyDescriptionView arg0) {
                                return arg0.getPolicyName();
                              }
                            })
                    ));
                    desc.getPolicyNames().getMember().sort(Ordering.natural());
                    policiesForBackendServer.addAll(desc.getPolicyNames().getMember());
                    return desc;
                  }
                })
        ));
        desc.getBackendServerDescriptions()
            .getMember()
            .sort(Ordering.natural().onResultOf(BackendServerDescription::getInstancePort));
      }
    } catch (final Exception ex) {
    }

    /// health check
    if (lb.getHealthCheckConfig() != null) {
      try {
        int interval = lb.getHealthCheckConfig().getInterval();
        String target = lb.getHealthCheckConfig().getTarget();
        int timeout = lb.getHealthCheckConfig().getTimeout();
        int healthyThresholds = lb.getHealthCheckConfig().getHealthyThreshold();
        int unhealthyThresholds = lb.getHealthCheckConfig().getUnhealthyThreshold();

        final HealthCheck hc = new HealthCheck();
        hc.setInterval(interval);
        hc.setHealthyThreshold(healthyThresholds);
        hc.setTarget(target);
        hc.setTimeout(timeout);
        hc.setUnhealthyThreshold(unhealthyThresholds);
        desc.setHealthCheck(hc);
      } catch (Exception ex) {
      }
    }

    // policies (EUCA-specific)
    final ArrayList<PolicyDescription> policies = Lists.newArrayList();
    for (final LoadBalancerPolicyDescriptionFullView lbPolicyFull : lbPolicies) {
      // for efficiency, add policies only if they are set for listeners
      // PublicKey policies should always be included bc it's referenced from BackendAuthenticationPolicyType
      final LoadBalancerPolicyDescriptionView lbPolicy = lbPolicyFull.getPolicyDescription();
      if (policiesOfListener.contains(lbPolicy.getPolicyName())
          || policiesForBackendServer.contains(lbPolicy.getPolicyName())
          || "PublicKeyPolicyType".equals(lbPolicy.getPolicyTypeName())) {
        policies.add(LoadBalancerPolicyHelper.AsPolicyDescription.INSTANCE.apply(lbPolicyFull));
      }
    }
    final PolicyDescriptions policyDescs = new PolicyDescriptions();
    policyDescs.setMember(policies);
    policyDescs.getMember()
        .sort(Ordering.natural().onResultOf(nonNull(PolicyDescription::getPolicyName)));
    desc.setPolicyDescriptions(policyDescs);

    return desc;
  }

  public static void checkWorkerCertificateExpiration(
      final LoadBalancerZonesView lb
  ) throws LoadBalancingException {
    try {
      for (final LoadBalancerZoneFullView lbZone : lb.getZones()) {
        for (final LoadBalancerServoInstanceView instance : lbZone.getServoInstances()) {
          if (LoadBalancerServoInstance.STATE.InService.equals(instance.getState())) {
            boolean expired = false;
            try { // Upgrade case: add expiration date to instance's launch time
              if (instance.getCertificateExpiration() == null) {
                final List<RunningInstancesItemType> instances =
                    EucalyptusActivityTasks.getInstance()
                        .describeSystemInstances(Lists.newArrayList(instance.getInstanceId()),
                            true);
                final Date launchDate = instances.get(0).getLaunchTime();
                final Calendar cal = Calendar.getInstance();
                cal.setTime(launchDate);
                cal.add(Calendar.DATE,
                    Integer.parseInt(LoadBalancingWorkerProperties.EXPIRATION_DAYS));

                try (final TransactionResource db = Entities.transactionFor(
                    LoadBalancerServoInstance.class)) {
                  final LoadBalancerServoInstance entity = Entities.uniqueResult(
                      LoadBalancerServoInstance.named(instance.getInstanceId()));
                  entity.setCertificateExpiration(cal.getTime());
                  expired = entity.isCertificateExpired();
                  Entities.persist(entity);
                  db.commit();
                }
              }
            } catch (final Exception ex) {
              LOG.warn("Failed to update ELB worker's certificate expiration date", ex);
            }
            if (expired || instance.isCertificateExpired()) {
              throw new InternalFailureException(String.format(
                  "LoadBalancing worker(%s)'s certificate has expired. Contact Cloud Administrator.",
                  instance.getInstanceId()));
            }
          }
        }
      }
    } catch (final LoadBalancingException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new LoadBalancingException(
          "Error while checking loadbalancing worker's certificate expiration", ex);
    }
  }
}
