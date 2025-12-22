export function decodeJwt(token) {
    const [headerB64, payloadB64] = token.split('.');

    if (!headerB64 || !payloadB64) {
        throw new Error("Invalid JWT: missing header or payload");
    }

    return {
        header: decodeJwtSegment(headerB64),
        payload: decodeJwtSegment(payloadB64)
    };
}

function decodeJwtSegment(segment) {
    const base64EncodedSegment = segment.replace(/-/g, '+').replace(/_/g, '/');
    const jsonSegment = decodeURIComponent(decodeBase64(base64EncodedSegment)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''))
        // Remove any NUL characters at the end of the string.
        .replace(/\0+$/g, '');
    return JSON.parse(jsonSegment);
}

function decodeBase64(input) {
    const _keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    let output = "";
    let chr1, chr2, chr3;
    let enc1, enc2, enc3, enc4;
    let i = 0;
    input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
    while (i < input.length) {
        enc1 = _keyStr.indexOf(input.charAt(i++));
        enc2 = _keyStr.indexOf(input.charAt(i++));
        enc3 = _keyStr.indexOf(input.charAt(i++));
        enc4 = _keyStr.indexOf(input.charAt(i++));
        chr1 = (enc1 << 2) | (enc2 >> 4);
        chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
        chr3 = ((enc3 & 3) << 6) | enc4;
        output = output + String.fromCharCode(chr1);
        if (enc3 !== 64) {
            output = output + String.fromCharCode(chr2);
        }
        if (enc4 !== 64) {
            output = output + String.fromCharCode(chr3);
        }
    }
    return decodeURI(output);
}