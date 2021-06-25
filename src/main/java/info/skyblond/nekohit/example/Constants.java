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

    public static final SmartContract WCA_CONTRACT = new SmartContract(new Hash160("0x91dd72528619e489f886cb000f99a37aae61f069"), Constants.NEOW3J);

    public static final FungibleToken CAT_TOKEN = new FungibleToken(new Hash160("0xf9077f4eec533d175eed50b789b4d8d55dacf5ff"), NEOW3J);

    public static final GasToken GAS_TOKEN = new GasToken(NEOW3J);

    // ContractOwner defind in `devnet.neo-express`
    public static final Account CONTRACT_OWNER_ACCOUNT = new Account(ECKeyPair.create(new BigInteger("95ba67afd784f405e2800a0bcb875c035c41545d4a11e8995f6f1175d95c2952", 16)));

    public static final Wallet CONTRACT_OWNER_WALLET = Wallet.withAccounts(CONTRACT_OWNER_ACCOUNT);

    // test_user defind in `devnet.neo-express`
    public static final Account TEST_USER_ACCOUNT = new Account(ECKeyPair.create(new BigInteger("3b0d3450b306d0e05fbcbe6d8bc73e6108d883f9ee99711a1019f7a423d9455e", 16)));

    public static final Wallet TEST_USER_WALLET = Wallet.withAccounts(TEST_USER_ACCOUNT);
    
    // the node address defined in `devnet.neo-express`
    private static final Account NODE_ACCOUNT = Account.fromWIF("KxPC9enS55zgQSUz7PMkS4QWsbABUZU58TMB2kkCaW1gnCcY7GUy");

    public static final Account GENESIS_ACCOUNT = Account.createMultiSigAccount(
        Arrays.asList(NODE_ACCOUNT.getECKeyPair().getPublicKey()), 1);

    public static final Wallet GENESIS_WALLET = Wallet.withAccounts(GENESIS_ACCOUNT, NODE_ACCOUNT);

    public static final Hash160 WCA_CONTRACT_HASH = Hash160.fromAddress("NR6We6gPCDmrYzv8PsVGvRzHG9ki5TpZrd");
}
