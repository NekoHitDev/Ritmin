package com.nekohit.neo;

import com.nekohit.neo.compile.CompileAndDeployUtils;
import com.nekohit.neo.contract.CatToken;
import com.nekohit.neo.contract.WCAContract;
import com.nekohit.neo.helper.Utils;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Wallet;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DeployContract {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://neo3-testnet.neoline.vip/")
    );

    private static final boolean REALLY_DEPLOY_FLAG = false;
    private static final Class<?> CONTRACT_CLASS = WCAContract.class;

    public static void main(String[] args) throws Throwable {
        Wallet deployWallet = Utils.readWalletWIF();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Expected contract owner address: ");
        Utils.require(deployWallet.getDefaultAccount().getAddress().equals(scanner.nextLine()),
                "Contract owner address doesn't match your deploy account!");

        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", deployWallet.getDefaultAccount().getAddress());
        if (CONTRACT_CLASS == WCAContract.class) {
            System.out.println("Paste CatToken address in hash160 (0x...): ");
            String catHash = scanner.nextLine();
            FungibleToken cat = new FungibleToken(new Hash160(catHash), NEOW3J);
            Utils.require("CAT".equals(cat.getSymbol()), "Token symbol not match!");
            Utils.require("CatToken".equals(cat.getName()), "Token name not match!");
            replaceMap.put("<CAT_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>", cat.getScriptHash().toAddress());
            replaceMap.put("<CAT_TOKEN_CONTRACT_HASH_PLACEHOLDER>", cat.getScriptHash().toString());
            System.out.println("Validate CatToken contract address: " + cat.getScriptHash().toAddress());
        }

        // compile contract
        CompilationUnit compileResult = new Compiler().compile(CONTRACT_CLASS.getCanonicalName(), replaceMap);

        System.out.println("Contract compiled:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());

        Hash160 contractHash = SmartContract.calcContractHash(
                deployWallet.getDefaultAccount().getScriptHash(),
                compileResult.getNefFile().getCheckSumAsInteger(),
                compileResult.getManifest().getName()
        );

        System.out.println("Deploy following contract to public net:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());
        System.out.println("Will deployed to 0x" + contractHash);
        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());
        System.out.println("Parameters:");
        replaceMap.forEach((k, v) -> System.out.println("\t" + k + ": " + v));

        System.out.println("Type 'confirmed' to continue...");
        System.err.println("Note: Once confirmed, you CANNOT abort this process.");
        String line = scanner.nextLine();
        scanner.close();

        Utils.require(line.toLowerCase().trim().equals("confirmed"), "Canceled.");

        System.out.println("Deploying contract... Do not stop this program!");

        if (REALLY_DEPLOY_FLAG) {
            Transaction tx = CompileAndDeployUtils.deployContract(
                    compileResult,
                    deployWallet.getDefaultAccount(),
                    deployWallet,
                    NEOW3J
            );
            System.out.println("Deployed tx: 0x" + tx.getTxId());
            Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
            System.out.println("Gas fee: " + Utils.getGasFeeFromTx(tx));
        } else {
            System.err.println("This is a simulation. No contract is deployed.");
        }

        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());
        System.out.println("Contract: " + CONTRACT_CLASS.getCanonicalName());
        System.out.println("Deployed address: " + contractHash.toAddress());
        System.out.println("Deployed hash: 0x" + contractHash);
        System.out.println("Little endian: 0x" + Hex.encodeHexString(contractHash.toLittleEndianArray(), true));

        if (REALLY_DEPLOY_FLAG && CONTRACT_CLASS == CatToken.class) {
            FungibleToken token = new FungibleToken(contractHash, NEOW3J);
            transferToken(token, deployWallet, deployWallet.getDefaultAccount().getScriptHash(), 1_00, null);
            System.out.println(token.getBalanceOf(deployWallet.getDefaultAccount()));
        }

    }

    public static void transferToken(
            FungibleToken token, Wallet wallet, Hash160 to, long amount, String identifier
    ) throws Throwable {
        NeoSendRawTransaction tx = token.transfer(
                wallet.getDefaultAccount(), to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
        ).sign().send();

        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }

        Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), NEOW3J);
        System.out.println("Transfer tx: 0x" + tx.getSendRawTransaction().getHash());
    }
}
