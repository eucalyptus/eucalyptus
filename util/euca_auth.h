#ifndef INCLUDE_EUCA_AUTH_H
#define INCLUDE_EUCA_AUTH_H

#ifndef NO_AXIS /* for compiling on systems without Axis */
#include "oxs_axiom.h"
#include "oxs_x509_cert.h"
#include "oxs_key_mgr.h"
#include "rampart_handler_util.h"
#include "rampart_sec_processed_result.h"
#include "rampart_error.h"

axis2_status_t __euca_authenticate(const axutil_env_t *env,axis2_msg_ctx_t *out_msg_ctx, axis2_op_ctx_t *op_ctx);

#define euca_authenticate(a,b,c) do{ if( __euca_authenticate(a,b,c) == AXIS2_FAILURE ) return NULL; }while(0)
#endif /* NO_AXIS */

/* 
 * functions for Walrus clients
 */
int euca_init_cert (void);
/* options for _get_cert: */
#define TRIM_CERT        0x01 /* remove the last newline */
#define CONCATENATE_CERT 0x02 /* remove all newlines */
#define INDENT_CERT      0x04 /* indent lines 2-N */
char * euca_get_cert (unsigned char options);
char * euca_sign_url (const char * verb, const char * date, const char * url);
char * base64_enc (unsigned char * in, int size);
char * base64_dec(unsigned char *in, int size);
#endif
     
