package egovframework.example.chatbot.mapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcTourPlaceChatMapperTest {

    @Test
    void searchPlacesBuildsTokenSqlWithoutFormatError() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(mock(DataSource.class));
        JdbcTourPlaceChatMapper mapper = new JdbcTourPlaceChatMapper(jdbcTemplate);

        try {
            mapper.searchPlaces("두샨베 호텔 3곳", "ko", 5);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof java.util.UnknownFormatConversionException
                || ex instanceof java.util.UnknownFormatConversionException) {
                throw ex;
            }
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void structuredRegionAndCategoryDoNotRequireEveryTextToken() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        JdbcTourPlaceChatMapper mapper = new JdbcTourPlaceChatMapper(jdbcTemplate);

        mapper.searchPlaces("두샨베 가족 공원 추천", "ko", 5);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));

        assertThat(sql.getValue()).contains("p.category_code = :categoryCode");
        assertThat(sql.getValue()).contains("p.region_code = :regionCode");
        assertThat(sql.getValue()).doesNotContain("and exists");
        assertThat(sql.getValue()).contains("order by");
        assertThat(sql.getValue()).contains("lower(i.title) = lower(:keyword)");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void textOnlyQueriesStillRequireAtLeastOneTokenMatch() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        JdbcTourPlaceChatMapper mapper = new JdbcTourPlaceChatMapper(jdbcTemplate);

        mapper.searchPlaces("National Flag", "en", 5);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));

        assertThat(sql.getValue()).contains("and (");
        assertThat(sql.getValue()).contains("token0");
        assertThat(sql.getValue()).contains("token1");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void operatingHoursUseTourPlaceOpenAndCloseTime() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        JdbcTourPlaceChatMapper mapper = new JdbcTourPlaceChatMapper(jdbcTemplate);

        mapper.findOperatingHours("Rudaki Park", "en");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));

        assertThat(sql.getValue()).contains("p.open_time");
        assertThat(sql.getValue()).contains("p.close_time");
        assertThat(sql.getValue()).doesNotContain("tour_place_operating_hour");
    }
}
