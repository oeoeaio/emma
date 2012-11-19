package fileManagement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataFactorConverter {
	
	void applyFactor(DataFile dataFile,Double factor){
		for(int i=0;i<dataFile.dataList.size();i++){
			dataFile.dataList.get(i).value *= factor; 
		}
	}
	
	Double getConversionFactor(Statement MySQL_Statement,DataFile dataFile){
		try{
		
			String getTypeSQL = "SELECT source_type,measurement_type FROM sources WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID;
			ResultSet getTypeRS = MySQL_Statement.executeQuery(getTypeSQL);
			if (getTypeRS.next()){
				if (getTypeRS.getString("source_type").equals("Light") && getTypeRS.getString("measurement_type").equals("OnTime")){
					String getCFSQL = "SELECT wattage from lights WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID;
					ResultSet getCFRS = MySQL_Statement.executeQuery(getCFSQL);

					if (getCFRS.next()){
						double w = getCFRS.getDouble("wattage");
						return w/dataFile.frequency;
					}
					else{
						System.out.println("lala3");
						return null;
					}
				}
				else{
					System.out.println("lala2");
					return null;
				}
			}
			else{
				System.out.println("lala1");
				return null;
			}
		}catch(SQLException sE){
			sE.printStackTrace();
			return null;
		}
 
	}
}
