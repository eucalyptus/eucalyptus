package com.eucalyptus.ws.client;

import org.mule.api.MessagingException;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class NioMessageAdapter extends AbstractMessageAdapter {

  private Object payload;

  public NioMessageAdapter( Object message ) throws MessagingException
  {
    if ( message instanceof EucalyptusMessage )
    {
      this.payload = ( EucalyptusMessage ) message;
    }
    else
    {
      throw new MessageTypeNotSupportedException( message, getClass() );
    }
  }

  public String getPayloadAsString( String encoding ) throws Exception
  {
    return this.payload.toString();
  }

  public byte[] getPayloadAsBytes() throws Exception
  {
    return this.payload.toString().getBytes();
  }

  public Object getPayload()
  {
    return payload;
  }

}
