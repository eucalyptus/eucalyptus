/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.component;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import com.amazonaws.regions.Regions;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "dns.spoof_regions",
                    description = "Options controlling DNS domain spoofing for AWS regions." )
public class RegionSpoofingResolver extends DnsResolver {
  @ConfigurableField( description = "Enable the spoofing resolver which allows for AWS DNS name emulation for instances.", initial = "false" )
  public static Boolean             enabled                   = Boolean.FALSE;
  @ConfigurableField( description = "Enable spoofing of the default AWS DNS names, e.g., ec2.amazonaws.com would resolve to the ENABLED cloud controller.", initial = "false" )
  public static Boolean             SPOOF_AWS_DEFAULT_REGIONS = Boolean.FALSE;
  @ConfigurableField( description = "Enable spoofing for the normal AWS regions, too. e.g., ec2.us-east-1.amazonaws.com would resolve to the ENABLED cloud controller.", initial = "false" )
  public static Boolean             SPOOF_AWS_REGIONS         = Boolean.FALSE;
  @ConfigurableField( description = "Internal region name. If set, the region name to expect as the second label in the DNS name. For example, to treat your Eucalyptus install like a region named 'eucalyptus', set this value to 'eucalyptus'.  Then, e.g., autoscaling.eucalyptus.amazonaws.com will resolve to the service address when using this DNS server." )
  public static String              REGION_NAME               = null;
  private static Logger             LOG                       = Logger.getLogger( RegionSpoofingResolver.class );
  private static final Name         awsDomain                 = Name.fromConstantString( "amazonaws.com." );
  private static final List<String> awsRegionNames            = Lists.transform( Arrays.asList( Regions.values( ) ),
                                                                                 Functions.toStringFunction( ) );
  
  @Override
  public boolean checkAccepts( DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !enabled || !RequestType.A.apply( query ) || !query.getName( ).subdomain( awsDomain ) ) {
      return false;
    } else if ( SPOOF_AWS_REGIONS ) {
      return true;
    } else {
      Name relativeName = query.getName( ).relativize( awsDomain );
      if ( relativeName.labels( ) > 2 ) {
        return false;
      } else if ( relativeName.labels( ) == 1 && SPOOF_AWS_DEFAULT_REGIONS ) {//e.g., s3.amazonaws.com
        return true;
      } else if ( relativeName.labels( ) == 2 ) {
        final String regionLabel = query.getName( ).getLabelString( 1 );
        if ( !SPOOF_AWS_REGIONS && awsRegionNames.contains( regionLabel.replace( "-", "_" ).toUpperCase( ) ) ) {
          return false;
        } else if ( REGION_NAME != null && !REGION_NAME.equals( regionLabel ) ) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    }
  }
  
  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    Name name = query.getName( );
    String label0 = name.getLabelString( 0 );
    String label1 = name.getLabelString( 1 );
    LOG.debug( "Trying to spoof "
               + name
               + " aws-service="
               + label0
               + " region="
               + label1
               + " spoof="
               + SPOOF_AWS_REGIONS
               + " region="
               + REGION_NAME );
    for ( ComponentId compId : ComponentIds.list( ) ) {
      if ( compId.getAwsServiceName( ).equals( label0 ) ) {
        LOG.debug( "Spoofing found component for name: "
                   + name
                   + " aws-service="
                   + label0
                   + " region="
                   + label1
                   + " component="
                   + compId.name( ) );
        List<ServiceConfiguration> configs = Lists.newArrayList( Topology.enabledServices( compId.getClass( ) ) );
        Collections.shuffle( configs );
        List<Record> answers = Lists.newArrayList( );
        for ( ServiceConfiguration config : configs ) {
          Record aRecord = DomainNameRecords.addressRecord( name, config.getInetAddress( ) );
          answers.add( aRecord );
          LOG.debug( "Spoofing found records for name: "
                     + name
                     + " config="
                     + config.getName( )
                     + " config-address="
                     + config.getInetAddress( ) );
        }
        return DnsResponse.forName( query.getName( ) )
                          .answer( answers );
      }
    }
    throw new NoSuchElementException( "Failed to lookup name: " + name );
  }

  @Override
  public int getOrder( ) {
    return DEFAULT_ORDER - 200;
  }
}
