package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import io.neow3j.crypto.ECDSASignature;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class SignMessage {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Account account = TestUtils.readAccountWIF(scanner);

        System.out.println("Address: `" + account.getAddress() + "`");
        System.out.println();
        System.out.println("Public key: `" + DatatypeConverter.printHexBinary(
                account.getECKeyPair().getPublicKey().getEncoded(true)) + "`");
        System.out.println("\n----");

        String message = "中文123 English and emoji 🙂";

        System.out.println("Message:\n```");
        System.out.println(message);
        System.out.println("```\n");
        signAndPrint(message, account.getECKeyPair());

        Files.list(Path.of("./compiled_contract"))
                .forEach(p -> {
                    try {
                        byte[] content = Files.readAllBytes(p);
                        System.out.println(p.getFileName());
                        signAndPrint(content, account.getECKeyPair());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static void signAndPrint(String message, ECKeyPair keyPair) {
        signAndPrint(message.getBytes(StandardCharsets.UTF_8), keyPair);
    }

    private static void signAndPrint(byte[] message, ECKeyPair keyPair) {
        ECDSASignature signature = keyPair.signAndGetECDSASignature(message);
        System.out.println("Signature r: `" + signature.r.toString(16) + "`");
        System.out.println("Signature s: `" + signature.s.toString(16) + "`");
        System.out.println("\n----");
    }
}
