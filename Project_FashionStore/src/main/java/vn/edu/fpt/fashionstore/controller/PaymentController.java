package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.service.MomoService;
import vn.edu.fpt.fashionstore.util.MomoUtils;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private MomoService momoService;

    /**
     * Create Momo payment request and redirect to Momo payment page
     * @param orderId Order ID
     * @param amount Amount to pay (in VND)
     * @param orderInfo Order information
     * @param model Model for view
     * @return Redirect to Momo payment URL
     */
    @PostMapping("/momo/create")
    public String createMomoPayment(
            @RequestParam String orderId,
            @RequestParam Double amount,
            @RequestParam String orderInfo,
            Model model) {
        try {
            String requestId = MomoUtils.generateRequestId();
            Map<String, Object> response = momoService.createPaymentRequest(orderId, amount, orderInfo, requestId);

            // Check if payment request was successful
            if (response != null && response.containsKey("payUrl")) {
                String payUrl = (String) response.get("payUrl");
                return "redirect:" + payUrl;
            } else {
                model.addAttribute("error", "Failed to create payment request");
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * Handle Momo payment return (redirect URL after payment)
     * @param params Payment response parameters from Momo
     * @param model Model for view
     * @return Payment result page
     */
    @GetMapping("/momo/return")
    public String momoReturn(@RequestParam Map<String, String> params, Model model) {
        try {
            // Verify signature
            boolean isValid = momoService.verifyPaymentResponse(params);

            String resultCode = params.get("resultCode");
            String orderId = params.get("orderId");
            String amount = params.get("amount");
            String transId = params.get("transId");

            // Result code 0 means success
            if ("0".equals(resultCode) && isValid) {
                return "redirect:/order/checkout/success?orderId=" + orderId + "&transId=" + transId;
            } else {
                return "redirect:/order/checkout?error=payment_failed";
            }
        } catch (Exception e) {
            return "redirect:/fashionstore/order/checkout";
        }
    }

    /**
     * Handle Momo payment notification (IPN - Instant Payment Notification)
     * @param params Payment notification parameters from Momo
     * @return JSON response
     */
    @PostMapping("/momo/notify")
    @ResponseBody
    public Map<String, Object> momoNotify(@RequestParam Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Verify signature
            boolean isValid = momoService.verifyPaymentResponse(params);

            if (!isValid) {
                response.put("resultCode", "1");
                response.put("message", "Invalid signature");
                return response;
            }

            String resultCode = params.get("resultCode");
            String orderId = params.get("orderId");
            String transId = params.get("transId");

            // Result code 0 means success
            if ("0".equals(resultCode)) {
                response.put("resultCode", "0");
                response.put("message", "Success");
            } else {
                response.put("resultCode", "1");
                response.put("message", "Payment failed");
            }

            return response;
        } catch (Exception e) {
            response.put("resultCode", "1");
            response.put("message", e.getMessage());
            return response;
        }
    }
}
