/*
 * Copyright 1997-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package javax.crypto;

import java.util.*;
import java.security.*;

import java.security.Provider.Service;

import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;

/**
 * This class instantiates implementations of JCE engine classes from
 * providers registered with the java.security.Security object.
 *
 * @author Jan Luehe
 * @author Sharon Liu
 * @since 1.4
 */

final class JceSecurity {

    // Used in KeyGenerator, Cipher and KeyAgreement.
    static final SecureRandom RANDOM = new SecureRandom();

    /*
     * Don't let anyone instantiate this.
     */
    private JceSecurity() {
    }

    static Instance getInstance(String type, Class clazz, String algorithm,
            String provider) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        Service s = GetInstance.getService(type, algorithm, provider);
        return GetInstance.getInstance(s, clazz);
    }

    static Instance getInstance(String type, Class clazz, String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Service s = GetInstance.getService(type, algorithm, provider);
        return GetInstance.getInstance(s, clazz);
    }

    static Instance getInstance(String type, Class clazz, String algorithm)
            throws NoSuchAlgorithmException {
        List services = GetInstance.getServices(type, algorithm);
        NoSuchAlgorithmException failure = null;
        for (Iterator t = services.iterator(); t.hasNext(); ) {
            Service s = (Service)t.next();
            try {
                Instance instance = GetInstance.getInstance(s, clazz);
                return instance;
            } catch (NoSuchAlgorithmException e) {
                failure = e;
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + algorithm
                + " not available", failure);
    }

    // Used to return whether this provider is properly signed and
    // can be used by JCE. These days just returns true. Still used
    // in SecretKeyFactory, KeyGenerator, Mac and KeyAgreement.
    static boolean canUseProvider(Provider p) {
        return true;
    }
}
