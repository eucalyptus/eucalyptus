/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common.network;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.util.Parameters;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 * Caching helper for lookup of resource information
 */
public class NetworkLookup {
  private static final EntityCache<AllocatedAddressEntity,AddressLookupResult> addressCache =
      new EntityCache<>(AllocatedAddressEntity.example(), AddressLookupResult::of);
  private static final Supplier<Iterable<AddressLookupResult>> addressSupplier =
      Suppliers.memoizeWithExpiration(addressCache, 30, TimeUnit.SECONDS);
  private static final EntityCache<NetworkInterface,EniLookupResult> eniCache =
      new EntityCache<>(NetworkInterface.exampleWithOwner(null), EniLookupResult::of);
  private static final AtomicReference<Map<String,EniLookupResult>> eniMap =
      new AtomicReference<>(Collections.emptyMap());

  @Nonnull
  public static LookupResult lookupByIp(final String ip) {
    final Option<String> vpcId = lookupAddress(ip)
        .flatMap(AddressLookupResult::getEniId)
        .flatMap(NetworkLookup::lookupEni)
        .map(EniLookupResult::getVpcId);
    return ImmutableLookupResult.builder()
        .vpcId(vpcId)
        .build();
  }

  @Nonnull
  public static LookupResult lookupByIp(final InetAddress address) {
    return lookupByIp(address.getHostAddress());
  }


  @Immutable
  public interface LookupResult {
    @Nonnull
    Option<String> getVpcId();
  }

  @Nonnull
  private static Option<AddressLookupResult> lookupAddress(final String address) {
    return Stream.ofAll(addressSupplier.get()).find(addressLookupResult -> addressLookupResult.getAddress().equals(address));
  }

  @Nonnull
  private static Option<EniLookupResult> lookupEni(final String eniId) {
    if (eniId==null) {
      return Option.none();
    }
    Option<EniLookupResult> result = Option.of(eniMap.get().get(eniId));
    if (!result.isDefined()) {
      result = Option.of(eniMap.updateAndGet( map -> {
        if ( map.containsKey(eniId) ) {
          return map; // already reloaded
        } else {
          final Map<String,EniLookupResult> updatedMap = Maps.newHashMap();
          for( final EniLookupResult eniLookupResult : eniCache.get() ) {
            updatedMap.put(eniLookupResult.getEniId(), eniLookupResult);
          }
          return ImmutableMap.copyOf(updatedMap);
        }
      } ).get(eniId));
    }
    return result;
  }

  private static final class AddressLookupResult implements Comparable<AddressLookupResult> {
    private final String address;
    private final Option<String> eniId;

    private AddressLookupResult(final String address, final String eniId) {
      this.address = Parameters.checkParamNotNullOrEmpty("address", address);
      this.eniId = Option.of(eniId);
    }

    static AddressLookupResult of(final AllocatedAddressEntity allocatedAddressEntity) {
      return new AddressLookupResult(
          allocatedAddressEntity.getAddress(),
          allocatedAddressEntity.getNetworkInterfaceId());
    }

    @Override
    public int compareTo(final AddressLookupResult o) {
      return address.compareTo(o.address);
    }

    public String getAddress() {
      return address;
    }

    public Option<String> getEniId() {
      return eniId;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("address", address)
          .add("eniId", eniId)
          .toString();
    }
  }

  private static final class EniLookupResult implements Comparable<EniLookupResult> {
    private final String eniId;
    private final String vpcId;

    public EniLookupResult(final String eniId, final String vpcId) {
      this.eniId = Parameters.checkParamNotNullOrEmpty("eniId", eniId);
      this.vpcId = Parameters.checkParamNotNullOrEmpty("vpcId", vpcId);
    }

    static EniLookupResult of(final NetworkInterface networkInterface) {
      return new EniLookupResult(
          networkInterface.getDisplayName(),
          networkInterface.getVpc().getDisplayName() );
    }

    @Override
    public int compareTo(final EniLookupResult o) {
      return eniId.compareTo(o.eniId);
    }

    public String getEniId() {
      return eniId;
    }

    public String getVpcId() {
      return vpcId;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("eniId", eniId)
          .add("vpcId", vpcId)
          .toString();
    }
  }
}
