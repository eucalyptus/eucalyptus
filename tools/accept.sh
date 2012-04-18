#!/bin/bash

if [ -f "$EUCALYPTUS/var/lib/eucalyptus/accept" ]; then
    ANS=`cat $EUCALYPTUS/var/lib/eucalyptus/accept`
    if ( test "$ANS" = "yes" -o "$ANS" = "y" -o "$ANS" = "Y" -o "$ANS" = "Yes" -o "$ANS" = "YES" ); then
	exit 0;
    fi
fi

TFILE=`mktemp eucafile.XXXXXX`
touch $TFILE
if [ ! -f "$TFILE" ]; then
    $TFILE="/tmp/eucafile.$$"
    touch $TFILE
    if [ ! -f "$TFILE" ]; then
	echo "cannot create tmpfile!"
	exit 1
    fi
fi
cat <<EOF >$TFILE
EUCALYPTUS SYSTEMS, INC 
SOFTWARE EVALUATION LICENSE AGREEMENT

This Software Evaluation License Agreement (the "Agreement") is a
legal agreement between you (either an individual or a single entity,
hereinafter referred to as either "you," "you" or "Licensee")  and
Eucalyptus Systems, Inc. ("Eucalyptus").

IMPORTANT - PLEASE READ CAREFULLY: BY SELECTING "I AGREE,"  YOU ARE
REPRESENTING THAT YOU CONSENT TO BE LEGALLY BOUND BY THIS AGREEMENT.
IF YOU ARE ACCEPTING THE AGREEMENT ON BEHALF OF A COMPANY OR AN
ENTITY, YOU REPRESENT AND WARRANT TO EUCALYPTUS THAT YOU HAVE ALL THE
REQUISITE POWER AND AUTHORITY, CORPORATE OR OTHERWISE, TO ENTER INTO
THIS AGREEMENT, AND BIND THE COMPANY OR ENTITY, AND YOUR ACCEPTANCE OF
THIS AGREEMENT SHALL BE TREATED AS ACCEPTANCE BY THAT COMPANY OR
ENTITY.

1.	Definitions. 

    (a) "Documentation" means any documentation specifically supplied
by Eucalyptus to you to enable you to use the Licensed Software.

    (b) "Licensed Software" means the executable, object code version
of a trial version of the Eucalyptus Enterprise Edition for VMware
(Part Number: ESI-VMWARE), and does not include programs or code
provided under separate license agreements, including, but not limited
to, open source license agreements.

2. Grant of License.  Subject to the terms and conditions herein,
including your acceptance of this Agreement, Eucalyptus hereby grants
you a limited, revocable, non-exclusive, non- sublicensable (except to
those independent contractors of Licensee as set forth below) and
non-transferable license during the Evaluation Period (as defined in
Section 8) to use and operate the Licensed Software solely in
accordance with the Documentation and only in connection with the
internal testing and evaluation of the Licensed Software.  This
license is for the Licensed Software in binary, executable object code
only, and no rights are granted to any source code, unless Licensee
has received source code for the Licensed Software directly from
Eucalyptus.  In such event, this Agreement shall govern your use of
such source code.

3. Restrictions on Use.  You hereby acknowledge and agree that you may
not, and you may not permit others to, (a) copy, display, transfer,
adapt, modify or distribute (electronically or otherwise) the Licensed
Software or Documentation, except as expressly set forth in Section 2
above, (b) reverse engineer, reverse assemble, reverse compile, or
otherwise translate the Licensed Software, unless expressly permitted
by applicable law without the possibility of contractual waiver, (c)
sublicense (except as expressly authorized under this Agreement),
assign or transfer the license for the Licensed Software or
Documentation, or (d) publish, rent or lease the Licensed Software or
Documentation or any copy thereof.  You agree to use the Licensed
Software or Documentation only as set forth above and in compliance
with the terms of this Agreement.

4. Ownership.  This Agreement is a license and not an agreement for
sale.  No title to or ownership in the Licensed Software or
Documentation is transferred to you.  Eucalyptus or its licensors, as
applicable, shall at all times remain the sole owners of all right,
title, interest in and to the Licensed Software, the Documentation and
any other Confidential Information (as defined below), including, but
not limited to, all trademarks, copyrights, patent rights, and all
other intellectual property rights embodied therein and any copies,
derivative works, corrections, bug fixes, enhancements, updates or
modifications made thereto.  Eucalyptus reserves all rights not
expressly granted to you in this Agreement and no other rights or
licenses are granted herein by implication, estoppel or otherwise.

5. WARRANTY DISCLAIMER.  TO THE MAXIMUM EXTENT PERMITTED UNDER
APPLICABLE LAW, THE LICENSED SOFTWARE AND ALL ASSOCIATED DOCUMENTATION
AND MATERIALS ARE PROVIDED TO YOU "AS IS," WITHOUT ANY WARRANTY OF ANY
KIND.  WITHOUT IN ANY WAY LIMITING THE GENERALITY OF THE FOREGOING,
EUCALYPTUS EXPRESSLY DISCLAIMS ALL WARRANTIES WITH RESPECT TO THE
LICENSED SOFTWARE OR DOCUMENTATION OF ANY KIND WHATSOEVER, EXPRESS OR
IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR
NON-INFRINGEMENT.  EUCALYPTUS DOES NOT WARRANT THE RESULTS OBTAINED OR
NOT OBTAINED BY YOUR USE OF THE LICENSED SOFTWARE.  EUCALYPTUS HEREBY
EXPRESSLY DISCLAIMS ANY WARRANTY THAT YOUR USE OF THE LICENSED
SOFTWARE WILL BE UNINTERRUPTED OR THAT THE OPERATION THEREOF WILL BE
ERROR- FREE OR SECURE.  SOME STATES DO NOT ALLOW LIMITATIONS ON
IMPLIED WARRANTIES, SO THE ABOVE LIMITATION MAY NOT APPLY TO YOU.

6. LIMITATION OF REMEDIES AND DAMAGES.  IN NO EVENT SHALL EUCALYPTUS
OR ANY OF ITS AFFILIATES, LICENSORS, DIRECTORS, OFFICERS, EMPLOYEES OR
AGENTS (COLLECTIVELY, "AFFILIATES") BE LIABLE FOR ANY LOST PROFITS,
LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER FAILURE, LOSS OF
INFORMATION, LOSS OF DATA, OR ANY DIRECT, INDIRECT, INCIDENTAL,
CONSEQUENTIAL, SPECIAL, EXEMPLARY, OR PUNITIVE DAMAGES (EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGES) UNDER ANY CONTRACT,
NEGLIGENCE, STRICT LIABILITY OR OTHER THEORY ARISING OUT OF OR
RELATING IN ANY WAY TO THE LICENSED SOFTWARE OR ANY OTHER SUBJECT
MATTER OF THIS AGREEMENT.  THIS LIMITATION WILL APPLY REGARDLESS OF
THE FAILURE OF THE ESSENTIAL PURPOSE OF ANY LIMITED REMEDY.  SOME
STATES DO NOT ALLOW THE EXCLUSION OR LIMITATION OF INCIDENTAL OR
CONSEQUENTIAL DAMAGES, SO THIS LIMITATION AND EXCLUSION MAY NOT APPLY
TO YOU.

7. Export Controls.  Licensee acknowledges that it is subject to
United States laws and regulations controlling the export of technical
data, computer software and other commodities and agrees not to export
or allow the export or re-export of such data, software or other
commodities in violation of such laws and regulations.

8. Evaluation Period.

       (a) The term of this Agreement is for a period defined in the
license key ("Evaluation Period"), commencing on the date you first
install the Licensed Software on a computer under your control.  The
Licensed Software may contain mechanisms that will terminate the
ability to use the Licensed Software at the end of Evaluation Period.

       (b) Upon expiration of the Evaluation Period, any future use of
the Licensed Software shall be subject to a separate commercial
license agreement to be negotiated by the parties.  Upon the earlier
of the expiration of the Evaluation Period or the termination of this
Agreement for any reason, if Licensee elects not to license the
Licensed Software (i) you shall immediately discontinue any use of the
Licensed Software and Documentation and you shall destroy all copies
of the Licensed Software and Documentation, and (ii) the license
granted herein shall expire and you shall have no further rights to
access or use the License Software.

9. Confidentiality.  Licensee agrees to treat the Licensed Software,
Documentation and all other material and information provided or
disclosed by Eucalyptus to Licensee "(Confidential Information") as
confidential and shall limit access to such Confidential Information
to its employees and independent contractors (such independent
contractors, solely upon prior written notice) who are required to
have the information for purposes authorized under this Agreement
("Authorized Parties").  Licensee will ensure that each Authorized
Party enters or has entered into a confidentiality agreement which
contains terms that are substantially similar to and that are at least
as protective of Eucalyptus's rights as are the terms set forth in the
relevant sections of this Agreement.  Licensee shall indemnify and
hold Eucalyptus harmless from and against any costs, losses, damages,
liability or expenses arising out of or related to the failure of its
employees and independent contractors who may in any way breach the
terms and conditions of this Agreement.  Licensee shall not use any
Confidential Information for any purpose other than as expressly
authorized under this Agreement.  Without limiting the foregoing,
Licensee shall use at least the same degree of care which it uses to
prevent the disclosure of its own confidential information of like
importance, but in no event less than reasonable care, to prevent the
disclosure of Eucalyptus's Confidential Information.  It is agreed
upon by both parties that any breach of this Section 10 by Licensee
shall constitute a material breach of this Agreement.

10. Miscellaneous.

    (a) Neither party may use other party's trade names or trademarks
or the existence of this Agreement for any publicity or marketing
activities without prior written consent of the other party.

    (b) If you provide any feedback to Eucalyptus concerning the
functionality and performance of the Licensed Software (including
identifying potential errors and improvements) ("Feedback"), you
hereby assign to Eucalyptus all right, title and interest in and to
the Feedback, and Eucalyptus is free to use the Feedback without any
payment or restriction.

    (c) This Agreement is not assignable by you (including by
operation of law) without the prior written consent of Eucalyptus.

    (d) This Agreement shall be governed by the laws of the State of
California, exclusive of its choice of law rules.  You submit to the
exclusive jurisdiction of the state and federal courts sitting in the
County of Santa Barbara in the State of California for the purpose of
resolving any dispute relating to this Agreement.  The United Nations
Convention on Contracts for the International Sale of Goods does not
apply to this Agreement.  In any action to enforce this Agreement, the
prevailing party will be entitled to costs and attorneys' fees.

    (e) This Agreement constitutes the final and complete
understanding between you and Eucalyptus with respect to the subject
matter of this Agreement.  Any modifications or waivers of this
Agreement must be in writing and signed by both parties hereto.

    (f) You acknowledge that any use by you of the Licensed Software,
the Documentation or Confidential Information in breach of this
Agreement will result in irreparable harm to Eucalyptus and that
Eucalyptus shall be entitled to seek preliminary and other injunctive
relief against such a breach or default, without the requirement of
posting a bond or other security.  Any injunctive relief shall be in
addition to and shall in no way limit any rights or remedies otherwise
available to Eucalyptus.

    (g) This Agreement may be terminated immediately by Eucalyptus in
the event of any breach by you of this Agreement.  Section 3, 4, 5, 6,
7, 8(b), 9 and 10 inclusive of all subsections therein, shall survive
such termination.  Upon any termination of this Agreement, you will
immediately discontinue use of Licensed Software and return the same,
including all Documentation and any Confidential Information, to
Eucalyptus.

    (h) Any notices under this Agreement shall be sent to Eucalyptus
by registered mail to Chief Executive Officer, Eucalyptus Systems,
Inc., 130 Castilian Drive, Goleta, CA 93117 (or at such other address
of which Eucalyptus may from time to time notify Licensee).  Any
notices required or permitted under this Agreement shall be sent to
Licensee at the most current mailing address for Licensee on file with
Eucalyptus.
EOF
if [ -f "$TFILE" ]; then
    more $TFILE
    COUNT=0 ANS="no" DONE=0
    while(test $COUNT -lt 5 -a $DONE -eq 0)
    do
	echo 
	echo -n "Do you agree to the above terms? (y/n): "
	read ANS
	if ( test "$ANS" = "yes" -o "$ANS" = "y" -o "$ANS" = "Y" -o "$ANS" = "Yes" -o "$ANS" = "YES" ); then
	    DONE=1
	elif ( test "$ANS" = "no" -o "$ANS" = "n" -o "$ANS" = "N" -o "$ANS" = "No" -o "$ANS" = "NO" ); then
	    DONE=2
	fi
	COUNT=$(($COUNT + 1))
     done
     
    echo
    if ( test $DONE -ne 1 ); then
	exit 1
    fi
    echo $ANS > $EUCALYPTUS/var/lib/eucalyptus/accept
    exit 0
fi
exit 1

