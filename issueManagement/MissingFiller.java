package issueManagement;

import java.sql.Connection;

public class MissingFiller {
	
	private final Connection dbConn;
	
	MissingFiller(Connection dbConn){
		this.dbConn = dbConn;
		System.out.println(this.dbConn.toString());
		
	}
	

}
