package me.jadenp.notbounties.sql;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SQLGetterTest {

    @Test
    void decodeWhitelist() {
        SQLGetter data = new SQLGetter();
        assertEquals(data.decodeWhitelist("c4284151-77e5-4a85-9b81-dcd41b9aebd4").get(0), UUID.fromString("c4284151-77e5-4a85-9b81-dcd41b9aebd4"));
        List<UUID> whitelist = data.decodeWhitelist("c4284151-77e5-4a85-9b81-dcd41b9aebd4,db177807-ea4a-434b-a0d7-1ea9d0b4b154,f9a64d8d-8d75-40ea-9353-f69b5e40e8e8");
        assertEquals(whitelist.get(0), UUID.fromString("c4284151-77e5-4a85-9b81-dcd41b9aebd4"));
        assertEquals(whitelist.get(1), UUID.fromString("db177807-ea4a-434b-a0d7-1ea9d0b4b154"));
        assertEquals(whitelist.get(2), UUID.fromString("f9a64d8d-8d75-40ea-9353-f69b5e40e8e8"));
    }

    @Test
    void encodeWhitelist() {
        SQLGetter data = new SQLGetter();
        assertEquals(data.encodeWhitelist(Arrays.asList(UUID.fromString("c4284151-77e5-4a85-9b81-dcd41b9aebd4"), UUID.fromString("db177807-ea4a-434b-a0d7-1ea9d0b4b154"), UUID.fromString("f9a64d8d-8d75-40ea-9353-f69b5e40e8e8")))
                , "c4284151-77e5-4a85-9b81-dcd41b9aebd4,db177807-ea4a-434b-a0d7-1ea9d0b4b154,f9a64d8d-8d75-40ea-9353-f69b5e40e8e8");
    }
}