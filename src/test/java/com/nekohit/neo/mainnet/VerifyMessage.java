package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class VerifyMessage {

    private static final String MESSAGE = "Here is the first patch: CatToken v1.0.1 and WcaContract v1.0.1\n\n" +
            "Release digests:\n\n" +
            "CatToken.manifest.json:\n" +
            "Signature r: 97A52C4790A931519AC472D9F61CA4AAA22D24B37E3326A5A47AA53AD97849C1\n" +
            "Signature s: 7ADCE0666A2FB06CFA2C9AB0A15FA3C504131F6E87783F1670B837CD34FE8227\n" +
            "Signature v: 1C\n\n" +
            "CatToken.nef:\n" +
            "Signature r: 8B387694CDF441FB498A5BDDA3CC5979FDF7C7DC6021BCFB1E8EF9BA3F0D2FE5\n" +
            "Signature s: 3F4D9E41AD89C5F3EE878BA44F7BDB146EE5C9A5861AF4B4671A68EC0CDB063A\n" +
            "Signature v: 1B\n\n" +
            "WCAContract.manifest.json:\n" +
            "Signature r: 7FD91B54161D943086FE9C4731F84A58418A7B9C5CBE421A054BF1F280B3F656\n" +
            "Signature s: A4CA640F639FA478EDB2A32390DEE60F65C6195DC54DEAA5B3A051074EDC525E\n" +
            "Signature v: 1C\n\n" +
            "WCAContract.nef:\n" +
            "Signature r: 9B5970D46005E91B6B1C5C66AFAA89CACEE352ACDFC48D99A22B085514AF5254\n" +
            "Signature s: 805E99F6128AEA383E427A6E1C14F1F8908BA0357348DE0F8FF60E1B065EFE64\n" +
            "Signature v: 1B\n\n";

    private static final String SIGNATURE_R_HEX = "A1A47535AA0ED7918C2C6F2D5CA6FE9B83E96FF2BD43AC3FE5DEB1C77626E06E";
    private static final String SIGNATURE_S_HEX = "46CC1ECB13EFE3B352BB843E8D2BE7E452F1901CB4E68765F2D9FF6FA3095648";
    private static final String SIGNATURE_V_HEX = "1B";

    private static final String PUBLIC_KEY = "03DE51F8DB110F0D7471EED5129B7521CEBC0C9BDA21A4D1DCD18DA9215C193195";
    private static final String EXPECTED_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";

    public static void main(String[] args) throws Exception {
        ECKeyPair.ECPublicKey pubKey = new ECKeyPair.ECPublicKey(PUBLIC_KEY);
        String actualAddress = Account.fromPublicKey(pubKey).getAddress();
        System.out.println("Address from public key: " + actualAddress);
        TestUtils.require(EXPECTED_ADDRESS.equals(actualAddress),
                "Public key doesn't match the expected address");

        System.out.println("Message:\n" + MESSAGE);
        System.out.println("----");
        System.out.println("Signature r hex: " + SIGNATURE_R_HEX);
        System.out.println("Signature s hex: " + SIGNATURE_S_HEX);
        System.out.println("Signature v hex: " + SIGNATURE_V_HEX);

        Sign.SignatureData signature = new Sign.SignatureData(
                DatatypeConverter.parseHexBinary(SIGNATURE_V_HEX)[0],
                DatatypeConverter.parseHexBinary(SIGNATURE_R_HEX),
                DatatypeConverter.parseHexBinary(SIGNATURE_S_HEX)
        );

        TestUtils.require(Sign.verifySignature(
                        MESSAGE.getBytes(StandardCharsets.UTF_8), signature, pubKey),
                "Invalid signature");
        System.err.println("Signature validated.");
    }
}
