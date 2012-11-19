package endUseWindow;

import javax.swing.JOptionPane;

public class Site {
	String siteID;
	String siteName;
	String givenName;
	String surname;
	String suburb;
	String state;
	
	public Site(String siteID,String siteName,String givenName,String surname,String suburb,String state){
		this.siteID = (siteID==null?"":siteID);
		this.siteName = (siteName==null?"":siteName);
		this.givenName = (givenName==null?"":givenName);
		this.surname = (surname==null?"":surname);
		this.suburb = (suburb==null?"":suburb);
		this.state = (state==null?"":state);
	}
	
	public boolean equalTo(Site otherSite){
		//NOTE: IGNORES siteIDs. Only tests whether user added information is equal.
		if (this.siteName.equals(otherSite.siteName)
				&& this.givenName.equals(otherSite.givenName)
				&& this.surname.equals(otherSite.surname)
				&& this.suburb.equals(otherSite.suburb)
				&& this.state.equals(otherSite.state)){
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isValid(){
		boolean isValid = false;
		//NOTE: IGNORES siteIDs. Only tests whether user added information is valid.
		if (siteName.matches("^[\\w\\s]{1,10}$")){
			if (givenName.matches("^[\\w\\s]{0,30}$")){
				if (surname.matches("^[\\w\\s]{0,30}$")){
					if (suburb.matches("^[\\w\\s]{0,30}$")){
						if (state.matches("^VIC|TAS|ACT|NSW|QLD|NT|SA|WA$") || state.equals("")){
							isValid = true;
						}
						else{
							JOptionPane.showMessageDialog(null,"The State provided is invalid.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
						}
					}	
					else{
						JOptionPane.showMessageDialog(null,"The Suburb provided is invalid.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Surname provided is invalid.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The Given Name provided is invalid.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Site Name provided is invalid.","Site Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		
		return isValid;
	}
	
	public void setSiteID(String siteID){
		this.siteID = siteID;
	}
	
	public String getSiteID(){
		return siteID;
	}
	
	public String getSiteName(){
		return siteName;
	}
	
	public String getGivenName(){
		return givenName;
	}
	
	public String getSurname(){
		return surname;
	}
	
	public String getSuburb(){
		return suburb;
	}
	
	public String getState(){
		return state;
	}
}
