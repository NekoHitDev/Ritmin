package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neow3j.contract.FungibleToken;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

public class Balance {

    private static final Logger log = LoggerFactory.getLogger(Balance.class);

    private static final Account target = Constants.CONTRACT_OWNER_ACCOUNT;

    public static void main(String[] args) throws Throwable {

        for (Map.Entry<Hash160, BigInteger> entry : target.getNep17Balances(Constants.NEOW3J).entrySet()) {
            FungibleToken token = new FungibleToken(entry.getKey(), Constants.NEOW3J);
            log.info("{} balance of target is: {} ", token.getSymbol(), token.getBalanceOf(target.getScriptHash()));
        }
    }
    
}
