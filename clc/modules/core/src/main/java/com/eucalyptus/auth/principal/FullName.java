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

package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Map;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

public interface FullName extends Serializable {
  
  public final static String EMPTY     = "";
  public final static String SEP_PATH  = "/";
  public final static String SEP       = ":";
  public final static String PREFIX    = "arn:aws";
  public final static String NOBODY_ID = "d00d";
  public final static String SYSTEM_ID = Integer.toString( 0xC0FFEE, 2 );
  
  public abstract String getUniqueId( );
  
  public abstract String getVendor( );
  
  public abstract String getRegion( );
  
  public abstract String getNamespace( );
  
  public abstract String getAuthority( );
  
  public abstract String getRelativeId( );
  
  public abstract String getPartition( );
  
  public abstract String toString( );
  
  public abstract int hashCode( );
  
  public abstract boolean equals( Object obj );
  
  public static final Function<String[], String> ASSEMBLE_PATH_PARTS = new Function<String[], String>( ) {
                                                                       @Override
                                                                       public String apply( String[] pathParts ) {
                                                                         StringBuilder rId = new StringBuilder( );
                                                                         for ( String pathPart : pathParts ) {
                                                                           rId.append( SEP_PATH.substring( 0, rId.length( ) == 0
                                                                             ? 0
                                                                             : 1 ) ).append( pathPart );
                                                                         }
                                                                         return rId.toString( );
                                                                       }
                                                                     };
  
  public class create {
    enum part {
      VENDOR, REGION, NAMESPACE, RELATIVEID, DONE
    }
    
    private Map<part, String>   partMap = Maps.newHashMap( );
    private part                current = part.VENDOR;
    private final StringBuilder buf;
    
    create( String name ) {
      this.buf = new StringBuilder( );
      this.buf.append( PREFIX ).append( SEP ).append( name ).append( SEP );
      this.current = part.REGION;
    }
    
    public static create vendor( String name ) {
      return new create( name );
    }
    
    public create service( String service ) {
      return new create( service );
    }
    
    public create region( String region ) {
      if ( this.current.ordinal( ) == part.REGION.ordinal( ) ) {
        this.buf.append( region ).append( SEP );
        this.current = part.NAMESPACE;
        this.partMap.put( part.REGION, region == null || region.length( ) == 0
          ? FullName.EMPTY
          : region );
      } else {
        throw new IllegalStateException( "Attempt to set region when the current part is: " + this.current );
      }
      return this;
    }
    
    public create accountId( String accountId ) {
      try {
        return namespace( accountId );
      } catch ( IllegalStateException ex ) {
        throw new IllegalStateException( "Attempt to set accountId when the current part is: " + this.current );
      }
    }
    
    public create namespace( String namespace ) {
      if ( this.current.ordinal( ) == part.NAMESPACE.ordinal( ) ) {
        this.buf.append( namespace ).append( SEP );
        this.current = part.RELATIVEID;
        this.partMap.put( part.NAMESPACE, namespace == null || namespace.length( ) == 0
          ? FullName.EMPTY
          : namespace );
      } else {
        throw new IllegalStateException( "Attempt to set namespace when the current part is: " + this.current );
      }
      return this;
    }
    
    public FullName end( ) {
      return relativeId( );
    }
    
    public FullName relativeId( String... relativePath ) {
      if ( this.current.ordinal( ) == part.RELATIVEID.ordinal( ) ) {
        StringBuilder rId = new StringBuilder( );
        for ( String s : relativePath ) {
          rId.append( s ).append( SEP_PATH );
        }
        this.partMap.put( part.DONE, this.buf.toString( ) );
        this.buf.append( rId.toString( ) );
        this.partMap.put( part.RELATIVEID, rId.toString( ) );
        this.current = part.DONE;
      } else {
        throw new IllegalStateException( "Attempt to set relative path when the current part is: " + this.current );
      }
      return new FullName( ) {
        @Override
        public String getUniqueId( ) {
          return this.getNamespace( );
        }
        
        @Override
        public String getVendor( ) {
          return create.this.partMap.get( part.VENDOR );
        }
        
        @Override
        public String getRegion( ) {
          return create.this.partMap.get( part.REGION );
        }
        
        @Override
        public String getNamespace( ) {
          return create.this.partMap.get( part.NAMESPACE );
        }
        
        @Override
        public String getAuthority( ) {
          return create.this.partMap.get( part.DONE );
        }
        
        @Override
        public String getRelativeId( ) {
          return create.this.partMap.get( part.RELATIVEID );
        }
        
        @Override
        public String getPartition( ) {
          return create.this.partMap.get( part.REGION );
        }
        
        @Override
        public String toString( ) {
          return create.this.buf.toString( );
        }
      };
    }
    
  }
  
}
