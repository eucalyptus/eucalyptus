package com.eucalyptus.auth.euare

import com.eucalyptus.auth.euare.common.msgs.ListServerCertificatesResponseType
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.BaseMessages
import org.apache.axiom.om.OMElement
import org.junit.Test

/**
 *
 */
class BindingTest {

  //TODO test with null values in model (preserve null)
  @Test
  void test() {
    ListServerCertificatesResponseType response = new ListServerCertificatesResponseType( )
    OMElement responseOm = BaseMessages.toOm( (BaseMessage) response )
    ListServerCertificatesResponseType responseRt = BaseMessages.fromOm( responseOm, ListServerCertificatesResponseType )
    responseRt.getListServerCertificatesResult().getServerCertificateMetadataList().getMemberList()
    println responseOm
    println response
    println responseRt
  }
}
