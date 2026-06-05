package network.xyo.client.account.address

object Bech32m {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val BECH32M_CONST = 0x2bc830a3

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        val generator = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        for (value in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor (value.toInt() and 0xff)
            for (i in 0 until 5) {
                if (((top ushr i) and 1) != 0) {
                    chk = chk xor generator[i]
                }
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val hrpLen = hrp.length
        val result = ByteArray(hrpLen * 2 + 1)
        for (i in 0 until hrpLen) {
            val c = hrp[i].code
            result[i] = (c ushr 5).toByte()
            result[i + hrpLen + 1] = (c and 31).toByte()
        }
        result[hrpLen] = 0
        return result
    }

    private fun verifyChecksum(hrp: String, data: ByteArray): Boolean {
        val expanded = hrpExpand(hrp)
        val values = ByteArray(expanded.size + data.size)
        System.arraycopy(expanded, 0, values, 0, expanded.size)
        System.arraycopy(data, 0, values, expanded.size, data.size)
        return polymod(values) == BECH32M_CONST
    }

    private fun createChecksum(hrp: String, data: ByteArray): ByteArray {
        val expanded = hrpExpand(hrp)
        val values = ByteArray(expanded.size + data.size + 6)
        System.arraycopy(expanded, 0, values, 0, expanded.size)
        System.arraycopy(data, 0, values, expanded.size, data.size)
        val pm = polymod(values) xor BECH32M_CONST
        val checksum = ByteArray(6)
        for (i in 0 until 6) {
            checksum[i] = ((pm ushr (5 * (5 - i))) and 31).toByte()
        }
        return checksum
    }

    fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray? {
        var acc = 0
        var bits = 0
        val ret = mutableListOf<Byte>()
        val maxv = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (value in data) {
            val v = value.toInt() and 0xff
            if (v ushr fromBits != 0) {
                return null
            }
            acc = ((acc shl fromBits) or v) and maxAcc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                ret.add(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) {
                ret.add(((acc shl (toBits - bits)) and maxv).toByte())
            }
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            return null
        }
        return ret.toByteArray()
    }

    fun encode(hrp: String, data: ByteArray): String {
        val converted = convertBits(data, 8, 5, true) ?: throw IllegalArgumentException("Could not convert bits")
        val checksum = createChecksum(hrp, converted)
        val combined = ByteArray(converted.size + checksum.size)
        System.arraycopy(converted, 0, combined, 0, converted.size)
        System.arraycopy(checksum, 0, combined, converted.size, checksum.size)
        
        val sb = StringBuilder()
        sb.append(hrp)
        sb.append('1')
        for (b in combined) {
            sb.append(CHARSET[b.toInt()])
        }
        return sb.toString()
    }

    class Decoded(val hrp: String, val data: ByteArray)

    fun decode(bechString: String): Decoded? {
        if (bechString.length < 8 || bechString.length > 90) return null
        var hasLower = false
        var hasUpper = false
        for (c in bechString) {
            if (c in 'a'..'z') hasLower = true
            if (c in 'A'..'Z') hasUpper = true
        }
        if (hasLower && hasUpper) return null
        val str = bechString.lowercase()
        val pos = str.lastIndexOf('1')
        if (pos < 1 || pos + 7 > str.length) return null
        val hrp = str.substring(0, pos)
        for (c in hrp) {
            if (c.code < 33 || c.code > 126) return null
        }
        val data = ByteArray(str.length - pos - 1)
        for (i in pos + 1 until str.length) {
            val d = CHARSET.indexOf(str[i])
            if (d == -1) return null
            data[i - pos - 1] = d.toByte()
        }
        if (!verifyChecksum(hrp, data)) return null
        val converted = convertBits(data.copyOfRange(0, data.size - 6), 5, 8, false) ?: return null
        return Decoded(hrp, converted)
    }
}
