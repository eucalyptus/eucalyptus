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
$namespace('org.owasp.esapi.codecs');
 */


UTF8 = {
    encode: function(sInput) {
        var input = sInput.replace(/\r\n/g, "\n");
        var utftext = '';

        for (var n = 0; n < input.length; n ++) {
            var c = input.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if (( c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }
        }

        return utftext;
    }
    ,

    decode: function(sInput) {
        var out = '';
        var i = c = c1 = c2 = 0;

        while (i < sInput.length) {
            c = sInput.charCodeAt(i);

            if (c < 128) {
                out += String.fromCharCode(c);
                i ++;
            }
            else if ((c > 191) && (c < 224)) {
                c2 = sInput.charCodeAt(i + 1);
                out += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                c2 = utftext.charCodeAt(i + 1);
                c3 = utftext.charCodeAt(i + 2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }
        }

        return out;
    }
};
