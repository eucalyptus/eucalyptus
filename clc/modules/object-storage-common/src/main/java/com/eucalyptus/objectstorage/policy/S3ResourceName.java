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

package com.eucalyptus.objectstorage.policy;

import java.util.Collection;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.google.common.collect.ImmutableList;

public class S3ResourceName extends Ern {

  private final String bucket;
  private final String object;

  public S3ResourceName( String account, String bucket, String object) {
    super( PolicySpec.VENDOR_S3, null, account );
    this.bucket = bucket;
    this.object = object;
  }

  public boolean isBucket() {
    if (this.object == null || "".equals(this.object)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(ARN_PREFIX).append(this.getService( )).append(":::").append(this.bucket);
    if (this.object != null) {
      sb.append(this.object);
    }
    return sb.toString();
  }

  public String getBucket() {
    return this.bucket;
  }

  public String getObject() {
    return this.object;
  }

  @Override
  public String getResourceType() {
    if (this.isBucket()) {
      return qualifiedName( PolicySpec.S3_RESOURCE_BUCKET );
    } else {
      return qualifiedName( PolicySpec.S3_RESOURCE_OBJECT );
    }
  }

  @Override
  public String getResourceName() {
    String resourceName = this.bucket;
    if (this.object != null) {
      resourceName += this.object;
    }
    return resourceName;
  }

  /**
   * The following ARN uses '*' to indicate all Amazon S3 resource (all bucket and objects in your account)
   *
   *   arn:aws:s3:::*
   *
   * Explode bucket * to match objects also.
   */
  @Nonnull
  @Override
  public Collection<Ern> explode() {
    if ( isBucket( ) && "*".equals( bucket ) ) {
      return ImmutableList.<Ern>of(
          this,
          new S3ResourceName( getAccount( ), "*", "/*" )
      );
    } else {
      return super.explode( );
    }
  }
}
