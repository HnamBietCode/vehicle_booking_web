package com.bookvehicle.example.sr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MomoService {

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partnerCode}")
    private String partnerCode;

    @Value("${momo.accessKey}")
    private String accessKey;

    @Value("${momo.secretKey}")
    private String secretKey;

    @Value("${momo.redirectUrl}")
    private String redirectUrl;

    @Value("${momo.ipnUrl}")
    private String ipnUrl;

    /**
     * Create MoMo payment request and return the payUrl
     */
    public String createDepositRequest(Long userId, BigDecimal amount) {
        try {
            String amountStr = String.valueOf(amount.longValue());
            String orderInfo = "Nap tien vao vi - User " + userId;
            String requestId = UUID.randomUUID().toString();
            // Prefix to identify the MoMo order
            String orderIdMoMo = "DEP_" + userId + "_" + System.currentTimeMillis();

            // Generate Visa/Mastercard Payment (payWithCC)
            String requestType = "payWithCC";
            String extraData = "";

            // 1. Create Signature (API V2)
            String rawHash = "accessKey=" + accessKey +
                    "&amount=" + amountStr +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + ipnUrl +
                    "&orderId=" + orderIdMoMo +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + redirectUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmacHex(rawHash);

            // 2. Create Body Request
            Map<String, Object> body = new HashMap<>();
            body.put("partnerCode", partnerCode);
            body.put("partnerName", "VehicleBooking");
            body.put("storeId", "VehicleBook_Wallet");
            body.put("requestId", requestId);
            body.put("amount", Long.parseLong(amountStr));
            body.put("orderId", orderIdMoMo);
            body.put("orderInfo", orderInfo);
            body.put("redirectUrl", redirectUrl);
            body.put("ipnUrl", ipnUrl);
            body.put("lang", "vi");
            body.put("extraData", extraData);
            body.put("requestType", requestType);
            body.put("signature", signature);

            // 3. Send POST Request
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

            // 4. Parse payUrl
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(response.getBody(), Map.class);
            System.out.println("MoMo V2 Response: " + result);

            return (String) result.get("payUrl");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
