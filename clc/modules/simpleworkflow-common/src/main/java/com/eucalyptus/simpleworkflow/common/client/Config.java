/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.simpleworkflow.common.client;

import java.beans.Introspector;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter;
import com.amazonaws.services.simpleworkflow.flow.WorkerBase;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Pair;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.reflect.AbstractInvocationHandler;

/**
 *
 */
@SuppressWarnings( { "Guava", "WeakerAccess" } )
public class Config {
  private static final ObjectMapper mapper = new ObjectMapper( )
      .setPropertyNamingStrategy( PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE );
  static {
    mapper.addMixIn( ClientConfiguration.class, ClientConfigurationMixin.class );
  }
  private static final ObjectMapper workerObjectMapper = buildWorkerObjectMapper();
  private static final AtomicReference<String> clientConfig = new AtomicReference<>( "" );

  /**
   * Parse a JSON format string for AWS SDK for Java ClientConfiguration.
   *
   * @param text The configuration in JSON
   * @return The configuration object
   */
  public static ClientConfiguration buildConfiguration( final String text ) {
    try {
      // create configuration with default values that can be overwritten by given config string
      final ClientConfiguration config = new ClientConfiguration(  );
      config.setCacheResponseMetadata( false );
      config.setConnectionMaxIdleMillis( 45_000 );
      config.setConnectionTimeout( 10_000 );
      config.setMaxConnections( 100 );
      config.setSecureRandom( Crypto.getSecureRandomSupplier( ).get( ) );
      config.setUseThrottleRetries( false );
      return Strings.isNullOrEmpty( text ) ?
          config :
          mapper.readerForUpdating( config ).readValue( source( text ) );
    } catch ( final IOException e ) {
      throw new IllegalArgumentException( "Invalid configuration: " + e.getMessage( ), e );
    }
  }

  public static AmazonSimpleWorkflow buildClient( final Supplier<User> user ) throws AuthException {
    final AWSCredentialsProvider credentialsProvider = new SecurityTokenAWSCredentialsProvider( user );
    final AmazonSimpleWorkflowClient client = new EucaSimpleWorkflowClient(
        credentialsProvider,
        clientConfig.get( )
    );
    client.setEndpoint( ServiceUris.remote( Topology.lookup( SimpleWorkflow.class ) ).toString( ) );
    client.addRequestHandler( new RequestHandler2( ) {
      private volatile String endpointHost;
      private final Supplier<Boolean> failbackChecker = Suppliers.memoizeWithExpiration( () -> {
        final String currentEndpointHost = endpointHost;
        if ( currentEndpointHost!=null &&
            !Internets.testLocal( currentEndpointHost ) &&
            Topology.isEnabledLocally( SimpleWorkflow.class ) ) {
          resetEndpoint( );
        }
        return true;
      }, 15, TimeUnit.SECONDS );

      @Override
      public void afterResponse( final Request<?> request, final Response<?> response ) {
        // Check and failover (failback) to local swf service if available
        endpointHost = request.getEndpoint( ).getHost( );
        failbackChecker.get( );
      }

      @Override
      public void afterError( final Request<?> request, final Response<?> response, final Exception e ) {
        final String errorMessage = Strings.nullToEmpty( e.getMessage( ) );
        boolean resetEndpoint = false;
        if (  errorMessage.contains( "Response Code: 404" ) || errorMessage.contains( "Response Code: 503" ) ) {
          resetEndpoint = true;
        } else if ( Exceptions.isCausedBy( e, ConnectException.class ) ) {
          resetEndpoint = true;
        } else if ( Exceptions.isCausedBy( e, ConnectTimeoutException.class ) ) {
          resetEndpoint = true;
        } else if ( Exceptions.isCausedBy( e, NoHttpResponseException.class ) ) {
          resetEndpoint = true;
        }
        if ( resetEndpoint ) {
          resetEndpoint( );
        }

        if ( e instanceof AmazonServiceException ) {
          final int status = ( (AmazonServiceException) e ).getStatusCode( );
          if ( status == 403 ) {
            credentialsProvider.refresh( );
          }
        }
      }

      private void resetEndpoint( ) {
        try {
          client.setEndpoint( ServiceUris.remote( Topology.lookup( SimpleWorkflow.class ) ).toString( ) );
        } catch ( final Exception e ) {
          // retry on next failure or failback attempt
        }
      }
    } );
    return client;
  }

  public static WorkflowWorker buildWorkflowWorker(
      final Class<? extends ComponentId> componentIdClass,
      final AmazonSimpleWorkflow client,
      final String domain,
      final String taskList,
      final String text ) {

    final List<Class<?>> workflowImpl =  Lists.newArrayList(WorkflowRegistry.lookupWorkflows( componentIdClass ));
    return buildWorkflowWorker(workflowImpl.toArray(new Class<?>[workflowImpl.size()]),
        client, domain, taskList, text);
  }

  public static WorkflowWorker buildWorkflowWorker(
      final Class<?>[] workflowImpl,
      final AmazonSimpleWorkflow client,
      final String domain,
      final String taskList,
      final String text ) {
    final WorkflowWorker workflowWorker = new WorkflowWorker( client, domain, taskList);
    workflowWorker.setRegisterDomain( true );
    workflowWorker.setDefaultConverter( new JsonDataConverter( workerObjectMapper ) );
    configure( workflowWorker, text );

    Package workerPackage = null;
    for ( final Class<?> workflowImplementation : workflowImpl ) {
      try {
        if ( workerPackage == null ) {
          workerPackage = workflowImplementation.getPackage( );
        }
        workflowWorker.addWorkflowImplementationType( workflowImplementation );
      } catch ( InstantiationException | IllegalAccessException e ) {
        throw new IllegalArgumentException( "Invalid workflow implementation: " + workflowImplementation, e );
      }
    }

    perhapsSetExceptionHandler( workerPackage, "decision", workflowWorker );

    return workflowWorker;
  }

  public static ActivityWorker buildActivityWorker(
      final Class<? extends ComponentId> componentIdClass,
      final AmazonSimpleWorkflow client,
      final String domain,
      final String taskList,
      final String text ) {

    final List<Class<?>> activitiesImpl =
        Lists.newArrayList(WorkflowRegistry.lookupActivities( componentIdClass ));
    return buildActivityWorker(activitiesImpl.toArray(new Class<?>[activitiesImpl.size()]),
        client,  domain, taskList, text);
  }

  public static ActivityWorker buildActivityWorker(
      final Class<?>[] activitiesImpl,
      final AmazonSimpleWorkflow client,
      final String domain,
      final String taskList,
      final String text
      ) {
    final ActivityWorker activityWorker = configure( new ActivityWorker( client, domain, taskList), text );
    activityWorker.setDataConverter( new JsonDataConverter( workerObjectMapper ) );

    Package workerPackage = null;
    for ( final Class<?> activitiesImplementation : activitiesImpl ) {
      try {
        if ( workerPackage == null ) {
          workerPackage = activitiesImplementation.getPackage();
        }
        activityWorker.addActivitiesImplementation( activitiesImplementation.newInstance( ) );
      } catch ( InstantiationException | IllegalAccessException | NoSuchMethodException e ) {
        throw new IllegalArgumentException( "Invalid activities implementation: " + activitiesImplementation, e );
      }
    }

    perhapsSetExceptionHandler( workerPackage, "activity", activityWorker );

    return activityWorker;
  }

  private static <T extends WorkerBase> T configure( final T worker, final String text ) {
    try {
      return Strings.isNullOrEmpty( text ) ?
          worker :
          mapper.readerForUpdating( worker ).<T>readValue( source( text ) );
    } catch ( IOException e ) {
      throw new IllegalArgumentException( "Invalid configuration: " + e.getMessage( ), e );
    }
  }

  private static void perhapsSetExceptionHandler( final Package logPackage,
                                                  final String type,
                                                  final WorkerBase worker ) {
    if ( logPackage != null ) {
      final Logger logger = Logger.getLogger( logPackage.getName() );
      //noinspection Convert2Lambda
      worker.setUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
        @SuppressWarnings( { "ThrowableResultOfMethodCallIgnored", "ConstantConditions" } )
        @Override
        public void uncaughtException( final Thread t, final Throwable e ) {
          if ( Exceptions.isCausedBy( e, ConnectException.class ) ) {
            logger.warn( "Connection error (retrying) for " + type + " worker " + t.getName() + "/" + t.getId() );
          } else if ( Exceptions.isCausedBy( e, ConnectTimeoutException.class ) ) {
            logger.warn( "Connection timeout (retrying) for " + type + " worker " + t.getName() + "/" + t.getId() );
          } else if ( Exceptions.isCausedBy( e, NoHttpResponseException.class ) ) {
            logger.warn( "No response (retrying) for " + type + " worker " + t.getName() + "/" + t.getId() );
          } else if ( Exceptions.isCausedBy( e, AmazonServiceException.class ) &&
              403 == (Exceptions.findCause( e, AmazonServiceException.class )).getStatusCode( ) ) {
            logger.warn( "Authentication failure (retrying) for " + type + " worker " + t.getName() + "/" + t.getId() );
          } else {
            logger.error( "Error in " + type + " worker " + t.getName() + "/" + t.getId(), e );
          }
        }
      } );
    }
  }

  private static StringReader source( final String text ) {
    return new StringReader( text ) {
      @Override public String toString( ) { return "property"; } // overridden for better source in error message
    };
  }

  public static final class NameValidatingChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue == null || !SimpleWorkflowMessage.FieldRegexValue.NAME_256.pattern( ).matcher( newValue.toString( ) ).matches( ) ) {
        throw new ConfigurablePropertyException( "Value length must be 1 - 256 characters" );
      }
    }
  }

  public static final class ClientConfigurationValidatingChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !newValue.toString( ).trim( ).isEmpty( ) ) try {
        Config.buildConfiguration( newValue.toString( ).trim( ) );
      } catch ( final IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
      clientConfig.set( Objects.toString( newValue, "" ).trim( ) );
    }
  }

  public static final class ActivityWorkerConfigurationValidatingChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !newValue.toString( ).trim( ).isEmpty( ) ) try {
        // dummy values used for validating JSON configuration
        Config.buildActivityWorker( SimpleWorkflow.class, client( ), "domain", "task-list", newValue.toString( ).trim( ) );
      } catch ( final IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static final class WorkflowWorkerConfigurationValidatingChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !newValue.toString( ).trim( ).isEmpty( ) ) try {
        // dummy values used for validating JSON configuration
        Config.buildWorkflowWorker( SimpleWorkflow.class, client( ), "domain", "task-list", newValue.toString( ).trim( ) );
      } catch ( final IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  /**
   * Extension of AmazonSimpleWorkflowClient to use a shared HTTP client.
   */
  private static final class EucaSimpleWorkflowClient extends AmazonSimpleWorkflowClient {
    private static final AtomicReference<Pair<String,EucaHttpClient>> clientPairRef = new AtomicReference<>( );

    EucaSimpleWorkflowClient(
        final AWSCredentialsProvider credentialsProvider,
        final String config
    ) {
      super( credentialsProvider, buildConfiguration( config ) );
      this.client.shutdown( );
      final Pair<String,EucaHttpClient> clientPair = clientPairRef.get( );
      if ( clientPair != null && clientPair.getLeft( ).equals( config ) ) {
        this.client = clientPair.getRight( ).ref( );
      } else {
        final EucaHttpClient eucaClient = new EucaHttpClient( clientConfiguration ).ref( );
        if ( clientPairRef.compareAndSet( clientPair, Pair.of( config, eucaClient ) ) ) {
          // unref/ref for atomic reference
          eucaClient.ref( );
          if ( clientPair != null ) {
            clientPair.getRight( ).unref( );
          }
        }
        this.client = eucaClient;
      }
    }
  }

  /**
   * Extension of AmazonHttpClient with reference tracking
   */
  private static final class EucaHttpClient extends AmazonHttpClient {
    private final AtomicInteger refs = new AtomicInteger( 0 );

    EucaHttpClient(
        final ClientConfiguration configuration
    ) {
      super( configuration, null, true, false );
    }

    public EucaHttpClient ref( ) {
      refs.incrementAndGet( );
      return this;
    }

    public EucaHttpClient unref( ) {
      if ( refs.decrementAndGet( ) <= 0 ) {
        super.shutdown( );
      }
      return this;
    }

    @Override
    public void shutdown( ) {
      unref( );
    }
  }

  private static AmazonSimpleWorkflow client( ) {
    return (AmazonSimpleWorkflow) Proxy.newProxyInstance( AmazonSimpleWorkflow.class.getClassLoader(), new Class<?>[]{ AmazonSimpleWorkflow.class }, new AbstractInvocationHandler() {
      @Override
      protected Object handleInvocation(
          @Nonnull final Object o,
          @Nonnull final Method method,
          @Nonnull final Object[] objects
      ) throws Throwable {
        throw new Exception( "dummy-client" );
      }
    } );
  }

  private static ObjectMapper buildWorkerObjectMapper( ) {
    final ObjectMapper workerObjectMapper = new ObjectMapper(  );
    workerObjectMapper.setAnnotationIntrospector(
        new JacksonAnnotationIntrospector( ) {
          private static final long serialVersionUID = 1L;
          @Override
          public boolean hasIgnoreMarker( final AnnotatedMember m ) {
            return isMethodBackedByTransientField( m ) || super.hasIgnoreMarker( m );
          }
        }
    );
    workerObjectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
    workerObjectMapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
    workerObjectMapper.enableDefaultTyping( ObjectMapper.DefaultTyping.NON_FINAL );
    return workerObjectMapper;
  }

  private static boolean isMethodBackedByTransientField( final AnnotatedMember m ) {
    boolean isMethodBackedByTransientField = false;
    if (m instanceof AnnotatedMethod ) {
      final String fieldName = Introspector.decapitalize( com.eucalyptus.util.Strings.trimPrefix( "get", m.getName() ) );
      for (final Field field : m.getMember().getDeclaringClass().getDeclaredFields()) {
        if (fieldName.equals(field.getName())) {
          isMethodBackedByTransientField = Modifier.isTransient(field.getModifiers());
          break;
        }
      }
    }
    return isMethodBackedByTransientField;
  }

  @SuppressWarnings( "unused" )
  private interface ClientConfigurationMixin {
    @JsonIgnore SecureRandom getSecureRandom();
    @JsonIgnore void setSecureRandom(SecureRandom secureRandom);
  }
}
