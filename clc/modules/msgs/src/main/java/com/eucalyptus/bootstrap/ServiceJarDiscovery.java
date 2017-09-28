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

package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 *
 */
public abstract class ServiceJarDiscovery implements Comparable<ServiceJarDiscovery> {
  private static Logger                         LOG       = Logger.getLogger( ServiceJarDiscovery.class );
  private static SortedSet<ServiceJarDiscovery> discovery = Sets.newTreeSet( );
  private static Multimap<Class, String>        classList = ArrayListMultimap.create( );

  public static void doSingleDiscovery( final ServiceJarDiscovery s ) {
    processClasspath( );
    ServiceJarDiscovery.runDiscovery( s );
  }

  private static void checkUniqueness( final Class c ) {
    if ( classList.get( c ).size( ) > 1 ) {

      LOG.fatal( "Duplicate bootstrap class registration: " + c.getName( ) );
      for ( final String fileName : classList.get( c ) ) {
        LOG.fatal( "\n==> Defined in: " + fileName );
      }
      System.exit( 1 );//GRZE: special case, broken installation
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  public static void runDiscovery( ) {
    for ( final ServiceJarDiscovery s : discovery ) {
      EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_DISCOVERY, s.getClass( ).getCanonicalName( ) ).trace( );
    }
    discovery.forEach( ServiceJarDiscovery::runDiscovery );
  }

  public static void runDiscovery( final ServiceJarDiscovery s ) {
    LOG.info( LogUtil.subheader( s.getClass( ).getSimpleName( ) ) );
    for ( final Class c : classList.keySet( ) ) {
      try {
        s.checkClass( c );
      } catch ( final Throwable t ) {
        LOG.debug( t, t );
      }
    }
  }

  private void checkClass( final Class candidate ) {
    try {
      if ( this.processClass( candidate ) ) {
        ServiceJarDiscovery.checkUniqueness( candidate );
        EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_LOADED_ENTRY, this.getClass( ).getSimpleName( ), candidate.getName( ) ).trace( );
      }
    } catch ( final Throwable e ) {
      if ( !(e instanceof InstantiationException) ) {
        LOG.trace( e, e );
      }
    }
  }

  /**
   * Process the potential bootstrap-related class. Return false or throw an exception if the class
   * is rejected.
   *
   * @param candidate The candidate class
   * @return true if the candidate is accepted.
   */
  public abstract boolean processClass( Class candidate ) throws Exception;

  @SuppressWarnings( "WeakerAccess" )
  public Double getDistinctPriority( ) {
    return this.getPriority( ) + ( .1d / this.getClass( ).hashCode( ) );
  }

  public abstract Double getPriority( );

  @Override
  public int compareTo( @Nonnull final ServiceJarDiscovery that ) {
    return this.getDistinctPriority( ).compareTo( that.getDistinctPriority( ) );
  }

  public static void processLibraries( ) {
    processClasspath( );
  }

  private static void processClasspath( ) {
    if ( classList.isEmpty( ) ) {
      Stream.of( "com.eucalyptus", "edu.ucsb.eucalyptus" ).forEach( ServiceJarDiscovery::processPackage );
    }
  }

  private static void processPackage( final String packageRoot ) {
    final ComponentClassScanner classScanner = new ComponentClassScanner();
    classScanner.addIncludeFilter(new AbstractClassTestingTypeFilter(){
      @Override
      protected boolean match( final ClassMetadata metadata ) {
        return metadata.isIndependent( );
      }
    });
    for ( final Class<?> clazz : classScanner.getComponentClasses(packageRoot) ) {
      classList.put( clazz, null );
    }

    final ComponentClassScanner scanner = new ComponentClassScanner();
    scanner.addIncludeFilter(new AssignableTypeFilter(ServiceJarDiscovery.class));
    scanner.addIncludeFilter(new AndTypeFilter(
        new AssignableTypeFilter(Predicate.class),
        new AnnotationTypeFilter(Bootstrap.Discovery.class)));

    final Collection<Class<?>> classes = scanner.getComponentClasses(packageRoot);
    for ( final Class<?> clazz : classes ) {
      if ( ServiceJarDiscovery.class.equals(clazz) ) continue;
      if ( ServiceJarDiscovery.class.isAssignableFrom( clazz ) ) {
        try {
          discovery.add( ServiceJarDiscovery.class.cast( clazz.newInstance() ) );
        } catch (InstantiationException e) {
          System.err.println( clazz.getName() );
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      } else { // must be a Discovery/Predicate
        discovery.add( fromDiscovery( clazz ) );
      }
    }
  }

  private static class ComponentClassScanner extends ClassPathScanningCandidateComponentProvider {

    private ComponentClassScanner() {
      super(false);
      setResourceLoader( jibxSafe( getResourceLoader( ) ) );
    }

    private static ResourceLoader jibxSafe( final ResourceLoader resourceLoader ) {
      return new DelegatingResourcePatternResolver( (ResourcePatternResolver) resourceLoader ) {
        @Override
        public Resource[] getResources( final String locationPattern ) throws IOException {
          return Stream.of( super.getResources( locationPattern ) )
              .filter( resource -> resource.getFilename( ) == null || !resource.getFilename( ).startsWith( "JiBX_" ) )
              .toArray( Resource[]::new );
        }
      };
    }

    @Override
    protected boolean isCandidateComponent( final AnnotatedBeanDefinition beanDefinition ) {
      final AnnotationMetadata metadata = beanDefinition.getMetadata();
      return metadata.isIndependent( );
    }

    Collection<Class<?>> getComponentClasses( String basePackage) {
      basePackage = basePackage == null ? "" : basePackage;
      List<Class<?>> classes = new ArrayList<>();
      for (BeanDefinition candidate : findCandidateComponents(basePackage)) {
        try {
          Class cls = ClassUtils.resolveClassName( candidate.getBeanClassName(),
              ClassUtils.getDefaultClassLoader() );
          classes.add(cls);
        } catch (Throwable e) {
          LOG.debug( e, e );
        }
      }
      return classes;
    }
  }


  private static final class AndTypeFilter implements TypeFilter {
    final TypeFilter[] filters;
    private AndTypeFilter( final TypeFilter... filters ) {
      this.filters = filters;
    }

    @Override
    public boolean match( MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
      boolean match = true;

      for ( final TypeFilter filter : filters ) {
        if ( !filter.match( metadataReader, metadataReaderFactory ) ) {
          match = false;
          break;
        }
      }

      return match;
    }
  }

  private static ServiceJarDiscovery fromDiscovery( final Class candidate ) {
    //noinspection Guava,unchecked
    return new ServiceJarDiscovery( ) {
      final Bootstrap.Discovery annote = Ats.from( candidate ).get( Bootstrap.Discovery.class );
      final Predicate<Class> instance = ( Predicate<Class> ) Classes.builder( candidate ).newInstance( );
      @Override
      public boolean processClass( Class discoveryCandidate ) throws Exception {
        @SuppressWarnings( "StaticPseudoFunctionalStyleMethod" )
        boolean classFiltered =
            this.annote.value( ).length == 0 || Iterables.any( Arrays.asList( this.annote.value( ) ), Classes.assignableTo( discoveryCandidate ) );
        if ( classFiltered ) {
          @SuppressWarnings( "unchecked" )
          boolean annotationFiltered =
              this.annote.annotations( ).length == 0 || Iterables.any( Lists.<Class<? extends Annotation>>newArrayList( this.annote.annotations( ) ), Ats.from( discoveryCandidate ) );
          return annotationFiltered && this.instance.apply( discoveryCandidate );
        } else {
          return false;
        }
      }

      @Override
      public Double getPriority( ) {
        return this.annote.priority( );
      }
    };
  }
}
