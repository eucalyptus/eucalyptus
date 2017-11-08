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

package com.eucalyptus.auth.policy.key;

import static com.eucalyptus.auth.policy.key.Key.EvaluationConstraint;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Ordered;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * IAM condition key constants.
 */
public class Keys {

  // Keys
  public static final String AWS_CURRENTTIME = "aws:currenttime";
  public static final String AWS_SOURCEIP = "aws:sourceip";
  public static final String AWS_SECURE_TRANSPORT = "aws:securetransport";
  public static final String AWS_TOKEN_ISSUETIME = "aws:tokenissuetime";
  public static final String AWS_FEDERATED_PROVIDER = "aws:federatedprovider";
  public static final String AWS_TAG_KEYS = "aws:tagkeys";

  public static final String S3_MAX_KEYS = "s3:max-keys";
  
  public static final String EC2_KEEPALIVE = "ec2:keepalive";
  public static final String EC2_EXPIRATIONTIME = "ec2:expirationtime";
    
  // Quota keys
  public static final String EC2_QUOTA_VM_INSTANCE_NUMBER = "ec2:quota-vminstancenumber";
  public static final String EC2_QUOTA_VM_INSTANCE_ACTIVE_NUMBER = "ec2:quota-vminstanceactivenumber";
  public static final String EC2_QUOTA_IMAGE_NUMBER = "ec2:quota-imagenumber";
  public static final String EC2_QUOTA_VOLUME_NUMBER = "ec2:quota-volumenumber";
  public static final String EC2_QUOTA_VOLUME_TOTAL_SIZE = "ec2:quota-volumetotalsize";
  public static final String EC2_QUOTA_SNAPSHOT_NUMBER = "ec2:quota-snapshotnumber";
  public static final String EC2_QUOTA_ADDRESS_NUMBER = "ec2:quota-addressnumber";
  public static final String EC2_QUOTA_SECURITYGROUP_NUMBER = "ec2:quota-securitygroupnumber";
  public static final String EC2_QUOTA_VPC_NUMBER = "ec2:quota-vpcnumber";
  public static final String EC2_QUOTA_INTERNETGATEWAY_NUMBER = "ec2:quota-internetgatewaynumber";

  public static final String S3_QUOTA_BUCKET_NUMBER = "s3:quota-bucketnumber";
  public static final String S3_QUOTA_BUCKET_TOTAL_SIZE = "s3:quota-buckettotalsize";
  public static final String S3_QUOTA_BUCKET_SIZE = "s3:quota-bucketsize";
  public static final String S3_QUOTA_BUCKET_OBJECT_NUMBER = "s3:quota-bucketobjectnumber";

  //FAKES!
  public static final String S3_QUOTA_BUCKET_NUMBER_FAKE = "s3:quota-bucketnumber";
  public static final String S3_QUOTA_BUCKET_TOTAL_SIZE_FAKE = "s3:quota-buckettotalsize";
  public static final String S3_QUOTA_BUCKET_SIZE_FAKE = "s3:quota-bucketsize";
  public static final String S3_QUOTA_BUCKET_OBJECT_NUMBER_FAKE = "s3:quota-bucketobjectnumber";

  private static final Map<String, Class<? extends Key>> KEY_MAP = Maps.newConcurrentMap( );
  private static final Map<String, KeyProvider> KEY_PROVIDER_MAP = Maps.newConcurrentMap( );

  /**
   * Get a key using all registered providers in priority order
   */
  public static Key getKeyByName( final String name ) {
    final List<KeyProvider> providers = Lists.newArrayList( KEY_PROVIDER_MAP.values( ) );
    Collections.sort( providers, Ordered.comparator( ) );
    for ( final KeyProvider provider : providers ) {
      if ( provider.provides( name ) ) {
        return provider.getKey( name );
      }
    }
    return null;
  }

  static Key getKeyInstance( Class<? extends Key> keyClass ) {
    try {
      Key key = keyClass.newInstance( );
      return key;
    } catch ( IllegalAccessException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( InstantiationException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( ExceptionInInitializerError e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( SecurityException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    }
  }
  
  static Class<? extends Key> getKeyClass( String name ) {
    return KEY_MAP.get( name );
  }

  public static Map<String,Key> getKeyInstances( final EvaluationConstraint constraint ) {
    final Map<String, Key> keyInstances = Maps.newHashMap( );
    final List<KeyProvider> providers = Lists.newArrayList( KEY_PROVIDER_MAP.values( ) );
    Collections.sort( providers, Ordered.comparator( ) );
    for ( final KeyProvider provider : providers ) {
      keyInstances.putAll( provider.getKeyInstances( constraint ) );
    }
    return keyInstances;
  }

  static Map<String,Key> getRegisteredKeyInstances( final EvaluationConstraint constraint ) {
    return Maps.transformValues(
        Maps.filterValues(
            KEY_MAP,
            CollectionUtils.propertyContainsPredicate(
                constraint,
                Functions.compose( PolicyKeyToEvaluationConstraints.INSTANCE, KeyClassToPolicyKey.INSTANCE ) )
        ),
        KeyClassToKeyInstance.INSTANCE
    );
  }

  public synchronized static boolean registerKey( String keyName, Class<? extends Key> keyClass ) {
    if ( KEY_MAP.containsKey( keyName ) ) {
      return false;
    }
    KEY_MAP.put( keyName, keyClass );
    return true;
  }

  public synchronized static boolean registerKeyProvider( KeyProvider provider ) {
    if ( KEY_PROVIDER_MAP.containsKey( provider.getName( ) ) ) {
      return false;
    }
    KEY_PROVIDER_MAP.put( provider.getName( ), provider );
    return true;
  }

  public static Function<Key,String> value( ) {
    return KeyToKeyValue.INSTANCE;
  }

  enum KeyToKeyValue implements Function<Key,String> {
    INSTANCE;

    @Override
    public String apply( final Key key ) {
      try {
        return key.value();
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  enum KeyClassToKeyInstance implements Function<Class<? extends Key>,Key> {
    INSTANCE;

    @Override
    public Key apply( final Class<? extends Key> keyClass ) {
      return getKeyInstance( keyClass );
    }
  }

  enum KeyClassToPolicyKey implements Function<Class<? extends Key>,PolicyKey> {
    INSTANCE;

    @Override
    public PolicyKey apply( final Class<? extends Key> keyClass ) {
      return Ats.from( keyClass ).get( PolicyKey.class );
    }
  }

  enum PolicyKeyToEvaluationConstraints implements Function<PolicyKey,List<EvaluationConstraint>> {
    INSTANCE;

    @Override
    public List<EvaluationConstraint> apply( final PolicyKey policyKey ) {
      return Arrays.asList( policyKey.evaluationConstraints() );
    }
  }
}
