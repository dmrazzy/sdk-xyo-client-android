package network.xyo.client.account.bip85

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.figure.hdwallet.bip32.ExtKey
import tech.figure.hdwallet.encoding.base58.base58DecodeChecked
import network.xyo.client.lib.byteArrayToHexString
import network.xyo.client.lib.hexStringToByteArray

class Bip85Test {

    private val BIP85_MASTER_XPRV = "xprv9s21ZrQH143K2LBWUUQRFXhucrQqBpKdRRxNVq2zBqsx8HVqFk2uYo8kmbaLLHRdqtQpUm98uKfu3vca1LqdGhUtyoFnCNkfmXRyPXLjbKb"

    @Test
    fun testBip85Paths() {
        assertEquals("83696968'/128169'/32'/0'", Bip85.bip85DefaultLeafTail())
        assertEquals("83696968'/128169'/32'/5'", Bip85.bip85DefaultLeafTail(5))

        assertEquals(true, Bip85.isBip85AbsolutePath("m/83696968'/128169'"))
        assertEquals(false, Bip85.isBip85AbsolutePath("m/44'/60'/0'/0/0"))

        assertEquals(true, Bip85.isUnderBip85Purpose("m/83696968'"))
        assertEquals(true, Bip85.isUnderBip85Purpose("m/83696968'/128169'"))
        assertEquals(false, Bip85.isUnderBip85Purpose("m/44'/60'/0'/0/0"))
    }

    @Test
    fun testBip85SpecHmacCase1() {
        val master = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val node = master.childKey("m/83696968'/0'/0'")
        val entropy = Bip85.bip85EntropyFromNode(node)
        assertEquals("efecfbccffea313214232d29e71563d941229afb4338c21f9517c41aaa0d16f0", byteArrayToHexString(entropy))
    }

    @Test
    fun testBip85SpecHmacCase2() {
        val master = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val node = master.childKey("m/83696968'/0'/1'")
        val entropy = Bip85.bip85EntropyFromNode(node)
        assertEquals("70c6e3e8ebee8dc4c0dbba66076819bb8c09672527c4277ca8729532ad711872", byteArrayToHexString(entropy))
    }

    @Test
    fun testBip85SpecHexEntropyApp() {
        val master = ExtKey.deserialize(BIP85_MASTER_XPRV.base58DecodeChecked())
        val node = master.childKey("m/83696968'/128169'/64'/0'")
        val entropy = Bip85.bip85EntropyFromNode(node)
        assertEquals("492db4698cf3b73a5a24998aa3e9d7fa96275d85724a91e71aa2d645442f8785", byteArrayToHexString(entropy))
    }
}
