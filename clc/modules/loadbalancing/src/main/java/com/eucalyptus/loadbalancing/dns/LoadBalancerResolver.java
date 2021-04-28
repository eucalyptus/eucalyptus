/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.dns;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import static com.eucalyptus.util.dns.DnsResolvers.DnsResponse;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancerHelper;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

/**
 *
 */
@ConfigurableClass(
    root = "services.loadbalancing",
    description = "Parameters controlling loadbalancing"
)
public class LoadBalancerResolver extends DnsResolvers.DnsResolver {

  private static final Logger logger = Logger.getLogger(LoadBalancerResolver.class);

  private static final int QUERY_ANSWER_EXPIRE_AFTER_SEC = 5;
  private static final LoadingCache<Name, List<String>> cachedAnswers = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(QUERY_ANSWER_EXPIRE_AFTER_SEC, TimeUnit.SECONDS)
      .build(new CacheLoader<Name, List<String>>() {
        @Override
        public List<String> load(final Name name) throws Exception {
          return resolveName(name);
        }
      });

  /// the LoadingCache is used to set the hard limit on memory usage
  private static final LoadingCache<Name, IpPermutation> nameToIpPermutations =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(1, TimeUnit.HOURS)
          .build(new CacheLoader<Name, IpPermutation>() {
            @Override
            public IpPermutation load(Name name) throws Exception {
              final List<String> ips = cachedAnswers.get(name);
              return new IpPermutation(ips);
            }
          });

  @ConfigurableField(description = "Enable the load balancing DNS resolver.  Note: dns.enable must also be 'true'", initial = "true")
  public static Boolean dns_resolver_enabled = Boolean.TRUE;

  @Override
  public boolean checkAccepts(final DnsRequest request) {
    final Record query = request.getQuery();
    if (!Bootstrap.isOperational() || !dns_resolver_enabled) {
      return false;
    } else if (query.getName().subdomain(LoadBalancerDomainName.getLoadBalancerSubdomain())) {
      return true;
    }
    return false;
  }

  static List<String> getIps(final String name) {
    try {
      return getIps(Name.fromString(name, Name.root));
    } catch (ExecutionException | TextParseException e) {
      return null;
    }
  }

  private static List<String> resolveName(final Name name) {
    final Name hostName = name.relativize(LoadBalancerDomainName.getLoadBalancerSubdomain());
    final Optional<LoadBalancerDomainName> domainName =
        LoadBalancerDomainName.findMatching(hostName);
    Set<String> ips = Collections.emptySet();
    if (domainName.isPresent()) {
      final Pair<String, String> accountNamePair =
          domainName.get().toScopedLoadBalancerName(hostName);
      final String accountNumber = accountNamePair.getLeft();
      final String loadBalancerName = accountNamePair.getRight();
      ips = LoadBalancerHelper.getServoMetadataSource()
          .resolveIpsForLoadBalancer(accountNumber, loadBalancerName);
    }
    List<String> ipList = Lists.newArrayList(ips);
    Collections.sort(ipList);
    return ipList;
  }

  private static class IpPermutation {
    private List<String> ips = null;
    private List<List<String>> ipPermutations = null;
    private int idx = 0;

    private IpPermutation(final List<String> ips) {
      this.ips = ips;
      ipPermutations = Lists.newArrayList(Collections2.permutations(ips));
      Collections.shuffle(ipPermutations);
    }

    public synchronized List<String> getNext() {
      try {
        final List<String> next = ipPermutations.get(idx);
        if (++idx >= ipPermutations.size()) {
          idx = 0;
        }
        return next;
      } catch (final Exception ex) {
        return Lists.newArrayList();
      }
    }

    public boolean isPermuatationFrom(final List<String> ips) {
      return this.ips.equals(ips);
    }
  }

  private static List<String> getIps(final Name name) throws ExecutionException {
    final List<String> ips = cachedAnswers.get(name);
    final IpPermutation old = nameToIpPermutations.get(name);
    if (!old.isPermuatationFrom(ips)) {
      nameToIpPermutations.invalidate(name);
    }
    return nameToIpPermutations.get(name).getNext();
  }

  @Override
  public DnsResponse lookupRecords(final DnsRequest request) {
    final Record query = request.getQuery();
    try {
      final Name name = query.getName();
      final List<String> ips = getIps(name);
      final List<Record> records = Lists.newArrayList();
      for (String ip : ips) {
        final InetAddress inetAddress = InetAddresses.forString(ip);
        records.add(DomainNameRecords.addressRecord(
            name,
            inetAddress,
            LoadBalancerDnsRecord.getLoadbalancerTTL()));
      }
      if (DnsResolvers.RequestType.A.apply(query)) {
        return DnsResponse.forName(name).answer(records);
      } else {
        return DnsResponse.forName(name).answer(Lists.<Record>newArrayList());
      }
    } catch (Exception ex) {

      logger.debug(ex);
    }
    return DnsResponse.forName(query.getName()).nxdomain();
  }
}
