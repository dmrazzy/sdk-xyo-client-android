package network.xyo.client.account.address

object XyoAddress {
    const val QUANT_ADDRESS_BYTE_LENGTH = 20

    fun isLegacyAddress(address: String): Boolean {
        if (address.length != 40) return false
        return address.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun isQuantAddress(address: String): Boolean {
        if (address != address.lowercase()) return false
        val decoded = tryDecodeQuantAddress(address) ?: return false
        return decoded.bytes.size == QUANT_ADDRESS_BYTE_LENGTH
    }

    fun isAddress(address: String): Boolean {
        return isLegacyAddress(address) || isQuantAddress(address)
    }

    class DecodedQuantAddress(val hrp: String, val bytes: ByteArray)

    fun tryDecodeQuantAddress(address: String): DecodedQuantAddress? {
        if (address != address.lowercase()) return null
        val decoded = Bech32m.decode(address) ?: return null
        if (decoded.data.size != QUANT_ADDRESS_BYTE_LENGTH) return null
        return DecodedQuantAddress(decoded.hrp, decoded.data)
    }

    fun encodeQuantAddress(hrp: String, bytes: ByteArray): String {
        return Bech32m.encode(hrp, bytes)
    }
}
