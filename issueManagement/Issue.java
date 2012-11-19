package issueManagement;


public class Issue {
	
	String issueID;
	long startDate = 0;
	long endDate = 0;
	String siteID;
	String sourceID;
	String siteName;
	String sourceName;
	String issueType;
	String urgency;
	String notes;
	
	Issue(String issueID,long startDate,long endDate,String siteID, String sourceID, String siteName, String sourceName, String issueType, String urgency, String notes){
		this.issueID = issueID;
		this.startDate = startDate;
		this.endDate = endDate;
		this.siteID = (siteID==null?"":siteID);
		this.sourceID = (sourceID==null?"":sourceID);
		this.siteName = (siteName==null?"":siteName);
		this.sourceName = (sourceName==null?"":sourceName);
		this.issueType = (issueType==null?"":issueType);
		this.urgency = (urgency==null?"":urgency);
		this.notes = (notes==null?"":notes);
	}
	
	
}
