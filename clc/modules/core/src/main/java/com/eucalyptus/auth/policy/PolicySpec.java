/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.auth.policy;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

/**
 * NOTE: Please do not add service specific IAM policy details here.
 */
public class PolicySpec {

  public static final String VERSION = "Version";

  public static final String STATEMENT = "Statement";
  public static final String SID = "Sid";
  public static final String EFFECT = "Effect";
  public static final String ACTION = "Action";
  public static final String NOTACTION = "NotAction";
  public static final String RESOURCE = "Resource";
  public static final String NOTRESOURCE = "NotResource";
  public static final String PRINCIPAL = "Principal";
  public static final String NOTPRINCIPAL = "NotPrincipal";
  public static final String CONDITION = "Condition";

  // Effect
  public static final Set<String> EFFECTS = ImmutableSet.copyOf( Iterators.transform( Iterators.forArray(EffectType.values()), new Function<EffectType,String>() {
    @Override
    public String apply( final EffectType effect ) {
      return effect.name( );
    }
  }) );

  // Vendor (AWS products)
	// Do not add vendors here (use modules)
  public static final String VENDOR_IAM = "iam";
  public static final String VENDOR_EC2 = "ec2";
  public static final String VENDOR_STS = "sts";
  public static final String VENDOR_IMAGINGSERVICE = "eucaimaging";
	// Do not add vendors here (use modules)

  public static final String ALL_PRINCIPALS = "*";

  public static final String ALL_ACTION = "*";

  // STS actions, based on IAM Using Temporary Security Credentials version 2011-06-15
  public static final String STS_ASSUMEROLE = "assumerole";
  public static final String STS_ASSUMEROLEWITHWEBIDENTITY = "assumerolewithwebidentity";
  public static final String STS_DECODEAUTHORIZATIONMESSAGE = "decodeauthorizationmessage";
  public static final String STS_GETACCESSTOKEN = "getaccesstoken"; // eucalyptus extension
  public static final String STS_GETCALLERIDENTITY = "getcalleridentity";
  public static final String STS_GETFEDERATIONTOKEN = "getfederationtoken";
  public static final String STS_GETIMPERSONATIONTOKEN = "getimpersonationtoken"; // eucalyptus extension
  public static final String STS_GETSESSIONTOKEN = "getsessiontoken";

  // Map vendors to resource vendors
  public static final Map<String, Set<String>> VENDOR_RESOURCE_VENDORS = new ImmutableMap.Builder<String,Set<String>>()
      .put( VENDOR_STS, ImmutableSet.of( VENDOR_IAM ) )
      .build();

  // Set of vendors with case insensitive resource names
  public static final Set<String> VENDORS_CASE_INSENSITIVE_RESOURCES = new ImmutableSet.Builder<String>()
      .add( VENDOR_EC2 )
      .build();

  // Action syntax
  public static final Pattern ACTION_PATTERN = Pattern.compile( "\\*|(?:([a-z0-9-]+):(\\S+))" );

  // Wildcard
  public static final String ALL_RESOURCE = "*";

  // IAM resource types (see IamPolicySpec for all resources)
  public static final String IAM_RESOURCE_USER = "user";
  public static final String IAM_RESOURCE_ROLE = "role";
  public static final String IAM_RESOURCE_INSTANCE_PROFILE = "instance-profile";
  public static final String IAM_RESOURCE_SERVER_CERTIFICATE = "server-certificate";
  public static final String IAM_RESOURCE_OPENID_CONNECT_PROVIDER = "oidc-provider";
  public static final String IAM_RESOURCE_ACCESS_KEY = "access-key";
  public static final String IAM_RESOURCE_SIGNING_CERTIFICATE = "signing-certificate";
  public static final String IAM_RESOURCE_POLICY = "policy";

  // STS selected resource types
  public static final String STS_RESOURCE_ASSUMED_ROLE = "assumed-role";

  // EC2 selected resource types for reference within core module
  public static final String EC2_RESOURCE_ADDRESS = "address";

  public static final Pattern IPV4_ADDRESS_RANGE_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:-(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?" );

  public static String qualifiedName( String vendor, String name ) {
    return name == null ? null : vendor + ":" + name;
  }

  public static String vendor( final String qualifiedName ) {
    int index = qualifiedName.indexOf( ':' );
    if ( index <= 0 ) {
      throw new IllegalArgumentException( "Name not qualified: " + qualifiedName );
    }
    return qualifiedName.substring( 0, index );
  }

  public static boolean isPermittedResourceVendor( final String vendor, final String resourceVendor ) {
    final Set<String> resourceVendors = VENDOR_RESOURCE_VENDORS.get( vendor );
    return resourceVendors == null ?
        vendor.equals( resourceVendor ) :
        resourceVendors.contains( resourceVendor );
  }

  public static String describeAction( final String vendor, final String resource ) {
    return "describe" + resource + "s";
  }

  public static String canonicalizeResourceName( final String type,
                                                 final String name ) {
    return type == null || !VENDORS_CASE_INSENSITIVE_RESOURCES.contains( vendor( type ) ) ?
        name :
        name.toLowerCase();
  }

}
