package example.hhgfy.logindemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import example.hhgfy.logindemo.Util.HttpUtil;
import okhttp3.FormBody;

/**
 * Created by hhgfy on 2018/5/16.
 */

public class RegisteActivity extends Activity {

    private Button btn_submit;
    private EditText username_registe;
    private EditText password_registe;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        setContentView(R.layout.registe);

        btn_submit=findViewById(R.id.registe_submit_btn);
        username_registe=findViewById(R.id.registe_username);
        password_registe=findViewById(R.id.registe_password);


        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usernameValue = username_registe.getText().toString();
                String passwordValue = password_registe.getText().toString();

                registe(usernameValue, passwordValue);
            }
        });

    }

    private void registe(final String username, final String password) {
        final String baseUrl = "http://120.25.224.151:5001/";

        final Handler handler=new Handler(){
            public void  handleMessage(Message msg){

                if (msg.what==1){ //注册成功 ，自动登录
                    Toast.makeText(RegisteActivity.this,"验证成功",Toast.LENGTH_SHORT).show();
                    // 在 SharedPreferences 中保存用户信息
                    putUserInfo(username,password);
                    Log.d("registe","注册后自动登录");
                    //跳转页面
                    Intent intent = new Intent(RegisteActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                }else if (msg.what==0){ //注册失败，已经被注册
                    Toast.makeText(RegisteActivity.this,"该用户名已被注册！",Toast.LENGTH_SHORT).show();
                    Log.d("registe",msg.obj.toString());
                    cleanEditText();
                }else if (msg.what==-1){//网络请求出错
                    Toast.makeText(RegisteActivity.this,"网络请求出错",Toast.LENGTH_SHORT).show();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg=new Message();

                try {
                    String url = baseUrl + "api/addUser";

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
                        msg.obj=resMsg;

                    }else  if ("fail".equals(resState)){
                        Log.d("msg",resMsg);
                        msg.what=0;
                        msg.obj=resMsg;
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

    private void cleanEditText() {
        username_registe.setText("");
        password_registe.setText("");
    }

    private void putUserInfo(String username, String password) {
        sp=this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        sp.edit()
                .putString("username",username)
                .putString("password",password)
                .putBoolean("isCheck",false)
                .putBoolean("auto_ischeck",false)
                .commit();
    }

}