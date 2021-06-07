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

package com.eucalyptus.storage.config;

import java.util.concurrent.TimeUnit;

import com.eucalyptus.util.Exceptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created by zhill on 5/5/14.
 *
 * A repository of configuration entities to be updated periodically and to get the current cached value
 */
public class ConfigurationCache {
  private static final long ENTRY_EXPIRATION_TIME_SEC = 5l; // 5 seconds for each entry's valid period

  protected static final LoadingCache<Class<? extends CacheableConfiguration>, CacheableConfiguration> configCache = CacheBuilder.newBuilder()
      .refreshAfterWrite(ENTRY_EXPIRATION_TIME_SEC, TimeUnit.SECONDS)
      .build(new CacheLoader<Class<? extends CacheableConfiguration>, CacheableConfiguration>() {
        @Override
        public CacheableConfiguration load(Class<? extends CacheableConfiguration> aClass) throws Exception {
          return (CacheableConfiguration) (aClass.newInstance()).getLatest();
        }
      });

  @SuppressWarnings( "unchecked" )
  public static <T extends CacheableConfiguration> T getConfiguration( Class<T> configType) {
    try {
      return (T) configCache.get(configType);
    } catch (Throwable f) {
      throw Exceptions.toUndeclared("No configuration entry found for type " + configType.getName(), f);
    }
  }
}
