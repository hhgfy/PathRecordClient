package example.hhgfy.logindemo.record;

import com.amap.api.location.AMapLocation;

import java.util.ArrayList;
import java.util.List;

public class PathRecord {
	private AMapLocation mStartPoint;
	private AMapLocation mEndPoint;
	private List<AMapLocation> mPathLinePoints = new ArrayList<AMapLocation>();
	private String mDistance;
	private String mDuration;
	private String mAveragespeed;
	private String mDate;
	private String username;
	private int mId = 0;

	public PathRecord() {

	}

	public PathRecord(int id,ArrayList<AMapLocation> lines, AMapLocation start,AMapLocation end) {
		this.mId=id;
		this.mPathLinePoints=lines;
		this.mStartPoint=start;
		this.mEndPoint=end;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public AMapLocation getStartpoint() {
		return mStartPoint;
	}

	public void setStartpoint(AMapLocation startpoint) {
		this.mStartPoint = startpoint;
	}

	public AMapLocation getEndpoint() {
		return mEndPoint;
	}

	public void setEndpoint(AMapLocation endpoint) {
		this.mEndPoint = endpoint;
	}

	public List<AMapLocation> getPathline() {
		return mPathLinePoints;
	}

	public void setPathline(List<AMapLocation> pathline) {
		this.mPathLinePoints = pathline;
	}

	public String getDistance() {
		return mDistance;
	}

	public void setDistance(String distance) {
		this.mDistance = distance;
	}

	public String getDuration() {
		return mDuration;
	}

	public void setDuration(String duration) {
		this.mDuration = duration;
	}

	public String getAveragespeed() {
		return mAveragespeed;
	}

	public void setAveragespeed(String averagespeed) {
		this.mAveragespeed = averagespeed;
	}

	public String getDate() {
		return mDate;
	}

	public void setDate(String date) {
		this.mDate = date;
	}

	public void addpoint(AMapLocation point) {
		mPathLinePoints.add(point);
	}

	@Override
	public String toString() {
		System.out.println(getDistance());
		float distance= Float.parseFloat(getDistance());
		float duration= Float.parseFloat(getDuration());
		StringBuilder record = new StringBuilder();
//		record.append("记录点数:" + getPathline().size() + ", ");
		record.append("路程:" + (int)distance + "m, ");
		record.append("时长:" + (int)duration + "s");
		return record.toString();
	}
}
