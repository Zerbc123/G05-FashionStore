package vn.edu.fpt.fashionstore.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.config.MomoConfig;
import vn.edu.fpt.fashionstore.util.MomoUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoService {

    @Autowired
    private MomoConfig momoConfig;

    private final Gson gson = new Gson();

    /**
     * Create payment request to Momo
     * @param orderId Order ID
     * @param amount Amount to pay (in VND)
     * @param orderInfo Order information
     * @param requestId Request ID (unique)
     * @return Momo payment response
     */
    public Map<String, Object> createPaymentRequest(String orderId, Double amount, String orderInfo, String requestId) {
        try {
            String requestType = momoConfig.getRequestType() != null ? momoConfig.getRequestType() : "captureWallet";
            String extraData = "";
            
            // Build raw signature data
            long amountLong = amount.longValue();
            // URL encode orderInfo to handle Vietnamese characters
            String encodedOrderInfo = URLEncoder.encode(orderInfo, StandardCharsets.UTF_8);
            String rawSignature = String.format(
                    "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    momoConfig.getAccessKey(),
                    amountLong,
                    extraData,
                    momoConfig.getNotifyUrl(),
                    orderId,
                    encodedOrderInfo,
                    momoConfig.getPartnerCode(),
                    momoConfig.getReturnUrl(),
                    requestId,
                    requestType
            );

            // Generate signature
            String signature = MomoUtils.generateSignature(rawSignature, momoConfig.getSecretKey());

            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("accessKey", momoConfig.getAccessKey());
            requestBody.addProperty("partnerCode", momoConfig.getPartnerCode());
            requestBody.addProperty("requestId", requestId);
            requestBody.addProperty("amount", amountLong);
            requestBody.addProperty("orderId", orderId);
            requestBody.addProperty("orderInfo", encodedOrderInfo);
            requestBody.addProperty("redirectUrl", momoConfig.getReturnUrl());
            requestBody.addProperty("ipnUrl", momoConfig.getNotifyUrl());
            requestBody.addProperty("extraData", extraData);
            requestBody.addProperty("requestType", requestType);
            requestBody.addProperty("signature", signature);
            requestBody.addProperty("lang", "vi");

            // Send request to Momo
            Map<String, Object> response = sendRequest(momoConfig.getEndpoint(), requestBody.toString());
            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Verify payment response from Momo
     * @param responseData Response data from Momo
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyPaymentResponse(Map<String, String> responseData) {
        try {
            String signature = responseData.get("signature");
            if (signature == null) {
                return false;
            }

            // Build raw signature data for verification
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    responseData.getOrDefault("accessKey", ""),
                    responseData.getOrDefault("amount", ""),
                    responseData.getOrDefault("extraData", ""),
                    responseData.getOrDefault("message", ""),
                    responseData.getOrDefault("orderId", ""),
                    responseData.getOrDefault("orderInfo", ""),
                    responseData.getOrDefault("orderType", ""),
                    responseData.getOrDefault("partnerCode", ""),
                    responseData.getOrDefault("payType", ""),
                    responseData.getOrDefault("requestId", ""),
                    responseData.getOrDefault("responseTime", ""),
                    responseData.getOrDefault("resultCode", ""),
                    responseData.getOrDefault("transId", "")
            );

            // Generate signature and compare
            String computedSignature = MomoUtils.generateSignature(rawSignature, momoConfig.getSecretKey());
            return signature.equals(computedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send HTTP POST request to Momo API
     * @param url Momo API endpoint
     * @param jsonBody Request body in JSON format
     * @return Response as Map
     */
    private Map<String, Object> sendRequest(String url, String jsonBody) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonBody));

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                return gson.fromJson(responseBody, Map.class);
            });
        }
    }
}
