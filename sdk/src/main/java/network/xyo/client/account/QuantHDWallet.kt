package network.xyo.client.account

import network.xyo.client.account.bip85.Bip85
import network.xyo.client.account.model.PreviousHashStore
import network.xyo.client.account.model.Wallet
import network.xyo.client.account.model.WalletStatic
import network.xyo.client.lib.hexStringToByteArray
import tech.figure.hdwallet.bip32.AccountType
import tech.figure.hdwallet.bip32.ExtKey
import tech.figure.hdwallet.bip32.toRootKey
import tech.figure.hdwallet.bip39.DeterministicSeed
import tech.figure.hdwallet.bip39.MnemonicWords

class QuantHDWallet private constructor(
    private val _extKey: ExtKey,
    val path: String?,
    private val _master: ExtKey?,
    private val _account: QuantAccount
): QuantAccount(_account.privateKey, _account.previousHash), Wallet {

    override fun derivePath(path: String): QuantHDWallet {
        if (path.startsWith("m/")) {
            if (Bip85.isBip85AbsolutePath(path)) {
                if (_master == null) {
                    throw Exception("Cannot derive BIP-85 absolute path: wallet has no master node")
                }
                val childNode = _master.childKey(path)
                return createFromNode(childNode, path, _master)
            }
            val parentPath = this.path
            if (parentPath != null && path.startsWith(parentPath)) {
                val childPath = path.substring(parentPath.length + 1)
                val newPath = if (parentPath.endsWith("/")) parentPath + childPath else parentPath + "/" + childPath
                return createFromNode(this._extKey.copy(depth = AccountType.ROOT).childKey("m/" + childPath), newPath, _master)
            }
            if (_master != null) {
                return createFromNode(_master.childKey(path), path, _master)
            }
            throw Exception("Invalid absolute path $path for wallet with path $parentPath")
        }
        val newPath = if (this.path == null) null else (if (this.path.endsWith("/")) this.path + path else this.path + "/" + path)
        return createFromNode(this._extKey.copy(depth = AccountType.ROOT).childKey("m/" + path), newPath, _master)
    }

    companion object: WalletStatic<QuantHDWallet> {
        val defaultPath = "m/44'/60'/0'/0/0"
        override var previousHashStore: PreviousHashStore? = null

        override fun fromExtendedKey(key: ExtKey): QuantHDWallet {
            val master = if (key.depth == AccountType.ROOT) key else null
            return createFromNode(key, null, master)
        }

        override fun fromMnemonic(mnemonic: String, path: String?): QuantHDWallet {
            return fromMnemonic(MnemonicWords.of(mnemonic), path)
        }

        override fun fromMnemonic(mnemonic: MnemonicWords, path: String?): QuantHDWallet {
            val seed = mnemonic.toSeed("".toCharArray())
            val master = seed.toRootKey()
            
            val targetPath = path ?: defaultPath
            val node = if (targetPath == "m" || targetPath == "m/") master else {
                val fullPath = if (targetPath.startsWith("m/")) targetPath else "m/$targetPath"
                master.childKey(fullPath)
            }
            return createFromNode(node, targetPath, master)
        }

        override fun fromSeed(seed: String): QuantHDWallet {
            return fromSeed(hexStringToByteArray(seed))
        }

        override fun fromSeed(seed: ByteArray): QuantHDWallet {
            return fromSeed(DeterministicSeed.fromBytes(seed))
        }

        override fun fromSeed(seed: DeterministicSeed): QuantHDWallet {
            val master = seed.toRootKey()
            return createFromNode(master, "m", master)
        }

        private fun createFromNode(
            node: ExtKey,
            path: String?,
            master: ExtKey?,
            previousHash: ByteArray? = null
        ): QuantHDWallet {
            val leafNode = if (Bip85.isUnderBip85Purpose(path)) node else node.copy(depth = AccountType.ROOT).childKey("m/" + Bip85.bip85DefaultLeafTail(0))
            val seed = Bip85.bip85EntropyFromNode(leafNode)
            val account = QuantAccount(seed, previousHash)
            return QuantHDWallet(node, path, master, account)
        }
    }
}
