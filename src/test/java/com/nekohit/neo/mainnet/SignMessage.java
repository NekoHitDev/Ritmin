package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
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

        StringBuilder stringBuilder = new StringBuilder();
        Files.list(Path.of("./compiled_contract"))
                .forEach(p -> {
                    try {
                        byte[] content = Files.readAllBytes(p);
                        stringBuilder.append(p.getFileName()).append(":\n");
                        stringBuilder.append(signAndFormat(content, account.getECKeyPair()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        System.out.println(stringBuilder);
    }

    private static String signAndFormat(byte[] message, ECKeyPair keyPair) {
        Sign.SignatureData signature = Sign.signMessage(message, keyPair);
        return "Signature r: " +
                DatatypeConverter.printHexBinary(signature.getR()) + "\n" +
                "Signature s: " +
                DatatypeConverter.printHexBinary(signature.getS()) + "\n" +
                "Signature v: " +
                DatatypeConverter.printHexBinary(new byte[]{signature.getV()}) + "\n\n";
    }
}
