package network.xyo.client.account.bip85

import tech.figure.hdwallet.bip32.ExtKey
import tech.figure.hdwallet.ec.extensions.toBytesPadded
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Bip85 {
    const val BIP85_PURPOSE = "83696968'"
    const val BIP85_HEX_APP = "128169'"
    const val BIP85_HMAC_KEY = "bip-entropy-from-k"
    const val BIP85_SEED_LENGTH = 32

    fun bip85DefaultLeafTail(index: Int = 0): String {
        return "$BIP85_PURPOSE/$BIP85_HEX_APP/32'/$index'"
    }

    fun isBip85AbsolutePath(path: String): Boolean {
        return path.startsWith("m/$BIP85_PURPOSE/")
    }

    fun isUnderBip85Purpose(path: String?): Boolean {
        if (path == null) return false
        return path == "m/$BIP85_PURPOSE" || path.startsWith("m/$BIP85_PURPOSE/")
    }

    fun bip85EntropyFromNode(node: ExtKey): ByteArray {
        val keyBytes = BIP85_HMAC_KEY.toByteArray(Charsets.UTF_8)
        val msgBytes = node.keyPair.privateKey.key.toBytesPadded(32)
        
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA512"))
        
        val hmac = mac.doFinal(msgBytes)
        return hmac.copyOfRange(0, BIP85_SEED_LENGTH)
    }
}
