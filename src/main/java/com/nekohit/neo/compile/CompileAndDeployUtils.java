package com.nekohit.neo.compile;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.contract.ContractManagement;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class CompileAndDeployUtils {
    public static Transaction deployContract(
            CompilationUnit res,
            Account account,
            Neow3j neow3j
    ) throws Throwable {
        Transaction tx = new ContractManagement(neow3j)
                .deploy(res.getNefFile(), res.getManifest())
                .signers(AccountSigner.global(account))
                .sign();
        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Deployment failed: "
                    + "'%s'\n", response.getError().getMessage()));
        } else {
            Await.waitUntilTransactionIsExecuted(tx.getTxId(), neow3j);
        }
        return tx;
    }
}
