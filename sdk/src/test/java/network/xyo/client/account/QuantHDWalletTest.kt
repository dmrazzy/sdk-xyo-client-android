package network.xyo.client.account

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import network.xyo.client.lib.byteArrayToHexString
import network.xyo.client.lib.hexStringToByteArray
import tech.figure.hdwallet.bip32.ExtKey
import tech.figure.hdwallet.encoding.base58.base58DecodeChecked

class QuantHDWalletTest {

    private val BIP85_MASTER_XPRV = "xprv9s21ZrQH143K2LBWUUQRFXhucrQqBpKdRRxNVq2zBqsx8HVqFk2uYo8kmbaLLHRdqtQpUm98uKfu3vca1LqdGhUtyoFnCNkfmXRyPXLjbKb"
    private val testHash = hexStringToByteArray("4b688df40bcedbe641ddb16ff0a1842d9c67ea1c3bf63f3e0471baa664531d1a")

    @Test
    fun testSignsAndVerifiesFromBip85MasterXprv() = runBlocking {
        val extKey = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val wallet = QuantHDWallet.fromExtendedKey(extKey)
        
        // 20 bytes equivalent to JS SDK address format
        assertEquals(20, wallet.address.size)
        assertTrue(wallet.addressString.startsWith("qm65"))
        assertEquals(43, wallet.addressString.length)

        val signature = wallet.sign(testHash)
        assertTrue(wallet.verify(testHash, signature))
    }

    @Test
    fun testDerivesViaExplicitBip85AbsolutePath() = runBlocking {
        val extKey = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val wallet = QuantHDWallet.fromExtendedKey(extKey)
        val child = wallet.derivePath("m/83696968'/128169'/32'/5'")
        
        assertNotEquals(wallet.addressString, child.addressString)
        assertTrue(child.addressString.startsWith("qm65"))
        assertEquals(43, child.addressString.length)

        val signature = child.sign(testHash)
        assertTrue(child.verify(testHash, signature))
    }

    @Test
    fun testDerivesViaBip85BaseCasePath() = runBlocking {
        val extKey = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val wallet = QuantHDWallet.fromExtendedKey(extKey)
        val child = wallet.derivePath("m/83696968'/0'/0'")

        assertTrue(child.addressString.startsWith("qm65"))
        val signature = child.sign(testHash)
        assertTrue(child.verify(testHash, signature))
    }

    @Test
    fun testDerivesViaClassicalBip32PathAutoExtendsToHexLeaf() = runBlocking {
        val extKey = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val wallet = QuantHDWallet.fromExtendedKey(extKey)
        val child = wallet.derivePath("m/44'/60'/0'/0/7")

        assertTrue(child.addressString.startsWith("qm65"))
        val signature = child.sign(testHash)
        assertTrue(child.verify(testHash, signature))
    }

    @Test
    fun testTwoDerivationsProduceSameAddress() = runBlocking {
        val extKey1 = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val extKey2 = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        
        val w1 = QuantHDWallet.fromExtendedKey(extKey1)
        val w2 = QuantHDWallet.fromExtendedKey(extKey2)
        
        val a = w1.derivePath("m/83696968'/128169'/32'/42'")
        val b = w2.derivePath("m/83696968'/128169'/32'/42'")
        
        assertEquals(a.addressString, b.addressString)
    }
}
