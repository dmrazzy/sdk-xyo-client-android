package network.xyo.chain.protocol.rpc.e2e

import kotlinx.coroutines.runBlocking
import network.xyo.chain.protocol.model.DefaultTransactionFees
import network.xyo.chain.protocol.payload.TransferPayload
import network.xyo.chain.protocol.rpc.runner.JsonRpcMempoolRunner
import network.xyo.chain.protocol.rpc.viewer.JsonRpcAccountBalanceViewer
import network.xyo.chain.protocol.rpc.viewer.JsonRpcBlockViewer
import network.xyo.chain.protocol.sdk.transaction.TransactionBuilder
import network.xyo.chain.protocol.xl1.AttoXL1
import network.xyo.client.account.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.math.BigInteger

@EnabledIfEnvironmentVariable(named = "XL1_E2E_RPC_URL", matches = ".+")
class Xl1CliTransferE2eTest {
    private val transport = E2eRpcSupport.transport()
    private val blockViewer = JsonRpcBlockViewer(transport)
    private val mempoolRunner = JsonRpcMempoolRunner(transport)
    private val accountBalanceViewer = JsonRpcAccountBalanceViewer(transport)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `transfers from the genesis reward address across several produced blocks`() = runBlocking {
        val sender = E2eRpcSupport.genesisRewardWallet()
        val senderAddress = sender.addressString
        val recipients = List(3) { Account.random() }
        val initialHead = blockViewer.currentBlock()

        val initialBalances = accountBalanceViewer.qualifiedAccountBalances(
            listOf(senderAddress) + recipients.map { it.addressString },
            network.xyo.chain.protocol.model.AccountBalanceConfig(),
        )
        val initialSenderBalance = initialBalances.data[senderAddress] ?: AttoXL1.ZERO
        val maxFeeExposure = AttoXL1.of(
            DefaultTransactionFees.default.base +
                DefaultTransactionFees.default.priority +
                DefaultTransactionFees.default.gasLimit,
        )
        val amount = conservativeTransferAmount(
            initialSenderBalance = initialSenderBalance,
            maxFeeExposure = maxFeeExposure,
            transferCount = recipients.size,
        )

        assertTrue(
            amount > AttoXL1.ZERO,
            "sender balance is too small for a safe transfer after fees; balance=$initialSenderBalance fees=$maxFeeExposure",
        )
        recipients.forEach { recipient ->
            val recipientAddress = recipient.addressString
            val initialRecipientBalance = initialBalances.data[recipientAddress] ?: AttoXL1.ZERO
            assertEquals(AttoXL1.ZERO, initialRecipientBalance, "fresh recipient should start at zero")
        }

        var observedBlock = initialHead.boundWitness.block
        recipients.forEach { recipient ->
            val recipientAddress = recipient.addressString
            val currentHead = blockViewer.currentBlock()
            val transaction = TransactionBuilder()
                .chain(currentHead.boundWitness.chain)
                .from(senderAddress)
                .signer(sender)
                .payload(
                    TransferPayload(
                        from = senderAddress,
                        transfers = mapOf(recipientAddress to amount.toHex().removePrefix("0x")),
                        epoch = System.currentTimeMillis(),
                    ),
                )
                .fees(DefaultTransactionFees.default)
                .range(currentHead.boundWitness.block, currentHead.boundWitness.block + 1000)
                .build()

            val signedTransaction = E2eRpcSupport.toSignedTransaction(transaction)
            mempoolRunner.submitTransactions(listOf(signedTransaction)).single()

            val advancedBlock = E2eRpcSupport.awaitBlockAtLeast(
                viewer = blockViewer,
                expectedMinimum = observedBlock + 1,
            )
            assertTrue(
                advancedBlock > observedBlock,
                "expected a new block after submitting transfer to $recipientAddress",
            )
            observedBlock = advancedBlock

            val finalBalances = E2eRpcSupport.awaitBalanceAtLeast(
                viewer = accountBalanceViewer,
                address = recipientAddress,
                expectedMinimum = amount,
            )

            assertEquals(amount, finalBalances.data[recipientAddress])
        }

        assertTrue(
            observedBlock >= initialHead.boundWitness.block + recipients.size,
            "expected at least ${recipients.size} new blocks after genesis; initial=${initialHead.boundWitness.block} final=$observedBlock",
        )
    }

    private fun conservativeTransferAmount(
        initialSenderBalance: AttoXL1,
        maxFeeExposure: AttoXL1,
        transferCount: Int,
    ): AttoXL1 {
        val count = BigInteger.valueOf(transferCount.toLong())
        val totalFeeBudget = maxFeeExposure.value.multiply(count)
        // Leave an extra fee budget in reserve to avoid skating exactly on the edge.
        val safeBudget = initialSenderBalance.value - totalFeeBudget - maxFeeExposure.value
        if (safeBudget <= BigInteger.ZERO) return AttoXL1.ZERO

        val perTransferBudget = safeBudget.divide(count)
        // Stay well below the maximum affordable amount per transfer.
        val conservativeAmount = perTransferBudget.divide(BigInteger.valueOf(4))
        return AttoXL1.ofOrNull(conservativeAmount) ?: AttoXL1.ZERO
    }
}
