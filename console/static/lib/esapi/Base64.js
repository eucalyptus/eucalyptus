/*
 * OWASP Enterprise Security API (ESAPI)
 *
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * <a href="http://www.owasp.org/index.php/ESAPI">http://www.owasp.org/index.php/ESAPI</a>.
 *
 * Copyright (c) 2008 - The OWASP Foundation
 *
 * The ESAPI is published by OWASP under the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
$namespace('org.owasp.esapi.codecs.Base64');
 */


Base64 = {
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    encode: function(sInput) {
        if (!sInput) {
            return null;
        }

        var out = '';
        var ch1,ch2,ch3,enc1,enc2,enc3,enc4;
        var i = 0;

        var input = UTF8.encode(sInput);

        while (i < input.length) {
            ch1 = input.charCodeAt(i++);
            ch2 = input.charCodeAt(i++);
            ch3 = input.charCodeAt(i++);

            enc1 = ch1 >> 2;
            enc2 = ((ch1 & 3) << 4) | (ch2 >> 4);
            enc3 = ((ch2 & 15) << 2) | (ch3 >> 6);
            enc4 = ch3 & 63;

            if (isNaN(ch2)) {
                enc3 = enc4 = 64;
            }
            else if (isNaN(ch3)) {
                enc4 = 64;
            }

            out += this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) + this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);
        }

        return out;
    },

    decode: function(sInput) {
        if (!sInput) {
            return null;
        }

        var out = '';
        var ch1, ch2, ch3, enc1, enc2, enc3, enc4;
        var i = 0;

        var input = sInput.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {
            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            ch1 = (enc1 << 2) | (enc2 >> 4);
            ch2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            ch3 = ((enc3 & 3) << 6) | enc4;

            out += String.fromCharCode(ch1);
            if (enc3 != 64) {
                out += String.fromCharCode(ch2);
            }
            if (enc4 != 64) {
                out += String.fromCharCode(ch3);
            }
        }

        out = UTF8.decode(out);
        return out;
    }
};
