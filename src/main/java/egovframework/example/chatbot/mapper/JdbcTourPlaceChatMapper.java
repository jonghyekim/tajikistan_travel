package egovframework.example.chatbot.mapper;

import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import egovframework.example.chatbot.support.ChatbotLexicon;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcTourPlaceChatMapper implements TourPlaceChatMapper {

    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm:ss")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTourPlaceChatMapper(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TourPlaceFact> searchPlaces(String keyword, String locale, int limit) {
        SearchTerms terms = SearchTerms.from(keyword);
        StringBuilder sql = new StringBuilder("""
            select p.place_id,
                   concat('tour_place:', p.place_id) as source_id,
                   i.title,
                   i.content,
                   i.address,
                   p.category_code,
                   p.region_code,
                   i.locale
              from tour_place p
              join tour_place_i18n i on i.place_id = p.place_id
             where p.is_active = true
               and i.locale = :locale
            """);

        MapSqlParameterSource params = params(keyword, locale).addValue("limit", limit);
        if (terms.categoryCode() != null) {
            sql.append(" and p.category_code = :categoryCode\n");
            params.addValue("categoryCode", terms.categoryCode());
        }
        if (terms.regionCode() != null) {
            sql.append(" and p.region_code = :regionCode\n");
            params.addValue("regionCode", terms.regionCode());
        }
        boolean hasStructuredFilter = terms.categoryCode() != null || terms.regionCode() != null;
        if ((terms.regionCode() == null || terms.categoryCode() == null) && !terms.textTokens().isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < terms.textTokens().size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                String paramName = "token" + i;
                sql.append(tokenExistsExpression(paramName));
                params.addValue(paramName, terms.textTokens().get(i));
            }
            sql.append(")\n");
        } else {
            for (int i = 0; i < terms.textTokens().size(); i++) {
                params.addValue("token" + i, terms.textTokens().get(i));
            }
        }

        sql.append("""
             order by
               case when lower(i.title) = lower(:keyword) then 100 else 0 end desc,
               case when lower(i.title) like lower(concat('%%', :keyword, '%%')) then 50 else 0 end desc,
            """);
        for (int i = 0; i < terms.textTokens().size(); i++) {
            if (i > 0) {
                sql.append(" + ");
            }
            String paramName = "token" + i;
            sql.append("case when ").append(tokenExistsExpression(paramName)).append(" then 1 else 0 end");
        }
        if (!terms.textTokens().isEmpty()) {
            sql.append(" desc, ");
        }
        sql.append("p.updated_at desc limit :limit\n");
        return jdbcTemplate.query(sql.toString(), params, tourPlaceMapper());
    }

    private String tokenExistsExpression(String paramName) {
        return """
            exists (
                select 1
                  from tour_place_i18n mi
                 where mi.place_id = p.place_id
                   and (lower(mi.title) like lower(concat('%%', :%s, '%%'))
                        or lower(mi.content) like lower(concat('%%', :%s, '%%'))
                        or lower(mi.address) like lower(concat('%%', :%s, '%%')))
            )
            """.formatted(paramName, paramName, paramName);
    }

    @Override
    public Optional<TourPlaceFact> findPlaceByKeyword(String keyword, String locale) {
        List<TourPlaceFact> places = searchPlaces(keyword, locale, 1);
        return places.stream().findFirst();
    }

    @Override
    public List<OperatingHourFact> findOperatingHours(String keyword, String locale) {
        List<String> tokens = searchTokens(keyword);
        StringBuilder sql = new StringBuilder("""
            select p.place_id as operating_hour_id,
                   p.place_id,
                   concat('operating_hour:', p.place_id) as source_id,
                   i.title as place_title,
                   p.open_time,
                   p.close_time,
                   i.locale
              from tour_place p
              join tour_place_i18n i on i.place_id = p.place_id
             where p.is_active = true
               and i.locale = :locale
               and (nullif(trim(p.open_time), '') is not null
                    or nullif(trim(p.close_time), '') is not null)
            """);
        MapSqlParameterSource params = params(keyword, locale);
        for (int i = 0; i < tokens.size(); i++) {
            String paramName = "token" + i;
            sql.append(" and ").append(operatingHourTokenExistsExpression(paramName)).append("\n");
            params.addValue(paramName, tokens.get(i));
        }
        sql.append("""
             order by
               case when lower(i.title) = lower(:keyword) then 100 else 0 end desc,
               case when lower(i.title) like lower(concat('%%', :keyword, '%%')) then 50 else 0 end desc,
               p.updated_at desc
             limit 5
            """);
        return jdbcTemplate.query(sql.toString(), params, operatingHourMapper());
    }

    private String operatingHourTokenExistsExpression(String paramName) {
        return """
            exists (
                select 1
                  from tour_place_i18n mi
                 where mi.place_id = p.place_id
                   and (lower(mi.title) like lower(concat('%%', :%s, '%%'))
                        or lower(mi.address) like lower(concat('%%', :%s, '%%')))
            )
            """.formatted(paramName, paramName);
    }

    private List<String> searchTokens(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keyword.toLowerCase(Locale.ROOT).split("\\s+"))
            .map(ChatbotLexicon::normalizeToken)
            .filter(value -> !value.isBlank())
            .filter(value -> !ChatbotLexicon.isSearchStopword(value))
            .filter(value -> !ChatbotLexicon.isGenericPlaceToken(value))
            .distinct()
            .collect(Collectors.toList());
    }

    private MapSqlParameterSource params(String keyword, String locale) {
        return new MapSqlParameterSource()
            .addValue("keyword", blankToNull(keyword))
            .addValue("locale", locale);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record SearchTerms(String categoryCode, String regionCode, List<String> textTokens) {

        private static SearchTerms from(String keyword) {
            if (keyword == null || keyword.isBlank()) {
                return new SearchTerms(null, null, List.of());
            }

            String categoryCode = null;
            String regionCode = null;
            List<String> textTokens = new ArrayList<>();

            String normalizedKeyword = ChatbotLexicon.normalizeQuery(keyword);
            for (String token : Arrays.stream(normalizedKeyword.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(ChatbotLexicon::normalizeToken)
                .filter(value -> !value.isBlank())
                .toList()) {
                if (ChatbotLexicon.isSearchStopword(token)) {
                    continue;
                }
                String category = ChatbotLexicon.categoryCode(token);
                if (category != null) {
                    categoryCode = category;
                    continue;
                }
                String region = ChatbotLexicon.regionCode(token);
                if (region != null) {
                    regionCode = region;
                    continue;
                }
                textTokens.add(token);
            }

            return new SearchTerms(categoryCode, regionCode, textTokens);
        }
    }

    private RowMapper<TourPlaceFact> tourPlaceMapper() {
        return (rs, rowNum) -> new TourPlaceFact(
            rs.getLong("place_id"),
            rs.getString("source_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("address"),
            rs.getString("category_code"),
            rs.getString("region_code"),
            rs.getString("locale")
        );
    }

    private RowMapper<OperatingHourFact> operatingHourMapper() {
        return (rs, rowNum) -> new OperatingHourFact(
            rs.getLong("operating_hour_id"),
            rs.getLong("place_id"),
            rs.getString("source_id"),
            rs.getString("place_title"),
            null,
            "ALL",
            parseTime(rs.getString("open_time")),
            parseTime(rs.getString("close_time")),
            false,
            null,
            null,
            rs.getString("locale")
        );
    }

    private LocalTime parseTime(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported database text format.
            }
        }
        throw new SQLException("Invalid tour_place operating time: " + value);
    }
}
