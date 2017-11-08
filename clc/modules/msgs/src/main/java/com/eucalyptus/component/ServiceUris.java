/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.StackConfiguration.BasicTransport;
import com.eucalyptus.ws.TransportDefinition;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.typeCompatibleWith;

public class ServiceUris {
  
  public static class UriParserBuilder {
    enum Lexemes {
      SCHEME {
        @Override
        String format( Object... args ) {
          checkParam( args, arrayWithSize( 1 ) );
          return "" + args[0];
        }
      },
      HOST {
        @Override
        String format( Object... args ) {
          checkParam( args, arrayWithSize( 1 ) );
          checkParam( args[0].getClass(), typeCompatibleWith( InetAddress.class ) );
          return "" + ( ( InetAddress ) args[0] ).getCanonicalHostName( );
        }
      },
      PORT {
        @Override
        String format( Object... args ) {
          checkParam( args, arrayWithSize( 1 ) );
          checkParam( args[0].getClass(), typeCompatibleWith( Integer.class ) );
          return COLON.format( ) + args[0];
        }
      },
      SERVICEPATH {
        @Override
        String format( Object... args ) {
          checkParam( args, arrayWithSize( 1 ) );
          checkParam( args[0].getClass(), typeCompatibleWith( String.class ) );
          return SLASH.format( ) + args[0];
        }
      },
      QUERY {
        Function<Map.Entry<String, String>, String> transform = new Function<Map.Entry<String, String>, String>( ) {
                                                                
                                                                @Override
                                                                public String apply( Entry<String, String> input ) {
                                                                  return input.getKey( ) + EQUALS.format( ) + input.getValue( );
                                                                }
                                                              };
        
        @Override
        String format( Object... args ) {
          if ( args != null && args.length > 0 ) {
            checkParam( args[0].getClass(), typeCompatibleWith( Map.class ) );
            Map queryArgs = ( Map ) args[0];
            if ( !queryArgs.isEmpty( ) ) {
              Iterable argPairString = Iterables.transform( queryArgs.entrySet( ), transform );
              return Joiner.on( Lexemes.AMPERSAND.format( ) ).join( argPairString );
            } else {
              return null;
            }
          } else {
            return null;
          }
        }
      },
      FRAGMENT {
        @Override
        final String format( Object... args ) {
          return "#" + args[0];
        }
      },
      QUESTIONMARK {
        @Override
        final String format( Object... args ) {
          return "?";
        }
      },
      AMPERSAND {
        @Override
        final String format( Object... args ) {
          return "&";
        }
      },
      EQUALS {
        @Override
        final String format( Object... args ) {
          return "=";
        }
      },
      COLON {
        @Override
        final String format( Object... args ) {
          return ":";
        }
      },
      SLASH {
        @Override
        final String format( Object... args ) {
          return "/";
        }
      };
      abstract String format( Object... args );
    }
    
    private ComponentId         componentId;
    private TransportDefinition scheme;
    private InetAddress         address;
    private Integer             port;
    private String              path     = null;
    private Map<String, String> query    = Maps.newTreeMap( );
    private boolean             internal = false;
    private String              fragment;
    
    UriParserBuilder( ComponentId componentId ) {
      super( );
      this.componentId = componentId;
    }
    
    public UriParserBuilder scheme( TransportDefinition uriScheme ) {
      this.scheme = uriScheme;
      return this;
    }
    
    public UriParserBuilder host( InetAddress address ) {
      this.address = address;
      return this;
    }
    
    public UriParserBuilder port( Integer port ) {
      this.port = port;
      return this;
    }
    
    public UriParserBuilder path( String... path ) {
      this.path = ( path != null && path.length > 0
        ? "/" + Joiner.on( "/" ).join( path )
        : "/" ).replaceAll( "^//*", "/" );
      return this;
    }
    
    public UriParserBuilder query( Map<String, String> query ) {
      if ( query != null ) {
        this.query.putAll( query );
      }
      return this;
    }
    
    public UriParserBuilder fragment( String fragment ) {
      this.fragment = fragment;
      return this;
    }
    
    public UriParserBuilder internal( ) {
      this.internal = true;
      return this;
    }

    public URI get( ) {
      checkParam( this.address, notNullValue() );
      checkParam( this.path, notNullValue() );
      if ( this.scheme == null ) this.scheme = BasicTransport.HTTP;
      if ( this.port == null ) this.port = this.componentId.getPort( );
      if ( this.internal ) this.path = this.componentId.getInternalServicePath( this.path );
      String schemeString = StackConfiguration.DEFAULT_HTTPS_ENABLED
        ? this.scheme.getSecureScheme( )
        : this.scheme.getScheme( );
      // Use hostname if the component supports it and the address was created with a name.
      final String hostNameString = componentId.isUseServiceHostName( ) && !this.address.toString().startsWith( "/" ) ?
          this.address.getHostName( ) :
          this.address.getHostAddress( );
      try {
        URI u = new URI( schemeString, null, hostNameString, this.port, ( "/" + this.path ).replaceAll( "^//", "/" ), Lexemes.QUERY.format( this.query ), null );
        u.parseServerAuthority( );
        return u;
      } catch ( URISyntaxException e ) {
        throw new RuntimeException( "Failed to construct URI: " + this.toString( ) + " because of: " + e, e );
      }
    }
    
    public URI getPublicify( ) {
      checkParam( this.address, notNullValue() );
      checkParam( this.path, notNullValue() );
      if ( this.scheme == null ) this.scheme = BasicTransport.HTTP;
      if ( this.port == null ) this.port = this.componentId.getPort( );
      if ( this.internal ) this.path = this.componentId.getInternalServicePath( this.path );
      String schemeString = StackConfiguration.DEFAULT_HTTPS_ENABLED
        ? this.scheme.getSecureScheme( )
        : this.scheme.getScheme( );
      String hostNameString = this.componentId.isPublicService() ? (StackConfiguration.USE_DNS_DELEGATION
        ? this.componentId.name( ) + "." + StackConfiguration.lookupDnsDomain( )
        : this.address.getHostAddress( )) : this.address.getHostAddress();
      String pathString = this.componentId.isPublicService() ? (StackConfiguration.USE_DNS_DELEGATION
        ? "/" : "/" + this.path)
        : "/" + this.path; // no path if dns delegation is used and service is public
      try {
        URI u = new URI( schemeString, null, hostNameString, this.port, (pathString ).replaceAll( "^//", "/" ), Lexemes.QUERY.format( this.query ), null );
        u.parseServerAuthority( );
        return u;
      } catch ( URISyntaxException e ) {
        throw new RuntimeException( "Failed to construct URI: " + this.toString( ) + " because of: " + e, e );
      }
    }

    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      builder.append( "UriParserBuilder:" );
      if ( this.componentId != null ) builder.append( "componentId=" ).append( this.componentId.name( ) ).append( ":" );
      if ( this.scheme != null ) builder.append( "scheme=" ).append( this.scheme ).append( ":" );
      if ( this.address != null ) builder.append( "address=" ).append( this.address ).append( ":" );
      if ( this.port != null ) builder.append( "port=" ).append( this.port ).append( ":" );
      if ( this.path != null ) builder.append( "path=" ).append( this.path ).append( ":" );
      if ( this.query != null ) builder.append( "query=" ).append( this.query ).append( ":" );
      builder.append( "internal=" ).append( this.internal ).append( ":" );
      if ( this.fragment != null ) builder.append( "fragment=" ).append( this.fragment );
      return builder.toString( );
    }
    
  }
  
  public static URI internal( final Class<? extends ComponentId> idClass, final InetAddress host, String... pathParts ) {
    return internal( Components.lookup( idClass ), host, pathParts );
  }
  
  public static URI internal( ComponentId compId, String... pathParts ) {
    return internal( compId, Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI internal( Component comp, final InetAddress host, String... pathParts ) {
    return internal( comp.getComponentId( ), host, pathParts );
  }
  
  public static URI internal( final Class<? extends ComponentId> idClass, String... pathParts ) {
    return internal( Components.lookup( idClass ), Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI internal( Component comp, String... pathParts ) {
    return internal( comp.getComponentId( ), Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI internal( ServiceConfiguration config, String... pathParts ) {
    return internal( config.getComponentId( ), config.getInetAddress( ), config.getPort( ), pathParts );
  }
  
  public static URI internal( ComponentId compId, final InetAddress host, String... pathParts ) {
    return internal( compId, host, compId.getPort( ), pathParts );
  }
  
  public static URI internal( ComponentId compId, final InetAddress host, Integer port, String... pathParts ) {
    return makeInternal( compId, host, port, pathParts ).get( );
  }
  
  public static URI remote( final Class<? extends ComponentId> idClass, final InetAddress host, String... pathParts ) {
    return remote( Components.lookup( idClass ), host, pathParts );
  }
  
  public static URI remote( Component comp, final InetAddress host, String... pathParts ) {
    return remote( comp.getComponentId( ), host, pathParts );
  }
  
  public static URI remote( final Class<? extends ComponentId> idClass, String... pathParts ) {
    return remote( Components.lookup( idClass ), Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI remote( Component comp, String... pathParts ) {
    return remote( comp.getComponentId( ), Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI remote( ComponentId compId, String... pathParts ) {
    return remote( compId, Internets.localHostInetAddress( ), pathParts );
  }
  
  public static URI remote( ServiceConfiguration config, String... pathParts ) {
    return remote( config.getComponentId( ), config.getInetAddress( ), config.getPort( ), pathParts );
  }
  
  public static URI remote( ComponentId compId, final InetAddress host, String... pathParts ) {
    return remote( compId, host, compId.getPort( ), pathParts );
  }
  
  public static URI remote( ComponentId compId, final InetAddress host, Integer port, String... pathParts ) {
    return make( compId, host, port, pathParts ).query( compId.getServiceQueryParameters() ).get( );
  }

  public static URI remotePublicify( ServiceConfiguration config, String... pathParts ) {
	return make( config.getComponentId( ), config.getInetAddress( ), config.getPort( ), pathParts ).getPublicify( );
  }

  public static URI remotePublicify( final Class<? extends ComponentId> idClass, String... pathParts ) {
	Component comp = Components.lookup( idClass );
	return make( comp.getComponentId(), Internets.localHostInetAddress( ), comp.getComponentId().getPort(), pathParts ).getPublicify();
  }

  private static UriParserBuilder makeInternal( ComponentId compId, final InetAddress host, Integer port, String... pathParts ) {
    return new UriParserBuilder( compId ).scheme( compId.getTransports( ).iterator( ).next( ) ).host( host ).port( port ).path( compId.getInternalServicePath( pathParts ) );
  }

  private static UriParserBuilder make( ComponentId compId, final InetAddress host, Integer port, String... pathParts ) {
    return new UriParserBuilder( compId ).scheme( compId.getTransports( ).iterator( ).next( ) ).host( host ).port( port ).path( compId.getServicePath( pathParts ) );
  }
}
