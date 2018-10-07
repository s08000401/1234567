package com.example.companybussystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    FirebaseDatabase mdatabase;
    DatabaseReference mRef;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//      將標題列隱藏
        getSupportActionBar().hide();
//      如果上一次登入正確的話，會將暫存的資料輸入信箱
        final EditText edUserid = (EditText) findViewById(R.id.email);
        SharedPreferences setting = getSharedPreferences("atm", MODE_PRIVATE);
        edUserid.setText(setting.getString("PREF_USERID", ""));
//      先進行判斷，如果未開啟定位功能的話，要求開啟
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestLocationPermission();
        }
//      進行忘記密碼頁面的跳轉
        TextView forget = (TextView) findViewById(R.id.forgetpassword);
        forget.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ForgetPassWord.class);
                startActivity(intent);
                finish();
                return false;
            }
        });
//      連接資料庫，去檢測現在有幾筆資料
        mdatabase = FirebaseDatabase.getInstance();
        mRef = mdatabase.getReferenceFromUrl("https://nutcproject09-d1136.firebaseio.com/Car0/Location");
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    i++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
//      登入按鈕按下去時
        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              連線FireBase Auth
                mAuth = FirebaseAuth.getInstance();
                EditText user_email = (EditText) findViewById(R.id.email);
                EditText user_password = (EditText) findViewById(R.id.password);
                String account = user_email.getText().toString();
                String password = user_password.getText().toString();
//              資料庫內至少高於兩筆資料，程式才會繼續執行，防呆裝置
                if (i > 2) {
//                  信箱未輸入時
                    if (TextUtils.isEmpty(account)) {
                        Toast toast = Toast.makeText(MainActivity.this,
                                "請輸入信箱", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
//                  密碼未輸入時
                    if (TextUtils.isEmpty(password)) {
                        Toast toast = Toast.makeText(MainActivity.this,
                                "請輸入密碼", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
//                  進行信箱密碼的檢查
                    mAuth.signInWithEmailAndPassword(account, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
//                              將正確的信箱暫存起來
                                EditText edUserid = (EditText) findViewById(R.id.email);
                                String uid = edUserid.getText().toString();
                                SharedPreferences setting =
                                        getSharedPreferences("atm", MODE_PRIVATE);
                                setting.edit()
                                        .putString("PREF_USERID", uid)
                                        .commit();
//                              建立一個提示，以免家長認為程式故障
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("登入中，請稍後" +"\n"+
                                        "登入後，將會繪製歷史路線，請稍後");
                                final AlertDialog dialog = builder.create();
                                dialog.show();
                                TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
                                messageText.setGravity(Gravity.CENTER_HORIZONTAL);
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                                      進行地圖頁面的跳轉
                                        Intent intent = new Intent();
                                        intent.setClass(MainActivity.this, MapsActivity.class);
//                                      進行Bundle 將剛剛抓取的UID傳送到MapsActivity
                                        Bundle bundle = new Bundle();
                                        bundle.putString("uid", uid.toString());
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        dialog.hide();
//                                      將登入頁面的內存推入後台
                                        finish();
                                    }
                                }, 3000);
                            } else {
//                              如果信箱密碼未出現再Auth裡 跳出提示窗
                                Toast toast = Toast.makeText(MainActivity.this, "登入失敗，請重新登入", Toast.LENGTH_SHORT);
                                toast.show();
                                return;
                            }
                        }
                    });
                }else {
//                  信箱未輸入時
                    if (TextUtils.isEmpty(account)) {
                        Toast toast = Toast.makeText(MainActivity.this,
                                "請輸入信箱", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
//                  密碼未輸入時
                    else if (TextUtils.isEmpty(password)) {
                        Toast toast = Toast.makeText(MainActivity.this,
                                "請輸入密碼", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
//                  資料庫未有兩筆以上資料時
                    else {
                        Toast toast = Toast.makeText(MainActivity.this,
                                "校車尚未出發", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        });
    }
//  SDK 6.0 以上，需請求位置權限
    private void requestLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int already = checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (already != PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            else {

            }
        }
    }
//  按下返回鍵時，將離開程式
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            System.exit(0);
        }
        return false;
    }
}
