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


CSSCodec = function() {
    var _super = new Codec();

    return {
        encode: _super.encode,

        decode: _super.decode,

        encodeCharacter: function(aImmune, c) {
            if (aImmune.indexOf(c) != -1) {
                return c;
            }

            var hex = Codec.getHexForNonAlphanumeric(c);
            if (hex == null) {
                return c;
            }

            return "\\" + hex + " ";
        },

        decodeCharacter: function(oPushbackString) {
            oPushbackString.mark();
            var first = oPushbackString.next();
            if (first == null) {
                oPushbackString.reset();
                return null;
            }

            if (first != '\\') {
                oPushbackString.reset();
                return null;
            }

            var second = oPushbackString.next();
            if (second == null) {
                oPushbackString.reset();
                return null;
            }

            if (oPushbackString.isHexDigit(second)) {
                var out = second;
                for (var i = 0; i < 6; i ++) {
                    var c = oPushbackString.next();
                    if (c == null || c.charCodeAt(0) == 0x20) {
                        break;
                    }
                    if (oPushbackString.isHexDigit(c)) {
                        out += c;
                    } else {
                        input.pushback(c);
                        break;
                    }
                }

                try {
                    var n = parseInt(out, 16);
                    return String.fromCharCode(n);
                } catch (e) {
                    oPushbackString.reset();
                    return null;
                }
            }

            return second;
        }
    };
};
