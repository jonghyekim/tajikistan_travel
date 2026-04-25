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
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcTourPlaceChatMapper implements TourPlaceChatMapper {

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

        sql.append(" order by ");
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
            select h.operating_hour_id,
                   p.place_id,
                   concat('operating_hour:', h.operating_hour_id) as source_id,
                   i.title as place_title,
                   h.day_of_week,
                   h.season_code,
                   h.opens_at,
                   h.closes_at,
                   h.is_closed,
                   h.last_admission_at,
                   h.note,
                   i.locale
              from tour_place_operating_hour h
              join tour_place p on p.place_id = h.place_id
              join tour_place_i18n i on i.place_id = p.place_id
             where p.is_active = true
               and h.is_active = true
               and i.locale = :locale
            """);
        MapSqlParameterSource params = params(keyword, locale);
        for (int i = 0; i < tokens.size(); i++) {
            String paramName = "token" + i;
            sql.append(" and ").append(operatingHourTokenExistsExpression(paramName)).append("\n");
            params.addValue(paramName, tokens.get(i));
        }
        sql.append("""
             order by p.place_id, h.season_code, h.day_of_week
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
            parseDayOfWeek(rs.getString("day_of_week")),
            rs.getString("season_code"),
            rs.getTime("opens_at") == null ? null : rs.getTime("opens_at").toLocalTime(),
            rs.getTime("closes_at") == null ? null : rs.getTime("closes_at").toLocalTime(),
            rs.getBoolean("is_closed"),
            rs.getTime("last_admission_at") == null ? null : rs.getTime("last_admission_at").toLocalTime(),
            rs.getString("note"),
            rs.getString("locale")
        );
    }

    private DayOfWeek parseDayOfWeek(String value) throws SQLException {
        try {
            return DayOfWeek.valueOf(value);
        } catch (RuntimeException ex) {
            throw new SQLException("Invalid day_of_week: " + value, ex);
        }
    }
}
