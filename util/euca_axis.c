// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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

//!
//! @file util/euca_axis.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "eucalyptus.h"
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

#include "misc.h"                      // check_file, logprintf
#include "fault.h"                     // log_eucafault
#include "euca_axis.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

axis2_status_t __euca_authenticate(const axutil_env_t * pEnv, axis2_msg_ctx_t * pOutMsgCtx, axis2_op_ctx_t * pOpCtx);
axis2_status_t verify_references(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_msg_ctx_t * pMsgCtx, axiom_soap_envelope_t * pEnvelope, rampart_context_t * pRampartCtx);
int verify_node(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_msg_ctx_t * pMsgCtx, axis2_char_t * sRef, short *pSigElems, rampart_context_t * pRampartCtx);
int verify_addr_hdr_elem_loc(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_char_t * sRef);

int InitWSSEC(axutil_env_t * pEnv, axis2_stub_t * pStub, char *sPolicyFile);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void throw_fault(void);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to log a failure and return an AXIS2 failure code
#define NO_U_FAIL(_x)                                                                     \
{                                                                                         \
	do {                                                                                  \
		AXIS2_LOG_ERROR(pEnv->log, AXIS2_LOG_SI, "[rampart][eucalyptus-verify] " #_x );   \
		AXIS2_ERROR_SET(pEnv->error, RAMPART_ERROR_FAILED_AUTHENTICATION, AXIS2_FAILURE); \
		return (AXIS2_FAILURE);                                                           \
	} while (0);                                                                          \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Logs an AXIS fault
//!
static void throw_fault(void)
{
    init_eucafaults(euca_this_component_name);
    log_eucafault("1009", "sender", euca_client_component_name, "receiver", euca_this_component_name, "keys_dir", "$EUCALYPTUS/var/lib/eucalyptus/keys/", NULL);
}

//!
//! Eucalyptus authentication
//!
//! @param[in]  pEnv pointer to the AXIS2 environment structure
//! @param[out] pOutMsgCtx pointer to the AXIS message context
//! @param[in]  pOpCtx pointer to teh AXIS operaton context
//!
//! @return AXIS2_SUCCESS on success or AXIS2_FAILURE on failure.
//!
//! @pre
//!
//! @post
//!
axis2_status_t __euca_authenticate(const axutil_env_t * pEnv, axis2_msg_ctx_t * pOutMsgCtx, axis2_op_ctx_t * pOpCtx)
{
    axis2_msg_ctx_t *pMsgCtx = NULL;   //<--- incoming msg context, it is NULL, see?
    rampart_context_t *pRampartCtx = NULL;
    axutil_property_t *pProperty = NULL;
    axiom_soap_envelope_t *pSoapEnvelope = NULL;
    axiom_soap_header_t *pSoapHeader = NULL;
    axiom_node_t *pSecNode = NULL;
    axiom_node_t *pSigNode = NULL;
    axiom_node_t *pKeyInfoNode = NULL;
    axiom_node_t *pSecTokenRefNode = NULL;
    axis2_char_t *sRef = NULL;
    axis2_char_t *sRefId = NULL;
    axiom_node_t *pTokenRefNode = NULL;
    axiom_node_t *pBstNode = NULL;
    axis2_char_t *sData = NULL;
    oxs_x509_cert_t *pCert = NULL;
    oxs_x509_cert_t *pRecvCert = NULL;
    axis2_char_t *sFileName = NULL;
    axis2_char_t *sRecvX509Buf = NULL;
    axis2_char_t *sMsgX509Buf = NULL;

    // First get the message context before doing anything dumb w/ a NULL pointer
    pMsgCtx = axis2_op_ctx_get_msg_ctx(pOpCtx, pEnv, AXIS2_WSDL_MESSAGE_LABEL_IN);

    // Print everything from the security results, just for testing now
    if ((pProperty = axis2_msg_ctx_get_property(pMsgCtx, pEnv, RAMPART_CONTEXT)) != NULL) {
        pRampartCtx = (rampart_context_t *) axutil_property_get_value(pProperty, pEnv);
        //AXIS2_LOG_CRITICAL(pEnv->log,AXIS2_LOG_SI," ======== PRINTING PROCESSED WSSEC TOKENS ======== ");
        rampart_print_security_processed_results_set(pEnv, pMsgCtx);
    }
    // Extract Security Node from header from enveloper from msg_ctx
    if ((pSoapEnvelope = axis2_msg_ctx_get_soap_envelope(pMsgCtx, pEnv)) == NULL)
        NO_U_FAIL("SOAP envelope cannot be found.");

    if ((pSoapHeader = axiom_soap_envelope_get_header(pSoapEnvelope, pEnv)) == NULL)
        NO_U_FAIL("SOAP header cannot be found.");

    if ((pSecNode = rampart_get_security_header(pEnv, pMsgCtx, pSoapHeader)) == NULL)   // <---- here it is!
        NO_U_FAIL("No node wsse:Security -- required: ws-security");

    // Find the wsse:Reference to the BinarySecurityToken
    // Path is: Security
    // *sec_node must be non-NULL, kkthx
    // the ds:Signature node
    if ((pSigNode = oxs_axiom_get_first_child_node_by_name(pEnv, pSecNode, OXS_NODE_SIGNATURE, OXS_DSIG_NS, OXS_DS)) == NULL)
        NO_U_FAIL("No node ds:Signature -- required: signature");

    // the ds:KeyInfo
    if ((pKeyInfoNode = oxs_axiom_get_first_child_node_by_name(pEnv, pSigNode, OXS_NODE_KEY_INFO, OXS_DSIG_NS, NULL)) == NULL)
        NO_U_FAIL("No node ds:KeyInfo -- required: signature key");

    // the wsse:SecurityTokenReference
    if ((pSecTokenRefNode = oxs_axiom_get_first_child_node_by_name(pEnv, pKeyInfoNode, OXS_NODE_SECURITY_TOKEN_REFRENCE, OXS_WSSE_XMLNS, NULL)) == NULL)
        NO_U_FAIL("No node wsse:SecurityTokenReference -- required: signing token");
    // in theory this is the branching point for supporting all kinds of tokens -- we only do BST Direct Reference
    // Find the wsse:Reference to the BinarySecurityToken
    // *sec_token_ref_node must be non-NULL
    // the wsse:Reference node **/
    pTokenRefNode = oxs_axiom_get_first_child_node_by_name(pEnv, pSecTokenRefNode, OXS_NODE_REFERENCE, OXS_WSSE_XMLNS, NULL);

    // pull out the name of the BST node
    sRef = oxs_token_get_reference(pEnv, pTokenRefNode);
    sRefId = axutil_string_substring_starting_at(axutil_strdup(pEnv, sRef), 1);

    // get the wsse:BinarySecurityToken used to sign the message
    if ((pBstNode = oxs_axiom_get_node_by_id(pEnv, pSecNode, "Id", sRefId, OXS_WSU_XMLNS)) == NULL) {
        oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Error retrieving elementwith ID=%s", sRefId);
        NO_U_FAIL("Cant find the required node");
    }
    // Find the wsse:Reference to the BinarySecurityToken
    // *bst_node must be non-NULL
    // pull out the data from the BST
    sData = oxs_axiom_get_node_content(pEnv, pBstNode);

    // create an oxs_X509_cert
    if ((pCert = oxs_key_mgr_load_x509_cert_from_string(pEnv, sData)) != NULL) {
        // FINALLY -- we have the certificate used to sign the message.  authenticate it HERE
        if ((sMsgX509Buf = oxs_x509_cert_get_data(pCert, pEnv)) == NULL)
            NO_U_FAIL("OMG WHAT NOW?!");

        if ((sFileName = rampart_context_get_receiver_certificate_file(pRampartCtx, pEnv)) == NULL)
            NO_U_FAIL("Policy for the service is incorrect -- ReceiverCertificate is not set!!");

        if (check_file(sFileName))
            NO_U_FAIL("No cert file ($EUCALYPTUS/var/lib/eucalyptus/keys/cloud-cert.pem) found, failing");

        if ((pRecvCert = oxs_key_mgr_load_x509_cert_from_pem_file(pEnv, sFileName)) != NULL) {
            sRecvX509Buf = oxs_x509_cert_get_data(pRecvCert, pEnv);
        } else {
            throw_fault();
            NO_U_FAIL("could not populate receiver certificate");
        }

        if (axutil_strcmp(sRecvX509Buf, sMsgX509Buf) != 0) {
            AXIS2_LOG_CRITICAL(pEnv->log, AXIS2_LOG_SI, " --------- Received x509 certificate value ---------");
            AXIS2_LOG_CRITICAL(pEnv->log, AXIS2_LOG_SI, sMsgX509Buf);
            AXIS2_LOG_CRITICAL(pEnv->log, AXIS2_LOG_SI, " --------- Local x509 certificate value! ---------");
            AXIS2_LOG_CRITICAL(pEnv->log, AXIS2_LOG_SI, sRecvX509Buf);
            AXIS2_LOG_CRITICAL(pEnv->log, AXIS2_LOG_SI, " ---------------------------------------------------");
            throw_fault();
            NO_U_FAIL("The certificate specified is invalid!");
        }

        if (verify_references(pSigNode, pEnv, pOutMsgCtx, pSoapEnvelope, pRampartCtx) == AXIS2_FAILURE) {
            return (AXIS2_FAILURE);
        }
    } else {
        throw_fault();
        oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_DEFAULT, "Cannot load certificate from string =%s", sData);
        NO_U_FAIL("Failed to build certificate from BinarySecurityToken");
    }

    oxs_x509_cert_free(pCert, pEnv);
    oxs_x509_cert_free(pRecvCert, pEnv);
    return (AXIS2_SUCCESS);
}

//!
//! Verifes that Body, Timestamp, To, Action, and MessageId elements are signed and located
//! where expected by the application logic. Timestamp is checked for expiration regardless
//! of its actual location.
//!
//! @param[in] pSigNode
//! @param[in] pEnv pointer to the AXIS2 environment structure
//! @param[in] pMsgCtx
//! @param[in] pEnvelope
//! @param[in] pRampartCtx
//!
//! @return AXIS2_SUCCESS on success or AXIS2_FAILURE on failure.
//!
//! @pre
//!
//! @post
//!
axis2_status_t verify_references(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_msg_ctx_t * pMsgCtx, axiom_soap_envelope_t * pEnvelope, rampart_context_t * pRampartCtx)
{
    int i = 0;
    axiom_node_t *pSiNode = NULL;
    axiom_node_t *pRefNode = NULL;
    axis2_status_t status = AXIS2_SUCCESS;
    axis2_char_t *sRef = NULL;
    axis2_char_t *sRefId = NULL;
    axis2_char_t *sText = NULL;
    axiom_node_t *pSignedNode = NULL;
    axiom_node_t *pEnvelopeNode = NULL;
    axutil_qname_t *pQname = NULL;
    axiom_element_t *pParentElem = NULL;
    axiom_children_qname_iterator_t *pQnameIter = NULL;

    short signed_elems[5] = { 0, 0, 0, 0, 0 };

    if ((pSiNode = oxs_axiom_get_first_child_node_by_name(pEnv, pSigNode, OXS_NODE_SIGNEDINFO, OXS_DSIG_NS, OXS_DS)) == NULL) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart]sig = %s", axiom_node_to_string(pSigNode, pEnv));
        NO_U_FAIL("Couldn't find SignedInfo!");
    }

    if ((pParentElem = axiom_node_get_data_element(pSiNode, pEnv)) == NULL) {
        NO_U_FAIL("Could not get Reference elem");
    }

    pEnvelopeNode = axiom_soap_envelope_get_base_node(pEnvelope, pEnv);
    pQname = axutil_qname_create(pEnv, OXS_NODE_REFERENCE, OXS_DSIG_NS, NULL);
    pQnameIter = axiom_element_get_children_with_qname(pParentElem, pEnv, pQname, pSiNode);
    while (axiom_children_qname_iterator_has_next(pQnameIter, pEnv)) {
        pRefNode = axiom_children_qname_iterator_next(pQnameIter, pEnv);
        sText = axiom_node_to_string(pRefNode, pEnv);

        // get reference to a signed element
        sRef = oxs_token_get_reference(pEnv, pRefNode);
        if ((sRef == NULL) || (strlen(sRef) == 0) || (sRef[0] != '#')) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unsupported reference ID in %s", sText);
            status = AXIS2_FAILURE;
            break;
        }

        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] %s, ref = %s", sText, sRef);

        // get rid of '#'
        sRefId = axutil_string_substring_starting_at(axutil_strdup(pEnv, sRef), 1);
        if ((pSignedNode = oxs_axiom_get_node_by_id(pEnv, pEnvelopeNode, OXS_ATTR_ID, sRefId, OXS_WSU_XMLNS)) == NULL) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Error retrieving elementwith ID=%s", sRefId);
            status = AXIS2_FAILURE;
            break;
        }

        if (verify_node(pSignedNode, pEnv, pMsgCtx, sRef, signed_elems, pRampartCtx)) {
            status = AXIS2_FAILURE;
            break;
        }
    }

    axutil_qname_free(pQname, pEnv);
    pQname = NULL;

    if (status == AXIS2_FAILURE) {
        NO_U_FAIL("Failed to verify location of signed elements!");
    }
    // This is needed to make sure that all security-critical elements are signed
    for (i = 0; i < 5; i++) {
        if (signed_elems[i] == 0) {
            NO_U_FAIL("Not all required elements are signed");
        }
    }

    return (status);
}

//!
//! Verifies XPath location of signed elements.
//!
//! @param[in] pSigNode
//! @param[in] pEnv pointer to the AXIS2 environment structure
//! @param[in] pMsgCtx
//! @param[in] sRef
//! @param[in] pSigElems
//! @param[in] pRampartCtx
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int verify_node(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_msg_ctx_t * pMsgCtx, axis2_char_t * sRef, short *pSigElems, rampart_context_t * pRampartCtx)
{
    axiom_node_t *pParent = NULL;

    if (!axutil_strcmp(OXS_NODE_BODY, axiom_util_get_localname(pSigNode, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Body", sRef);
        pSigElems[0] = 1;

        pParent = axiom_node_get_parent(pSigNode, pEnv);
        if (axutil_strcmp(OXS_NODE_ENVELOPE, axiom_util_get_localname(pParent, pEnv))) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected parent element for Body with ID = %s", sRef);
            return (EUCA_ERROR);
        }

        if ((pParent = axiom_node_get_parent(pParent, pEnv)) != NULL) {
            AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] parent of pEnvelope = %s", axiom_node_to_string(pParent, pEnv));
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed Body with ID = %s", sRef);
            return (EUCA_ERROR);
        }
    } else if (!axutil_strcmp(RAMPART_SECURITY_TIMESTAMP, axiom_util_get_localname(pSigNode, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Timestamp", sRef);
        pSigElems[1] = 1;

        /* Regardless of the location of the Timestamp, verify the one that is signed */
        if (AXIS2_FAILURE == rampart_timestamp_token_validate(pEnv, pMsgCtx, pSigNode, rampart_context_get_clock_skew_buffer(pRampartCtx, pEnv))) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for Timestamp with ID = %s", sRef);
            return (EUCA_ERROR);
        }
    } else if (!axutil_strcmp(AXIS2_WSA_ACTION, axiom_util_get_localname(pSigNode, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is Action", sRef);
        pSigElems[2] = 1;

        if (verify_addr_hdr_elem_loc(pSigNode, pEnv, sRef)) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for Action with ID = %s", sRef);
            return (EUCA_ERROR);
        }
    } else if (!axutil_strcmp(AXIS2_WSA_TO, axiom_util_get_localname(pSigNode, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is To", sRef);
        pSigElems[3] = 1;

        if (verify_addr_hdr_elem_loc(pSigNode, pEnv, sRef)) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for To with ID = %s", sRef);
            return (EUCA_ERROR);
        }
    } else if (!axutil_strcmp(AXIS2_WSA_MESSAGE_ID, axiom_util_get_localname(pSigNode, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is MessageId", sRef);
        pSigElems[4] = 1;

        if (verify_addr_hdr_elem_loc(pSigNode, pEnv, sRef)) {
            oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Validation failed for MessageId with ID = %s", sRef);
            return (EUCA_ERROR);
        }
    } else {
        AXIS2_LOG_WARNING(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] node %s is UNKNOWN", sRef);
    }

    return (EUCA_OK);
}

//!
//! Verify that an addressing element is located in Envelope/Header tags
//!
//! @param[in] pSigNode
//! @param[in] pEnv pointer to the AXIS2 environment structure
//! @param[in] sRef
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int verify_addr_hdr_elem_loc(axiom_node_t * pSigNode, const axutil_env_t * pEnv, axis2_char_t * sRef)
{
    axiom_node_t *pParent = axiom_node_get_parent(pSigNode, pEnv);

    if (axutil_strcmp(OXS_NODE_HEADER, axiom_util_get_localname(pParent, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] parent of addressing elem is %s", axiom_node_to_string(pParent, pEnv));
        oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed addressing elem with ID = %s", sRef);
        return (EUCA_ERROR);
    }

    pParent = axiom_node_get_parent(pParent, pEnv);
    if (axutil_strcmp(OXS_NODE_ENVELOPE, axiom_util_get_localname(pParent, pEnv))) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] second parent of addressing elem is %s", axiom_node_to_string(pParent, pEnv));
        oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed addressing elem with ID = %s", sRef);
        return (EUCA_ERROR);
    }

    if ((pParent = axiom_node_get_parent(pParent, pEnv)) != NULL) {
        AXIS2_LOG_DEBUG(pEnv->log, AXIS2_LOG_SI, "[euca-rampart] parent of Envelope = %s", axiom_node_to_string(pParent, pEnv));
        oxs_error(pEnv, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Unexpected location of signed Body with ID = %s", sRef);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] pEnv pointer to the AXIS2 environment structure
//! @param[in] pStub a pointer to the AXIS2 stub structure
//! @param[in] sPolicyFile policy file name string
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int InitWSSEC(axutil_env_t * pEnv, axis2_stub_t * pStub, char *sPolicyFile)
{
    axis2_svc_client_t *pSvcClient = NULL;
    neethi_policy_t *pPolicy = NULL;
    axis2_status_t status = AXIS2_FAILURE;

    if ((pSvcClient = axis2_stub_get_svc_client(pStub, pEnv)) == NULL) {
        LOGERROR("could not get svc_client from stub\n");
        return (EUCA_ERROR);
    }

    axis2_svc_client_engage_module(pSvcClient, pEnv, "rampart");
    if ((pPolicy = neethi_util_create_policy_from_file(pEnv, sPolicyFile)) == NULL) {
        LOGERROR("could not initialize policy file %s\n", sPolicyFile);
        return (EUCA_ERROR);
    }

    status = axis2_svc_client_set_policy(pSvcClient, pEnv, pPolicy);
    return (EUCA_OK);
}
