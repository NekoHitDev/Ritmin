package info.skyblond.nekohit.test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.skyblond.nekohit.example.Constants;
import info.skyblond.nekohit.neo.contract.CatToken;
import info.skyblond.nekohit.neo.contract.WCAContract;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Wallet;

/**
 * This class is the basic frame work for testing contract. 
 * It offers functions like compile and deploy contract, 
 * set up contract address, offering some constants, etc. 
 */
public class ContractTestFrame {
    private static final Logger logger = LoggerFactory.getLogger(ContractTestFrame.class);

    private static Hash160 catTokenAddress = null;
    private static Hash160 wcaContractAddress = null;
    private static FungibleToken catToken = null;
    private static SmartContract wcaContract = null;

    protected static final Class<CatToken> CAT_TOKEN_CLASS = CatToken.class;
    protected static final Class<WCAContract> WCA_CONTRACT_CLASS = WCAContract.class;
    protected static final Wallet GENESIS_WALLET = Constants.GENESIS_WALLET;
    protected static final Wallet CONTRACT_OWNER_WALLET = Constants.CONTRACT_OWNER_WALLET;
    protected static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://127.0.0.1:50012"));
    protected static final GasToken GAS_TOKEN = new GasToken(NEOW3J);

    protected static Hash160 getCatTokenAddress() {
        return Objects.requireNonNull(catTokenAddress);
    }

    protected static Hash160 getWcaContractAddress() {
        return Objects.requireNonNull(wcaContractAddress);
    }
    
    protected static FungibleToken getCatToken() {
        return Objects.requireNonNull(catToken);
    }

    protected static SmartContract getWcaContract() {
        return Objects.requireNonNull(wcaContract);
    }

    /**
     * Get gas fee from given test
     * @param tx the transaction
     * @return the gas fee in decimal representation
     */
    private static double getGasFeeFromTx(Transaction tx) {
        var fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }

    /**
     * Compile the given contract class and try to deploy it with GENESIS account. 
     * Pleas do check if the contract has been deployed, otherwise it will thorw an 
     * error when trying to deploy duplicated contract.
     * 
     * @param <T>           Contract Class Type
     * @param contractClass Contract class
     * @return The contract address Hash160
     * @throws Throwable if anything goes wrong
     */
    private static <T> Hash160 compileAndDeploy(Class<T> contractClass) throws Throwable {
        var compileResult = new Compiler().compile(contractClass.getCanonicalName());
        var tx = new ContractManagement(NEOW3J)
                .deploy(compileResult.getNefFile(), compileResult.getManifest())
                .signers(Signer.global(GENESIS_WALLET.getDefaultAccount().getScriptHash()))
                .wallet(GENESIS_WALLET)
                .sign();
        var response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Deployment was not successful. Error message from neo-node was: "
                    + "'%s'\n", response.getError().getMessage()));
        }
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), Constants.NEOW3J);
        var contractHash = SmartContract.calcContractHash(
            GENESIS_WALLET.getDefaultAccount().getScriptHash(), 
            compileResult.getNefFile().getCheckSumAsInteger(), 
            compileResult.getManifest().getName()
        );
        logger.info("Contract {} deployed at 0x{}, gas fee: {}", contractClass.getName(), contractHash, getGasFeeFromTx(tx));
        return contractHash;
    }

    /**
     * Initial(deploy) contract when initalizing the test class.
     * @throws Throwable
     */
    public ContractTestFrame() throws Throwable {
        // Considering there might be multiple test cases runing at the same time
        // it better to have a lock. Lock first, then deploy the contract.
        synchronized(NEOW3J) {
            // deploy Cat Token
            if (catTokenAddress == null) {
                catTokenAddress = compileAndDeploy(CAT_TOKEN_CLASS);
                catToken = new FungibleToken(catTokenAddress, NEOW3J);
            }
            // deploy WCA Contract
            if (wcaContractAddress == null) {
                wcaContractAddress = compileAndDeploy(WCA_CONTRACT_CLASS);
                wcaContract = new SmartContract(wcaContractAddress, NEOW3J);
            }
        }
        // give contract owner some gas to play with
        prepareGas(CONTRACT_OWNER_WALLET.getDefaultAccount().getScriptHash(), 10_00000000, false);
    }

    // ---------- some handy functions below ----------

    /**
     * Transfer some token with additional string as payload
     * @param token      token type
     * @param wallet     from wallet, use default account
     * @param to         dest
     * @param amount     in fraction
     * @param identifier the payload
     * @param wait       true will wait tx confirmed, false won't wait
     * @throws Throwable if anything goes wrong
     */
    protected static void transferToken(
        FungibleToken token, Wallet wallet, Hash160 to, long amount, String identifier, boolean wait
    ) throws Throwable {
        NeoSendRawTransaction tx = token.transferFromDefaultAccount(
            wallet, to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
        ).signers(Signer.calledByEntry(wallet.getDefaultAccount())).sign().send();

        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }
        
        if (wait)
            Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), Constants.NEOW3J);

        logger.info(
            "Transfer {} {} from {} to {}, tx: {}", 
            amount, token.getSymbol(), 
            wallet.getDefaultAccount().getAddress(),
            to.toAddress(), tx.getSendRawTransaction().getHash()
        );
    }
    
    /**
     * Transfer cat token from contract owner's wallet to a given account
     * @param to         dest
     * @param amount     amount in fraction
     * @param wait       if wait tx confirmed
     * @throws Throwable if anything goes wrong
     */
    protected static void prepareCatToken(Hash160 to, long amount, boolean wait) throws Throwable {
        transferToken(catToken, CONTRACT_OWNER_WALLET, to, amount, null, wait);
    }

    /**
     * Transfer gas from genesis's wallet to a given account
     * @param to         dest
     * @param amount     amount in fraction
     * @param wait       if wait tx confirmed
     * @throws Throwable if anything goes wrong
     */
    protected static void prepareGas(Hash160 to, long amount, boolean wait) throws Throwable {
        transferToken(GAS_TOKEN, GENESIS_WALLET, to, amount, null, wait);
    }

    /**
     * Invoke a contract function
     * @param contract   the contract
     * @param function   the function
     * @param parameters the parameters
     * @param signers    the signers
     * @param wallet     signer's wallet
     * @return NeoApplicationLog contains the returned stack
     * @throws Throwable if anything goes wrong
     */
    protected static NeoApplicationLog invokeFunction(
        SmartContract contract, String function, 
        ContractParameter[] parameters, Signer[] signers, Wallet wallet
    ) throws Throwable {
        var tx = contract
                .invokeFunction(function, parameters)
                .signers(signers).wallet(wallet).sign();
        var response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Error when invoking %s: %s", function, response.getError().getMessage()));
        }
        logger.info("{} tx: {}", function, tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), Constants.NEOW3J);
        logger.info("{} gas fee: {}", function, getGasFeeFromTx(tx));
        return tx.getApplicationLog();
    }

    /**
     * Do a test invocation, run the contract, but won't cause any change to 
     * blockchain, also won't cost any gas.
     * 
     * @param contract   the contract
     * @param function   the function
     * @param parameters the parameters
     * @param signers    the signers
     * @return           InvocationResult contains the retured stack
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
        return tx.getInvocationResult();
    }

    protected static boolean isPublicChain() {
        var value = System.getenv("PUBLIC_CHAIN");
        return value != null && !value.isBlank();
    }
}