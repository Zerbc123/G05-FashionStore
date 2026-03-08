package vn.edu.fpt.fashionstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MomoRequestDTO {

    @JsonProperty("partnerCode")  private String partnerCode;
    @JsonProperty("accessKey")    private String accessKey;
    @JsonProperty("requestId")    private String requestId;
    @JsonProperty("amount")       private String amount;
    @JsonProperty("orderId")      private String orderId;
    @JsonProperty("orderInfo")    private String orderInfo;
    @JsonProperty("returnUrl")    private String returnUrl;
    @JsonProperty("notifyUrl")    private String notifyUrl;
    @JsonProperty("extraData")    private String extraData;
    @JsonProperty("requestType")  private String requestType;
    @JsonProperty("signature")    private String signature;

    private MomoRequestDTO() {}

    public String getPartnerCode() { return partnerCode; }
    public String getAccessKey()   { return accessKey; }
    public String getRequestId()   { return requestId; }
    public String getAmount()      { return amount; }
    public String getOrderId()     { return orderId; }
    public String getOrderInfo()   { return orderInfo; }
    public String getReturnUrl()   { return returnUrl; }
    public String getNotifyUrl()   { return notifyUrl; }
    public String getExtraData()   { return extraData; }
    public String getRequestType() { return requestType; }
    public String getSignature()   { return signature; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MomoRequestDTO obj = new MomoRequestDTO();

        public Builder partnerCode(String v)  { obj.partnerCode  = v; return this; }
        public Builder accessKey(String v)    { obj.accessKey    = v; return this; }
        public Builder requestId(String v)    { obj.requestId    = v; return this; }
        public Builder amount(String v)       { obj.amount       = v; return this; }
        public Builder orderId(String v)      { obj.orderId      = v; return this; }
        public Builder orderInfo(String v)    { obj.orderInfo    = v; return this; }
        public Builder returnUrl(String v)    { obj.returnUrl    = v; return this; }
        public Builder notifyUrl(String v)    { obj.notifyUrl    = v; return this; }
        public Builder extraData(String v)    { obj.extraData    = v; return this; }
        public Builder requestType(String v)  { obj.requestType  = v; return this; }
        public Builder signature(String v)    { obj.signature    = v; return this; }
        public MomoRequestDTO build()         { return obj; }
    }
}

