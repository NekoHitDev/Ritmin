package info.skyblond.nekohit.example;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Signer;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class TokenTransfer {

    private static final Logger log = LoggerFactory.getLogger(TokenTransfer.class);

    // config
    private static final Wallet fromWallet      = Constants.GENESIS_WALLET;
    private static final Account receiver       = Constants.CONTRACT_OWNER_ACCOUNT;
    private static final BigInteger amount      = BigInteger.valueOf(10000000_00);
    private static final FungibleToken token    = Constants.GAS_TOKEN;
    public static void main(String[] args) throws Throwable {

        log.info("transfer {} {} token(s)", amount, token.getSymbol());
        log.info("sender old balance: {} {}", token.getBalanceOf(fromWallet.getDefaultAccount()), token.getSymbol());
        log.info("receiver old balance: {} {}", token.getBalanceOf(receiver), token.getSymbol());

        // do transfer
        NeoSendRawTransaction tx = token.transferFromDefaultAccount(
                fromWallet, 
                receiver.getScriptHash(), 
                amount
            ).signers(Signer.calledByEntry(fromWallet.getDefaultAccount())).sign().send();

        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }

        Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), Constants.NEOW3J);

        log.info("sender new balance: {} {}", token.getBalanceOf(fromWallet.getDefaultAccount()), token.getSymbol());
        log.info("receiver new balance: {} {}", token.getBalanceOf(receiver), token.getSymbol());
    }
}
