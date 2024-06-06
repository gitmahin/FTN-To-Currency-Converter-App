package com.example.ftntocurrency;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    LinearLayout linearLayout;
    private ImageView save_user;
    private Button convert_btn;
    private TextView result;
    private EditText FTN;
    private EditText dollar;
    private EditText coun_Code;
    private ImageView save_icon;
    private EditText ftn_minus;
    private Button clear_btn;
    private EditText userEdit;
    private Switch switchtoftn;
    private TextView modeTextView;

    private Button refreshButton;
    private TextView showftnprice;

    private ExecutorService executorService;
    private Handler mainThreadHandler;

    double ftn_to_dollar_equ;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout = findViewById(R.id.user_container);
        save_user = findViewById(R.id.save_user);
        convert_btn = findViewById(R.id.convert_button);
        result = findViewById(R.id.textResult);
        FTN = findViewById(R.id.FTN);
        dollar = findViewById(R.id.dollar);
        coun_Code = findViewById(R.id.country_code);
        save_icon = findViewById(R.id.saveIcon);
        ftn_minus = findViewById(R.id.ftn_minus);
        clear_btn = findViewById(R.id.clear_btn);
        userEdit = findViewById(R.id.userEdit);
        modeTextView = findViewById(R.id.modeTextView);
        refreshButton = findViewById(R.id.refreshButton);
        showftnprice = findViewById(R.id.showftnprice);
        switchtoftn = findViewById(R.id.switchtoftn);
        switchtoftn.setChecked(false);

        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        showftnprice.setText("Loading...");
        getData();

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showftnprice.setText("Refreshing...");
                reinitializeFtnPrice();
            }
        });
        switchtoftn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (switchtoftn.isChecked()) {
                        Toast.makeText(MainActivity.this, "Switched to FTN mode", Toast.LENGTH_SHORT).show();
                        modeTextView.setText("Enter your FTN");
                        FTN.setHint("0.123230123");
                        convert_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    String ftn_s = FTN.getText().toString();
                                    String curren_s = dollar.getText().toString();
                                    String country_code = coun_Code.getText().toString().toUpperCase();
                                    String ftn_minus_s = ftn_minus.getText().toString();
//  switched the function to ftn mode
                                    if (!ftn_s.isEmpty() && !curren_s.isEmpty() && !country_code.isEmpty()) {
                                        double ftn_d = Double.parseDouble(ftn_s);
                                        double curren_d = Double.parseDouble(curren_s);

                                        if (!ftn_minus_s.isEmpty()) {
                                            double ftn_minus_d = Double.parseDouble(ftn_minus_s);
                                            double minus_ftn_result = ftn_d - ftn_minus_d;
                                            double ftn_to_dollar = minus_ftn_result * ftn_to_dollar_equ;
                                            double currency_result = ftn_to_dollar * curren_d;
                                            result.setText(currency_result + " " + "(" + country_code + ")");
                                        } else {
                                            double ftn_to_dollar = ftn_d * ftn_to_dollar_equ;
                                            double currency_result = ftn_to_dollar * curren_d;
                                            result.setText(currency_result + " " + "(" + country_code + ")");
                                        }

                                    } else {
                                        Toast.makeText(MainActivity.this, "Cannot calculate empty", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Switched to Dollar mode", Toast.LENGTH_SHORT).show();
                        modeTextView.setText("Enter your Dollar");
                        FTN.setHint("1.002942 $");
                        dollarModeCalculation();
                    }
            }
        });

        dollarModeCalculation();

        save_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String curren_s = dollar.getText().toString();
                    String country_code = coun_Code.getText().toString();
                    SharedPreferences sp = getSharedPreferences("ftntocurrency", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("curren_str", curren_s);
                    editor.putString("cc_str", country_code);
                    editor.apply();
                    Toast.makeText(MainActivity.this, "Currency & Country Code saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Cannot save data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        clear_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                result.setText("");
                FTN.setText("");
                ftn_minus.setText("");
                userEdit.setText("");
            }
        });

        try {
            SharedPreferences getShared = getSharedPreferences("ftntocurrency", MODE_PRIVATE);
            String curren_value = getShared.getString("curren_str", "110");
            String cc_value = getShared.getString("cc_str", "BDT");
            dollar.setText(curren_value);
            coun_Code.setText(cc_value);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot retrive data from storage", Toast.LENGTH_SHORT).show();
        }

        save_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String userEdit_str = userEdit.getText().toString();
                    if (!userEdit_str.isEmpty()) {
                        if (!result.getText().toString().isEmpty()) {
                            view = getLayoutInflater().inflate(R.layout.card_user, null, false);
                            TextView user_income = view.findViewById(R.id.user_income);
                            String newUserIncome = "[ " + userEdit_str + " ] " + result.getText().toString();
                            user_income.setText(newUserIncome);
                            addNewElement(newUserIncome);
                            Toast.makeText(MainActivity.this, "User: " + userEdit_str + " saved", Toast.LENGTH_SHORT).show();
                            result.setText("");
                            FTN.setText("");
                            ftn_minus.setText("");
                            userEdit.setText("");
                            reinitialize();
                        } else {
                            Toast.makeText(MainActivity.this, "Amount is required!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "User name is required", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showData();
    }

    public void reinitializeFtnPrice() {
        getData();
    }

    public void getData() {
        String url = "https://etherscan.io/token/0xaedf386b755465871ff874e3e37af5976e247064";

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
                    Document document = Jsoup.connect(url).get();
                    Elements element = document.select("#ContentPlaceHolder1_tr_valuepertoken");
                    Elements ftnvalue = element.select("div span:nth-child(1)");
                    String price = extractPrice(ftnvalue.text());

                    mainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "FTN price: " + price + "$", Toast.LENGTH_SHORT).show();
                            showftnprice.setText(price);
                            Double parsePricetoDouble = Double.parseDouble(price);
                            ftn_to_dollar_equ = parsePricetoDouble;
                        }
                    });

                } catch (Exception e) {
                    mainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "ERROR CONNECTION TIMEDOUT", Toast.LENGTH_SHORT).show();
                            showftnprice.setText("Connecting...");
                        }
                    });

                }
            }
        });

    }

    public static String extractPrice(String text) {
        Pattern pattern = Pattern.compile("\\$([0-9]+\\.[0-9]{2})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }


    public void dollarModeCalculation() {
        if (!switchtoftn.isChecked()) {
            modeTextView.setText("Enter your Dollar");
            FTN.setHint("1.002942 $");
            convert_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        String ftn_s = FTN.getText().toString();
                        String curren_s = dollar.getText().toString();
                        String country_code = coun_Code.getText().toString().toUpperCase();
                        String ftn_minus_s = ftn_minus.getText().toString();
//  switched the function to dollar mode
                        if (!ftn_s.isEmpty() && !curren_s.isEmpty() && !country_code.isEmpty()) {
                            double ftn_d = Double.parseDouble(ftn_s);
                            double curren_d = Double.parseDouble(curren_s);

                            if (!ftn_minus_s.isEmpty()) {
                                double ftn_minus_d = Double.parseDouble(ftn_minus_s);
                                double minus_ftn_result = ftn_d - (ftn_minus_d * ftn_to_dollar_equ);
                                double currency_result = minus_ftn_result * curren_d;
                                result.setText(currency_result + " " + "(" + country_code + ")");
                            } else {
                                double currency_result = ftn_d * curren_d;
                                result.setText(currency_result + " " + "(" + country_code + ")");
                            }

                        } else {
                            Toast.makeText(MainActivity.this, "Cannot calculate empty", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void reinitialize() {
        showData();
    }

    private void addNewElement(String newElement) {
        SharedPreferences sp = getSharedPreferences("ftnuser", MODE_PRIVATE);
        String json = sp.getString("data_array", null);

        Gson gson = new Gson();
        List<String> dataList;

        if (json != null) {
            Type type = new TypeToken<List<String>>() {
            }.getType();
            dataList = gson.fromJson(json, type);
        } else {
            dataList = new ArrayList<>();
        }

        dataList.add(newElement);

        String updatedJson = gson.toJson(dataList);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("data_array", updatedJson);
        editor.apply();
    }

    public void showData() {
        try {
            SharedPreferences sp = getSharedPreferences("ftnuser", MODE_PRIVATE);
            String json = sp.getString("data_array", null);
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {
                }.getType();
                List<String> dataList = gson.fromJson(json, type);
                linearLayout.removeAllViews();

                for (String data : dataList) {
                    View view = getLayoutInflater().inflate(R.layout.card_user, null, false);
                    TextView user_income = view.findViewById(R.id.user_income);
                    ImageView del_btn = (ImageView) view.findViewById(R.id.delete_btn);
                    user_income.setText(data);
                    linearLayout.addView(view);
                    del_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteElement(data);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot retrieve data from storage", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteElement(String elementToDelete) {
        SharedPreferences sp = getSharedPreferences("ftnuser", MODE_PRIVATE);
        String json = sp.getString("data_array", null);
        try {
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {
                }.getType();
                List<String> dataList = gson.fromJson(json, type);

                if (dataList.remove(elementToDelete)) {
                    String updatedJson = gson.toJson(dataList);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("data_array", updatedJson);
                    editor.apply();
                    Toast.makeText(this, "User Deleted", Toast.LENGTH_SHORT).show();
                    linearLayout.removeAllViews();
                    showData();
                } else {
                    Toast.makeText(this, "Element not found", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "No data to delete", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}