package example.hhgfy.logindemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by hhgfy on 2018/5/10.
 */

public class HomeActivity extends Activity {

    private Button btn_logout;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        setContentView(R.layout.home);

        btn_logout=findViewById(R.id.logout_btn);

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sp.edit().clear().commit();
                Toast.makeText(HomeActivity.this,"验证失败",Toast.LENGTH_SHORT).show();
                Log.d("logout","退出登录");
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }


}
