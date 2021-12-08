package com.nekohit.neo.mainnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class VerifyMessage {

    private static final String MESSAGE = "中文123 English and emoji \uD83D\uDE42";
    private static final String SIGNATURE_R_HEX = "156F687CC0F9E67C1A180EFF193437D13390C727CC8F57A15108ADF2A86B76AA";
    private static final String SIGNATURE_S_HEX = "BDACDCB33121FA7BDE640982140F42F44F62DEAF0C1994051927B4AD154B664A";
    private static final String SIGNATURE_V_HEX = "1B";

    private static final String PUBLIC_KEY = "03DE51F8DB110F0D7471EED5129B7521CEBC0C9BDA21A4D1DCD18DA9215C193195";
    private static final String EXPECTED_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";

    public static void main(String[] args) throws Exception {
        ECKeyPair.ECPublicKey pubKey = new ECKeyPair.ECPublicKey(PUBLIC_KEY);
        String actualAddress = Account.fromPublicKey(pubKey).getAddress();
        System.out.println("Address from public key: " + actualAddress);
        Utils.require(EXPECTED_ADDRESS.equals(actualAddress),
                "Public key doesn't match the expected address");

        System.out.println("Message:\n" + MESSAGE);
        System.out.println("Signature r hex: " + SIGNATURE_R_HEX);
        System.out.println("Signature s hex: " + SIGNATURE_S_HEX);
        System.out.println("Signature v hex: " + SIGNATURE_V_HEX);

        Sign.SignatureData signature = new Sign.SignatureData(
                DatatypeConverter.parseHexBinary(SIGNATURE_V_HEX)[0],
                DatatypeConverter.parseHexBinary(SIGNATURE_R_HEX),
                DatatypeConverter.parseHexBinary(SIGNATURE_S_HEX)
        );

        Utils.require(Sign.verifySignature(
                        MESSAGE.getBytes(StandardCharsets.UTF_8), signature, pubKey),
                "Invalid signature");
    }
}
