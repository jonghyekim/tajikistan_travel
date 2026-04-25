package egovframework.example.chatbot.mapper;

import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.support.ChatbotLexicon;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class JdbcEmergencyContactChatMapper implements EmergencyContactChatMapper {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcEmergencyContactChatMapper(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<EmergencyContactFact> findActiveContacts(String keyword, String locale) {
        List<String> tokens = searchTokens(keyword);
        StringBuilder sql = new StringBuilder("""
            select c.contact_id,
                   concat('emergency_contact:', c.contact_id) as source_id,
                   c.code,
                   i.title,
                   i.description,
                   c.phone_dial,
                   c.phone_display,
                   c.badge_type,
                   i.badge_label,
                   i.locale
              from emergency_contact c
              join emergency_contact_i18n i on i.contact_id = c.contact_id
             where c.is_active = true
               and i.locale = :locale
            """);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("locale", locale);
        if (!tokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < tokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                String paramName = "token" + i;
                sql.append(contactTokenExpression(paramName));
                params.addValue(paramName, tokens.get(i));
            }
            sql.append(")\n");
        }
        sql.append(" order by c.sort_order asc\n");
        return jdbcTemplate.query(sql.toString(), params, contactMapper());
    }

    private String contactTokenExpression(String paramName) {
        return """
            lower(c.code) like lower(concat('%%', :%s, '%%'))
            or exists (
                select 1
                  from emergency_contact_i18n mi
                 where mi.contact_id = c.contact_id
                   and (lower(mi.title) like lower(concat('%%', :%s, '%%'))
                        or lower(mi.description) like lower(concat('%%', :%s, '%%'))
                        or lower(mi.badge_label) like lower(concat('%%', :%s, '%%')))
            )
            or lower(c.phone_display) like lower(concat('%%', :%s, '%%'))
            """.formatted(paramName, paramName, paramName, paramName, paramName);
    }

    private List<String> searchTokens(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ChatbotLexicon.normalizeQuery(keyword).split("\\s+"))
            .map(ChatbotLexicon::normalizeToken)
            .filter(value -> !value.isBlank())
            .filter(value -> !ChatbotLexicon.isEmergencyStopword(value))
            .map(ChatbotLexicon::contactAlias)
            .distinct()
            .collect(Collectors.toList());
    }

    private RowMapper<EmergencyContactFact> contactMapper() {
        return (rs, rowNum) -> new EmergencyContactFact(
            rs.getLong("contact_id"),
            rs.getString("source_id"),
            rs.getString("code"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("phone_dial"),
            rs.getString("phone_display"),
            rs.getString("badge_type"),
            rs.getString("badge_label"),
            rs.getString("locale")
        );
    }
}
