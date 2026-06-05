package network.xyo.client.account

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import network.xyo.client.lib.byteArrayToHexString
import network.xyo.client.lib.hexStringToByteArray

class QuantAccountTest {

    @Test
    fun testQuantAccountNistAcvpKeyGenVector() = runBlocking {
        // From paulmillr/acvp-vectors -> ML-DSA-keyGen-FIPS204 first ML-DSA-65 AFT case
        val seedHex = "1bd67dc782b2958e189e315c040dd1f64c8ab232a6a170e1a7a52c33f10851b1"
        val pkFirst32Hex = "43ad6560d3bb684667a559ee6ec7c816020e5b65671f270f2353a8c912b6c26b"
        val pkLast32Hex = "486432b090f0cce86aa84b661ff22d4a56035e821a1ce30f33afeb6c7b8fa9ce"

        val account = QuantAccount(hexStringToByteArray(seedHex))
        
        // ML-DSA-65 public key size is 1952 bytes
        assertEquals(1952, account.publicKey.size)
        
        val pkHex = byteArrayToHexString(account.publicKey)
        assertEquals(pkFirst32Hex, pkHex.take(64))
        assertEquals(pkLast32Hex, pkHex.takeLast(64))
    }

    @Test
    fun testQuantAccountRoundTripSignVerify() = runBlocking {
        val seedHex = "1bd67dc782b2958e189e315c040dd1f64c8ab232a6a170e1a7a52c33f10851b1"
        val account = QuantAccount(hexStringToByteArray(seedHex))

        val message = hexStringToByteArray("deadbeefcafef00d")
        val signature = account.sign(message)

        assertTrue(account.verify(message, signature))
    }
}
