package com.magomez.androidapps.regalos.linkpreview;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Which addresses a link preview may connect to: only the public internet. Loopback, private
 * networks, link-local (169.254.x.x, where cloud metadata lives), carrier-grade NAT, multicast and
 * the reserved ranges are all refused, so a pasted link cannot make the server reach anything of
 * Heroku's or its own.
 */
public final class PublicAddresses {

    private PublicAddresses() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    public static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        if (address instanceof Inet4Address) {
            return isPublicV4(address.getAddress());
        }
        if (address instanceof Inet6Address) {
            return isPublicV6(address.getAddress());
        }
        return false;
    }

    private static boolean isPublicV4(byte[] ip) {
        int a = ip[0] & 0xFF;
        int b = ip[1] & 0xFF;
        int c = ip[2] & 0xFF;
        return a != 0                                   // 0.0.0.0/8 "this network"
                && a != 10 && a != 127                  // private, loopback
                && !(a == 169 && b == 254)              // link-local, cloud metadata
                && !(a == 172 && b >= 16 && b <= 31)    // private
                && !(a == 192 && b == 168)              // private
                && !(a == 100 && b >= 64 && b <= 127)   // 100.64.0.0/10 carrier-grade NAT
                && !(a == 192 && b == 0 && c == 0)      // 192.0.0.0/24 IETF protocol assignments
                && !(a == 192 && b == 0 && c == 2)      // 192.0.2.0/24 documentation
                && !(a == 198 && (b == 18 || b == 19))  // 198.18.0.0/15 benchmarking
                && !(a == 198 && b == 51 && c == 100)   // 198.51.100.0/24 documentation
                && !(a == 203 && b == 0 && c == 113)    // 203.0.113.0/24 documentation
                && a < 240;                             // 240.0.0.0/4 reserved and broadcast
    }

    private static boolean isPublicV6(byte[] ip) {
        int first = ip[0] & 0xFF;
        if ((first & 0xFE) == 0xFC) {
            return false;                               // fc00::/7 unique local
        }
        if (isMappedV4(ip) || isNat64(ip)) {
            // ::ffff:a.b.c.d and 64:ff9b::a.b.c.d reach an IPv4 address: judge that one.
            return isPublicV4(new byte[]{ip[12], ip[13], ip[14], ip[15]});
        }
        return first != 0;                              // ::/8 reserved (::, ::1, IPv4-compatible)
    }

    private static boolean isMappedV4(byte[] ip) {
        for (int i = 0; i < 10; i++) {
            if (ip[i] != 0) {
                return false;
            }
        }
        return (ip[10] & 0xFF) == 0xFF && (ip[11] & 0xFF) == 0xFF;
    }

    private static boolean isNat64(byte[] ip) {
        if ((ip[0] & 0xFF) != 0x00 || (ip[1] & 0xFF) != 0x64 || (ip[2] & 0xFF) != 0xFF || (ip[3] & 0xFF) != 0x9B) {
            return false;
        }
        for (int i = 4; i < 12; i++) {
            if (ip[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
