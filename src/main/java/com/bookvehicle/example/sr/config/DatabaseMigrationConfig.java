package com.bookvehicle.example.sr.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateDatabase() {
        try {
            // Alter columns to match new Rating entity requirements
            jdbcTemplate.execute("ALTER TABLE ratings MODIFY COLUMN ref_type ENUM('RENTAL', 'BOOKING', 'OTHER') NOT NULL");
            jdbcTemplate.execute("ALTER TABLE ratings MODIFY COLUMN ref_id BIGINT NULL");
            
            // Set existing drivers to available by default
            jdbcTemplate.execute("UPDATE drivers SET is_available = TRUE WHERE is_available = FALSE");
            
            // Add current_address to vehicles
            try {
                jdbcTemplate.execute("ALTER TABLE vehicles ADD COLUMN current_address VARCHAR(300)");
            } catch (Exception e) {
                // Column might already exist, ignore error for this specific statement
                System.out.println("Column current_address may already exist: " + e.getMessage());
            }

            // Recreate view to include current_address
            jdbcTemplate.execute("CREATE OR REPLACE VIEW v_available_vehicles AS " +
                    "SELECT v.id, v.category, v.name AS vehicle_name, v.license_plate, v.color, v.current_address, " +
                    "v.price_per_km, v.price_per_hour, v.price_per_day, v.avg_rating, v.total_trips, " +
                    "d.full_name AS driver_name, u.phone AS driver_phone, d.is_available AS driver_available " +
                    "FROM vehicles v " +
                    "LEFT JOIN drivers d ON d.id = v.assigned_driver " +
                    "LEFT JOIN users u ON u.id = d.user_id " +
                    "WHERE v.status = 'AVAILABLE'");
            
            System.out.println("====== DATABASE MIGRATION APPLIED SUCESFULLY ======");
        } catch (Exception e) {
            System.err.println("====== DATABASE MIGRATION FAILED (may have already run): " + e.getMessage() + " ======");
        }
    }
}
