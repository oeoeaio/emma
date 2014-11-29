package fileManagement;

public class DataPoint {

	Long dateTime;
	Double value;
	
	public DataPoint(Long dateTime,Double value){
		this.dateTime = dateTime;
		this.value = value;
	}
	
	public Double getValue(){
		return this.value;
	}
	
	public long getDateTime(){
		return this.dateTime;
	}
}
