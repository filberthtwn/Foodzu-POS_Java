package com.app.superpos.orders;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.superpos.Constant;
import com.app.superpos.R;
import com.app.superpos.adapter.OrderDetailsAdapter;
import com.app.superpos.bt_device.DeviceListActivity;
import com.app.superpos.database.DatabaseAccess;
import com.app.superpos.helpers.SharedPreferencesHelper;
import com.app.superpos.model.OrderDetails;
import com.app.superpos.model.OrderList;
import com.app.superpos.networking.ApiClient;
import com.app.superpos.networking.ApiInterface;
import com.app.superpos.pdf_report.BarCodeEncoder;
import com.app.superpos.pdf_report.PDFTemplate;
import com.app.superpos.utils.BaseActivity;
import com.app.superpos.utils.IPrintToPrinter;
import com.app.superpos.utils.PrefMng;
import com.app.superpos.utils.Tools;
import com.app.superpos.utils.WoosimPrnMng;
import com.app.superpos.utils.printerFactory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsActivity extends BaseActivity {


    ImageView imgNoProduct;
    TextView txtNoProducts, txtSubTotalPrice, txtSgst, txtCgst, txtDiscount, txtTotalCost;
    String invoiceId,shopName, orderDate,orderTime, orderPrice, customerName, discount,shopAddress,shopEmail,shopContact;
    OrderList orderDetail;
    double  calculatedTotalPrice;

    Button btnPdfReceipt,btnThermalPrinter;
    List<OrderDetails> orderDetails = new ArrayList<>();

    //how many headers or column you need, add here by using ,
    //headers and get clients para meter must be equal
    private String[] header = {"Description", "Price"};


    String longText, shortText, userName;

    private PDFTemplate templatePDF;
    SharedPreferences sp;
    String currency;
    Bitmap bm = null;
    ProgressDialog loading;
    RecyclerView recyclerView;
    DecimalFormat f;

    private static final int REQUEST_CONNECT = 100;

    private WoosimPrnMng mPrnMng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        recyclerView = findViewById(R.id.recycler);
        imgNoProduct = findViewById(R.id.image_no_product);
        txtSubTotalPrice = findViewById(R.id.txt_total_price);
        txtSgst = findViewById(R.id.txt_sgst);
        txtCgst = findViewById(R.id.txt_cgst);
        txtDiscount = findViewById(R.id.txt_discount);
        txtTotalCost = findViewById(R.id.txt_total_cost);
        btnPdfReceipt = findViewById(R.id.btn_pdf_receipt);
        txtNoProducts = findViewById(R.id.txt_no_products);
        btnThermalPrinter = findViewById(R.id.btn_thermal_printer);

        sp = getSharedPreferences(Constant.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        shopName = sp.getString(Constant.SP_SHOP_NAME, "N/A");
        shopEmail = sp.getString(Constant.SP_SHOP_EMAIL, "N/A");
        shopContact = sp.getString(Constant.SP_SHOP_CONTACT, "N/A");
        shopAddress = sp.getString(Constant.SP_SHOP_ADDRESS, "N/A");
        userName = sp.getString(Constant.SP_STAFF_NAME, "N/A");
        currency = sp.getString(Constant.SP_CURRENCY_SYMBOL, "N/A");
        f = new DecimalFormat("#0.00");


        orderPrice = getIntent().getExtras().getString(Constant.ORDER_PRICE);
        orderDate = getIntent().getExtras().getString(Constant.ORDER_DATE);
        orderTime = getIntent().getExtras().getString(Constant.ORDER_TIME);
        discount = getIntent().getExtras().getString(Constant.DISCOUNT);
        invoiceId = getIntent().getExtras().getString(Constant.INVOICE_ID);
        customerName = getIntent().getExtras().getString(Constant.CUSTOMER_NAME);
        orderDetail = (OrderList) getIntent().getExtras().getSerializable(Constant.ORDER_DETAIL);

        getProductsData(invoiceId);

        imgNoProduct.setVisibility(View.GONE);
        txtNoProducts.setVisibility(View.GONE);

        getSupportActionBar().setHomeButtonEnabled(true); //for back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//for back button
        getSupportActionBar().setTitle(R.string.order_details);


        // set a GridLayoutManager with default vertical orientation and 3 number of columns
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(OrderDetailsActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager); // set LayoutManager to RecyclerView

        recyclerView.setHasFixedSize(true);
        if (orderDetail == null) return;
        double getOrderPrice = Double.parseDouble(orderPrice);
        txtSubTotalPrice.setText(getString(R.string.sub_total)+": "+currency+f.format(getOrderPrice));
        txtDiscount.setText(getString(R.string.discount) + " : " + currency+ discount);

        OrderDetailsAdapter.subTotalPrice=0;

        //for pdf report
        shortText = "Customer Name: Mr/Mrs. " + customerName;
        longText = "< Have a nice day. Visit again >";
        templatePDF = new PDFTemplate(getApplicationContext());


        BarCodeEncoder qrCodeEncoder = new BarCodeEncoder();
        try {
            bm = qrCodeEncoder.encodeAsBitmap(invoiceId, BarcodeFormat.CODE_128, 600, 300);
        } catch (WriterException e) {
            Log.d("Data", e.toString());
        }


        btnPdfReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                templatePDF.openDocument();
                templatePDF.addMetaData(Constant.ORDER_RECEIPT, Constant.ORDER_RECEIPT, "Smart POS");
                templatePDF.addTitle(shopName, shopAddress+ "\n Email: " + shopEmail + "\nContact: " + shopContact + "\nInvoice ID:" + invoiceId, orderDate + " " + orderTime+"\nServed By: "+userName);
                templatePDF.addParagraph(shortText);

                templatePDF.createTable(header, getPDFReceipt());
                templatePDF.addImage(bm);

                templatePDF.addRightParagraph(longText);

                templatePDF.closeDocument();
                templatePDF.viewPDF();


            }
        });



        btnThermalPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check if the Bluetooth is available and on.
                if (!Tools.isBlueToothOn(OrderDetailsActivity.this)) return;
                PrefMng.saveActivePrinter(OrderDetailsActivity.this, PrefMng.PRN_WOOSIM_SELECTED);
                //Pick a Bluetooth device
                Intent i = new Intent(OrderDetailsActivity.this, DeviceListActivity.class);
                startActivityForResult(i, REQUEST_CONNECT);
            }
        });


    }

    //for pdf
    private ArrayList<String[]> getPDFReceipt() {
        ArrayList<String[]> rows = new ArrayList<>();

        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(OrderDetailsActivity.this);
        databaseAccess.open();

        String name, price, qty, weight;
        double costTotal;

        double subtotal = 0.0;
        double totalSgst = 0.0;
        double totalCgst = 0.0;

        for (int i = 0; i < orderDetails.size(); i++) {
            OrderDetails orderDetail = orderDetails.get(i);
            name = orderDetail.getProductName();
            price = orderDetail.getProductPrice();
            qty = orderDetail.getProductQuantity();
            weight = orderDetail.getProductWeight();
            subtotal = subtotal + Double.parseDouble(price);
            totalSgst = totalSgst + (orderDetail.getPriceWithSgst() * Integer.parseInt(qty));
            totalCgst = totalCgst + (orderDetail.getPriceWithCgst() * Integer.parseInt(qty));
            costTotal = Integer.parseInt(qty) * Double.parseDouble(price);
            rows.add(new String[]{name + "\n" + weight + "\n" + "(" + qty + "x" + currency + price + ")", currency + costTotal});
        }
        rows.add(new String[]{"..........................................", ".................................."});
        rows.add(new String[]{"Sub Total: ", "(+)"+currency + f.format(Double.parseDouble(orderPrice))});
        rows.add(new String[]{"Total Sgst: ", "(+)"+currency + f.format(totalSgst)});
        rows.add(new String[]{"Total Cgst: ", "(+)"+currency + f.format(totalCgst)});
        rows.add(new String[]{"Discount: ", "(-)"+currency + discount});
        rows.add(new String[]{"..........................................", ".................................."});
        rows.add(new String[]{"Total Price: ", currency + f.format(Double.parseDouble(orderPrice) + totalSgst + totalCgst)});
        return rows;
    }





    public void getProductsData(String invoiceId) {


        loading=new ProgressDialog(OrderDetailsActivity.this);
        loading.setCancelable(false);
        loading.setMessage(getString(R.string.please_wait));
        loading.show();
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<List<OrderDetails>> call;
        call = apiInterface.OrderDetailsByInvoice(invoiceId);

        call.enqueue(new Callback<List<OrderDetails>>() {
            @Override
            public void onResponse(@NonNull Call<List<OrderDetails>> call, @NonNull Response<List<OrderDetails>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderDetails = response.body();
                    setupPriceSummaryView();
                    loading.dismiss();
                    if (orderDetails.isEmpty()) {
                        Toasty.warning(OrderDetailsActivity.this, R.string.no_product_found, Toast.LENGTH_SHORT).show();


                    } else {

                        OrderDetailsAdapter orderDetailsAdapter = new OrderDetailsAdapter(OrderDetailsActivity.this, orderDetails);

                        recyclerView.setAdapter(orderDetailsAdapter);




                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OrderDetails>> call, @NonNull Throwable t) {

                loading.dismiss();
                Toast.makeText(OrderDetailsActivity.this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                Log.d("Error : ", t.toString());
            }
        });


    }

    private void setupPriceSummaryView() {
        double totalPriceWithCgst = 0.0;
        double totalPriceWithSgst = 0.0;
        for (int i = 0; i < orderDetails.size(); i++) {
            OrderDetails orderDetail = orderDetails.get(i);
            int quantity = Integer.valueOf(orderDetail.getProductQuantity());
            totalPriceWithCgst = totalPriceWithCgst + orderDetail.getPriceWithCgst() * quantity;
            totalPriceWithSgst = totalPriceWithSgst + orderDetail.getPriceWithSgst() * quantity;
        }
        txtCgst.setText(getString(R.string.cgst) + " : " + currency + f.format(totalPriceWithCgst));
        txtSgst.setText(getString(R.string.sgst) + " : " + currency + f.format(totalPriceWithSgst));
        double totalTax = totalPriceWithCgst + totalPriceWithSgst;
        double subtotalPrice = Double.parseDouble(orderDetail.getOrderPrice());
        double discount = Double.parseDouble(orderDetail.getDiscount());
        calculatedTotalPrice = subtotalPrice + totalTax - discount;
        txtTotalCost.setText(
            getString(R.string.total_price)
                + ": "
                + currency
                + f.format(calculatedTotalPrice)
        );
    }

    //for back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CONNECT && resultCode == RESULT_OK) {
            try {
                //Get device address to print to.
                String blutoothAddr = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                //The interface to print text to thermal printers.
                IPrintToPrinter testPrinter = new TestPrinter(
                    this,
                    orderDetail.getIncrementedId(),
                    shopName,
                    shopAddress,
                    shopEmail,
                    shopContact,
                    invoiceId,
                    orderDate,
                    orderTime,
                    shortText,
                    longText,
                    Double.parseDouble(orderPrice),
                    f.format(calculatedTotalPrice),
                    discount,
                    currency,
                    userName,orderDetails
                );
                //Connect to the printer and after successful connection issue the print command.
                mPrnMng = printerFactory.createPrnMng(this, blutoothAddr, testPrinter);
                SharedPreferencesHelper.instance.storePrinterAddress(
                        OrderDetailsActivity.this,
                        blutoothAddr
                );
            } catch (Exception e) {
                System.out.println("PRINTER NOT SAVED");
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (mPrnMng != null) mPrnMng.releaseAllocatoins();
        super.onDestroy();
    }







}

