package com.nekohit.neo.contract;

import com.nekohit.neo.TestUtils;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.test.DeployContext;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class is the basic framework for testing contract.
 * It offers functions like compile and deploy contract,
 * set up contract address, offering some constants, etc.
 */
public class ContractTestFramework {
    private static final Logger logger = LoggerFactory.getLogger(ContractTestFramework.class);

    @RegisterExtension
    private static final ContractTestExtension ext = new ContractTestExtension();

    protected static final Account CONTRACT_OWNER_ACCOUNT = TestUtils.createAccountFromPrivateKey("4d742d3c83124e4fe037488ff1428f57d092e436b120cd45b4f808c45f6b4700");
    protected static FungibleToken catToken = null;
    protected static SmartContract wcaContract = null;
    protected static Neow3j neow3j = null;
    protected static GasToken gasToken = null;
    protected static ContractTestExtension.GenesisAccount genesisAccount = null;

    @SuppressWarnings("unused")
    @DeployConfig(CatToken.class)
    public static DeployConfiguration catTokenConfig(DeployContext ctx) {
        DeployConfiguration conf = new DeployConfiguration();
        conf.setSubstitution("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", CONTRACT_OWNER_ACCOUNT.getAddress());
        conf.setSubstitution("<USD_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>", GasToken.SCRIPT_HASH.toAddress());
        conf.setSubstitution("<USD_TOKEN_CONTRACT_HASH_PLACEHOLDER>", GasToken.SCRIPT_HASH.toString());
        return conf;
    }

    @SuppressWarnings("unused")
    @DeployConfig(WCAContract.class)
    public static DeployConfiguration wcaContractConfig(DeployContext ctx) {
        DeployConfiguration conf = new DeployConfiguration();
        conf.setSubstitution("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", CONTRACT_OWNER_ACCOUNT.getAddress());
        return conf;
    }

    @BeforeAll
    public static void setUpAll() {
        genesisAccount = ext.getGenesisAccount();
        neow3j = ext.getNeow3j();
        gasToken = new GasToken(neow3j);
        try {
            catToken = new FungibleToken(ext.getDeployedContract(CatToken.class).getScriptHash(), neow3j);
        } catch (ExtensionConfigurationException e) {
            logger.warn("Error when getting cat token", e);
        }
        try {
            wcaContract = ext.getDeployedContract(WCAContract.class);
        } catch (ExtensionConfigurationException e) {
            logger.warn("Error when getting wca contract", e);
        }

        try {
            // give contract owner some gas to play with
            transferToken(
                    gasToken, genesisAccount.getMultiSigAccount(),
                    CONTRACT_OWNER_ACCOUNT.getScriptHash(),
                    5010000_00000000L, null, true
            );
            // mint some cat
            transferToken(
                    gasToken, CONTRACT_OWNER_ACCOUNT, getCatTokenAddress(),
                    // 1_000_000_000 CAT -> 500_000_000 GAS
                    500_000_000_000000L, null, true
            );
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Hash160 getCatTokenAddress() {
        return Objects.requireNonNull(catToken.getScriptHash());
    }

    public static Hash160 getWcaContractAddress() {
        return Objects.requireNonNull(wcaContract.getScriptHash());
    }

    public static FungibleToken getCatToken() {
        return Objects.requireNonNull(catToken);
    }

    public static FungibleToken tokenFromAddress(Hash160 token) {
        return new FungibleToken(token, neow3j);
    }

    public static SmartContract getWcaContract() {
        return Objects.requireNonNull(wcaContract);
    }

    // ---------- some handy functions below ----------

    /**
     * Transfer some token with additional string as payload
     *
     * @param token      token type
     * @param account    from account
     * @param to         dest
     * @param amount     in fraction
     * @param identifier the payload
     * @param wait       true will wait tx confirmed, false won't wait
     * @throws Throwable if anything goes wrong
     */
    protected static void transferToken(
            FungibleToken token, Account account, Hash160 to, long amount, String identifier, boolean wait
    ) throws Throwable {
        Transaction tx;
        if (account == genesisAccount.getMultiSigAccount()) {
            // multi sign
            tx = token.transfer(
                            genesisAccount.getMultiSigAccount(), to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
                    )
                    .getUnsignedTransaction()
                    .addMultiSigWitness(genesisAccount.getMultiSigAccount().getVerificationScript(),
                            genesisAccount.getSignerAccounts());

        } else {
            // normal account
            tx = token.transfer(
                    account, to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
            ).sign();
        }

        NeoSendRawTransaction resp = tx.send();

        if (resp.hasError()) {
            throw new Exception(resp.getError().getMessage());
        }

        if (wait) {
            Await.waitUntilTransactionIsExecuted(resp.getSendRawTransaction().getHash(), neow3j);
        }

        logger.info(
                "Transfer {} {} from {} to {}, gas: {}",
                amount, token.getSymbol(),
                account.getAddress(),
                to.toAddress(), TestUtils.getGasFeeFromTx(tx)
        );
    }

    protected static Account getTestAccount() {
        Account account = Account.create();
        try {
            transferToken(
                    catToken, CONTRACT_OWNER_ACCOUNT,
                    account.getScriptHash(),
                    10000_00, null, false
            );
            transferToken(
                    gasToken, genesisAccount.getMultiSigAccount(),
                    account.getScriptHash(),
                    10000_00000000L, null, true
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return account;
    }

    /**
     * Invoke a contract function
     *
     * @param contract   the contract
     * @param function   the function
     * @param parameters the parameters
     * @param signers    the signers
     * @return NeoApplicationLog contains the returned stack
     * @throws Throwable if anything goes wrong
     */
    protected static NeoApplicationLog invokeFunction(
            SmartContract contract, String function,
            ContractParameter[] parameters, Signer[] signers
    ) throws Throwable {
        var tx = contract
                .invokeFunction(function, parameters)
                .signers(signers)
                .sign();
        var response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Error when invoking %s: %s", function, response.getError().getMessage()));
        }
//        logger.info("{} tx: {}", function, tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), neow3j);
        logger.info("{} gas fee: {}", function, TestUtils.getGasFeeFromTx(tx));
        var appLog = tx.getApplicationLog();
        assertEquals(1, appLog.getExecutions().size());
        if (appLog.getExecutions().get(0).getState() != NeoVMStateType.HALT) {
            throw new Exception(appLog.getExecutions().get(0).getException());
        }
        return appLog;
    }

    /**
     * Do a test invocation, run the contract, but won't cause any change to
     * blockchain, also won't cost any gas.
     *
     * @param contract   the contract
     * @param function   the function
     * @param parameters the parameters
     * @param signers    the signers
     * @return InvocationResult contains the returned stack
     * @throws Exception if anything goes wrong
     */
    protected static InvocationResult testInvoke(
            SmartContract contract, String function,
            ContractParameter[] parameters, Signer[] signers
    ) throws Exception {
        var tx = contract.callInvokeFunction(function, Arrays.asList(parameters), signers);
        if (tx.hasError()) {
            throw new Exception(String.format("Error when test invoking %s: %s", function, tx.getError().getMessage()));
        }
        if (tx.getInvocationResult().hasStateFault()) {
            throw new Exception(tx.getInvocationResult().getException());
        }
        return tx.getInvocationResult();
    }
}
