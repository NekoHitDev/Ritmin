package info.skyblond.nekohit.neo;

import info.skyblond.nekohit.neo.contract.CatToken;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.SmartContract;
import io.neow3j.wallet.Account;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.util.Arrays;

/**
 * Get CatToken address for PRIVATE TEST NET
 */
public class Compile {
    private static final Class<CatToken> CONTRACT_CLASS = CatToken.class;
    // the node address defined in `devnet.neo-express`
    private static final Account NODE_ACCOUNT = Account.fromWIF("KxPC9enS55zgQSUz7PMkS4QWsbABUZU58TMB2kkCaW1gnCcY7GUy");
    private static final Account GENESIS_ACCOUNT = Account.createMultiSigAccount(
            Arrays.asList(NODE_ACCOUNT.getECKeyPair().getPublicKey()), 1);

    public static void main(String[] args) throws IOException {
        var compileResult = new Compiler().compile(CONTRACT_CLASS.getCanonicalName());
        // Path buildNeow3jPath = Paths.get("idea-build-result");
        // buildNeow3jPath.toFile().mkdirs();
        // ContractUtils.writeNefFile(
        //     compileResult.getNefFile(), 
        //     compileResult.getManifest().getName(), 
        //     buildNeow3jPath
        // );
        // ContractUtils.writeContractManifestFile(
        //     compileResult.getManifest(), buildNeow3jPath
        // );
        var contractHash = SmartContract.calcContractHash(
                GENESIS_ACCOUNT.getScriptHash(),
                compileResult.getNefFile().getCheckSumAsInteger(),
                compileResult.getManifest().getName()
        );
        System.out.println(contractHash);
        System.out.println(contractHash.toAddress());
        System.out.println(Hex.encodeHexString(contractHash.toLittleEndianArray(), true));
    }
}
