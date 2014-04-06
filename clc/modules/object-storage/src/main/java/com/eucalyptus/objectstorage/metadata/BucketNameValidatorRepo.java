/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.metadata;

import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Instantiates and holds a set of validation rules for bucket names.
 *
 * The rules were retrieved from -
 * http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html
 * on March 26, 2014
 *
 * @author Wes.Wannemacher@eucalyptus.com
 */
public class BucketNameValidatorRepo {

    private static final Validator<String> extendedValidator = setupExtended();
    private static final Validator<String> dnsCompliantValidator = setupDnsCompliant();

    private static final Logger LOG = Logger.getLogger(BucketNameValidatorRepo.class);

    private static final Pattern IP_MATCHER = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public static Validator<String> getBucketNameValidator() {
        if (ObjectStorageGlobalConfiguration.bucket_naming_restrictions != null
                && "extended".equalsIgnoreCase(ObjectStorageGlobalConfiguration.bucket_naming_restrictions) ) {
            return extendedValidator;
        }
        else if (ObjectStorageGlobalConfiguration.bucket_naming_restrictions != null
                && "dns-compliant".equalsIgnoreCase(ObjectStorageGlobalConfiguration.bucket_naming_restrictions)) {
            return dnsCompliantValidator;
        }
        else {
            return new Validator<String>() {
                @Override
                public boolean check(String value) {
                    LOG.error("the value " + ObjectStorageGlobalConfiguration.bucket_naming_restrictions
                            + " is not valid, must be either 'extended' or 'dns-compliant'. No validation " +
                            "will be done on the specified bucket name (may result in errors).");
                    if (value != null && value.length() > 0) {
                        return true;
                    }
                    return false;
                }
            };
        }
    }

    private static Validator<String> setupExtended() {
        IteratingValidator<String> extended = new IteratingValidator<String>();
        List<Validator<String>> checks = Lists.newArrayListWithExpectedSize(3);
        checks.add(new Validator<String>() {
            // not null // sanity
            @Override
            public boolean check(String bucketName) {
                return bucketName != null && bucketName.length() > 1 ;
            }
        });
        checks.add(new Validator<String>() {
            // not more than 255 characters
            @Override
            public boolean check(String bucketName) {
                return bucketName.length() <= 255 ;
            }
        });
        checks.add(new Validator<String>() {
            // any combination of uppercase letters, lowercase letters,
            // numbers, periods (.), dashes (-) and underscores (_)
            @Override
            public boolean check(String bucketName) {
                for (char ch : bucketName.toCharArray()) {
                    boolean isBad = (
                            ! isLowerOrNumber(ch) &&
                            ! (ch >= 'A' && ch <= 'Z') &&
                            ch != '.' &&
                            ch != '-' &&
                            ch != '_'
                    );
                    if ( isBad ) {
                        return false;
                    }
                }
                return true;
            }
        });
        extended.setValidators(checks);
        return extended;
    }

    private static Validator<String> setupDnsCompliant() {
        IteratingValidator<String> dnsCompliant = new IteratingValidator<String>();
        List<Validator<String>> checks = Lists.newArrayListWithExpectedSize(5);
        checks.add(extendedValidator);
        checks.add(new Validator<String>() {
            // Bucket names must be at least 3 and no more than 63 characters long.
            @Override
            public boolean check(String bucketName) {
                return bucketName.length() >= 3 && bucketName.length() <= 63;
            }
        });
        checks.add(new Validator<String>() {
            // make sure the name does not start or end with a period
            @Override
            public boolean check(String bucketName) {
                if ( bucketName.charAt(0) == '.' || bucketName.charAt( bucketName.length() - 1 ) == '.' ) {
                    return false;
                }
                return true;
            }
        });
        checks.add(new Validator<String>() {
            // Bucket names must be a series of one or more labels. Adjacent labels
            // are separated by a single period (.). Bucket names can contain lowercase
            // letters, numbers, and dashes. Each label must start and end with a
            // lowercase letter or a number.
            @Override
            public boolean check(String value) {
                List<String> labels;
                if (value.contains(".")) {
                    // using StringTokenizer so that we can check for consecutive periods, plus
                    // rumor has it that StringTokenizer may be faster than String.split[*]
                    // * [citation needed]
                    StringTokenizer tokenizer = new StringTokenizer(value, ".", true);
                    labels = Lists.newArrayList();
                    String lastTok = "";
                    while ( tokenizer.hasMoreElements() ) {
                        String curTok = tokenizer.nextToken();
                        if (! ".".equals(curTok) ) {
                            labels.add(curTok);
                        }
                        else if (".".equals(lastTok) && ".".equals(curTok)) {
                            return false;
                        }
                        lastTok = curTok;
                    }
                }
                else {
                    labels = Lists.newArrayListWithExpectedSize(1);
                    labels.add(value);
                }
                for (String label : labels) {
                    char[] asChars = label.toCharArray();

                    // Each label must start and end with a
                    // lowercase letter or a number.
                    if ( ! isLowerOrNumber( asChars[0] )
                            || ! isLowerOrNumber( asChars[ asChars.length - 1 ] ) ) {
                        return false;
                    }

                    // Bucket names can contain lowercase
                    // letters, numbers, and dashes.
                    for (char ch : asChars) {
                        if (! (isLowerOrNumber(ch) || ch == '-') ) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });
        checks.add(new Validator<String>() {
            // Bucket names must not be formatted as an IP address
            // (e.g., 192.168.5.4).
            @Override
            public boolean check(String value) {
                return ! IP_MATCHER.matcher(value).matches();
            }
        });
        dnsCompliant.setValidators(checks);
        return dnsCompliant;
    }

    private static class IteratingValidator<T> implements Validator<T> {

        private List<Validator<T>> validators;

        @Override
        public boolean check(T bucketName) {
            // if no checks have been configured, it's probably best to consider the check a "pass"
            if (getValidators().size() < 1) {
                return true;
            }
            for (Validator<T> checker : getValidators()) {
                if (! checker.check(bucketName)) {
                    return false;
                }
            }
            return true;
        }

        public List<Validator<T>> getValidators() {
            if (validators == null) {
                return Lists.newArrayList();
            }
            return validators;
        }

        public void setValidators(List<Validator<T>> validators) {
            this.validators = validators;
        }
    }

    private static boolean isLowerOrNumber(char c) {
        if ((c >= 'a' && c <= 'z') // lowercase
                || (c >= '0' && c <= '9') ) { // number
            return true;
        }
        else {
            return false;
        }
    }

}
