/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

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
        <ds:SignedInfo>
	  <!-- <ref-id> points to a signed element. Body, Timestamp, To, Action, and MessageId element are expected to be signed-->
	  <ds:Reference URI="#<ref-id>>
	  [..snip..]
	  </ds:Reference>
	</ds:SignedInfo>
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
#include "rampart_constants.h"
#include "axis2_addr.h"
#include "axiom_util.h"
#include "rampart_timestamp_token.h"

#include <neethi_policy.h>
#include <neethi_util.h>
#include <axutil_utils.h>
#include <axis2_client.h>
#include <axis2_stub.h>

#include "misc.h" /* check_file, logprintf */
#include "fault.h" // log_eucafault
#include "euca_axis.h"

#define NO_U_FAIL(x) do{ \
AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI, "[rampart][eucalyptus-verify] " #x );\
AXIS2_ERROR_SET(env->error, RAMPART_ERROR_FAILED_AUTHENTICATION, AXIS2_FAILURE);\
return AXIS2_FAILURE; \
}while(0)

static void throw_fault (void)
{
  init_eucafaults (euca_this_component_name);
  log_eucafault ("1009", 
		 "sender", euca_client_component_name, 
		 "receiver", euca_this_component_name, 
		 "keys_dir", "$EUCALYPTUS/var/lib/eucalyptus/keys/",
		 NULL);
}

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
      throw_fault();
      NO_U_FAIL("could not populate receiver certificate");
    }

    if( axutil_strcmp(recv_x509_buf,msg_x509_buf)!=0){
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Received x509 certificate value ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, msg_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Local x509 certificate value! ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, recv_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," ---------------------------------------------------" );
      throw_fault();
      NO_U_FAIL("The certificate specified is invalid!");
    }
    if(verify_references(sig_node, env, out_msg_ctx, soap_envelope, rampart_context) == AXIS2_FAILURE) {
      return AXIS2_FAILURE;
  }

  }
  else 
  {
    throw_fault();
    oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_DEFAULT, "Cannot load certificate from string =%s", data); 
    NO_U_FAIL("Failed to build certificate from BinarySecurityToken");
  }
  oxs_x509_cert_free(_cert, env);
  oxs_x509_cert_free(recv_cert, env);

  return AXIS2_SUCCESS;

}

/**
 * Verifes that Body, Timestamp, To, Action, and MessageId elements are signed and located
 * where expected by the application logic. Timestamp is checked for expiration regardless
 * of its actual location.
 */
axis2_status_t verify_references(axiom_node_t *sig_node, const axutil_env_t *env, axis2_msg_ctx_t *msg_ctx, 
				 axiom_soap_envelope_t *envelope, rampart_context_t *rampart_context) {
  axiom_node_t *si_node = NULL;
  axiom_node_t *ref_node = NULL;
  axis2_status_t status = AXIS2_SUCCESS;

  si_node = oxs_axiom_get_first_child_node_by_name(env,sig_node, OXS_NODE_SIGNEDINFO, OXS_DSIG_NS, OXS_DS);

  if(!si_node) {
    axis2_char_t *tmp = axiom_node_to_string(sig_node, env);
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart]sig = %s", tmp);
    NO_U_FAIL("Couldn't find SignedInfo!");
  }

  axutil_qname_t *qname = NULL;
  axiom_element_t *parent_elem = NULL; 
  axiom_children_qname_iterator_t *qname_iter = NULL; 

  parent_elem = axiom_node_get_data_element(si_node, env);  
  if(!parent_elem)                                                                                                                          
    {                                                                                                                                        
       NO_U_FAIL("Could not get Reference elem");                                                                                                                           
    }     

  axis2_char_t *ref = NULL;
  axis2_char_t *ref_id = NULL;
  axiom_node_t *signed_node = NULL;
  axiom_node_t *envelope_node = NULL;

  short signed_elems[5] = {0,0,0,0,0};

  envelope_node = axiom_soap_envelope_get_base_node(envelope, env);

  qname = axutil_qname_create(env, OXS_NODE_REFERENCE, OXS_DSIG_NS, NULL);                                                                            
  qname_iter = axiom_element_get_children_with_qname(parent_elem, env, qname, si_node); 
  while (axiom_children_qname_iterator_has_next(qname_iter , env)) {
      ref_node = axiom_children_qname_iterator_next(qname_iter, env);     
      axis2_char_t *txt = axiom_node_to_string(ref_node, env); 

      /* get reference to a signed element */
      ref = oxs_token_get_reference(env, ref_node);
      if(ref == NULL || strlen(ref) == 0 || ref[0] != '#') {
	oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unsupported reference ID in %s", txt);
	status = AXIS2_FAILURE;
	break;
      }

      AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] %s, ref = %s", txt, ref); 
  
      /* get rid of '#' */
      ref_id = axutil_string_substring_starting_at(axutil_strdup(env, ref), 1);
      signed_node = oxs_axiom_get_node_by_id(env, envelope_node, OXS_ATTR_ID, ref_id, OXS_WSU_XMLNS);
      if(!signed_node) {
	  oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Error retrieving elementwith ID=%s", ref_id);
	  status = AXIS2_FAILURE;
	  break;
      }
      if(verify_node(signed_node, env, msg_ctx, ref, signed_elems, rampart_context)) {
	status = AXIS2_FAILURE;
	break;
      }
    }         

  
  axutil_qname_free(qname, env);                                                                                                           
  qname = NULL;                   
  
  if(status == AXIS2_FAILURE) {
    NO_U_FAIL("Failed to verify location of signed elements!");
  }

  /* This is needed to make sure that all security-critical elements are signed */
  for(int i = 0; i < 5; i++) {
    if(signed_elems[i] == 0) {
      NO_U_FAIL("Not all required elements are signed");
    }
  }

  return status;

}

/**
 * Verifies XPath location of signed elements.
 */ 
int verify_node(axiom_node_t *signed_node, const axutil_env_t *env, axis2_msg_ctx_t *msg_ctx, axis2_char_t *ref, 
		short *signed_elems, rampart_context_t *rampart_context) {

  if(!axutil_strcmp(OXS_NODE_BODY, axiom_util_get_localname(signed_node, env))) {
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Body", ref); 
    signed_elems[0] = 1;

    axiom_node_t *parent = axiom_node_get_parent(signed_node,env);
    if(axutil_strcmp(OXS_NODE_ENVELOPE, axiom_util_get_localname(parent, env))) {
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected parent element for Body with ID = %s", ref);
       return 1;
    }

     parent = axiom_node_get_parent(parent,env);
     if(parent) {
       AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] parent of Envelope = %s", axiom_node_to_string(parent, env));
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed Body with ID = %s", ref);
       return 1;
     }

  } else if(!axutil_strcmp(RAMPART_SECURITY_TIMESTAMP, axiom_util_get_localname(signed_node, env))) {
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Timestamp", ref); 
    signed_elems[1] = 1;

    /* Regardless of the location of the Timestamp, verify the one that is signed */
    if(AXIS2_FAILURE == rampart_timestamp_token_validate(env, msg_ctx, signed_node, 
							 rampart_context_get_clock_skew_buffer(rampart_context, env))) {
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for Timestamp with ID = %s", ref);
      return 1;
    }

  } else if(!axutil_strcmp(AXIS2_WSA_ACTION, axiom_util_get_localname(signed_node, env))) {
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Action", ref); 
    signed_elems[2] = 1;

    if(verify_addr_hdr_elem_loc(signed_node, env, ref)) {
	oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for Action with ID = %s", ref);
	return 1;
      }

  } else if(!axutil_strcmp(AXIS2_WSA_TO, axiom_util_get_localname(signed_node, env))) {
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is To", ref); 
    signed_elems[3] = 1;
 
    if(verify_addr_hdr_elem_loc(signed_node, env, ref)) {
	oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for To with ID = %s", ref);
	return 1;
      }


  } else if(!axutil_strcmp(AXIS2_WSA_MESSAGE_ID, axiom_util_get_localname(signed_node, env))) {
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is MessageId", ref); 
    signed_elems[4] = 1;

    if(verify_addr_hdr_elem_loc(signed_node, env, ref)) {
	oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for MessageId with ID = %s", ref);
	return 1;
      }

  } else {
    AXIS2_LOG_WARNING(env->log, AXIS2_LOG_SI, "[euca-rampart] node %s is UNKNOWN", ref); 
  }

    return 0;
}

/**
 * Verify that an addressing element is located in <Envelope>/<Header>
 */
int verify_addr_hdr_elem_loc(axiom_node_t *signed_node, const axutil_env_t *env, axis2_char_t *ref) {

    axiom_node_t *parent = axiom_node_get_parent(signed_node,env);

    if(axutil_strcmp(OXS_NODE_HEADER, axiom_util_get_localname(parent, env))) {
       AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] parent of addressing elem is %s", axiom_node_to_string(parent, env));
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed addressing elem with ID = %s", ref);
       return 1;

    }
     parent = axiom_node_get_parent(parent,env);

    if(axutil_strcmp(OXS_NODE_ENVELOPE, axiom_util_get_localname(parent, env))) {
       AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] second parent of addressing elem is %s", axiom_node_to_string(parent, env));
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed addressing elem with ID = %s", ref);
       return 1;

    }

     parent = axiom_node_get_parent(parent,env);
     if(parent) {
       AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "[euca-rampart] parent of Envelope = %s", axiom_node_to_string(parent, env));
       oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed Body with ID = %s", ref);
       return 1;
     }

     return 0;
}


int InitWSSEC(axutil_env_t *env, axis2_stub_t *stub, char *policyFile) {
  axis2_svc_client_t *svc_client = NULL;
  neethi_policy_t *policy = NULL;
  axis2_status_t status = AXIS2_FAILURE;

  //return(0);

  svc_client =  axis2_stub_get_svc_client(stub, env);
  if (!svc_client) {
    logprintfl (EUCAERROR, "could not get svc_client from stub\n");
    return(1);
  }
  axis2_svc_client_engage_module(svc_client, env, "rampart");

  policy = neethi_util_create_policy_from_file(env, policyFile);
  if (!policy) {
    logprintfl (EUCAERROR, "could not initialize policy file %s\n", policyFile);
    return(1);
  }
  status = axis2_svc_client_set_policy(svc_client, env, policy);
    
  return(0);
}
