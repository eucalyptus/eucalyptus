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


PercentCodec = function() {
    var _super = new Codec();

    var ALPHA_NUMERIC_STR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    var RFC_NON_ALPHANUMERIC_UNRESERVED_STR = "-._~";
    var ENCODED_NON_ALPHA_NUMERIC_UNRESERVED = true;
    var UNENCODED_STR = ALPHA_NUMERIC_STR + (ENCODED_NON_ALPHA_NUMERIC_UNRESERVED ? "" : RFC_NON_ALPHANUMERIC_UNRESERVED_STR);

    var getTwoUpperBytes = function(b) {
        var out = '';
        if (b < -128 || b > 127) {
            throw new IllegalArgumentException("b is not a byte (was " + b + ")");
        }
        b &= 0xFF;
        if (b < 0x10) {
            out += '0';
        }
        return out + b.toString(16).toUpperCase();
    };

    return {
        encode: _super.encode,

        decode: _super.decode,

        encodeCharacter: function(aImmune, c) {
            if (UNENCODED_STR.indexOf(c) > -1) {
                return c;
            }

            var bytes = UTF8.encode(c);
            var out = '';
            for (var b = 0; b < bytes.length; b++) {
                out += '%' + getTwoUpperBytes(bytes.charCodeAt(b));
            }
            return out;
        },

        decodeCharacter: function(oPushbackString) {
            oPushbackString.mark();
            var first = oPushbackString.next();
            if (first == null || first != '%') {
                oPushbackString.reset();
                return null;
            }

            var out = '';
            for (var i = 0; i < 2; i++) {
                var c = oPushbackString.nextHex();
                if (c != null) {
                    out += c;
                }
            }
            if (out.length == 2) {
                try {
                    var n = parseInt(out, 16);
                    return String.fromCharCode(n);
                } catch (e) {
                }
            }
            oPushbackString.reset();
            return null;
        }
    };
};
