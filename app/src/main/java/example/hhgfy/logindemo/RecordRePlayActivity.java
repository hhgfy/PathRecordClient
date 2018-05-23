package example.hhgfy.logindemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapLoadedListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.trace.LBSTraceClient;
import com.amap.api.trace.TraceLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import example.hhgfy.logindemo.Util.HttpUtil;
import example.hhgfy.logindemo.database.DbAdapter;
import example.hhgfy.logindemo.record.PathRecord;
import example.hhgfy.logindemo.recorduitl.TraceRePlay;
import example.hhgfy.logindemo.recorduitl.Util;


/**
 * 实现轨迹回放、纠偏后轨迹回放
 */
public class RecordRePlayActivity extends Activity implements OnMapLoadedListener, OnClickListener {
    private final static int AMAP_LOADED = 2;

    private ToggleButton mDisplaybtn;

    private MapView mMapView;
    private AMap mAMap;
    private Marker mOriginStartMarker, mOriginEndMarker, mOriginRoleMarker;
    //    private Marker mGraspStartMarker, mGraspEndMarker, mGraspRoleMarker;
    private Polyline mOriginPolyline;  //mGraspPolyline;

    private int mRecordItemId; //前一个activity传来的 ID
    private List<LatLng> mOriginLatLngList;//经纬度坐标点集合
//    private List<LatLng> mGraspLatLngList;

    private boolean mOriginChecked = true;
    private ExecutorService mThreadPool;
    private TraceRePlay mRePlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorddisplay_activity);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        mDisplaybtn = (ToggleButton) findViewById(R.id.displaybtn);
        mDisplaybtn.setOnClickListener(this);


        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2 + 3;
        mThreadPool = Executors.newFixedThreadPool(threadPoolSize);

        //获取ID参数
        Intent recordIntent = getIntent();
        if (recordIntent != null) {
            //获取传来的 record  id
            mRecordItemId = recordIntent.getIntExtra(RecordActivity.RECORD_ID, -1);
        }

        initMap();
    }

    //初始化 地图
    private void initMap() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
            mAMap.setOnMapLoadedListener(this);
        }
    }


    /**
     * 将原始轨迹小人设置到起点
     */
    private void resetOriginRole() {
        if (mOriginLatLngList == null) {
            return;
        }
        LatLng startLatLng = mOriginLatLngList.get(0);
        if (mOriginRoleMarker != null) {
            mOriginRoleMarker.setPosition(startLatLng);
        }
    }


    private void startMove() {
        if (mRePlay != null) {
            mRePlay.stopTrace();
        }
        mRePlay = rePlayTrace(mOriginLatLngList, mOriginRoleMarker);

    }

    private void stopMove() {
        mRePlay.stopTrace();
    }

    /**
     * 轨迹回放方法
     */
    private TraceRePlay rePlayTrace(List<LatLng> list, final Marker updateMarker) {
        TraceRePlay replay = new TraceRePlay(list, 100,
                new TraceRePlay.TraceRePlayListener() {

                    @Override
                    public void onTraceUpdating(LatLng latLng) {
                        if (updateMarker != null) {
                            updateMarker.setPosition(latLng); // 更新小人实现轨迹回放
                        }
                    }

                    @Override
                    public void onTraceUpdateFinish() {
                        mDisplaybtn.setChecked(false);
                        mDisplaybtn.setClickable(true);
                    }
                });
        mThreadPool.execute(replay);
        return replay;
    }

    public void onBackClick(View view) {
        this.finish();
        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
        }
    }

    private LatLngBounds getBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        if (mOriginLatLngList == null) {
            return b.build();
        }
        for (int i = 0; i < mOriginLatLngList.size(); i++) {
            b.include(mOriginLatLngList.get(i));
        }
        return b.build();

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String responseStr= (String) msg.obj;
            try {
                JSONObject response = new JSONObject(responseStr);
                String resState =response.getString("state");
                String resMsg=response.getString("msg");
                JSONObject resData=response.getJSONObject("record");

                if ("success".equals(resState)) {
                    System.out.println(" 获取轨迹详情API 响应正常");

                    int id=resData.getInt("id"); //id
                    AMapLocation start=Util.parseLocation(resData.getString("start")); //起点
                    AMapLocation  end =Util.parseLocation(resData.getString("end"));  //终点
                    String pathlineStr=resData.getString("lines");
                    ArrayList<AMapLocation> pathline=Util.parseLocations(pathlineStr);  //中间点集合

                    if (msg.what==AMAP_LOADED){
                        setupRecord(start,end,pathline);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 轨迹数据初始化
     */
    private void setupRecord(AMapLocation startLoc,AMapLocation endLoc ,ArrayList<AMapLocation> pointList) {
        // 轨迹纠偏初始化
//        LBSTraceClient mTraceClient = new LBSTraceClient(getApplicationContext());

//        DbAdapter dbhelper = new DbAdapter(this.getApplicationContext());
//        dbhelper.open();
//        PathRecord mRecord = dbhelper.queryRecordById(mRecordItemId);
//        dbhelper.close();

        if (pointList == null || startLoc == null || endLoc == null) {
            System.out.println("获取数据异常，有坐标点为空 ！");
            return;
        }
        LatLng startLatLng = new LatLng(startLoc.getLatitude(), startLoc.getLongitude());
        LatLng endLatLng = new LatLng(endLoc.getLatitude(), endLoc.getLongitude());
        mOriginLatLngList = Util.parseLatLngList(pointList);
        //添加轨迹到地图上
        addOriginTrace(startLatLng, endLatLng, mOriginLatLngList);
    }

    //载入地图
    @Override
    public void onMapLoaded() {

        final String baseUrl = "http://120.25.224.151:5001/";
        new Thread(new Runnable() {
            @Override
            public void run() {
//                Message msg=new Message();
                Message msg = handler.obtainMessage();

                try {
                    String url = baseUrl + "api/getPathlineByID/"+mRecordItemId;
                    HttpUtil httpUtil = new HttpUtil();
                    String responseData=httpUtil.get(url);
                    msg.obj=responseData;
                    msg.what = AMAP_LOADED;
                } catch (Exception e) {
                    e.printStackTrace();
                };
                //将响应信息 发给handler处理
                handler.sendMessage(msg);
            }
        }).start();
    }

    //回放按钮点击事件
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.displaybtn) {
            //开始回放按钮被选中，进行回放
            if (mDisplaybtn.isChecked()) {
                Log.d("回放", "开始回放");
                startMove();
//                mDisplaybtn.setClickable(false);
            } else {
                Log.d("回放", "停止回放");
                stopMove();
            }

            //回放结束 ，开始回放按钮重置
//            mDisplaybtn.setChecked(false);
            //将小人放到起点
            resetOriginRole();
        }
    }


    /**
     * 地图上添加原始轨迹线路及起终点、轨迹动画小人
     *
     * @param startPoint
     * @param endPoint
     * @param originList
     */
    private void addOriginTrace(LatLng startPoint, LatLng endPoint, List<LatLng> originList) {
        mOriginPolyline = mAMap.addPolyline(new PolylineOptions().color(Color.BLUE).addAll(originList));
        mOriginStartMarker = mAMap.addMarker(new MarkerOptions().position(startPoint).icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
        mOriginEndMarker = mAMap.addMarker(new MarkerOptions().position(endPoint).icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));

        try {
            mAMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getBounds(), 50));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mOriginRoleMarker = mAMap.addMarker(new MarkerOptions().position(startPoint).icon(
                BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.walk))));
    }

    /**
     * 设置是否显示原始轨迹
     *
     * @param enable
     */
    private void setOriginEnable(boolean enable) {
        mDisplaybtn.setClickable(true);
        if (mOriginPolyline == null || mOriginStartMarker == null
                || mOriginEndMarker == null || mOriginRoleMarker == null) {
            return;
        }
        if (enable) {
            mOriginPolyline.setVisible(true);
            mOriginStartMarker.setVisible(true);
            mOriginEndMarker.setVisible(true);
            mOriginRoleMarker.setVisible(true);
        } else {
            mOriginPolyline.setVisible(false);
            mOriginStartMarker.setVisible(false);
            mOriginEndMarker.setVisible(false);
            mOriginRoleMarker.setVisible(false);
        }
    }
}

    /**
     * 地图上添加纠偏后轨迹线路及起终点、轨迹动画小人
     */
//    private void addGraspTrace(List<LatLng> graspList, boolean mGraspChecked) {
//        if (graspList == null || graspList.size() < 2) {
//            return;
//        }
//        LatLng startPoint = graspList.get(0);
//        LatLng endPoint = graspList.get(graspList.size() - 1);
//        mGraspPolyline = mAMap.addPolyline(new PolylineOptions()
//                .setCustomTexture(
//                        BitmapDescriptorFactory
//                                .fromResource(R.drawable.grasp_trace_line))
//                .width(40).addAll(graspList));
//        mGraspStartMarker = mAMap.addMarker(new MarkerOptions().position(
//                startPoint).icon(
//                BitmapDescriptorFactory.fromResource(R.drawable.start)));
//        mGraspEndMarker = mAMap.addMarker(new MarkerOptions()
//                .position(endPoint).icon(
//                        BitmapDescriptorFactory.fromResource(R.drawable.end)));
//        mGraspRoleMarker = mAMap.addMarker(new MarkerOptions().position(
//                startPoint).icon(
//                BitmapDescriptorFactory.fromBitmap(BitmapFactory
//                        .decodeResource(getResources(), R.drawable.walk))));
//        if (!mGraspChecked) {
//            mGraspPolyline.setVisible(false);
//            mGraspStartMarker.setVisible(false);
//            mGraspEndMarker.setVisible(false);
//            mGraspRoleMarker.setVisible(false);
//        }
//    }

    /**
     * 设置是否显示纠偏后轨迹
     *
     * @param enable
     */
//    private void setGraspEnable(boolean enable) {
//        mDisplaybtn.setClickable(true);
//        if (mGraspPolyline == null || mGraspStartMarker == null
//                || mGraspEndMarker == null || mGraspRoleMarker == null) {
//            return;
//        }
//        if (enable) {
//            mGraspPolyline.setVisible(true);
//            mGraspStartMarker.setVisible(true);
//            mGraspEndMarker.setVisible(true);
//            mGraspRoleMarker.setVisible(true);
//        } else {
//            mGraspPolyline.setVisible(false);
//            mGraspStartMarker.setVisible(false);
//            mGraspEndMarker.setVisible(false);
//            mGraspRoleMarker.setVisible(false);
//        }
//    }



//    /**
//     * 轨迹纠偏完成数据回调
//     */
//    @Override
//    public void onFinished(int arg0, List<LatLng> list, int arg2, int arg3) {
//        addGraspTrace(list, mGraspChecked);
//        mGraspLatLngList = list;
//    }
//
//    @Override
//    public void onRequestFailed(int arg0, String arg1) {
//        Toast.makeText(this.getApplicationContext(), "轨迹纠偏失败:" + arg1,
//                Toast.LENGTH_SHORT).show();
//
//    }
//
//    @Override
//    public void onTraceProcessing(int arg0, int arg1, List<LatLng> arg2) {
//
//    }


