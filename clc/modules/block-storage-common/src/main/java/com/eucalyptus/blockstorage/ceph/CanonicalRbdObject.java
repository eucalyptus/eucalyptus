/*************************************************************************
 * Copyright 2016-2017 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.ceph;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.google.common.base.Strings;

public class CanonicalRbdObject {
  private static final Logger LOG = Logger.getLogger(CanonicalRbdObject.class);

  private final String pool;
  private final String image;
  private final String snapshot;

  public String getPool() {
    return pool;
  }

  public CanonicalRbdObject withPool(String pool) {
    return new CanonicalRbdObject( pool, this.image, this.snapshot );
  }

  public String getImage() {
    return image;
  }

  public CanonicalRbdObject withImage(String image) {
    return new CanonicalRbdObject( this.pool, image, this.snapshot );
  }

  public String getSnapshot() {
    return snapshot;
  }

  public CanonicalRbdObject withSnapshot(String snapshot) {
    return new CanonicalRbdObject( this.pool, this.image, snapshot );
  }

  public CanonicalRbdObject( ) {
    this( null, null, null );
  }

  public CanonicalRbdObject( final String pool, final String image, final String snapshot ) {
    this.pool = pool;
    this.image = image;
    this.snapshot = snapshot;
  }

  @Override
  public String toString() {
    return "CanonicalRbdObject [pool=" + pool + ", image=" + image + ", snapshot=" + snapshot + "]";
  }

  public String toCanonicalString() {
    String canonicalId = new String();

    if (!Strings.isNullOrEmpty(this.pool))
      canonicalId = this.pool;
    if (!Strings.isNullOrEmpty(this.image))
      canonicalId += "/" + this.image;
    if (!Strings.isNullOrEmpty(this.snapshot))
      canonicalId += "@" + this.snapshot;

    return canonicalId;
  }

  public static CanonicalRbdObject parse(String canonicalId) {
    try {
      if (!Strings.isNullOrEmpty(canonicalId)) {
        // iqn may be of the form pool/image,pool/image if the snapshot was uploaded pre 4.4. It is always of the form pool/image in 4.4
        String newCanonicalId = canonicalId.contains(",") ? canonicalId.substring(0, canonicalId.indexOf(',')) : canonicalId;

        List<String> poolSplit = CephRbdInfo.POOL_IMAGE_SPLITTER.splitToList(newCanonicalId);
        if (poolSplit != null && poolSplit.size() == 2) {
          CanonicalRbdObject resource = new CanonicalRbdObject().withPool(poolSplit.get(0));
          if (poolSplit.get(1).contains(CephRbdInfo.IMAGE_SNAPSHOT_DELIMITER)) {
            List<String> imageSnapshotSplit = CephRbdInfo.IMAGE_SNAPSHOT_SPLITTER.splitToList(poolSplit.get(1));
            if (imageSnapshotSplit != null && imageSnapshotSplit.size() == 2) {
              return resource.withImage(imageSnapshotSplit.get(0)).withSnapshot(imageSnapshotSplit.get(1));
            } else {
              LOG.warn("Expected single occurence of @ in canonical ID " + newCanonicalId);
              return null;
            }
          } else {
            return resource.withImage(poolSplit.get(1));
          }
        } else {
          LOG.warn("Expected single occurence of / in canonical ID " + newCanonicalId);
          return null;
        }
      } else {
        // Caller may pass in a null ID which may be OK.
        // If not, errors will be handled at a higher level.
        LOG.debug("Invalid canonical ID " + canonicalId);
        return null;
      }
    } catch (Exception e) {
      LOG.warn("Failed to parse " + canonicalId, e);
      return null;
    }
  }
}
