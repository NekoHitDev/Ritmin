package com.nekohit.neo.contract;

import com.nekohit.neo.TestUtils;
import io.neow3j.wallet.Account;

import java.util.List;

public class TestConstants {
    public static final Account NODE_ACCOUNT = TestUtils.createAccountFromPrivateKey("57363779306c5100ca960cc39055f93fb114640c63616f2c570af809dc4b5c8e");
    public static final Account GENESIS_ACCOUNT = Account.createMultiSigAccount(
            List.of(NODE_ACCOUNT.getECKeyPair().getPublicKey()), 1);
    public static final Account CONTRACT_OWNER_ACCOUNT = TestUtils.createAccountFromPrivateKey("4d742d3c83124e4fe037488ff1428f57d092e436b120cd45b4f808c45f6b4700");

}
