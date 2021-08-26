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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CompileAndDeployUtils {
    private static byte[] getModifiedClass(String canonicalClassName, Map<String, String> replaceMap) throws IOException {
        ClassReader reader = new ClassReader(canonicalClassName);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        classNode.methods.forEach((methodNode) -> {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode.getType() == AbstractInsnNode.LDC_INSN) {
                    LdcInsnNode node = (LdcInsnNode) insnNode;
                    if (node.cst instanceof String && replaceMap.containsKey(node.cst)) {
                        node.cst = replaceMap.get(node.cst);
                    }
                }
            }
        });

        if (classNode.invisibleAnnotations != null) {
            classNode.invisibleAnnotations
                    .forEach((it) -> processAnnotationNode(it, replaceMap));
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void processAnnotationNode(AnnotationNode annotationNode, Map<String, String> replaceMap) {

        // safety check
        if (annotationNode.values == null || annotationNode.values.size() % 2 != 0) {
            return;
        }

        // for each name-value pair
        for (int i = 0; i < annotationNode.values.size(); i += 2) {
            // The value might be different types
            Object value = annotationNode.values.get(i + 1);
            if (value == null) {
                continue;
            }

            // We only focused on String, AnnotationNode, List<String>
            // and List<AnnotationNode>
            if (value instanceof String) {
                // do the modification
                if (replaceMap.containsKey(value)) {
                    annotationNode.values.set(i + 1, replaceMap.get(value));
                }
            } else if (value instanceof AnnotationNode) {
                processAnnotationNode((AnnotationNode) value, replaceMap);
            } else if (value instanceof List) {
                List<Object> casted = (List<Object>) value;
                for (int j = 0; j < casted.size(); j++) {
                    Object elem = casted.get(j);
                    if (elem instanceof String) {
                        // do the modification
                        if (replaceMap.containsKey(elem)) {
                            casted.set(j, replaceMap.get(elem));
                        }
                    } else if (elem instanceof AnnotationNode) {
                        processAnnotationNode((AnnotationNode) elem, replaceMap);
                    }
                }
            }
        }
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
