/*
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 */

package com.eucalyptus.storage.config;

import com.eucalyptus.util.Exceptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhill on 5/5/14.
 *
 * A repository of configuration entities to be updated periodically and to get the current cached value
 */
public class ConfigurationCache {
    private static final long ENTRY_EXPIRATION_TIME_SEC = 5l; //5 seconds for each entry's valid period

    protected static final LoadingCache<Class<? extends CacheableConfiguration>, CacheableConfiguration> configCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(ENTRY_EXPIRATION_TIME_SEC, TimeUnit.SECONDS)
            .build(new CacheLoader<Class<? extends CacheableConfiguration>, CacheableConfiguration>() {
                @Override
                public CacheableConfiguration load(Class<? extends CacheableConfiguration> aClass) throws Exception {
                    return (CacheableConfiguration) (aClass.newInstance()).getLatest();
                }
            });

    public static <T extends CacheableConfiguration> T getConfiguration(Class<T> configType) {
        try {
            return (T)configCache.get(configType);
        } catch(Throwable f) {
            throw Exceptions.toUndeclared("No configuration entry found for type " + configType.getName(), f);
        }
    }
}
