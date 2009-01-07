/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.XMLParser;
import junit.framework.TestCase;

import java.util.List;

public class XMLParserTest extends TestCase {


    public void testParse() throws Throwable {
        String parseTestString = "<AccessControlPolicy> <AccessControlList> <Grant> <Grantee> <DisplayName>nekro</DisplayName> </Grantee> <Permission>FULL_CONTROL</Permission> </Grant> <Grant> <Grantee> <DisplayName>lolwho</DisplayName> </Grantee> <Permission>NO_U</Permission> </Grant></AccessControlList> </AccessControlPolicy>";

        XMLParser xmlParser = new XMLParser(parseTestString);

        String ownerDisplayName = xmlParser.getValue("//Owner/DisplayName");

        List<String> displayNames = xmlParser.getValues("//AccessControlList/Grant/Grantee/DisplayName");

        System.out.println(ownerDisplayName);
    }

    public XMLParserTest() {
		super();
	}

}