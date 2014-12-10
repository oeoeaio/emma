package fileManagement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import endUseWindow.LogWindow;
import endUseWindow.Site;


public class SiteNotFoundWindow extends JFrame implements ActionListener {
	static final long serialVersionUID = 474802667938989652L;

	//Main Panel
	JPanel mainPanel = new JPanel(new BorderLayout());
	//Top Panel
	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel topLabel = new JLabel("");
	//Middle Panel
	JPanel middlePanel = new JPanel(new GridLayout(1,3));
	//Option 1 Panel
	JPanel o1Panel = new JPanel(new BorderLayout());
	JPanel o1InfoPanel = new JPanel(new BorderLayout());
	JPanel o1InputPanel = new JPanel(new GridLayout(0,2));
	JLabel o1SiteNameL = new JLabel("Site");
	JLabel o1ConcentratorL = new JLabel("Concentrator:");
	JLabel o1StartDateL = new JLabel("Start Date:");
	JLabel o1EndDateL = new JLabel("End Date:");
	JLabel o1GivenNameL = new JLabel("Given Name:");
	JLabel o1SurnameL = new JLabel("Surname:");
	JLabel o1SuburbL = new JLabel("Town/Suburb");
	JLabel o1StateL = new JLabel("State");
	JComboBox<String> o1SiteName = new JComboBox<String>();
	JTextField o1Concentrator = new JTextField(10);
	JTextField o1StartDate = new JTextField(10);
	JTextField o1EndDate = new JTextField(10);
	JTextField o1GivenName = new JTextField(10);
	JTextField o1Surname = new JTextField(10);
	JTextField o1Suburb = new JTextField(10);
	JTextField o1State = new JTextField(10);
	JRadioButton o1Button = new JRadioButton("<html>Select an existing<br>site from a list<br>(below).</html>");
	//Option 2 Panel
	JPanel o2Panel = new JPanel(new BorderLayout());
	JPanel o2InfoPanel = new JPanel(new BorderLayout());
	JPanel o2InputPanel = new JPanel(new GridLayout(0,2));
	JLabel o2SiteNameL = new JLabel("Site Name");
	JLabel o2ConcentratorL = new JLabel("Concentrator:");
	JLabel o2StartDateL = new JLabel("Start Date:");
	JLabel o2EndDateL = new JLabel("End Date:");
	JLabel o2GivenNameL = new JLabel("Given Name");
	JLabel o2SurnameL = new JLabel("Surname:");
	JLabel o2SuburbL = new JLabel("Town/Suburb");
	JLabel o2StateL = new JLabel("State");
	JTextField o2SiteName = new JTextField(10);
	JTextField o2Concentrator = new JTextField(10);
	JTextField o2StartDate = new JTextField(10);
	JTextField o2EndDate = new JTextField(10);
	JTextField o2GivenName = new JTextField(10);
	JTextField o2Surname = new JTextField(10);
	JTextField o2Suburb = new JTextField(10);
	JTextField o2State = new JTextField(10);
	JRadioButton o2Button = new JRadioButton("<html>Create a new Site<br>using the details below.</html>");
	//Option 3 Panel
	JPanel o3Panel = new JPanel(new BorderLayout());
	JRadioButton o3Button = new JRadioButton("<html>Do nothing and cease<br>processing of this file<br>for the time being.</html>");
	ButtonGroup options = new ButtonGroup();

	//Bottom Panel
	JPanel bottomPanel = new JPanel(new FlowLayout());
	JButton submitButton = new JButton("Submit");

	Site newSite = null;
	ArrayList<Site> siteList = new ArrayList<Site>();

	Connection dbConn;
	LogWindow logWindow;

	SiteNotFoundWindow(Connection dbConn,LogWindow logWindow){
		this.dbConn = dbConn;
		this.logWindow = logWindow;
	}

	void buildGUI(String siteName){
		this.setVisible(false);
		this.setSize(600,300);
		this.setLocation(300,200);
		this.setTitle("Site Not Found");

		mainPanel.add(topPanel,BorderLayout.NORTH);
		mainPanel.add(middlePanel,BorderLayout.CENTER);
		mainPanel.add(bottomPanel,BorderLayout.SOUTH);

		topPanel.add(topLabel);
		topLabel.setText("The site name '"+siteName+"' could not be matched. Please select an option to resolve this problem.");

		middlePanel.add(o1Panel);
		middlePanel.add(o2Panel);
		middlePanel.add(o3Panel);

		o1Panel.setBorder(BorderFactory.createMatteBorder(1,1,1,0,Color.black));
		o2Panel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
		o3Panel.setBorder(BorderFactory.createMatteBorder(1,0,1,1,Color.black));

		options.add(o1Button);
		options.add(o2Button);
		options.add(o3Button);
		o1Button.addActionListener(this);
		o2Button.addActionListener(this);
		o3Button.addActionListener(this);
		o3Button.setSelected(true);

		o1Panel.add(o1Button,BorderLayout.NORTH);
		o1Panel.add(o1InfoPanel,BorderLayout.CENTER);

		o1InfoPanel.setBorder(new EmptyBorder(0,5,0,0));
		o1InfoPanel.add(new JLabel("<html><b>Site from File:</b></html>"));
		o1InfoPanel.add(o1InputPanel);

		o1InputPanel.add(o1SiteNameL);
		o1InputPanel.add(o1SiteName);
		o1InputPanel.add(o1ConcentratorL);
		o1InputPanel.add(o1Concentrator);
		o1InputPanel.add(o1StartDateL);
		o1InputPanel.add(o1StartDate);
		o1InputPanel.add(o1EndDateL);
		o1InputPanel.add(o1EndDate);
		o1InputPanel.add(o1GivenNameL);
		o1InputPanel.add(o1GivenName);
		o1InputPanel.add(o1SurnameL);
		o1InputPanel.add(o1Surname);
		o1InputPanel.add(o1SuburbL);
		o1InputPanel.add(o1Suburb);
		o1InputPanel.add(o1StateL);
		o1InputPanel.add(o1State);

		try{ //get available sites
			ResultSet allSites = dbConn.createStatement().executeQuery("SELECT * FROM sites");
			if (allSites.next()){
				allSites.beforeFirst();
				while(allSites.next()){
					siteList.add(new Site(allSites.getString("site_id"),allSites.getString("site_name"),allSites.getString("concentrator"),allSites.getString("start_date"),allSites.getString("end_date"),allSites.getString("given_name"),allSites.getString("surname"),allSites.getString("suburb"),allSites.getString("state")));
					o1SiteName.addItem(allSites.getString("site_id")+": "+allSites.getString("site_name"));
				}
			}
			else{
				o1Button.setEnabled(false);
				o1SiteName.setEnabled(false);
				o1SiteName.addItem("No Sites Found");
			}
		}catch(SQLException sE){
			sE.printStackTrace();
			o1Button.setEnabled(false);
			o1SiteName.setEnabled(false);
			o1SiteName.addItem("No Sites Found");
		}
		o1Concentrator.setEnabled(false);
		o1StartDate.setEnabled(false);
		o1EndDate.setEnabled(false);
		o1GivenName.setEnabled(false);
		o1Surname.setEnabled(false);
		o1Suburb.setEnabled(false);
		o1State.setEnabled(false);
		o1SiteName.addActionListener(this);
		o1SiteName.setSelectedIndex(0);

		o2Panel.add(o2Button,BorderLayout.NORTH);
		o2Panel.add(o2InfoPanel,BorderLayout.CENTER);

		o2InfoPanel.setBorder(new EmptyBorder(0,5,0,0));
		o2InfoPanel.add(new JLabel("<html><b>New Site:</b></html>"));
		o2InfoPanel.add(o2InputPanel);

		o2InputPanel.add(o2SiteNameL);
		o2InputPanel.add(o2SiteName);
		o2InputPanel.add(o2ConcentratorL);
		o2InputPanel.add(o2Concentrator);
		o2InputPanel.add(o2StartDateL);
		o2InputPanel.add(o2StartDate);
		o2InputPanel.add(o2EndDateL);
		o2InputPanel.add(o2EndDate);
		o2InputPanel.add(o2GivenNameL);
		o2InputPanel.add(o2GivenName);
		o2InputPanel.add(o2SurnameL);
		o2InputPanel.add(o2Surname);
		o2InputPanel.add(o2SuburbL);
		o2InputPanel.add(o2Suburb);
		o2InputPanel.add(o2StateL);
		o2InputPanel.add(o2State);

		o3Panel.add(o3Button, BorderLayout.NORTH);

		bottomPanel.add(submitButton);
		submitButton.addActionListener(this);

		getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.validate();
		this.setVisible(true);
		this.toFront();
		this.requestFocus();
	}

	Site getNewSite(final String siteName){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI(siteName);
			}
		});

		try {
			synchronized (submitButton) {
				submitButton.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.setVisible(false);
		this.dispose();

		return newSite;
	}


	@Override
	public void actionPerformed(ActionEvent aE) {
		if(aE.getSource().equals(o1SiteName) && siteList.size()>0){
			Site selectedSite = siteList.get(o1SiteName.getSelectedIndex());
			o1Concentrator.setText(selectedSite.getConcentrator());
			o1StartDate.setText(selectedSite.getStartDate());
			o1EndDate.setText(selectedSite.getEndDate());
			o1GivenName.setText(selectedSite.getGivenName());
			o1Surname.setText(selectedSite.getSurname());
			o1Suburb.setText(selectedSite.getSuburb());
			o1State.setText(selectedSite.getState());
		}
		else if (aE.getSource().equals(submitButton)){
			if (o1Button.isSelected()){
				Site testSite = siteList.get(o1SiteName.getSelectedIndex());
				if (testSite.isValid()){
					newSite = testSite;
					synchronized (submitButton) {
						submitButton.notify();
					}
				}
				else{
					JOptionPane.showMessageDialog(this, "Error: site information provided is invalid.");
					newSite = null;
				}
			} else if(o2Button.isSelected()){
				Site testSite = new Site(null,o2SiteName.getText(),o2Concentrator.getText(),o2StartDate.getText(),o2EndDate.getText(),o2GivenName.getText(),o2Surname.getText(),o2Suburb.getText(),o2State.getText());
				if (testSite.isValid()){
					try{
						String addNewSiteSQL = "INSERT INTO sites (site_name,concentrator,start_date,end_date,given_name,surname,suburb,state) VALUES("+(testSite.getSiteName().equals("")?"NULL":"'"+testSite.getSiteName()+"'")+","+(testSite.getConcentrator().equals("")?"NULL":"'"+testSite.getConcentrator()+"'")+","+(testSite.getStartDate().equals("")?"NULL":"'"+testSite.getStartDate()+"'")+","+(testSite.getEndDate().equals("")?"NULL":"'"+testSite.getEndDate()+"'")+","+(testSite.getGivenName().equals("")?"NULL":"'"+testSite.getGivenName()+"'")+","+(testSite.getSurname().equals("")?"NULL":"'"+testSite.getSurname()+"'")+","+(testSite.getSuburb().equals("")?"NULL":"'"+testSite.getSuburb()+"'")+","+(testSite.getState().equals("")?"NULL":"'"+testSite.getState()+"'")+")";
						System.out.println(addNewSiteSQL);
						dbConn.createStatement().executeUpdate(addNewSiteSQL);
						logWindow.println("Added new site with name '"+testSite.getSiteName()+"'.");
						ResultSet new_source_id = dbConn.createStatement().executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
						new_source_id.next();
						testSite.setSiteID(new_source_id.getString("current_id"));
						newSite = testSite;
						synchronized (submitButton) {
							submitButton.notify();
						}
					}catch(SQLException sE){
						sE.printStackTrace();
						logWindow.println("Failed to add new site with name '"+testSite.getSiteName()+"'.\r\nProcessing of this file will be terminated for the time being.");
						newSite = null;
					}
				}
				else{
					JOptionPane.showMessageDialog(this, "Error: site information provided is invalid.");
					newSite = null;
				}
			} else if(o3Button.isSelected()){
				newSite = null;
				synchronized (submitButton) {
					submitButton.notify();
				}
			}
        }
	}
}