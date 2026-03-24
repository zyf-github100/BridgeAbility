package com.rongzhiqiao;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.storage.provider=local",
        "app.auth.verification.require-email-code=true",
        "spring.mail.host=smtp.example.com",
        "spring.mail.port=465",
        "spring.mail.username=verify@example.com",
        "spring.mail.password=test-password"
})
@AutoConfigureMockMvc
class EmailVerificationAuthFlowTests {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JavaMailSender javaMailSender;

    private String account;

    @BeforeEach
    void setUpMailSender() {
        when(javaMailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getDefaultInstance(new Properties())));
    }

    @AfterEach
    void cleanup() {
        if (account == null) {
            return;
        }
        Long userId = jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE account = ?",
                rs -> rs.next() ? rs.getLong("id") : null,
                account
        );
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
        }
    }

    @Test
    void registerWithEmailVerificationCode() throws Exception {
        account = "mail_" + System.currentTimeMillis();
        String email = account + "@example.com";

        mockMvc.perform(post("/api/auth/register/email-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        String code = extractCode(messageCaptor.getValue());

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "account", account,
                                "password", "Pass@123456",
                                "nickname", "Email Flow",
                                "email", email,
                                "emailVerificationCode", code
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value(account))
                .andReturn();

        JsonNode payload = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(account, payload.path("data").path("account").asText());
    }

    private String extractCode(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        String body = content == null ? "" : content.toString();
        Matcher matcher = CODE_PATTERN.matcher(body);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find(), "Verification code not found in email body");
        return matcher.group(1);
    }
}
