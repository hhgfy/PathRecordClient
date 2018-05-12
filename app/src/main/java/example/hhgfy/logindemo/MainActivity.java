package example.hhgfy.logindemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import example.hhgfy.logindemo.Util.HttpUtil;
import example.hhgfy.logindemo.record.PathRecord;
import example.hhgfy.logindemo.database.DbAdapter;
import okhttp3.FormBody;
//import amap.com.recorduitl.Util;


public class MainActivity extends Activity implements LocationSource, AMapLocationListener {//TraceListener
	private final static int CALLTRACE = 0;
	private MapView mMapView;
	private AMap mAMap; //地图控制器对象
	private OnLocationChangedListener mListener;
	private AMapLocationClient mLocationClient; //定位服务
	private AMapLocationClientOption mLocationOption; //定位参数设置
	private PolylineOptions mPolyoptions; //折线的选项 颜色,宽度等
	private Polyline mpolyline;  //折线对象
	private PathRecord record; //用于存储 轨迹信息 的对象
	private long mStartTime; //开始时间
	private long mEndTime;
	private ToggleButton btn; //开始/停止 按钮
	private DbAdapter DbHepler; //数据库操作
	private TextView mResultShow; //左上角显示路程
	private SharedPreferences sp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("onCreate");
		super.onCreate(savedInstanceState);
		sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);

		//绑定视图
		setContentView(R.layout.basicmap_activity);
		mMapView = (MapView) findViewById(R.id.map);
		mMapView.onCreate(savedInstanceState);// 此方法必须重写
		//初始化AMap 和 折线
		init();
		initpolyline();
	}

	/**
	 * 初始化AMap对象
	 */
	private void init() {
		if (mAMap == null) {
			mAMap = mMapView.getMap();
			setUpMap();
		}
		btn = (ToggleButton) findViewById(R.id.locationbtn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("onClick");
				//按下按钮 ,重置地图, 清空上一次残留
				//开始记录
				if (btn.isChecked()) {
					mAMap.clear(true); //删除myLocationOverlay以外的覆盖物
					if (record != null) {
						record = null;
					}
					record = new PathRecord();
					mStartTime = System.currentTimeMillis();
					record.setDate(getcueDate(mStartTime));
					mResultShow.setText("总路程");
				}
				//结束记录
				else {
					mEndTime = System.currentTimeMillis();
					DecimalFormat decimalFormat = new DecimalFormat("0.0");
					mResultShow.setText(
							decimalFormat.format( getDistance(record.getPathline()) ) + "米");
					//保存结果到数据库
					saveRecord(record.getPathline(), record.getDate());
				}
			}
		});
		mResultShow = (TextView) findViewById(R.id.show_all_dis);


	}

	protected void saveRecord(List<AMapLocation> list, String time) {
		if (list != null && list.size() > 0) {
			DbHepler = new DbAdapter(this);
			DbHepler.open();
			String duration = getDuration();
			float distance = getDistance(list);
			String average = getAverage(distance);
			String pathlineSring = getPathLineString(list);
			AMapLocation firstLocaiton = list.get(0);
			AMapLocation lastLocaiton = list.get(list.size() - 1);
			String stratpoint = amapLocationToString(firstLocaiton);
			String endpoint = amapLocationToString(lastLocaiton);
			String username=sp.getString("username",null);

			//插入一条路径记录
			DbHepler.createrecord(username, String.valueOf(distance), duration, average,
					pathlineSring, stratpoint, endpoint, time);
			DbHepler.close();

			postData(username, String.valueOf(distance), duration, average,
					pathlineSring, stratpoint, endpoint, time);

			Toast.makeText(MainActivity.this, "记录到路径,长度为: " +(int)distance +" 米", Toast.LENGTH_LONG)
					.show();
		} else {
			Toast.makeText(MainActivity.this, "没有记录到路径", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void postData(final String username, final String distance, final String duration,
						  final String averagespeed, final String pathline, final String startpoint,
						  final String endpoint, final String date) {

		final String baseUrl = "http://120.25.224.151:5001/";

		new Thread(new Runnable() {
			@Override
			public void run() {
				Message msg=new Message();

				try {

					String url = baseUrl + "api/postData";

					FormBody formBody = new FormBody.Builder()
							.add("username", username)
							.add("distance",distance)
							.add("duration",duration)
							.add("averagespeed",averagespeed)
							.add("pathline",pathline)
							.add("startpoint",startpoint)
							.add("endpoint",endpoint)
							.add("date",date)
							.build();

					HttpUtil httpUtil = new HttpUtil();
					String responseData = httpUtil.post(url, formBody);
					Log.d("responseData","------------------\n\n"+responseData);

					if ("1".equals(responseData)){
						Toast.makeText(MainActivity.this, "上传路径成功", Toast.LENGTH_SHORT).show();
					}else  if ("0".equals(responseData)){
						Toast.makeText(MainActivity.this, "上传路径失败", Toast.LENGTH_SHORT).show();
					}

				} catch (Exception e) {
					e.printStackTrace();
				};
			}
		}).start();
	}

	//	获取这段路程的总时间 单位:秒
	private String getDuration() {
		return String.valueOf((mEndTime - mStartTime) / 1000f);
	}

	//	获取平均速度
	private String getAverage(float distance) {
		return String.valueOf(distance / (float) (mEndTime - mStartTime));
	}

	//获取路程 ,根据每个相邻两点间距相加得出
	private float getDistance(List<AMapLocation> list) {
		float distance = 0;

		if (list == null || list.size() == 0) {
			return distance;
		}
		for (int i = 0; i < list.size() - 1; i++) {
			AMapLocation firstpoint = list.get(i);
			AMapLocation secondpoint = list.get(i + 1);
			LatLng firstLatLng = new LatLng(firstpoint.getLatitude(),
					firstpoint.getLongitude());
			LatLng secondLatLng = new LatLng(secondpoint.getLatitude(),
					secondpoint.getLongitude());
			double betweenDis = AMapUtils.calculateLineDistance(firstLatLng,
					secondLatLng);
			distance = (float) (distance + betweenDis);
		}
		return distance;
	}

	//拼接所有 中间点 到一个字符串
	private String getPathLineString(List<AMapLocation> list) {
		if (list == null || list.size() == 0) {
			return "";
		}
		StringBuffer pathline = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			AMapLocation location = list.get(i);
			String locString = amapLocationToString(location);
			pathline.append(locString).append(";");
		}
		String pathLineString = pathline.toString();
		pathLineString = pathLineString.substring(0,
				pathLineString.length() - 1);
		System.out.println("pathLineString :  /////"+pathLineString);
		return pathLineString;
	}


	private String amapLocationToString(AMapLocation location) {
		StringBuffer locString = new StringBuffer();
		locString.append(location.getLatitude()).append(",");
		locString.append(location.getLongitude()).append(",");
		locString.append(location.getProvider()).append(",");
		locString.append(location.getTime()).append(",");
		locString.append(location.getSpeed()).append(",");
		locString.append(location.getBearing());
		return locString.toString();
	}

	/**
	 * 初始化折线
	 */
	private void initpolyline() {
		mPolyoptions = new PolylineOptions();
		mPolyoptions.width(10f);
		mPolyoptions.color(Color.BLUE);

	}

	/**
	 * 初始化 设置一些amap的属性
	 */
	private void setUpMap() {
		mAMap.setLocationSource(this);// 设置定位监听
		mAMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
		mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		// 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种

		MyLocationStyle myLocationStyle=new MyLocationStyle();
		myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
		myLocationStyle.interval(2000);
		//去掉精度圆圈
		myLocationStyle.radiusFillColor(Color.TRANSPARENT);//设置精度圆圈透明
		myLocationStyle.strokeWidth(0);

		mAMap.setMyLocationStyle(myLocationStyle);
		mAMap.setMyLocationEnabled(true);

		mAMap.moveCamera(new CameraUpdateFactory().zoomTo(17)); //设置默认缩放级别为17 ,范围 3~19 ,19最精细
		System.out.println("setMap");
	}


	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}


	@Override
	protected void onPause() {
		super.onPause();
		mMapView.onPause();
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mMapView.onSaveInstanceState(outState);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		startlocation();
	}

	@Override
	public void deactivate() {
		mListener = null;
		if (mLocationClient != null) {
			mLocationClient.stopLocation();
			mLocationClient.onDestroy();

		}
		mLocationClient = null;
	}

	/**
	 * 定位结果回调 ,2秒一次
	 * @param amapLocation 位置信息类
     */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null && amapLocation.getErrorCode() == 0) {
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
				LatLng mylocation = new LatLng(amapLocation.getLatitude(),
						amapLocation.getLongitude());
				mAMap.moveCamera(CameraUpdateFactory.changeLatLng(mylocation));
				if (btn.isChecked()) {
					record.addpoint(amapLocation);
					mPolyoptions.add(mylocation);

					redrawline();// 画线
				}
			} else {
				String errText = "定位失败," + amapLocation.getErrorCode() + ": "
						+ amapLocation.getErrorInfo();
				Log.e("AMap Error", errText);
			}
		}
	}

	/**
	 * 开始定位。
	 */
	private void startlocation() {
		if (mLocationClient == null) {
			mLocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			// 设置定位监听
			mLocationClient.setLocationListener(this);
			// 设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			//2秒执行一次定位
			mLocationOption.setInterval(2000);

			// 设置定位参数
			mLocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mLocationClient.startLocation();

		}
	}

	/**
	 * 实时轨迹画线
	 */
	private void redrawline() {
		if (mPolyoptions.getPoints().size() > 1) {
			if (mpolyline != null) {
				mpolyline.setPoints(mPolyoptions.getPoints());
			} else {
				mpolyline = mAMap.addPolyline(mPolyoptions);
			}
		}

	}

	//格式化日期
	@SuppressLint("SimpleDateFormat")
	private String getcueDate(long time) {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd  HH:mm:ss ");
		Date curDate = new Date(time);
		String date = formatter.format(curDate);
		return date;
	}

	//跳转到 历史记录的activity
	public void record(View view) {
		Intent intent = new Intent(MainActivity.this, RecordActivity.class);
		startActivity(intent);
	}

	
}
