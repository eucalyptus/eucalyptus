/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage;

import com.eucalyptus.objectstorage.metadata.BucketLifecycleManager;
import com.eucalyptus.objectstorage.metadata.DbBucketLifecycleManagerImpl;

/**
 * Manager factory for bucket lifecycle metadata handler. Returns an instance for the configured manager.
 *
 */
public class BucketLifecycleManagers {
  private static BucketLifecycleManager manager = new DbBucketLifecycleManagerImpl();
  private static BucketLifecycleManager mocked;

  public static BucketLifecycleManager getInstance() {
    if (mocked != null) {
      return mocked;
    } else {
      return manager;
    }
  }

  /**
   * this allows a mock BucketLifecycleManager to be injected (for unit testing purposes) visibility is package, so tests must live in the same
   * package
   *
   * @param mock
   */
  static void setInstance(BucketLifecycleManager mock) {
    mocked = mock;
  }

}
