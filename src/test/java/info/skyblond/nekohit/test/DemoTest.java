package info.skyblond.nekohit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static info.skyblond.nekohit.example.Constants.GAS_TOKEN;
import static info.skyblond.nekohit.example.Constants.CONTRACT_OWNER_ACCOUNT;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;

@TestInstance(Lifecycle.PER_CLASS)
public class DemoTest {
    public static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://127.0.0.1:50012"));

    @Test
    void demo() throws UnexpectedReturnTypeException, IOException {
        assertEquals(0, GAS_TOKEN.getBalanceOf(CONTRACT_OWNER_ACCOUNT).intValue());
    }
}
