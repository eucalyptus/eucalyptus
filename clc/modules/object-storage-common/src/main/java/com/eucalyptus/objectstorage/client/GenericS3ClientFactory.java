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

package com.eucalyptus.objectstorage.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.sf.hajdbc.util.SystemProperties;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A convenience wrapper for an AWS Java SDK S3 Client that sets default
 * timeouts etc, options, etc for operating against the ObjectStorage service
 * in Eucalyptus. All internal system uses of ObjectStorage should use this
 * or an extension of it rather than internal dispatch mechanisms.
 */
public class GenericS3ClientFactory {
    private static final Logger LOG = Logger.getLogger(GenericS3ClientFactory.class);

    /*
    S3 SDK Client config defaults for Euca
     */
    private static final int CONNECTION_TIMEOUT_MS = 10 * 1000; //10 sec timeout
    private static final int DEFAULT_MAX_CONNECTIONS = 64; //64 http connections. default for SDK is 50
    private static final int DEFAULT_SOCKET_READ_TIMEOUT_MS = 30 * 1000; //30 second socket read timeout. default is 50s
    private static final int DEFAULT_MAX_ERROR_RETRY = 3; //3 retries on a failure
    private static final int DEFAULT_BUFFER_SIZE = 512 * 1024; //512KB.
    private static final Random randomizer = new Random(System.currentTimeMillis());

    static {
        System.setProperty(SDKGlobalConfiguration.DISABLE_REMOTE_REGIONS_FILE_SYSTEM_PROPERTY, "disable"); //anything non-null disables it
        System.setProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE, String.valueOf(DEFAULT_BUFFER_SIZE));  //512KB upload buffer, to handle most small objects
        System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", "disable"); //disable etag validation on GETs
    }

    /**
     * A credentials provider that will get the first active access key for the given user.
     * A refresh may use the same credentials or new ones if the set of active keys has changed
     */
    public static class EucaUserCredentialsProvider implements AWSCredentialsProvider {
        private User eucaUser;
        private AWSCredentials currentCredential;

        public EucaUserCredentialsProvider(User user) {
            this.eucaUser = user;
        }

        @Override
        public synchronized AWSCredentials getCredentials() {
            if(currentCredential == null) {
                updateCreds();
            }
            return currentCredential;
        }

        protected void updateCreds() {
            try {
                AccessKey userAccessKey = Iterables.find(eucaUser.getKeys(), new Predicate<AccessKey>() {

                    @Override
                    public boolean apply(@Nullable AccessKey accessKey) {
                        return accessKey != null && accessKey.isActive();
                    }
                });
                currentCredential = new BasicAWSCredentials(userAccessKey.getAccessKey(), userAccessKey.getSecretKey());
            } catch(AuthException e) {
                throw new RuntimeException("No active credentials for the user");
            }
        }

        @Override
        public synchronized void refresh() {
            updateCreds();
        }
    }

    public static AWSCredentialsProvider getEucaUserAWSCredentialsProvider(User user) {
        return new EucaUserCredentialsProvider(user);
    }


        /**
         * Uses the first key found for the given user to construct an s3 client
         * @param clientUser
         * @param useHttps
         * @return
         * @throws AuthException if user has no access keys active
         * @throws java.util.NoSuchElementException if no OSG found ENABLED
         */
    protected static AmazonS3Client getS3ClientForUser(final User clientUser, boolean useHttps) throws AuthException, NoSuchElementException {
        try {
            return getS3Client(getEucaUserAWSCredentialsProvider(clientUser), useHttps);
        } catch(Exception e) {
            LOG.error("Could not generate s3 client for user " + clientUser.getUserId() + " because no active access keys found.", e);
            throw new AuthException("No active access keys found for user", e);
        }
    }

    protected static ClientConfiguration getDefaultConfiguration(boolean withHttps) {
        ClientConfiguration config = new ClientConfiguration();
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
        config.setMaxErrorRetry(DEFAULT_MAX_ERROR_RETRY);
        config.setUseReaper(true);
        config.setSocketTimeout(DEFAULT_SOCKET_READ_TIMEOUT_MS);
        config.setProtocol(withHttps ? Protocol.HTTPS : Protocol.HTTP);
        return config;
    }

    public static S3ClientOptions getDefaultClientOptions() {
        S3ClientOptions ops = new S3ClientOptions();
        ops.setPathStyleAccess(true);
        return ops;
    }


    public static AmazonS3Client getS3Client(AWSCredentialsProvider provider, boolean https) throws NoSuchElementException {
        ClientConfiguration config = getDefaultConfiguration(https);
        AmazonS3Client s3Client = new AmazonS3Client(provider, config);
        s3Client.setS3ClientOptions(getDefaultClientOptions());
        s3Client.setEndpoint(getRandomOSGUri().toString());
        return s3Client;
    }

    /**
     * Returns a configured S3 client for the specified set of credentials.
     * @param credentials
     * @param https
     * @return
     * @throws NoSuchElementException if no ENABLED OSG found
     */
    public static AmazonS3Client getS3Client(AWSCredentials credentials, boolean https) throws NoSuchElementException {
        ClientConfiguration config = getDefaultConfiguration(https);
        AmazonS3Client s3Client = new AmazonS3Client(credentials, config);
        s3Client.setS3ClientOptions(getDefaultClientOptions());
        s3Client.setEndpoint(getRandomOSGUri().toString());
        return s3Client;
    }

    protected static URI getRandomOSGUri() throws NoSuchElementException {
        //Get a random OSG endpoint
        List<ServiceConfiguration> osgs = Lists.newArrayList(Topology.lookupMany(ObjectStorage.class));
        if(osgs == null || osgs.size() == 0) {
            throw new NoSuchElementException("No ENABLED OSGs found. Cannot generate client with no set endpoint");
        } else {
            int osgIndex = randomizer.nextInt(osgs.size());
            LOG.trace("Using osg index " + osgIndex + " from list: " + osgs);
            return ServiceUris.remote(osgs.get(osgIndex));
        }
    }
}