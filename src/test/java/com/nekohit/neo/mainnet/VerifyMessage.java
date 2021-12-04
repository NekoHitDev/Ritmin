package com.nekohit.neo.mainnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.wallet.Account;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class VerifyMessage {

    private static final String MESSAGE = "中文123 English and1 emoji \uD83D\uDE42";
    private static final String SIGNATURE_R_HEX = "5885b1ecb416a2ef62f5b0e6b5e4b4fcc11ef4b50f8020b2a6f8a7302cc74777";
    private static final String SIGNATURE_S_HEX = "69a7b323f050c4a091a121218344c25096e25035b41a11ffc9aabb58451d78ab";

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

        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPoint point = spec.getCurve().decodePoint(pubKey.getEncoded(false));
        ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        signer.init(false, new ECPublicKeyParameters(point, domain));
        Utils.require(signer.verifySignature(MESSAGE.getBytes(StandardCharsets.UTF_8),
                new BigInteger(SIGNATURE_R_HEX, 16),
                new BigInteger(SIGNATURE_S_HEX, 16)), "Invalid signature");
    }
}
