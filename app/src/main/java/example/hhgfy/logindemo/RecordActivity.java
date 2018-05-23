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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import example.hhgfy.logindemo.Util.HttpUtil;
import example.hhgfy.logindemo.database.DbAdapter;
import example.hhgfy.logindemo.record.PathRecord;


public class RecordActivity extends Activity implements OnItemClickListener {

	private Button btn_logout;
	private TextView title;

	private SharedPreferences sp;
	private RecordAdapter mAdapter;
	private ListView mAllRecordListView;
	private DbAdapter mDataBaseHelper;
//	private List<PathRecord> mAllRecord = new ArrayList<PathRecord>();
	private List<PathRecord> recordByUser=new ArrayList<>();
	public static final String RECORD_ID = "record_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
		setContentView(R.layout.recordlist);

		mAllRecordListView = (ListView) findViewById(R.id.recordlist);
		btn_logout=findViewById(R.id.logout_btn);
		title=findViewById(R.id.title_center);

		mDataBaseHelper = new DbAdapter(this);
		mDataBaseHelper.open();
//		searchAllRecordFromDB(); //查询数据库


//		queryRecordByUsername(sp.getString("username",null));

		getRecordListData(sp.getString("username",null));

		for (PathRecord record:recordByUser){
			System.out.println(record.toString());
		}

//		mAdapter = new RecordAdapter(this, recordByUser);
//		mAllRecordListView.setAdapter(mAdapter);
//		mAllRecordListView.setOnItemClickListener(this);
//

		btn_logout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sp.edit().clear().commit();
				Log.d("logout","退出登录");
				Intent intent = new Intent(RecordActivity.this, LoginActivity.class);
				startActivity(intent);
				finish();//防止 按返回键回该页面
			}
		});

		String username= sp.getString("username","nobody");
		title.setText(username+" 的个人中心");


	}

	private void getRecordListData(final String username) {

		final String baseUrl = "http://120.25.224.151:5001/";

		final Handler handler=new Handler(){
			public void  handleMessage(Message msg){
				String responseStr= (String) msg.obj;
				try {
					JSONObject response = new JSONObject(responseStr);
					String resState =response.getString("code");
					String resMsg=response.getString("msg");
					String resData=response.getString("data");

					System.out.println("code:"+ resState);
					System.out.println("msg:"+resMsg);
					System.out.println("data:"+resData);

					if ("0".equals(resState)) {
						System.out.println("响应正常");


						JSONArray array = new JSONArray(resData);
						List<PathRecord> allRecord = new ArrayList<PathRecord>();

						for (int i = 0; i < array.length(); i++) {
							JSONObject item = array.getJSONObject(i);
							PathRecord record = new PathRecord();
							if (item != null) {
								record.setId(item.getInt("id"));
								record.setDistance(item.getString("distance"));
								record.setDuration(item.getString("duration"));
								record.setDate(item.getString("date"));
								record.setAveragespeed(item.getString("averageSpeed"));

								record.setUsername(null);
								record.setPathline(null);
								record.setStartpoint(null);
								record.setEndpoint(null);

								allRecord.add(record);
							}
						}
						System.out.println("list 赋值");
						recordByUser=allRecord;
						mAdapter = new RecordAdapter(RecordActivity.this, recordByUser);
						mAllRecordListView.setAdapter(mAdapter);
						mAllRecordListView.setOnItemClickListener(RecordActivity.this);

					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}



		};


		new Thread(new Runnable() {
			@Override
			public void run() {
				Message msg=new Message();

				try {
					String url = baseUrl + "webapi/recordByUser/"+username+"?page=1&limit=50";
					HttpUtil httpUtil = new HttpUtil();
					String responseData=httpUtil.get(url);
					msg.obj=responseData;//响应发给handler处理

				} catch (Exception e) {
					e.printStackTrace();
				};
				handler.sendMessage(msg);
			}
		}).start();


	}

//	private void searchAllRecordFromDB() {
//		mAllRecord = mDataBaseHelper.queryRecordAll();
//	}

	private void queryRecordByUsername(String username){
		recordByUser=mDataBaseHelper.queryRecordByUsername(username);
	}

	public void onBackClick(View view) {
		this.finish();
	}


	 // item 点击事件,跳转至轨迹回放界面，并携带轨迹ID参数
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		System.out.println("点击了 item "+position);

		//getItem返回的是 PathRecord
		PathRecord recorditem = (PathRecord) parent.getAdapter().getItem(position);
		Intent intent = new Intent(RecordActivity.this, RecordRePlayActivity.class);
		intent.putExtra(RECORD_ID, recorditem.getId());
		startActivity(intent);
	}
}
