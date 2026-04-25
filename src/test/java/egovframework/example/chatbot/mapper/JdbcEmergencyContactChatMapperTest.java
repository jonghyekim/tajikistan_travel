package egovframework.example.chatbot.mapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcEmergencyContactChatMapperTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findActiveContactsBuildsTokenSqlWithoutFormatError() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        JdbcEmergencyContactChatMapper mapper = new JdbcEmergencyContactChatMapper(jdbcTemplate);

        mapper.findActiveContacts("경찰", "ko");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        assertThat(sql.getValue()).contains("token0");
        assertThat(sql.getValue()).contains("phone_display");
    }
}
