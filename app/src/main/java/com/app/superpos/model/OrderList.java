package com.app.superpos.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class OrderList implements Serializable {



    @SerializedName("order_id")
    private String orderId;

    @SerializedName("invoice_id")
    private String invoiceId;

    @SerializedName("order_date")
    private String orderDate;
    @SerializedName("order_time")
    private String orderTime;

    @SerializedName("order_type")
    private String orderType;


    @SerializedName("order_price")
    private String orderPrice;


    @SerializedName("order_payment_method")
    private String orderPaymentMethod;

    @SerializedName("discount")
    private String discount;

    @SerializedName("tax")
    private String tax;

    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("order_note")
    private String orderNote;


    @SerializedName("served_by")
    private String servedBy;

    @SerializedName("value")
    private String value;
    @SerializedName("sgst")
    private String sgst;
    @SerializedName("cgst")
    private String cgst;
    @SerializedName("reset_id")
    private String incrementedId = "0";

    private double totalPriceWithTax = 0.0;


    public String getInvoiceId() {
        return invoiceId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getOrderPaymentMethod() {
        return orderPaymentMethod;
    }


    public String getValue() {
        return value;
    }

    public String getServedBy() {
        return servedBy;
    }

    public String getTax() {
        return tax;
    }

    public String getDiscount() {
        return discount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public String getOrderPrice() {
        return orderPrice;
    }
    public double getSgstTax() {
        return Double.parseDouble(sgst);
    }
    public double getCgstTax() {
        return Double.parseDouble(cgst);
    }
    public String getIncrementedId() {
        return incrementedId;
    }
    public double getTotalPriceWithTax(){
        double orderTotalPrice = Double.parseDouble(orderPrice);
        double priceWithSgst = orderTotalPrice * (getSgstTax()/ 100);
        double priceWithCgst = orderTotalPrice * (getCgstTax()/ 100);
        return orderTotalPrice + priceWithSgst + priceWithCgst;
    }
}