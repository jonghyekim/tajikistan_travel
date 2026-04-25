package egovframework.example.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class OperatingHourDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperatingHourDataInitializer.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OperatingHourDataInitializer(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Long> placeIds = jdbcTemplate.queryForList(
            """
            select p.place_id
              from tour_place p
             where p.is_active = true
               and not exists (
                   select 1
                     from tour_place_operating_hour h
                    where h.place_id = p.place_id
               )
             order by p.place_id
            """,
            Map.of(),
            Long.class
        );

        if (placeIds.isEmpty()) {
            return;
        }

        for (Long placeId : placeIds) {
            seedDefaultWeeklyHours(placeId);
        }
        log.info("Seeded default operating hours for {} tour places", placeIds.size());
    }

    private void seedDefaultWeeklyHours(Long placeId) {
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            boolean closed = dayOfWeek == DayOfWeek.MONDAY;
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("placeId", placeId)
                .addValue("dayOfWeek", dayOfWeek.name())
                .addValue("seasonCode", "ALL")
                .addValue("opensAt", closed ? null : LocalTime.of(9, 0))
                .addValue("closesAt", closed ? null : LocalTime.of(18, 0))
                .addValue("closed", closed)
                .addValue("lastAdmissionAt", closed ? null : LocalTime.of(17, 30))
                .addValue("note", closed ? "Regular closing day" : "Default operating hours. Verify before visiting.")
                .addValue("active", true);

            jdbcTemplate.update(
                """
                insert into tour_place_operating_hour
                    (place_id, day_of_week, season_code, opens_at, closes_at, is_closed, last_admission_at, note, is_active)
                values
                    (:placeId, :dayOfWeek, :seasonCode, :opensAt, :closesAt, :closed, :lastAdmissionAt, :note, :active)
                """,
                params
            );
        }
    }
}
