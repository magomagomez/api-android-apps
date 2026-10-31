package com.magomez.androidapps.regalos.linkpreview;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

/** A link preview only reaches the public internet: nothing of Heroku's network or the server itself. */
class PublicAddressesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "127.8.9.10", "0.0.0.0", "10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.1.18",
            "169.254.169.254", "100.64.0.1", "100.127.255.255", "192.0.0.8", "192.0.2.1", "198.18.0.1",
            "198.51.100.7", "203.0.113.9", "224.0.0.1", "240.0.0.1", "255.255.255.255",
            "::", "::1", "fe80::1", "fc00::1", "fd12:3456::1", "ff02::1", "::ffff:127.0.0.1", "::ffff:169.254.169.254",
            "64:ff9b::a00:1", "64:ff9b::7f00:1", "::127.0.0.1"})
    void privateLoopbackAndReservedAddressesAreRefused(String address) throws UnknownHostException {
        assertThat(PublicAddresses.isPublic(InetAddress.getByName(address))).as(address).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "151.101.1.69", "172.15.0.1", "172.32.0.1", "100.63.255.255", "100.128.0.1",
            "2a00:1450:4003:80e::200e", "2606:4700::6810:84e5", "64:ff9b::808:808"})
    void publicAddressesAreAllowed(String address) throws UnknownHostException {
        assertThat(PublicAddresses.isPublic(InetAddress.getByName(address))).as(address).isTrue();
    }
}
