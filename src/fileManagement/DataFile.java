package fileManagement;

import java.io.File;
import java.util.LinkedList;

public class DataFile {
	
	public LinkedList<DataPoint> dataList = new LinkedList<DataPoint>();
	
	String siteID = "";
	String sourceID = "";
	String fileID = "";
	String fileName = "";
	File file;
	String meterSerial = "";
	int frequency = 0;
	String sourceType = "";
	String measurementType = "";
	String startDate = "";
	String endDate = "";
	Double rangeMin = 0.0;
	Double rangeMax = 0.0;
	
	public void addRow(DataPoint newPoint){
		dataList.add(newPoint);
	}
}
