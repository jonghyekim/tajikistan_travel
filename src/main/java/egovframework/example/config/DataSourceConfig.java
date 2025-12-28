package egovframework.example.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl(
            "jdbc:mysql://localhost:3306/tajikistan_local" +
            "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
        );
        ds.setUsername("root");
        ds.setPassword("root1234");

        return ds;
    }
}
