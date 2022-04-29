package com.app.superpos.model;

import com.google.gson.annotations.SerializedName;

public class OrderDetails {

    public OrderDetails() { }

    public OrderDetails(
            String name,
            String price,
            String quantity,
            String weight
    ) {
        this.productName = name;
        this.productPrice = price;
        this.productQuantity = quantity;
        this.productWeight = weight;
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


    public String getOrderDetailsId() {
        return orderDetailsId;
    }

    public double getCostTotal() {
        return Integer.parseInt(productQuantity) * Double.parseDouble(productPrice);
    }
}