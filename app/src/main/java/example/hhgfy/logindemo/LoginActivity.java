package example.hhgfy.logindemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import example.hhgfy.logindemo.Util.HttpUtil;
import okhttp3.FormBody;

public class LoginActivity extends AppCompatActivity {

    private EditText username_login;
    private EditText password_login;
    private CheckBox rememberpassword;
    private CheckBox autologin;

    private Button btn_login;
    private Button btn_registe;

    private SharedPreferences sp;
    private String usernameValue;
    private String passwordValue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //只有当前的应用程序才可以对当前这个SharedPreferences读写
        sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);

        //找到布局及其对应的控件
        setContentView(R.layout.login);
        username_login = findViewById(R.id.login_username);
        password_login = findViewById(R.id.login_password);
        rememberpassword = findViewById(R.id.login_rememberpassword);
        autologin = findViewById(R.id.login_autologin);

        btn_login = findViewById(R.id.login_btn);
        btn_registe=findViewById(R.id.registe_btn);

        //是否选中 记住密码
        if (sp.getBoolean("isCheck", false)) {
            rememberpassword.setChecked(true);
            username_login.setText(sp.getString("username", ""));
            password_login.setText(sp.getString("password", ""));

            password_login.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            //是否选中 自动登录
            if (sp.getBoolean("auto_ischeck", false)) {
                autologin.setChecked(true);
                Log.d("login","自动登录");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        //点击登录按钮
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //获取用户名和密码值
                usernameValue = username_login.getText().toString();
                passwordValue = password_login.getText().toString();

                login(usernameValue, passwordValue);
            }
        });

        //点击注册按钮
        btn_registe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("login","跳转到注册页");
                Intent intent = new Intent(LoginActivity.this, RegisteActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void putUserInfo(String username, String password) {
        sp=this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        sp.edit()
                .putString("username",username)
                .putString("password",password)
                .putBoolean("isCheck",rememberpassword.isChecked())
                .putBoolean("auto_ischeck",autologin.isChecked())
                .commit();
    }

    private void login(final String username, final String password) {
        final String baseUrl = "http://120.25.224.151:5001/";

        final Handler handler=new Handler(){
            public void  handleMessage(Message msg){
                if (msg.what==1){
                    Toast.makeText(LoginActivity.this,"验证成功",Toast.LENGTH_SHORT).show();
                    // 在 SharedPreferences 中保存用户信息
                    putUserInfo(username,password);
                    Log.d("login","手动登录");
                    //跳转页面
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                }else if (msg.what==0){ //用户名密码错误
                    Toast.makeText(LoginActivity.this,"验证失败",Toast.LENGTH_SHORT).show();
                    Log.d("username",msg.toString());
                    cleanPass();
                }else if (msg.what==-1){//网络请求出错
                    Toast.makeText(LoginActivity.this,"网络请求出错",Toast.LENGTH_SHORT).show();
                    cleanPass();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg=new Message();

                try {
                    String url = baseUrl + "api/login";

                    FormBody formBody = new FormBody.Builder()
                            .add("username", username)
                            .add("password", password)
                            .build();

                    HttpUtil httpUtil = new HttpUtil();
                    String responseData = httpUtil.post(url, formBody);
                    Log.d("responseData","------------------\n\n"+responseData);

                    //解析json 格式
                    JSONObject response=new JSONObject(responseData);
                    String resState =response.getString("state");
                    String resMsg=response.getString("msg");


                    if ("success".equals(resState)){
                        Log.d("msg",resMsg);
                        msg.what=1;

                    }else  if ("fail".equals(resState)){
                        Log.d("msg",resMsg);
                        msg.what=0;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    msg.what=-1;
                    msg.obj=e;
                }
                handler.sendMessage(msg);
            }
        }).start();

    }



    //清除密码
    private void cleanPass() {
        password_login = findViewById(R.id.login_password);
        password_login.setText("");
    }


//    private void getCheckUser(final String username, final String password) {
//
//        boolean result = false;
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String url = baseUrl + "api/login";
//
//                    FormBody formBody = new FormBody.Builder()
//                            .add("username", username)
//                            .add("password", password)
//                            .build();
//
//                    HttpUtil httpUtil = new HttpUtil();
//                    String responseData = httpUtil.post(url, formBody);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }).start();
//
//    }
}

