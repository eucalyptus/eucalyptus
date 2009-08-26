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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "misc.h"
#include "data.h"

void test_command (char * command)
{
    char * result = system_output(command);
    int max = 160;

    if (result && strlen(result)>max) {
        result[max-4] = '.';
        result[max-3] = '.';
        result[max-2] = '.';
        result[max-1] = 0;
    }
    printf("--->%s executed\noutput=[%s]\n\n", command, result);
    free (result);
}

int main (int argc, char **argv) 
{
    printf ("=====> testing misc.c\n");

    test_command("date");
    test_command("ls / -l | sort");
    test_command("/foo");
    {
        char c = 0;
        long l = 0;
        int i = 0;
        long long ll = 0;

        sscanf_lines  ("a1\na\na2\n", "a%d", &i);
        assert (i==1);
        sscanf_lines  ("a\nab3\na   4\na5", "a %d", &i);
        assert (i==4);
        sscanf_lines  ("", "%d", &i);
        sscanf_lines  ("\n\n\n", "%d", &i);
        sscanf_lines  ("abcdefg6", "g%d", &i);
        assert (i!=6);
        sscanf_lines  ("abcdefg", "ab%cdefg", &c);
        assert (c=='c');
        sscanf_lines  ("a\na    7\na\n", "a %ld", &l);
        assert (l==7L);
        sscanf_lines  ("a\n8a\na9\n", "a %lld", &ll);
        assert (ll==9L);
    }

    printf ("=====> testing data.c\n");
    {
#define INSTS 50
        bunchOfInstances * bag = NULL;
        ncInstance * inst = NULL;
        ncInstance * Insts[INSTS];
        int i, n;

        free_instance (NULL);
        free_instance (&inst);
        inst = allocate_instance ("i1", NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0);
        assert(inst!=NULL);
        free_instance (&inst);
        assert(inst==NULL);
        
        n = total_instances (&bag);
        assert(n==0);
        bag=NULL;

        inst = find_instance(&bag, "foo");
        assert(inst==NULL);
        bag=NULL;

        n = remove_instance(&bag, NULL);
        assert(n!=0);
        bag=NULL;

        for (i=0; i<INSTS; i++) {
            char id[10];
            sprintf(id, "i-%d", i);
            inst = Insts[i] = allocate_instance(id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0);
            assert (inst!=NULL);
            n = add_instance(&bag, inst);
            assert (n==0);
        }
        n = total_instances (&bag);
        assert (n==INSTS);
        n = remove_instance(&bag, Insts[0]);
        assert (n==0);
        n = remove_instance(&bag, Insts[INSTS-1]);
        assert (n==0);
        n = total_instances (&bag);
        assert (n==INSTS-2);

        printf ("OK\n");
    }
    return 0;
}
