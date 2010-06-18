/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
/* BRIEF EXAMPLE MSG:
<soapenv:Envelope>.
  <soapenv:Header>
    [..snip..]
    <wsse:Security>
      [..snip..]
      <wsse:BinarySecurityToken xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                              EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
                              ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
                              wsu:Id="CertId-469">[..snip..]</wsse:BinarySecurityToken>
      [..snip..]
      <ds:Signature>
        <ds:KeyInfo Id="KeyId-374652">
          <wsse:SecurityTokenReference xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="STRId-22112351">
            <!-- this thing points to the wsse:BinarySecurityToken above -->
            <wsse:Reference URI="#CertId-469" ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
          </wsse:SecurityTokenReference>
        </ds:KeyInfo>
      </ds:Signature>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>...</soapenv:Body>
</soapenv:Envelope>.
*/

#include "oxs_axiom.h"
#include "oxs_x509_cert.h"
#include "oxs_key_mgr.h"
#include "rampart_handler_util.h"
#include "rampart_sec_processed_result.h"
#include "rampart_error.h"
#include "axis2_op_ctx.h"
#include "rampart_context.h"

#include <neethi_policy.h>
#include <neethi_util.h>
#include <axutil_utils.h>
#include <axis2_client.h>
#include <axis2_stub.h>

#include "misc.h" /* check_file, logprintf */
#include "euca_axis.h"

#define NO_U_FAIL(x) do{ \
AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI, "[rampart][eucalyptus-verify] " #x );\
AXIS2_ERROR_SET(env->error, RAMPART_ERROR_FAILED_AUTHENTICATION, AXIS2_FAILURE);\
return AXIS2_FAILURE; \
}while(0)

axis2_status_t __euca_authenticate(const axutil_env_t *env,axis2_msg_ctx_t *out_msg_ctx, axis2_op_ctx_t *op_ctx)
{
  //***** First get the message context before doing anything dumb w/ a NULL pointer *****/
  axis2_msg_ctx_t *msg_ctx = NULL; //<--- incoming msg context, it is NULL, see?
  msg_ctx = axis2_op_ctx_get_msg_ctx(op_ctx, env, AXIS2_WSDL_MESSAGE_LABEL_IN);  

  //***** Print everything from the security results, just for testing now *****//
  rampart_context_t *rampart_context = NULL;
  axutil_property_t *property = NULL;

  property = axis2_msg_ctx_get_property(msg_ctx, env, RAMPART_CONTEXT);
  if(property)
  {
     rampart_context = (rampart_context_t *)axutil_property_get_value(property, env);
     //     AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," ======== PRINTING PROCESSED WSSEC TOKENS ======== ");
     rampart_print_security_processed_results_set(env,msg_ctx);
  }

  //***** Extract Security Node from header from enveloper from msg_ctx *****//
  axiom_soap_envelope_t *soap_envelope = NULL;
  axiom_soap_header_t *soap_header = NULL;
  axiom_node_t *sec_node = NULL;


  soap_envelope = axis2_msg_ctx_get_soap_envelope(msg_ctx, env);
  if(!soap_envelope) NO_U_FAIL("SOAP envelope cannot be found."); 
  soap_header = axiom_soap_envelope_get_header(soap_envelope, env);
  if (!soap_header) NO_U_FAIL("SOAP header cannot be found.");
  sec_node = rampart_get_security_header(env, msg_ctx, soap_header); // <---- here it is!
  if(!sec_node)NO_U_FAIL("No node wsse:Security -- required: ws-security");

  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** Path is: Security/
  //** *sec_node must be non-NULL, kkthx **//
  axiom_node_t *sig_node = NULL;
  axiom_node_t *key_info_node = NULL;
  axiom_node_t *sec_token_ref_node = NULL;
  /** the ds:Signature node **/
  sig_node = oxs_axiom_get_first_child_node_by_name(env,sec_node, OXS_NODE_SIGNATURE, OXS_DSIG_NS, OXS_DS );
  if(!sig_node)NO_U_FAIL("No node ds:Signature -- required: signature");
  /** the ds:KeyInfo **/
  key_info_node = oxs_axiom_get_first_child_node_by_name(env, sig_node, OXS_NODE_KEY_INFO, OXS_DSIG_NS, NULL );
  if(!key_info_node)NO_U_FAIL("No node ds:KeyInfo -- required: signature key");
  /** the wsse:SecurityTokenReference **/ 
  sec_token_ref_node = oxs_axiom_get_first_child_node_by_name(env, key_info_node,OXS_NODE_SECURITY_TOKEN_REFRENCE, OXS_WSSE_XMLNS, NULL);
  if(!sec_token_ref_node)NO_U_FAIL("No node wsse:SecurityTokenReference -- required: signing token");
  //** in theory this is the branching point for supporting all kinds of tokens -- we only do BST Direct Reference **/

  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** *sec_token_ref_node must be non-NULL **/
  axis2_char_t *ref = NULL;
  axis2_char_t *ref_id = NULL;
  axiom_node_t *token_ref_node = NULL;
  axiom_node_t *bst_node = NULL;
  /** the wsse:Reference node **/
  token_ref_node = oxs_axiom_get_first_child_node_by_name(env, sec_token_ref_node,OXS_NODE_REFERENCE, OXS_WSSE_XMLNS, NULL);
  /** pull out the name of the BST node **/
  ref = oxs_token_get_reference(env, token_ref_node);
  ref_id = axutil_string_substring_starting_at(axutil_strdup(env, ref), 1);
  /** get the wsse:BinarySecurityToken used to sign the message **/
  bst_node = oxs_axiom_get_node_by_id(env, sec_node, "Id", ref_id, OXS_WSU_XMLNS);
  if(!bst_node){oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Error retrieving elementwith ID=%s", ref_id);NO_U_FAIL("Cant find the required node");}


  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** *bst_node must be non-NULL **/
  axis2_char_t *data = NULL;
  oxs_x509_cert_t *_cert = NULL;
  oxs_x509_cert_t *recv_cert = NULL;
  axis2_char_t *file_name = NULL;
  axis2_char_t *recv_x509_buf = NULL;
  axis2_char_t *msg_x509_buf = NULL;

  /** pull out the data from the BST **/
  data = oxs_axiom_get_node_content(env, bst_node);
  /** create an oxs_X509_cert **/
  _cert = oxs_key_mgr_load_x509_cert_from_string(env, data);
  if(_cert)
  {
    //***** FINALLY -- we have the certificate used to sign the message.  authenticate it HERE *****//
    msg_x509_buf = oxs_x509_cert_get_data(_cert,env);
    if(!msg_x509_buf)NO_U_FAIL("OMG WHAT NOW?!");
    /*
    recv_x509_buf = (axis2_char_t *)rampart_context_get_receiver_certificate(rampart_context, env);
    if(recv_x509_buf)
        recv_cert = oxs_key_mgr_load_x509_cert_from_string(env, recv_x509_buf);
    else
    {
        file_name = rampart_context_get_receiver_certificate_file(rampart_context, env);
        if(!file_name) NO_U_FAIL("Policy for the service is incorrect -- ReceiverCertificate is not set!!");
	if (check_file(file_name)) NO_U_FAIL("No cert file ($EUCALYPTUS/var/lib/eucalyptus/keys/cloud-cert.pem) found, failing");
        recv_cert = oxs_key_mgr_load_x509_cert_from_pem_file(env, file_name);
    }
    */

    file_name = rampart_context_get_receiver_certificate_file(rampart_context, env);
    if(!file_name) NO_U_FAIL("Policy for the service is incorrect -- ReceiverCertificate is not set!!");
    if (check_file(file_name)) NO_U_FAIL("No cert file ($EUCALYPTUS/var/lib/eucalyptus/keys/cloud-cert.pem) found, failing");
    recv_cert = oxs_key_mgr_load_x509_cert_from_pem_file(env, file_name);

    if (recv_cert) {
      recv_x509_buf = oxs_x509_cert_get_data(recv_cert,env);
    } else {
      NO_U_FAIL("could not populate receiver cert");
    }

    if( axutil_strcmp(recv_x509_buf,msg_x509_buf)!=0){
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Received x509 certificate value ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, msg_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Local x509 certificate value! ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, recv_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," ---------------------------------------------------" );
      NO_U_FAIL("The certificate specified is invalid!");
    }
  }
  else 
  {
    oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_DEFAULT, "Cannot load certificate from string =%s", data); 
    NO_U_FAIL("Failed to build certificate from BinarySecurityToken");
  }
  oxs_x509_cert_free(_cert, env);
  oxs_x509_cert_free(recv_cert, env);
  return AXIS2_SUCCESS;

}

int InitWSSEC(axutil_env_t *env, axis2_stub_t *stub, char *policyFile) {
  axis2_svc_client_t *svc_client = NULL;
  neethi_policy_t *policy = NULL;
  axis2_status_t status = AXIS2_FAILURE;

  //return(0);

  svc_client =  axis2_stub_get_svc_client(stub, env);
  if (!svc_client) {
    logprintfl (EUCAERROR, "InitWSSEC(): ERROR could not get svc_client from stub\n");
    return(1);
  }
  axis2_svc_client_engage_module(svc_client, env, "rampart");

  policy = neethi_util_create_policy_from_file(env, policyFile);
  if (!policy) {
    logprintfl (EUCAERROR, "InitWSSEC(): ERROR could not initialize policy file %s\n", policyFile);
    return(1);
  }
  status = axis2_svc_client_set_policy(svc_client, env, policy);
    
  return(0);
}

