package network.xyo.client.account.address

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XyoAddressTest {

    private val legacyAddress = "1234567890abcdef1234567890abcdef12345678"
    private val expectedQuantAddress = "qm651qyyq79says4nyw2qga892hrrdfchsluxleee2j"
    private val garbage = "not-an-address"

    private val addressBytes = ByteArray(20) { i ->
        ((i * 7 + 1) and 0xff).toByte()
    }

    @Test
    fun testLegacyAddressValidation() {
        assertTrue(XyoAddress.isLegacyAddress(legacyAddress))
        assertFalse(XyoAddress.isLegacyAddress(expectedQuantAddress))
        assertFalse(XyoAddress.isLegacyAddress(garbage))
    }

    @Test
    fun testQuantAddressEncoding() {
        val encoded = XyoAddress.encodeQuantAddress("qm65", addressBytes)
        assertEquals(expectedQuantAddress, encoded)
    }

    @Test
    fun testQuantAddressDecoding() {
        val decoded = XyoAddress.tryDecodeQuantAddress(expectedQuantAddress)
        assertNotNull(decoded)
        assertEquals("qm65", decoded!!.hrp)
        assertArrayEquals(addressBytes, decoded.bytes)
    }

    @Test
    fun testQuantAddressValidation() {
        assertTrue(XyoAddress.isQuantAddress(expectedQuantAddress))
        assertFalse(XyoAddress.isQuantAddress(expectedQuantAddress.uppercase())) // Bech32m addresses must be lowercase strictly for canonical checks in our wrapper
        assertFalse(XyoAddress.isQuantAddress(legacyAddress))
        assertFalse(XyoAddress.isQuantAddress(garbage))
    }

    @Test
    fun testAddressValidation() {
        assertTrue(XyoAddress.isAddress(legacyAddress))
        assertTrue(XyoAddress.isAddress(expectedQuantAddress))
        assertFalse(XyoAddress.isAddress(garbage))
    }

    @Test
    fun testTamperedChecksumRejected() {
        // Tamper with the last character
        val lastChar = expectedQuantAddress.last()
        val tampered = expectedQuantAddress.dropLast(1) + (if (lastChar == 'q') 'p' else 'q')
        assertFalse(XyoAddress.isQuantAddress(tampered))
        assertNull(XyoAddress.tryDecodeQuantAddress(tampered))
    }
}
