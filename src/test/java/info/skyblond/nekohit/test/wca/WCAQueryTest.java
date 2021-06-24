package info.skyblond.nekohit.test.wca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import info.skyblond.nekohit.test.ContractTestFramework;

/**
 * This class test query methods for WCA. 
 * Including valid response and invalid or expection handle.
 */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(ContractTestFramework.class)
public class WCAQueryTest extends ContractTestFramework  {
    @Test
    void testInvalidQueryWCA() {
        assertEquals(
            "", 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryWCA(getWcaContract(), "some_invalid_id");
            })
        );
    }

    @Test
    void testValidQueryWCA() throws Throwable {
        var identifier = ContractInvokeHelper.createWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, "test_query_valid_wca_" + System.currentTimeMillis(), 
            CONTRACT_OWNER_WALLET
        );
        assertNotEquals(
            "", 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryWCA(getWcaContract(), identifier);
            })
        );
    }
}
