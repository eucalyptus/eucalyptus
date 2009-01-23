package edu.ucsb.eucalyptus.transport.client;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;

public interface Client {

  public EucalyptusMessage send( EucalyptusMessage msg ) throws AxisFault;

  public OMElement sync( OMElement omMsg ) throws AxisFault;

  public void dispatch( EucalyptusMessage msg ) throws AxisFault;

  public void async( OMElement omMsg ) throws AxisFault;

  public String getUri();
}
