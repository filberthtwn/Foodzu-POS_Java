package com.app.superpos.orders;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.app.superpos.R;
import com.app.superpos.model.OrderDetails;
import com.app.superpos.pdf_report.BarCodeEncoder;
import com.app.superpos.utils.IPrintToPrinter;
import com.app.superpos.utils.WoosimPrnMng;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.woosim.printer.WoosimCmd;

import java.text.DecimalFormat;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class TestPrinter implements IPrintToPrinter {


    String name, price, qty, weight;
    double cost_total, subTotal, totalPrice;
    DecimalFormat f;

    private Context context;
    List<OrderDetails> orderDetailsList;
    String incrementedId, currency, servedBy, shopName, shopAddress, shopEmail, shopContact, invoiceId, orderDate, orderTime, customerName, footer, tax, discount;
    Bitmap bm;
    WoosimPrnMng printManager;

    public TestPrinter(Context context, String incrementedId, String shopName, String shopAddress, String shopEmail, String shopContact, String invoiceId, String orderDate, String orderTime, String customerName, String footer, double subTotal, String totalPrice, String discount, String currency, String served_by, List<OrderDetails> orderDetailsList) {
        this.incrementedId = String.format("%04d", Integer.parseInt(incrementedId));
        this.context = context;
        this.shopName = shopName;
        this.shopAddress = shopAddress;
        this.shopEmail = shopEmail;
        this.shopContact = shopContact;
        this.invoiceId = invoiceId;
        this.orderDate = orderDate;
        this.orderTime = orderTime;
        this.customerName = customerName;
        this.footer = footer;
        this.subTotal = subTotal;
        this.totalPrice =  Math.round(
            Double.parseDouble(totalPrice)
        );
        this.discount = discount;
        this.currency = currency;
        this.servedBy = served_by;
        this.orderDetailsList = orderDetailsList;

        f = new DecimalFormat("#0.00");
    }

    @Override
    public void printContent(WoosimPrnMng prnMng) {
        this.printManager = prnMng;
        printMainReceiptContent();
        printSmallReceiptContent();
    }

    private void printMainReceiptContent() {
        //Generate barcode
        BarCodeEncoder qrCodeEncoder = new BarCodeEncoder();
        bm = null;

        try {
            bm = qrCodeEncoder.encodeAsBitmap(invoiceId, BarcodeFormat.CODE_128, 400, 48);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        printManager.printStr(incrementedId, 1, WoosimCmd.ALIGN_CENTER);
        String[] shopNames = shopName.split(",");
        for (String name : shopNames) {
            printManager.printStr(name, 1, WoosimCmd.ALIGN_CENTER);
        }
        printManager.printStr(shopAddress, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Customer Receipt ", 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Contact: " + shopContact, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Invoice ID: " + invoiceId, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Order Time: " + orderTime + " " + orderDate, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr(customerName, 1, WoosimCmd.ALIGN_CENTER);

        printManager.printStr("Email: " + shopEmail, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Served By: " + servedBy, 1, WoosimCmd.ALIGN_CENTER);

        printManager.printStr("--------------------------------");

        printManager.printStr("  Items        Price  Qty  Total", 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("--------------------------------");

        double getItemPrice;
        double priceWithSgst = 0.0;
        double priceWithCgst = 0.0;

        for (int i = 0; i < orderDetailsList.size(); i++) {
            OrderDetails orderDetails = orderDetailsList.get(i);
            name = orderDetailsList.get(i).getProductName();
            price = orderDetailsList.get(i).getProductPrice();
            getItemPrice = Double.parseDouble(price);
            qty = orderDetailsList.get(i).getProductQuantity();
            weight = orderDetailsList.get(i).getProductWeight();
            cost_total = Integer.parseInt(qty) * Double.parseDouble(price);
            printManager.leftRightAlign(name + " " + f.format(getItemPrice) + "x" + qty, "=" + f.format(cost_total));
            priceWithSgst = priceWithSgst + (cost_total * orderDetails.getSgst()/ 100);
            priceWithCgst = priceWithCgst + (cost_total * orderDetails.getCgst()/ 100);
        }

        printManager.printStr("--------------------------------");
        printManager.printStr("Sub Total: " + currency + f.format(subTotal), 1, WoosimCmd.ALIGN_RIGHT);
        printManager.printStr("Sgst (+): " + currency + f.format(priceWithSgst), 1, WoosimCmd.ALIGN_RIGHT);
        printManager.printStr("Cgst (+): " + currency + f.format(priceWithCgst), 1, WoosimCmd.ALIGN_RIGHT);
        printManager.printStr("Discount (-): " + currency + f.format(Double.parseDouble(discount)), 1, WoosimCmd.ALIGN_RIGHT);
        printManager.printStr("--------------------------------");
        printManager.printStr("Total Price: " + currency + f.format(totalPrice), 1, WoosimCmd.ALIGN_RIGHT);

        printManager.printNewLine();
        printManager.printStr(footer, 1, WoosimCmd.ALIGN_CENTER);

        printManager.printNewLine();
        printManager.printStr("--------------------------------");
        printManager.printNewLine();
    }

    private void printSmallReceiptContent() {
        printManager.printStr(incrementedId, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("Order Time: " + orderTime + " " + orderDate, 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("  Items        Price  Qty  Total", 1, WoosimCmd.ALIGN_CENTER);
        printManager.printStr("--------------------------------");
        for (int i = 0; i < orderDetailsList.size(); i++) {
            name = orderDetailsList.get(i).getProductName();
            price = orderDetailsList.get(i).getProductPrice();
            qty = orderDetailsList.get(i).getProductQuantity();
            weight = orderDetailsList.get(i).getProductWeight();
            cost_total = Integer.parseInt(qty) * Double.parseDouble(price);
            printManager.leftRightAlign(name + " " + f.format(Double.parseDouble(price)) + "x" + qty, "=" + f.format(cost_total));
        }
        printManager.printStr("--------------------------------");
        printManager.printStr("Total Price: " + currency + f.format(totalPrice), 1, WoosimCmd.ALIGN_RIGHT);
        printEnded(printManager);
        printManager.printNewLine();
    }

    @Override
    public void printEnded(WoosimPrnMng prnMng) {
        //Do any finalization you like after print ended.
        if (prnMng.printSucc()) {
            Toasty.success(context, R.string.print_succ, Toast.LENGTH_LONG).show();
        } else {
            Toasty.error(context, R.string.print_error, Toast.LENGTH_LONG).show();
        }
    }
}
