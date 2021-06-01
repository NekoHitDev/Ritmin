package info.skyblond.nekohit.example;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.ContractParameterType;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    public static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://127.0.0.1:50012"));

    public static final FungibleToken CAT_TOKEN = new FungibleToken(
            new Hash160("df5526bbbaa3a4f01d14d4455f564c45859f2fa7"), NEOW3J);
    public static final GasToken GAS_TOKEN = new GasToken(NEOW3J);

    // ContractOwner defind in `devnet.neo-express`
    public static final Account CONTRACT_OWNER_ACCOUNT = new Account(
            ECKeyPair.create(new BigInteger("95ba67afd784f405e2800a0bcb875c035c41545d4a11e8995f6f1175d95c2952", 16)));

    public static final Wallet CONTRACT_OWNER_WALLET = Wallet.withAccounts(CONTRACT_OWNER_ACCOUNT);

    public static final Account GENESIS_ACCOUNT = Account.fromAddress("NKvMswbG7QsRTEU9dkY2uY7ReKxmhXrc1M");

    public static final Hash160 WCA_CONTRACT_HASH = Hash160.fromAddress("NR6We6gPCDmrYzv8PsVGvRzHG9ki5TpZrd");

    public static double getGasWithDecimals(long value) {
        return value / Math.pow(10, 8);
    }

    public static double getGasWithDecimals(BigInteger value) {
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(8)).doubleValue();
    }

    public static double getCatWithDecimals(long value) {
        return value / Math.pow(10, 2);
    }

    public static double getCatWithDecimals(BigInteger value) {
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(2)).doubleValue();
    }

    private static void transferToken(FungibleToken token, Wallet wallet, Hash160 to, long amount, String identifier)
            throws Throwable {
        var tx = token
                .transferFromDefaultAccount(wallet, to, BigInteger.valueOf(amount),
                        ContractParameter.string(identifier))
                .signers(Signer.calledByEntry(wallet.getDefaultAccount().getScriptHash())).sign();
        var response = tx.send();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                log.info("Transfer token tx: {}", tx.getTxId());
            });
        } else {
            log.error("Error when transfer token: {}", response.getError().getMessage());
        }
        log.info("Transfer token gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
    }

    public static void transferCatToken(Wallet wallet, Hash160 to, long amount, String identifier) throws Throwable {
        transferToken(CAT_TOKEN, wallet, to, amount, identifier);
    }

    public static void transferGasToken(Wallet wallet, Hash160 to, long amount) throws Throwable {
        transferToken(GAS_TOKEN, wallet, to, amount, null);
    }

    public static Wallet prepaWallet(long amount) throws Throwable {
        var testWallet = Wallet.create();
        transferCatToken(CONTRACT_OWNER_WALLET, testWallet.getDefaultAccount().getScriptHash(), amount, null);
        transferGasToken(CONTRACT_OWNER_WALLET, testWallet.getDefaultAccount().getScriptHash(), 1_00000000L);
        return testWallet;
    }

    /**
     * This is a custome version to support long values.
     * @see ContractParameter#array(Object...)
     */
    public static ContractParameter arrayParameter(Object... entries) {
        if (entries.length == 0) {
            throw new IllegalArgumentException("At least one parameter is required to create an " +
                    "array contract parameter.");
        }
        if (Arrays.stream(entries).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Cannot add a null object to an array contract " +
                    "parameter.");
        }
        ContractParameter[] params = Arrays.stream(entries)
                .map(Utils::castToContractParameter)
                .toArray(ContractParameter[]::new);
        return new ContractParameter(null, ContractParameterType.ARRAY, params);
    }

    private static ContractParameter castToContractParameter(Object o) {
        if (o instanceof ContractParameter) {
            return (ContractParameter) o;
        } else if (o instanceof Boolean) {
            return ContractParameter.bool((Boolean) o);
        } else if (o instanceof Integer) {
            return ContractParameter.integer((Integer) o);
        } else if (o instanceof Long) {
            return ContractParameter.integer(BigInteger.valueOf((Long) o));
        } else if (o instanceof BigInteger) {
            return ContractParameter.integer((BigInteger) o);
        } else if (o instanceof byte[]) {
            return ContractParameter.byteArray((byte[]) o);
        } else if (o instanceof String) {
            return ContractParameter.string((String) o);
        } else {
            throw new IllegalArgumentException("The provided object could not be casted into " +
                    "a supported contract parameter type.");
        }
    }
}
