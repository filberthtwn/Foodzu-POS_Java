package com.app.superpos.pos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.superpos.Constant;
import com.app.superpos.R;
import com.app.superpos.adapter.CartAdapter;
import com.app.superpos.database.DatabaseAccess;
import com.app.superpos.helpers.SharedPreferencesHelper;
import com.app.superpos.model.Customer;
import com.app.superpos.model.OrderDetails;
import com.app.superpos.networking.ApiClient;
import com.app.superpos.networking.ApiInterface;
import com.app.superpos.orders.OrdersActivity;
import com.app.superpos.orders.TestPrinter;
import com.app.superpos.utils.BaseActivity;
import com.app.superpos.utils.IPrintToPrinter;
import com.app.superpos.utils.Tools;
import com.app.superpos.utils.Utils;
import com.app.superpos.utils.WoosimPrnMng;
import com.app.superpos.utils.printerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductCart extends BaseActivity implements ProductCartDelegate {

    private final int REQUEST_CONNECT = 100;
    private WoosimPrnMng mPrnMng = null;
    List<HashMap<String, String>> cartProductList;
    CartAdapter productCartAdapter;
    ImageView imgNoProduct;
    Button btnSubmitOrder;
    TextView txtNoProduct, txtTotalPrice;
    LinearLayout linearLayout;
    DatabaseAccess databaseAccess = DatabaseAccess.getInstance(ProductCart.this);


    List<String> customerNames, orderTypeNames, paymentMethodNames;
    List<Customer> customerData;
    ArrayAdapter<String> customerAdapter, orderTypeAdapter, paymentMethodAdapter;
    SharedPreferences sp;
    String servedBy,staffId,shopTax,userName,currency,shopID,ownerId,shopName,shopEmail,shopContact,shopAddress;
    DecimalFormat f;

    List<OrderDetails> orderDetails = new ArrayList<>();
    Boolean isNeedPrinting = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_cart);

        getSupportActionBar().setHomeButtonEnabled(true); //for back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//for back button
        getSupportActionBar().setTitle(R.string.product_cart);
        f = new DecimalFormat("#0.00");
        sp = getSharedPreferences(Constant.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        shopName = sp.getString(Constant.SP_SHOP_NAME, "N/A");
        shopEmail = sp.getString(Constant.SP_SHOP_EMAIL, "N/A");
        shopContact = sp.getString(Constant.SP_SHOP_CONTACT, "N/A");
        shopAddress = sp.getString(Constant.SP_SHOP_ADDRESS, "N/A");
        servedBy = sp.getString(Constant.SP_STAFF_NAME, "");
        staffId = sp.getString(Constant.SP_STAFF_ID, "");
        shopTax= sp.getString(Constant.SP_TAX, "");
        userName = sp.getString(Constant.SP_STAFF_NAME, "N/A");
        currency= sp.getString(Constant.SP_CURRENCY_SYMBOL, "");

        shopID = sp.getString(Constant.SP_SHOP_ID, "");
        ownerId = sp.getString(Constant.SP_OWNER_ID, "");



        getCustomers(shopID,ownerId);

        RecyclerView recyclerView = findViewById(R.id.cart_recyclerview);
        imgNoProduct = findViewById(R.id.image_no_product);
        btnSubmitOrder = findViewById(R.id.btn_submit_order);
        txtNoProduct = findViewById(R.id.txt_no_product);
        linearLayout = findViewById(R.id.linear_layout);
        txtTotalPrice = findViewById(R.id.txt_total_price);

        txtNoProduct.setVisibility(View.GONE);


        // set a GridLayoutManager with default vertical orientation and 3 number of columns
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager); // set LayoutManager to RecyclerView


        recyclerView.setHasFixedSize(true);


        databaseAccess.open();


        //get data from local database
        cartProductList = databaseAccess.getCartProduct();

        Log.d("CartSize", "" + cartProductList.size());

        if (cartProductList.isEmpty()) {

            imgNoProduct.setImageResource(R.drawable.empty_cart);
            imgNoProduct.setVisibility(View.VISIBLE);
            txtNoProduct.setVisibility(View.VISIBLE);
            btnSubmitOrder.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            txtTotalPrice.setVisibility(View.GONE);
        } else {
            setupTotalPriceView();
            imgNoProduct.setVisibility(View.GONE);
            productCartAdapter = new CartAdapter(this, ProductCart.this, cartProductList, txtTotalPrice, btnSubmitOrder, imgNoProduct, txtNoProduct);
            recyclerView.setAdapter(productCartAdapter);
        }
        btnSubmitOrder.setOnClickListener(v -> dialog());
    }


    public void proceedOrder(String type, String paymentMethod, String customerName, String discount) {
        databaseAccess = DatabaseAccess.getInstance(ProductCart.this);
        databaseAccess.open();

        int itemCount = databaseAccess.getCartItemCount();

        databaseAccess.open();
        double orderPrice = databaseAccess.getTotalPrice();


        if (itemCount > 0) {

            databaseAccess.open();
            //get data from local database
            final List<HashMap<String, String>> lines;
            lines = databaseAccess.getCartProduct();

            if (lines.isEmpty()) {
                Toasty.error(ProductCart.this, R.string.no_product_found, Toast.LENGTH_SHORT).show();
            } else {

                //get current timestamp
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
                String currentYear = new SimpleDateFormat("yyyy", Locale.ENGLISH).format(new Date());
                //H denote 24 hours and h denote 12 hour hour format
                String currentTime = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date()); //HH:mm:ss a

                //timestamp use for invoice id for unique
                Long tsLong = System.currentTimeMillis() / 1000;
                String timeStamp = tsLong.toString();
                Log.d("Time", timeStamp);
                //Invoice number=INV+StaffID+CurrentYear+timestamp
                String invoiceNumber="INV"+staffId+currentYear+timeStamp;

                final JSONObject obj = new JSONObject();
                try {


                    obj.put("invoice_id", invoiceNumber);
                    obj.put("order_date", currentDate);
                    obj.put("order_time", currentTime);
                    obj.put("order_type", type);
                    obj.put("order_payment_method", paymentMethod);
                    obj.put("customer_name", customerName);

                    obj.put("order_price", String.valueOf(orderPrice));
                    obj.put("discount", discount);
                    obj.put("served_by", servedBy);
                    obj.put("shop_id", shopID);
                    obj.put("owner_id", ownerId);

                    JSONArray array = new JSONArray();


                    for (int i = 0; i < lines.size(); i++) {

                        databaseAccess.open();
                        String invoiceId = lines.get(i).get("invoice_id");
                        String productId = lines.get(i).get("product_id");
                        String productName = lines.get(i).get("product_name");
                        String productImage = lines.get(i).get("product_image");



                        String productWeightUnit = lines.get(i).get("product_weight_unit");



                        JSONObject objp = new JSONObject();
                        objp.put("invoice_id", invoiceId);
                        objp.put("product_id", productId);
                        objp.put("product_name", productName);
                        objp.put("product_image", productImage);
                        objp.put("product_weight", lines.get(i).get("product_weight") + " " + productWeightUnit);
                        objp.put("product_qty", lines.get(i).get("product_qty"));
                        objp.put("product_price", lines.get(i).get("product_price"));
                        objp.put("product_order_date", currentDate);
                        String sgstInString = lines.get(i).get("sgst");
                        String cgstInString = lines.get(i).get("cgst");
                        orderDetails.add(
                            new OrderDetails(
                                productName,
                                lines.get(i).get("product_price"),
                                lines.get(i).get("product_qty"),
                                lines.get(i).get("product_weight") + " " + productWeightUnit,
                                sgstInString,
                                cgstInString
                            )
                        );
                        array.put(objp);

                    }
                    obj.put("lines", array);


                } catch (JSONException e) {
                    e.printStackTrace();
                }


                Utils utils=new Utils();

                if(utils.isNetworkAvailable(ProductCart.this))
                {
                   orderSubmit(obj);
                }
                else
                {
                    Toasty.error(this, R.string.no_network_connection, Toast.LENGTH_SHORT).show();
                }




            }

        } else {
            Toasty.error(ProductCart.this, R.string.no_product_in_cart, Toast.LENGTH_SHORT).show();
        }
    }





    private void orderSubmit(final JSONObject obj) {

        Log.d("Json",obj.toString());


        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.show();

        RequestBody body2 = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), String.valueOf(obj));
        Call<OrderDetails> call = apiInterface.submitOrders(body2);
        call.enqueue(new Callback<OrderDetails>() {
            @Override
            public void onResponse(
                @NonNull Call<OrderDetails> call,
                @NonNull Response<OrderDetails> response
            ) {
                if (response.isSuccessful()) {
                    try {
                        String incrementedId = response.body().getResetId();
                        obj.put("incremented_id", incrementedId);
                        if (isNeedPrinting) {
                            printReceipt(obj);
                        }
                        progressDialog.dismiss();
                        Toasty.success(
                            ProductCart.this,
                            R.string.order_successfully_done,
                            Toast.LENGTH_SHORT
                        ).show();
                        databaseAccess.open();
                        databaseAccess.emptyCart();
                        dialogSuccess();
                    } catch (JSONException e) {
                        System.out.println("===ERROR");
                        System.out.println(e);
                        progressDialog.dismiss();
                        Toasty.error(
                    ProductCart.this,
                            R.string.error,
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                } else {
                    Toasty.error(ProductCart.this, R.string.error, Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Log.d("error", response.toString());
                }
            }

            @Override
            public void onFailure(
                Call<OrderDetails> call,
                Throwable t
            ) {
                Log.d("onFailure", t.toString());
            }
        });


    }

    private void printReceipt(final JSONObject obj) {
        try {
            String activePrinterAddr = SharedPreferencesHelper
                    .instance
                    .getPrinterAddress(
                            ProductCart.this
                    );
            //The interface to print text to thermal printers.
            double orderPriceInDouble = Double.parseDouble(obj.getString("order_price"));
            double discountInDouble = Double.parseDouble(obj.getString("discount"));
            double calculatedTotalPrice = orderPriceInDouble + discountInDouble;
            IPrintToPrinter testPrinter = new TestPrinter(
                this,
                obj.getString("incremented_id"),
                shopName,
                shopAddress,
                shopEmail,
                shopContact,
                obj.getString("invoice_id"),
                obj.getString("order_date"),
                obj.getString("order_time"),
                "Customer Name: Mr/Mrs. " + obj.getString("customer_name"),
                "< Have a nice day. Visit again >",
                Double.parseDouble(obj.getString("order_price")),
                f.format(calculatedTotalPrice),
                obj.getString("discount"),
                currency,
                userName,
                orderDetails
            );
            //Connect to the printer and after successful connection issue the print command.
            mPrnMng = printerFactory.createPrnMng(this, activePrinterAddr, testPrinter);
            SharedPreferencesHelper.instance.storePrinterAddress(
                ProductCart.this,
                activePrinterAddr
            );
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mPrnMng != null) mPrnMng.releaseAllocatoins();
        super.onDestroy();
    }

    public void dialogSuccess() {


        AlertDialog.Builder dialog = new AlertDialog.Builder(ProductCart.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        dialog.setView(dialogView);
        dialog.setCancelable(false);

        ImageButton dialogBtnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        Button dialogBtnViewAllOrders = dialogView.findViewById(R.id.btn_view_all_orders);

        AlertDialog alertDialogSuccess = dialog.create();

        dialogBtnCloseDialog.setOnClickListener(v -> {

            alertDialogSuccess.dismiss();

            Intent intent = new Intent(ProductCart.this,PosActivity.class);
            startActivity(intent);
            finish();

        });


        dialogBtnViewAllOrders.setOnClickListener(v -> {

            alertDialogSuccess.dismiss();

            Intent intent = new Intent(ProductCart.this, OrdersActivity.class);
            startActivity(intent);
            finish();

        });

        alertDialogSuccess.show();


    }

    //dialog for taking otp code
    public void dialog() {

        databaseAccess.open();
        double totalTax = databaseAccess.getTotalTax();

        String shopCurrency = currency;
       // String tax = shopTax;

        double getTax = totalTax;



        AlertDialog.Builder dialog = new AlertDialog.Builder(ProductCart.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        dialog.setView(dialogView);
        dialog.setCancelable(false);

        final Button dialogBtnNoPrint = dialogView.findViewById(R.id.btn_payment_dialog_noPrint);
        final Button dialogBtnSubmit = dialogView.findViewById(R.id.btn_submit);
        final ImageButton dialogBtnClose = dialogView.findViewById(R.id.btn_close);
        final TextView dialogOrderPaymentMethod = dialogView.findViewById(R.id.dialog_order_status);
        final TextView dialogOrderType = dialogView.findViewById(R.id.dialog_order_type);
        final TextView dialogCustomer = dialogView.findViewById(R.id.dialog_customer);
        final TextView dialogTxtTotal = dialogView.findViewById(R.id.dialog_txt_total);
        final TextView dialogTxtTotalTax = dialogView.findViewById(R.id.dialog_txt_total_tax);
        final TextView dialogTxtLevelTax = dialogView.findViewById(R.id.dialog_level_tax);
        final TextView dialogTxtTotalCost = dialogView.findViewById(R.id.dialog_txt_total_cost);
        final EditText dialogEtxtDiscount = dialogView.findViewById(R.id.etxt_dialog_discount);


        final ImageButton dialogImgCustomer = dialogView.findViewById(R.id.img_select_customer);
        final ImageButton dialogImgOrderPaymentMethod = dialogView.findViewById(R.id.img_order_payment_method);
        final ImageButton dialogImgOrderType = dialogView.findViewById(R.id.img_order_type);


        dialogTxtLevelTax.setText(getString(R.string.total_tax));
        double totalCost = CartAdapter.totalPrice;
        dialogTxtTotal.setText(shopCurrency + totalCost);


        dialogTxtTotalTax.setText(shopCurrency + f.format(getTax));


        double discount = 0;
        double calculatedTotalCost = totalCost + getTax - discount;
        dialogTxtTotalCost.setText(shopCurrency + calculatedTotalCost);


        dialogEtxtDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("data", s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                double discount = 0;
                String getDiscount = s.toString();
                if (!getDiscount.isEmpty() && !getDiscount.equals(".")) {
                    double calculatedTotalCost = totalCost + getTax;
                    discount = Double.parseDouble(getDiscount);
                    if (discount > calculatedTotalCost) {
                        dialogEtxtDiscount.setError(getString(R.string.discount_cant_be_greater_than_total_price));
                        dialogEtxtDiscount.requestFocus();

                        dialogBtnSubmit.setVisibility(View.INVISIBLE);

                    } else {

                        dialogBtnSubmit.setVisibility(View.VISIBLE);
                        calculatedTotalCost = totalCost + getTax - discount;
                        dialogTxtTotalCost.setText(shopCurrency + calculatedTotalCost);
                    }
                } else {

                    double calculatedTotalCost = totalCost + getTax - discount;
                    dialogTxtTotalCost.setText(shopCurrency + calculatedTotalCost);
                }


            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("data", s.toString());
            }
        });


        orderTypeNames = new ArrayList<>();
        databaseAccess.open();

        //get data from local database
        final List<HashMap<String, String>> orderType;
        orderType = databaseAccess.getOrderType();

        for (int i = 0; i < orderType.size(); i++) {

            // Get the ID of selected Country
            orderTypeNames.add(orderType.get(i).get("order_type_name"));

        }


        //payment methods
        paymentMethodNames = new ArrayList<>();
        databaseAccess.open();

        //get data from local database
        final List<HashMap<String, String>> paymentMethod;
        paymentMethod = databaseAccess.getPaymentMethod();

        for (int i = 0; i < paymentMethod.size(); i++) {

            // Get the ID of selected Country
            paymentMethodNames.add(paymentMethod.get(i).get("payment_method_name"));

        }


        dialogImgOrderPaymentMethod.setOnClickListener(v -> {

            paymentMethodAdapter = new ArrayAdapter<>(ProductCart.this, android.R.layout.simple_list_item_1);
            paymentMethodAdapter.addAll(paymentMethodNames);

            AlertDialog.Builder dialog1 = new AlertDialog.Builder(ProductCart.this);
            View dialogView1 = getLayoutInflater().inflate(R.layout.dialog_list_search, null);
            dialog1.setView(dialogView1);
            dialog1.setCancelable(false);

            Button dialogButton = (Button) dialogView1.findViewById(R.id.dialog_button);
            EditText dialogInput = (EditText) dialogView1.findViewById(R.id.dialog_input);
            TextView dialogTitle = (TextView) dialogView1.findViewById(R.id.dialog_title);
            ListView dialogList = (ListView) dialogView1.findViewById(R.id.dialog_list);


            dialogTitle.setText(R.string.select_payment_method);
            dialogList.setVerticalScrollBarEnabled(true);
            dialogList.setAdapter(paymentMethodAdapter);

            dialogInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    Log.d("data", s.toString());
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    paymentMethodAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d("data", s.toString());
                }
            });


            final AlertDialog alertDialog = dialog1.create();

            dialogButton.setOnClickListener(v1 -> alertDialog.dismiss());

            alertDialog.show();


            dialogList.setOnItemClickListener((parent, view, position, id) -> {

                alertDialog.dismiss();
                String selectedItem = paymentMethodAdapter.getItem(position);
                dialogOrderPaymentMethod.setText(selectedItem);


            });
        });


        dialogImgOrderType.setOnClickListener(v -> {


            orderTypeAdapter = new ArrayAdapter<>(ProductCart.this, android.R.layout.simple_list_item_1);
            orderTypeAdapter.addAll(orderTypeNames);

            AlertDialog.Builder dialog12 = new AlertDialog.Builder(ProductCart.this);
            View dialogView12 = getLayoutInflater().inflate(R.layout.dialog_list_search, null);
            dialog12.setView(dialogView12);
            dialog12.setCancelable(false);

            Button dialogButton = (Button) dialogView12.findViewById(R.id.dialog_button);
            EditText dialogInput = (EditText) dialogView12.findViewById(R.id.dialog_input);
            TextView dialogTitle = (TextView) dialogView12.findViewById(R.id.dialog_title);
            ListView dialogList = (ListView) dialogView12.findViewById(R.id.dialog_list);


            dialogTitle.setText(R.string.select_order_type);
            dialogList.setVerticalScrollBarEnabled(true);
            dialogList.setAdapter(orderTypeAdapter);

            dialogInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    Log.d("data", s.toString());
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    orderTypeAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d("data", s.toString());
                }
            });


            final AlertDialog alertDialog = dialog12.create();

            dialogButton.setOnClickListener(v13 -> alertDialog.dismiss());

            alertDialog.show();


            dialogList.setOnItemClickListener((parent, view, position, id) -> {

                alertDialog.dismiss();
                String selectedItem = orderTypeAdapter.getItem(position);


                dialogOrderType.setText(selectedItem);


            });
        });


        dialogImgCustomer.setOnClickListener(v -> {
            customerAdapter = new ArrayAdapter<>(ProductCart.this, android.R.layout.simple_list_item_1);
            customerAdapter.addAll(customerNames);

            AlertDialog.Builder dialog13 = new AlertDialog.Builder(ProductCart.this);
            View dialogView13 = getLayoutInflater().inflate(R.layout.dialog_list_search, null);
            dialog13.setView(dialogView13);
            dialog13.setCancelable(false);

            Button dialogButton = (Button) dialogView13.findViewById(R.id.dialog_button);
            EditText dialogInput = (EditText) dialogView13.findViewById(R.id.dialog_input);
            TextView dialogTitle = (TextView) dialogView13.findViewById(R.id.dialog_title);
            ListView dialogList = (ListView) dialogView13.findViewById(R.id.dialog_list);

            dialogTitle.setText(R.string.select_customer);
            dialogList.setVerticalScrollBarEnabled(true);
            dialogList.setAdapter(customerAdapter);

            dialogInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    Log.d("data", s.toString());
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    customerAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d("data", s.toString());
                }
            });


            final AlertDialog alertDialog = dialog13.create();

            dialogButton.setOnClickListener(v12 -> alertDialog.dismiss());

            alertDialog.show();


            dialogList.setOnItemClickListener((parent, view, position, id) -> {

                alertDialog.dismiss();
                String selectedItem = customerAdapter.getItem(position);


                dialogCustomer.setText(selectedItem);


            });
        });


        final AlertDialog alertDialog = dialog.create();
        alertDialog.show();
        dialogBtnNoPrint.setOnClickListener(it -> {
            isNeedPrinting = false;
            String orderType1 = dialogOrderType.getText().toString().trim();
            String orderPaymentMethod = dialogOrderPaymentMethod.getText().toString().trim();
            String customerName = dialogCustomer.getText().toString().trim();
            String discount1 = dialogEtxtDiscount.getText().toString().trim();
            if (discount1.isEmpty()) {
                discount1 = "0.00";
            }
            proceedOrder(orderType1, orderPaymentMethod, customerName, discount1);
        });
        dialogBtnSubmit.setOnClickListener(v -> {
            isNeedPrinting = true;
            if (!Tools.isBlueToothOn(ProductCart.this)) {
                return;
            };
            String orderType1 = dialogOrderType.getText().toString().trim();
            String orderPaymentMethod = dialogOrderPaymentMethod.getText().toString().trim();
            String customerName = dialogCustomer.getText().toString().trim();
            String discount1 = dialogEtxtDiscount.getText().toString().trim();
            if (discount1.isEmpty()) {
                discount1 = "0.00";
            }
            proceedOrder(orderType1, orderPaymentMethod, customerName, discount1);
            alertDialog.dismiss();
        });


        dialogBtnClose.setOnClickListener(v -> alertDialog.dismiss());


    }




    public void getCustomers(String shopId,String ownerId) {

        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);

        Call<List<Customer>> call;


        call = apiInterface.getCustomers("",shopId,ownerId);

        call.enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(@NonNull Call<List<Customer>> call, @NonNull Response<List<Customer>> response) {


                if (response.isSuccessful() && response.body() != null) {

                    customerData = response.body();

                    customerNames = new ArrayList<>();

                    for (int i = 0; i < customerData.size(); i++) {

                       customerNames.add(customerData.get(i).getCustomerName());

                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<List<Customer>> call, @NonNull Throwable t) {

                //write own action
            }
        });


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

    public void setupTotalPriceView() {
        System.out.println("===SETUP TOTAL PRICE VIEW");
        databaseAccess.open();
        cartProductList = databaseAccess.getCartProduct();
        databaseAccess.close();

        double totalProductPrice = 0.0;
        double totalSgstTax = 0.0;
        double totalCgstTax = 0.0;

        for (int i = 0; i < cartProductList.size(); i++) {
            HashMap<String, String> cartProduct = cartProductList.get(i);
            double productQuantity  = Double.parseDouble(cartProduct.get("product_qty"));
            double productPrice = Double.parseDouble(cartProduct.get("product_price")) * productQuantity;
            totalProductPrice = totalProductPrice + productPrice;
            double sgstInPercent = Double.parseDouble(cartProduct.get("sgst"))/ 100;
            totalSgstTax = totalSgstTax + (productPrice * sgstInPercent);
            double cgstInPercent = Double.parseDouble(cartProduct.get("cgst"))/ 100;
            totalCgstTax = totalCgstTax + (productPrice * cgstInPercent);
        }
        txtTotalPrice.setText(
            getApplicationContext().getString(R.string.total_price)
            + currency + f.format(totalProductPrice)
            +"\nTotal SGST: "
            + currency + f.format(totalSgstTax)
            +"\nTotal CGST: "
            + currency + f.format(totalCgstTax)
            +"\nPrice with Tax: "
            +currency + f.format(totalProductPrice + totalSgstTax + totalCgstTax)
        );
    }

    @Override
    public void onUpdateTotalPriceView() {
        setupTotalPriceView();
    }
}

