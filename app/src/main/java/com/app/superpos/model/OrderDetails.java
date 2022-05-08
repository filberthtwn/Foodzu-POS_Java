package com.app.superpos.model;

import com.google.gson.annotations.SerializedName;

public class OrderDetails {

    public OrderDetails() { }

    public OrderDetails(
            String name,
            String price,
            String quantity,
            String weight,
            String sgst,
            String cgst
    ) {
        this.productName = name;
        this.productPrice = price;
        this.productQuantity = quantity;
        this.productWeight = weight;
        this.sgst = sgst;
        this.cgst = cgst;
    }

    @SerializedName("order_details_id")
    private String orderDetailsId;

    @SerializedName("invoice_id")
    private String invoiceId;

    @SerializedName("product_order_date")
    private String productOrderDate;
    @SerializedName("product_name")
    private String productName;

    @SerializedName("product_quantity")
    private String productQuantity;

    @SerializedName("product_weight")
    private String productWeight;

    @SerializedName("product_price")
    private String productPrice;

    @SerializedName("value")
    private String value;

    @SerializedName("product_image")
    private String productImage;

    @SerializedName("reset_id")
    private int resetId;

    @SerializedName("sgst")
    private String sgst = "0.0";

    @SerializedName("cgst")
    private String cgst = "0.0";


    public String getInvoiceId() {
        return invoiceId;
    }


    public String getProductName() {
        return productName;
    }

    public String getProductOrderDate() {
        return productOrderDate;
    }

    public String getProductQuantity() {
        return productQuantity;
    }

    public String getProductPrice() {
        return productPrice;
    }


    public String getValue() {
        return value;
    }

    public String getProductImage() {
        return productImage;
    }


    public String getProductWeight() {
        return productWeight;
    }
    public String getResetId() {
        return String.valueOf(resetId);
    }
    public double getPriceWithSgst() {
        double sgstInDouble = Double.parseDouble(sgst);
        return Double.parseDouble(productPrice) * (sgstInDouble/ 100);
    }
    public double getPriceWithCgst() {
        double cgstInDouble = Double.parseDouble(cgst);
        return Double.parseDouble(productPrice) * (cgstInDouble/ 100);
    }
    public double getSgst() {
        if (sgst == null) return 0.0;
        return Double.parseDouble(sgst);
    }
    public double getCgst() {
        if (cgst == null) return 0.0;
        return Double.parseDouble(cgst);
    }

    public String getOrderDetailsId() {
        return orderDetailsId;
    }

    public double getCostTotal() {
        return Integer.parseInt(productQuantity) * Double.parseDouble(productPrice);
    }
}