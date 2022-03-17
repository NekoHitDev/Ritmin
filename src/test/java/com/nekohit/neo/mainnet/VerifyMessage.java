package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

public class VerifyMessage {

    private static final String MESSAGE = """
            CatToken.manifest.json:
            Signature r: 97A52C4790A931519AC472D9F61CA4AAA22D24B37E3326A5A47AA53AD97849C1
            Signature s: 7ADCE0666A2FB06CFA2C9AB0A15FA3C504131F6E87783F1670B837CD34FE8227
            Signature v: 1C

            CatToken.nef:
            Signature r: 8B387694CDF441FB498A5BDDA3CC5979FDF7C7DC6021BCFB1E8EF9BA3F0D2FE5
            Signature s: 3F4D9E41AD89C5F3EE878BA44F7BDB146EE5C9A5861AF4B4671A68EC0CDB063A
            Signature v: 1B

            WCAContract.manifest.json:
            Signature r: 7FD91B54161D943086FE9C4731F84A58418A7B9C5CBE421A054BF1F280B3F656
            Signature s: A4CA640F639FA478EDB2A32390DEE60F65C6195DC54DEAA5B3A051074EDC525E
            Signature v: 1C

            WCAContract.nef:
            Signature r: 9B5970D46005E91B6B1C5C66AFAA89CACEE352ACDFC48D99A22B085514AF5254
            Signature s: 805E99F6128AEA383E427A6E1C14F1F8908BA0357348DE0F8FF60E1B065EFE64
            Signature v: 1B

            """;

    private static final File WORK_DIR = new File("./compiled_contract");
    private static final String PUBLIC_KEY = "03DE51F8DB110F0D7471EED5129B7521CEBC0C9BDA21A4D1DCD18DA9215C193195";
    private static final String EXPECTED_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";

    public static void main(String[] args) throws Exception {
        ECKeyPair.ECPublicKey pubKey = new ECKeyPair.ECPublicKey(PUBLIC_KEY);
        String actualAddress = Account.fromPublicKey(pubKey).getAddress();
        System.out.println("Address from public key: " + actualAddress);
        TestUtils.require(EXPECTED_ADDRESS.equals(actualAddress),
                "Public key doesn't match the expected address");

        AtomicReference<File> currentFile = new AtomicReference<>(null);
        AtomicReference<String> currentR = new AtomicReference<>(null);
        AtomicReference<String> currentS = new AtomicReference<>(null);
        AtomicReference<String> currentV = new AtomicReference<>(null);

        MESSAGE.lines().forEach(line -> {
            if (line.endsWith(":")) {
                currentFile.set(new File(WORK_DIR, line.split(":")[0].trim()));
            } else if (line.startsWith("Signature r: ")) {
                currentR.set(line.split("Signature r: ")[1]);
            } else if (line.startsWith("Signature s: ")) {
                currentS.set(line.split("Signature s: ")[1]);
            } else if (line.startsWith("Signature v: ")) {
                currentV.set(line.split("Signature v: ")[1]);
            }

            if (currentFile.get() != null
                    && currentR.get() != null
                    && currentS.get() != null
                    && currentV.get() != null) {
                Sign.SignatureData signature = new Sign.SignatureData(
                        DatatypeConverter.parseHexBinary(currentV.get())[0],
                        DatatypeConverter.parseHexBinary(currentR.get()),
                        DatatypeConverter.parseHexBinary(currentS.get())
                );

                byte[] content;
                try {
                    content = Files.readAllBytes(currentFile.get().toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                TestUtils.require(Sign.verifySignature(content, signature, pubKey), "Invalid signature");
                System.err.println("Signature validated: " + currentFile.get().getName());
                currentFile.set(null);
                currentR.set(null);
                currentS.set(null);
                currentV.set(null);
            }
        });


    }
}
