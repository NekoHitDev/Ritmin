package com.nekohit.neo.mainnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class VerifyMessage {

    private static final String MESSAGE = "The first official release of NekoHit Project (Contracts).\n" +
            "In this release, we have CatToken v1.0.0 and WcaContract v1.0.0\n\n" +
            "Release digests:\n\n" +
            "CatToken.manifest.json:\n" +
            "Signature r: 1245C47B2EB95184F4E302424AD11236AF23D831C80E39AF2B9684FA9667CED1\n" +
            "Signature s: 29BE88FDF760F9D1BE800C0A9CAEF048F2B7D6CA58DCB4212868AA709F64B826\n" +
            "Signature v: 1C\n\n" +
            "CatToken.nef:\n" +
            "Signature r: 310860D0C98591B961A175BC290E25EC3124114A8882583D5F911775816DEE66\n" +
            "Signature s: 5381CF8ED116BFD395BFC0C24C819467E04B79DD192078F67486584C7A9C0703\n" +
            "Signature v: 1B\n\n" +
            "WCAContract.manifest.json:\n" +
            "Signature r: 1D5369F45C0ECB8ED37F4E38EF2CFAA97F76D8289052002C3AC53BC4EC7E869F\n" +
            "Signature s: EFAB21BFA7973956E645859BC45095EB6BE67DFEB71171212C435EA39CB4B77F\n" +
            "Signature v: 1B\n\n" +
            "WCAContract.nef:\n" +
            "Signature r: F680E7257F13DCE98C7D27674979A2F9D427660A43A7A97101E6C824026A3C83\n" +
            "Signature s: 6EE70F19364078E66BF5BFF681346142FCBF3BF10427F5872867A7F976B0C8A6\n" +
            "Signature v: 1C\n\n";

    private static final String SIGNATURE_R_HEX = "45C75DD78E93FDCBDE18427076E4A1B84269B6ABFEC7F626F812361568908F6B";
    private static final String SIGNATURE_S_HEX = "1524D575F7C88EF6299E1A5FBB5024E5D844796F9C41DE6F880393F804313E29";
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
        System.out.println("----");
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
        System.err.println("Signature validated.");
    }
}
