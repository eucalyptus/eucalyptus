package com.eucalyptus.ws.client;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;


public interface Client {

  EucalyptusMessage send( EucalyptusMessage msg ) throws Exception;

  void dispatch( EucalyptusMessage msg ) throws Exception;

  String getUri( );//TODO: change to endpoint

}
