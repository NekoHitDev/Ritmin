package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.Arrays;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class Constants {

    public static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://127.0.0.1:50012"));

    public static final SmartContract WCA_CONTRACT = new SmartContract(new Hash160("0x2950f1d5992ed56539c7b1e4d8b9a4e449dc4dde"), Constants.NEOW3J);

    public static final FungibleToken CAT_TOKEN = new FungibleToken(new Hash160("df5526bbbaa3a4f01d14d4455f564c45859f2fa7"), NEOW3J);

    public static final GasToken GAS_TOKEN = new GasToken(NEOW3J);

    // ContractOwner defind in `devnet.neo-express`
    public static final Account CONTRACT_OWNER_ACCOUNT = new Account(ECKeyPair.create(new BigInteger("95ba67afd784f405e2800a0bcb875c035c41545d4a11e8995f6f1175d95c2952", 16)));

    public static final Wallet CONTRACT_OWNER_WALLET = Wallet.withAccounts(CONTRACT_OWNER_ACCOUNT);

    // the node address defined in `devnet.neo-express`
    private static final Account NODE_ACCOUNT = Account.fromWIF("KxPC9enS55zgQSUz7PMkS4QWsbABUZU58TMB2kkCaW1gnCcY7GUy");

    public static final Account GENESIS_ACCOUNT = Account.createMultiSigAccount(
        Arrays.asList(NODE_ACCOUNT.getECKeyPair().getPublicKey()), 1);

    public static final Wallet GENESIS = Wallet.withAccounts(GENESIS_ACCOUNT, NODE_ACCOUNT);

    public static final Hash160 WCA_CONTRACT_HASH = Hash160.fromAddress("NR6We6gPCDmrYzv8PsVGvRzHG9ki5TpZrd");
}
