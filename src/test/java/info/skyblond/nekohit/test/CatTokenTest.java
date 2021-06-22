package info.skyblond.nekohit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.wallet.Wallet;

/**
 * Test the CatToken.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CatTokenTest extends ContractTestFrame {

    public CatTokenTest() throws Throwable {
        super();
    }

    private static final long TRANSFER_AMOUNT = 1000_00;

    private Wallet testWallet = Wallet.create();

    @BeforeAll
    void prepareTestAccount() throws Throwable {
        prepareGas(testWallet.getDefaultAccount().getScriptHash(), 10_00000000, true);
    }

    @Test
    void testSymbol() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getSymbol(), "CAT");
    }

    @Test
    void testTotalSupply() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getTotalSupply().longValue(), 1_000_000_000_00L);
    }

    @Test 
    void testOwnerHash() {
        if (isPublicChain()) {
            // TODO should be another wallet address, using GitHub Secrets
            assertTrue(false, "This branch shouldn't be public chain");
        } else {
            // TODO wallet defined in neo-express config file
        }
    }

    // TODO: test invalid addr, invalid amount, invalid sign, normal transfer(balance change)
}
