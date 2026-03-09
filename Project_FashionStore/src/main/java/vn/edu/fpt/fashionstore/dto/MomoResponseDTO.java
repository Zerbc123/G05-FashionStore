package vn.edu.fpt.fashionstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoResponseDTO {

    @JsonProperty("partnerCode")  private String partnerCode;
    @JsonProperty("accessKey")    private String accessKey;
    @JsonProperty("requestId")    private String requestId;
    @JsonProperty("amount")       private String amount;
    @JsonProperty("orderId")      private String orderId;
    @JsonProperty("responseTime") private String responseTime;
    @JsonProperty("message")      private String message;
    @JsonProperty("localMessage") private String localMessage;
    @JsonProperty("errorCode")    private String errorCode;
    @JsonProperty("payUrl")       private String payUrl;
    @JsonProperty("deeplink")     private String deeplink;
    @JsonProperty("qrCodeUrl")    private String qrCodeUrl;
    @JsonProperty("signature")    private String signature;

    public String getPartnerCode()  { return partnerCode; }
    public String getAccessKey()    { return accessKey; }
    public String getRequestId()    { return requestId; }
    public String getAmount()       { return amount; }
    public String getOrderId()      { return orderId; }
    public String getResponseTime() { return responseTime; }
    public String getMessage()      { return message; }
    public String getLocalMessage() { return localMessage; }
    public String getErrorCode()    { return errorCode; }
    public String getPayUrl()       { return payUrl; }
    public String getDeeplink()     { return deeplink; }
    public String getQrCodeUrl()    { return qrCodeUrl; }
    public String getSignature()    { return signature; }

    public void setPartnerCode(String v)  { this.partnerCode  = v; }
    public void setAccessKey(String v)    { this.accessKey    = v; }
    public void setRequestId(String v)    { this.requestId    = v; }
    public void setAmount(String v)       { this.amount       = v; }
    public void setOrderId(String v)      { this.orderId      = v; }
    public void setResponseTime(String v) { this.responseTime = v; }
    public void setMessage(String v)      { this.message      = v; }
    public void setLocalMessage(String v) { this.localMessage = v; }
    public void setErrorCode(String v)    { this.errorCode    = v; }
    public void setPayUrl(String v)       { this.payUrl       = v; }
    public void setDeeplink(String v)     { this.deeplink     = v; }
    public void setQrCodeUrl(String v)    { this.qrCodeUrl    = v; }
    public void setSignature(String v)    { this.signature    = v; }

    public boolean isSuccess() { return "0".equals(errorCode); }

    @Override
    public String toString() {
        return "MomoResponseDTO{" +
                "orderId='" + orderId + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                ", payUrl='" + payUrl + '\'' +
                ", transId not in create-response" +
                '}';
    }
}