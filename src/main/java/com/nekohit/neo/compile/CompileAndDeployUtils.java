package com.nekohit.neo.compile;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class CompileAndDeployUtils {
    private static byte[] getModifiedClass(String canonicalClassName, Map<String, String> replaceMap) throws IOException {
        ClassReader reader = new ClassReader(canonicalClassName);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ReplaceClassVisitor cv = new ReplaceClassVisitor(writer, replaceMap);
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    public static <T> CompilationUnit compileModifiedContract(Class<T> contractClass, Map<String, String> replaceMap) throws IOException {
        byte[] modifiedClass = getModifiedClass(contractClass.getCanonicalName(), replaceMap);
        return new Compiler().compile(new ByteArrayInputStream(modifiedClass));
    }

    public static Transaction deployContract(
            CompilationUnit res,
            Account account,
            Wallet wallet,
            Neow3j neow3j
    ) throws Throwable {
        Transaction tx = new ContractManagement(neow3j)
                .deploy(res.getNefFile(), res.getManifest())
                .signers(AccountSigner.global(account.getScriptHash()))
                .wallet(wallet)
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
