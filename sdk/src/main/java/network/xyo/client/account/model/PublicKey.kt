package network.xyo.client.account.model

import network.xyo.client.lib.JsonSerializable

interface PublicKey {
    val address: ByteArray
    fun verify(msg: ByteArray, signature: ByteArray): Boolean

    val addressString: String
        get() = JsonSerializable.bytesToHex(address)
}