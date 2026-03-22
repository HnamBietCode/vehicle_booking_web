package com.bookvehicle.example.sr.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, double[]> cache = new ConcurrentHashMap<>();

    public GeocodingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public record LatLng(double lat, double lng) {}

    /**
     * Geocode a Vietnamese address to lat/lng using Nominatim.
     * Tries multiple query strategies for better results.
     */
    public LatLng geocode(String address, String ward, String district, String province) {
        if (address == null || address.isBlank()) {
            return null;
        }

        String normalizedAddress = address.trim();

        // Build multiple query strategies
        List<String> queries = buildQueries(normalizedAddress, ward, district, province);

        for (String query : queries) {
            if (query == null || query.isBlank()) continue;

            // Check cache first
            double[] cached = cache.get(query.toLowerCase());
            if (cached != null) {
                return new LatLng(cached[0], cached[1]);
            }

            try {
                String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=vn&q="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("User-Agent", "VehicleBookingApp/1.0")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) continue;

                JsonNode root = objectMapper.readTree(response.body());
                if (!root.isArray() || root.isEmpty()) continue;

                JsonNode first = root.get(0);
                double lat = first.get("lat").asDouble();
                double lng = first.get("lon").asDouble();

                if (lat != 0.0 || lng != 0.0) {
                    cache.put(query.toLowerCase(), new double[]{lat, lng});
                    return new LatLng(lat, lng);
                }
            } catch (Exception e) {
                // Continue to next query strategy
            }

            // Small delay to avoid rate limiting
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        return null;
    }

    private List<String> buildQueries(String address, String ward, String district, String province) {
        String fullAddress = buildFullAddress(address, ward, district, province);
        String locationOnly = buildLocationParts(ward, district, province);
        String districtProvince = buildDistrictProvince(district, province);

        return List.of(
                address + ", Viet Nam",
                fullAddress,
                address,
                locationOnly,
                districtProvince
        ).stream()
                .filter(q -> q != null && !q.isBlank())
                .distinct()
                .toList();
    }

    private String buildFullAddress(String address, String ward, String district, String province) {
        StringBuilder sb = new StringBuilder(address);
        if (ward != null && !ward.isBlank()) sb.append(", ").append(ward.trim());
        if (district != null && !district.isBlank()) sb.append(", ").append(district.trim());
        if (province != null && !province.isBlank()) sb.append(", ").append(province.trim());
        sb.append(", Viet Nam");
        return sb.toString();
    }

    private String buildLocationParts(String ward, String district, String province) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (ward != null && !ward.isBlank()) { sb.append(ward.trim()); first = false; }
        if (district != null && !district.isBlank()) {
            if (!first) sb.append(", ");
            sb.append(district.trim());
            first = false;
        }
        if (province != null && !province.isBlank()) {
            if (!first) sb.append(", ");
            sb.append(province.trim());
        }
        if (!sb.isEmpty()) sb.append(", Viet Nam");
        return sb.toString();
    }

    private String buildDistrictProvince(String district, String province) {
        if (district == null || district.isBlank() || province == null || province.isBlank()) {
            return "";
        }
        return district.trim() + ", " + province.trim() + ", Viet Nam";
    }
}
