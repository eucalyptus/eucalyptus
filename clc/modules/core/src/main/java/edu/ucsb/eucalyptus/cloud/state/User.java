package edu.ucsb.eucalyptus.cloud.state;

import java.util.concurrent.*;

public class User {

  private String userName;
  private ConcurrentMap<Class, AssetMap> maps;

  public User() {
    this.maps = new ConcurrentSkipListMap<Class, AssetMap>();
  }

  @SuppressWarnings( "unchecked" )
  private <TYPE> AssetMap<TYPE> getAssetMap( Class<TYPE> c ) {
    if ( !maps.containsKey( c ) ) {
      maps.putIfAbsent( c, new AssetMap() );
    }
    return ( AssetMap<TYPE> ) maps.get( c );
  }

  public <TYPE> TYPE getAsset( Class<TYPE> c, String key ) throws DoesNotExistException {
    AssetMap<TYPE> instMap = this.getAssetMap( c );
    try {
      return instMap.lookup( key );
    } catch ( DoesNotExistException e ) {
      throw new DoesNotExistException( getErrorMessage( c, key ) );
    }
  }

  private String getErrorMessage( final Class c, final String key ) {
    return String.format( "%s->%s->%s", this.getUserName(), c.getSimpleName(), key );
  }

  public <TYPE> void add( Class<TYPE> c, String key, TYPE newValue ) throws AlreadyExistsException {
    AssetMap<TYPE> instMap = this.getAssetMap( c );
    if ( instMap.putIfAbsent( key, newValue ) != null ) {
      throw new AlreadyExistsException( this.getErrorMessage( c, key ) + "=" + instMap.get( key ) );
    }
  }

  public String getUserName() {
    return userName;
  }

  class AssetMap<ASSET_TYPE> extends ConcurrentSkipListMap<String, ASSET_TYPE> {
    private ConcurrentMap<String, ASSET_TYPE> assets;

    AssetMap() {
      this.assets = new ConcurrentSkipListMap<String, ASSET_TYPE>();
    }

    public ASSET_TYPE lookup( String key ) throws DoesNotExistException {
      if ( !assets.containsKey( key ) ) {
        throw new DoesNotExistException( key );
      }
      return this.assets.get( key );
    }
  }

}
