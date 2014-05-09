package com.eucalyptus.imaging;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;

public class Imager extends UserMetadata<Imager.STATE> implements ImagingMetadata{
	public enum STATE {
		available
	}

	@Override
	public String getPartition( ) {
		return ComponentIds.lookup( Eucalyptus.class ).name( );
	}
	  
	@Override
	public FullName getFullName( ) {
		return FullName.create.vendor( "euca" )
                       .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                       .namespace( this.getOwnerAccountNumber( ) )
                       .relativeId( "imager", this.getDisplayName( ) );
	}
}
