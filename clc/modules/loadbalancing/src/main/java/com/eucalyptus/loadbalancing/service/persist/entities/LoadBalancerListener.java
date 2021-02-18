/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenerView;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 */
@SuppressWarnings("deprecation")
@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_listener")
public class LoadBalancerListener extends AbstractPersistent implements LoadBalancerListenerView {
  private static Logger LOG = Logger.getLogger(LoadBalancerListener.class);

  public static class ELBPortRestrictionChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        if (newValue instanceof String) {
          final Set<Integer> range = PortRangeMapper.apply((String) newValue);
        }
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Malformed port: value should be [port(, port)] or [port-port]");
      }
    }
  }

  private static Function<String, Set<Integer>> PortRangeMapper =
      new Function<String, Set<Integer>>() {

        @SuppressWarnings("deprecation")
        @Override
        @Nullable
        public Set<Integer> apply(@Nullable String input) {
          try {
            if (input.contains("-")) {
              if (StringUtils.countMatches(input, "-") != 1) {
                throw new Exception("malformed range");
              }
              final String[] tokens = input.split("-");
              if (tokens.length != 2) {
                throw new Exception("invalid range");
              }
              final int beginPort = Integer.parseInt(tokens[0]);
              final int endPort = Integer.parseInt(tokens[1]);
              if (beginPort < 1 || endPort > 65535 || beginPort > endPort) {
                throw new Exception("invald range");
              }
              return ContiguousSet.create(Range.closed(beginPort, endPort),
                  DiscreteDomain.integers());
            } else if (input.contains(",")) {
              final String[] tokens = input.split(",");
              if (tokens.length != StringUtils.countMatches(input, ",") + 1) {
                throw new Exception("malformed list");
              }

              final Set<Integer> ports = Sets.newHashSet();
              for (final String token : tokens) {
                final int portNum = Integer.parseInt(token);
                if (token.isEmpty() || portNum < 1 || portNum > 65535) {
                  throw new Exception("invald port number");
                }
                ports.add(portNum);
              }
              return ports;
            } else {
              final int portNum = Integer.parseInt(input);
              if (input.isEmpty() || portNum < 1 || portNum > 65535) {
                throw new Exception("invald port number");
              }
              return Sets.newHashSet(portNum);
            }
          } catch (final Exception ex) {
            throw Exceptions.toUndeclared(ex);
          }
        }
      };

  private final static String DEFAULT_PORT_RESTRICTION = "22";
  @ConfigurableField(displayName = "restricted_ports",
      description = "The ports restricted for use as a loadbalancer port. Format should be port(, port) or [port-port]",
      initial = DEFAULT_PORT_RESTRICTION,
      changeListener = ELBPortRestrictionChangeListener.class
  )
  public static String RESTRICTED_PORTS = DEFAULT_PORT_RESTRICTION;

  public enum PROTOCOL {
    HTTP,
    HTTPS,
    TCP,
    SSL,
    NONE,
    ;

    public static PROTOCOL from(final String protocol) {
      final String upperProtocol = protocol == null ? null : protocol.toUpperCase();
      return PROTOCOL.valueOf(upperProtocol);
    }
  }

  private static final long serialVersionUID = 1L;

  private LoadBalancerListener() {
  }

  public static LoadBalancerListener named(final LoadBalancer lb, int lbPort) {
    LoadBalancerListener newInstance = new LoadBalancerListener();
    newInstance.loadbalancer = lb;
    newInstance.loadbalancerPort = lbPort;
    newInstance.uniqueName = newInstance.createUniqueName();
    return newInstance;
  }

  private LoadBalancerListener(Builder builder) {
    this.loadbalancer = builder.lb;
    this.instancePort = builder.instancePort;
    this.instanceProtocol = builder.instanceProtocol;
    this.loadbalancerPort = builder.loadbalancerPort;
    this.protocol = builder.protocol;
    this.sslCertificateArn = builder.sslCertificateArn;
  }

  public void setSSLCertificateId(final String certArn) {
    this.sslCertificateArn = certArn;
  }

  public static class Builder {
    public Builder(LoadBalancer lb, int instancePort, int loadbalancerPort, PROTOCOL protocol) {
      this.lb = lb;
      this.instancePort = instancePort;
      this.loadbalancerPort = loadbalancerPort;
      this.protocol = protocol.name().toLowerCase();
    }

    public Builder instanceProtocol(PROTOCOL protocol) {
      this.instanceProtocol = protocol.name().toLowerCase();
      return this;
    }

    public Builder withSSLCerntificate(String arn) {
      this.sslCertificateArn = arn;
      return this;
    }

    public LoadBalancerListener build() {
      return new LoadBalancerListener(this);
    }

    private LoadBalancer lb = null;
    private Integer instancePort = null;
    private String instanceProtocol = null;
    private Integer loadbalancerPort = null;
    private String protocol = null;
    private String sslCertificateArn = null;
  }

  @ManyToOne
  @JoinColumn(name = "metadata_loadbalancer_fk")
  private LoadBalancer loadbalancer = null;

  @Column(name = "instance_port", nullable = false)
  private Integer instancePort = null;

  @Column(name = "instance_protocol", nullable = true)
  private String instanceProtocol = null;

  @Column(name = "loadbalancer_port", nullable = false)
  private Integer loadbalancerPort = null;

  @Column(name = "protocol", nullable = false)
  private String protocol = null;

  @Column(name = "certificate_id", nullable = true)
  private String sslCertificateArn = null;

  @Column(name = "unique_name", nullable = false, unique = true)
  private String uniqueName = null;

  @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @JoinTable(name = "metadata_policy_has_listeners", joinColumns = {
      @JoinColumn(name = "metadata_listener_fk")}, inverseJoinColumns = @JoinColumn(name = "metadata_policy_fk"))
  private List<LoadBalancerPolicyDescription> policies = null;

  public int getInstancePort() {
    return this.instancePort;
  }

  public PROTOCOL getInstanceProtocol() {
    if (this.instanceProtocol == null) {
      return PROTOCOL.NONE;
    }

    return PROTOCOL.from(this.instanceProtocol);
  }

  public int getLoadbalancerPort() {
    return this.loadbalancerPort;
  }

  public PROTOCOL getProtocol() {
    return PROTOCOL.from(this.protocol);
  }

  public String getCertificateId() {
    return this.sslCertificateArn;
  }

  public void addPolicy(final LoadBalancerPolicyDescription policy) {
    if (this.policies == null) {
      this.policies = Lists.newArrayList();
    }
    if (!this.policies.contains(policy)) {
      this.policies.add(policy);
    }
  }

  public void removePolicy(final String policyName) {
    if (this.policies == null || policyName == null) {
      return;
    }
    LoadBalancerPolicyDescription toDelete = null;
    for (final LoadBalancerPolicyDescription pol : this.policies) {
      if (policyName.equals(pol.getPolicyName())) {
        toDelete = pol;
      }
    }
    if (toDelete != null) {
      this.policies.remove(toDelete);
    }
  }

  public void removePolicy(final LoadBalancerPolicyDescription policy) {
    if (this.policies == null || policy == null) {
      return;
    }
    this.policies.remove(policy);
  }

  public void resetPolicies() {
    if (this.policies == null) {
      return;
    }
    this.policies.clear();
  }

  public List<LoadBalancerPolicyDescription> getPolicyDescriptions() {
    return this.policies;
  }

  public static boolean protocolSupported(Listener listener) {
    try {
      final PROTOCOL protocol = PROTOCOL.from(listener.getProtocol());
      if (PROTOCOL.HTTP.equals(protocol) || PROTOCOL.TCP.equals(protocol) || PROTOCOL.HTTPS.equals(
          protocol) || PROTOCOL.SSL.equals(protocol)) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean acceptable(Listener listener) {
    try {
      if (!(listener.getInstancePort() > 0 &&
          listener.getLoadBalancerPort() > 0 &&
          !Strings.isNullOrEmpty(listener.getProtocol()))) {
        return false;
      }

      PROTOCOL protocol = PROTOCOL.from(listener.getProtocol());
      if (!Strings.isNullOrEmpty(listener.getInstanceProtocol())) {
        protocol = PROTOCOL.from(listener.getInstanceProtocol());
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean validRange(Listener listener) {
    try {
      if (!(listener.getInstancePort() > 0 &&
          listener.getLoadBalancerPort() > 0 &&
          !Strings.isNullOrEmpty(listener.getProtocol()))) {
        return false;
      }

      int lbPort = listener.getLoadBalancerPort();
      int instancePort = listener.getInstancePort();

      if (!(lbPort >= 1 && lbPort <= 65535)) {
        return false;
      }

      if (!(instancePort >= 1 && instancePort <= 65535)) {
        return false;
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // port 22: used as sshd by servo instances
  public static boolean portAvailable(Listener listener) {
    try {
      if (!(listener.getInstancePort() > 0 &&
          listener.getLoadBalancerPort() > 0 &&
          !Strings.isNullOrEmpty(listener.getProtocol()))) {
        return false;
      }

      int lbPort = listener.getLoadBalancerPort();
      int instancePort = listener.getInstancePort();
      return !PortRangeMapper.apply(RESTRICTED_PORTS).contains(lbPort);
    } catch (Exception e) {
      return false;
    }
  }

  @PrePersist
  private void generateOnCommit() {
    if (this.uniqueName == null) {
      this.uniqueName = createUniqueName();
    }
  }

  protected String createUniqueName() {
    return String.format("listener-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(),
        this.loadbalancer.getDisplayName(), this.loadbalancerPort);
  }

  @Override
  public String toString() {
    return String.format(
        "Listener for %s: %nProtocol=%s, Port=%d, InstancePort=%d, InstanceProtocol=%s, CertId=%s",
        this.loadbalancer.getDisplayName(), this.protocol, this.loadbalancerPort, this.instancePort,
        this.instanceProtocol, this.sslCertificateArn);
  }
}
