#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#define M1 "aa:bb:00:00:00:00"
#define I1 "127.0.0.0"
#define M2 "aa:bb:00:00:00:01"
#define I2 "127.0.0.1"
#define M3 "aa:bb:00:00:00:02"
#define I3 "127.0.0.2"
#define M4 "aa:bb:00:00:00:03"
#define I4 "127.0.0.3"

#include "dhcp.h"

int main (int argc, char ** argv)
{
    int err;

    err=dhcp_add_dev(NULL, NULL); assert(err!=0);
    err=dhcp_add_dev("eucadev3", "192.168.3.1"); assert(err==0);
    err=dhcp_add_dev("eucadev4", "192.168.4.1"); assert(err==0);
    err=dhcp_add_entry(NULL, NULL); assert(err!=0);
    err=dhcp_add_entry(M1, I1); assert(err==0);

/*
    err=dhcp_del_entry(M1); assert(err==0);
    err=dhcp_add_entry(M2, I2);
    err=dhcp_add_entry(M3, I3); 
    err=dhcp_add_entry(M4, I4); assert(err==0);
    err=dhcp_add_entry(M4, I4); assert(err!=0);
    err=dhcp_add_entry(M2, I2); assert(err!=0);
    err=dhcp_add_entry(M3, I3); assert(err!=0);
    err=dhcp_del_entry(M1); assert(err!=0);
    err=dhcp_del_entry(M2); assert(err==0);
    err=dhcp_del_entry(M4); assert(err==0);
    err=dhcp_del_entry(M3); assert(err==0);
    err=dhcp_add_entry(M1, I1); assert(err==0);
    err=dhcp_del_entry(M1); assert(err==0);
*/

    printf("all tests passed\n");
    
    return 0;
}
