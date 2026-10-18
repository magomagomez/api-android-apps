package com.magomez.androidapps.jcfit.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The login request and response are exactly the contract's. */
class JcfitAuthJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void loginResponseIsOnlyTheToken() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(new JcfitLoginResponse("abc")));

        assertThat(json.toString()).isEqualTo("{\"token\":\"abc\"}");
    }

    @Test
    void loginRequestReadsUsernameAndPassword() throws Exception {
        JcfitLoginRequest request = MAPPER.readValue("{\"username\":\"magomez\",\"password\":\"x\"}",
                JcfitLoginRequest.class);

        assertThat(request).isEqualTo(new JcfitLoginRequest("magomez", "x"));
    }
}
