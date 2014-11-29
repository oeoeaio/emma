package endUseWindow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class MySQLConnection extends JFrame implements ActionListener{
	private static final long serialVersionUID = -789947022143855605L;
	
	
	//Panels
	JPanel mainPanel = new JPanel(new BorderLayout());
	JPanel topPanel = new JPanel(new BorderLayout());
	JPanel addressPanel = new JPanel(new BorderLayout());
	JPanel dbNamePanel = new JPanel(new BorderLayout());
	JPanel userPassPanel = new JPanel(new BorderLayout());
	JPanel bottomPanel = new JPanel(new BorderLayout());
	
	//Labels
	JLabel topLabel = new JLabel("Please provide the address of the MySQL database.");
	JLabel addressLabel = new JLabel("IP Address:");
	JLabel portLabel = new JLabel("Port:");
	JLabel dbNameLabel = new JLabel("Database Name:");
	JLabel userLabel = new JLabel("Username:");
	JLabel passLabel = new JLabel("Password:");
	
	
	//Inputs 
	JTextField addressInput = new JTextField(15);
	JTextField portInput = new JTextField(4);
	JTextField dbNameInput = new JTextField(15);
	JTextField userInput = new JTextField(15);
	JPasswordField passInput = new JPasswordField(15);
	
	//Variables
	public String ipAddress = "";
	public String portNumber = "";
	public String dbName = "";
	public String userName = "";
	public char[] pWord = new char[0];
	
	//Buttons
	JButton submitButton = new JButton("Submit");
	JButton cancelButton = new JButton("Cancel");
	
	private Preferences mySQLSettings = Preferences.userRoot().node("EndUseMySQLSettings");
	
	EndUseWindow managementWindow;
	
	boolean continueOK = false;
	
	public Connection dbConn = null;
	
	void buildGUI(){
		this.setSize(600,200);
		this.setLocation(300,200);
		this.setTitle("Please provide the address of the MySQL database.");
		this.setVisible(true);

		mainPanel.setLayout(new GridLayout(5,1));
		topPanel.setLayout(new FlowLayout());
		addressPanel.setLayout(new FlowLayout());
		dbNamePanel.setLayout(new FlowLayout());
		userPassPanel.setLayout(new FlowLayout());
		bottomPanel.setLayout(new FlowLayout());


		mainPanel.add(topPanel);
		mainPanel.add(addressPanel);
		mainPanel.add(dbNamePanel);
		mainPanel.add(userPassPanel);
		mainPanel.add(bottomPanel);

		topPanel.add(topLabel);
		addressPanel.add(addressLabel);
		addressPanel.add(addressInput);
		addressPanel.add(portLabel);
		addressPanel.add(portInput);
		dbNamePanel.add(dbNameLabel);
		dbNamePanel.add(dbNameInput);
		userPassPanel.add(userLabel);
		userPassPanel.add(userInput);
		userPassPanel.add(passLabel);
		userPassPanel.add(passInput);
		bottomPanel.add(submitButton);
		bottomPanel.add(cancelButton);

		submitButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		addressInput.setText(mySQLSettings.get("ipAddress", "localhost"));
		portInput.setText(mySQLSettings.get("portNumber", "3306"));
		dbNameInput.setText(mySQLSettings.get("dbName", "enduse"));
		userInput.setText(mySQLSettings.get("userName", ""));

		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.validate();
	}
	
	Connection retrieveDB(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
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
		return dbConn;
	}
	
	public Connection getCopyConnection(){
		return getConnection(ipAddress,portNumber,dbName,userName,pWord);
	}
	
	public static Connection getConnection(String address,String port,String dbName,String user,char[] pass) {
		String concatPass = "";
		for (int i=0;i<pass.length;i++){
			concatPass = concatPass + pass[i];
		}
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Connection c = DriverManager.getConnection("jdbc:mysql://"+address+":"+port+"/"+dbName,user,concatPass);
			return c;
		} catch (SQLException sE){
			sE.printStackTrace();
			System.out.println(sE.getErrorCode());
			if (sE.getErrorCode()==1045){
				JOptionPane.showMessageDialog(null,"Username or password combination is incorrect for the specified database.","Login Error",JOptionPane.ERROR_MESSAGE);
			}
			else if (sE.getErrorCode()==1049){
				JOptionPane.showMessageDialog(null,"No database with the name provided could be found.","Database Error",JOptionPane.ERROR_MESSAGE);
			}
			else{ 
				JOptionPane.showMessageDialog(null,"The database could not be found at the address provided.","Input Error",JOptionPane.ERROR_MESSAGE);
			}
			return null;
		} catch (IllegalAccessException iAE){
			JOptionPane.showMessageDialog(null,"An error occured when connecting to the database.","Permission Error",JOptionPane.ERROR_MESSAGE);
			return null;
		} catch (ClassNotFoundException cNFE){
			JOptionPane.showMessageDialog(null,"Drivers for connecting to MySQL database not found.\r\nPlease download and install Connector/J from:\r\nhttp://dev.mysql.com/downloads/connector/j/","Missing Drivers",JOptionPane.ERROR_MESSAGE);
			return null;
		} catch (InstantiationException iE){ //should not happen
			JOptionPane.showMessageDialog(null,"An error occured when connecting to the database.","Error",JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent aE){
		if (aE.getSource().equals(submitButton)){
			if (addressInput.getText().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") || addressInput.getText().equals("localhost")){
				if (portInput.getText().matches("\\d{4}")){
					dbConn = getConnection(addressInput.getText(),portInput.getText(),dbNameInput.getText(),userInput.getText(),passInput.getPassword());
					if (dbConn!=null){
						synchronized (submitButton) {
							mySQLSettings.put("ipAddress", addressInput.getText());
							mySQLSettings.put("portNumber",portInput.getText());
							mySQLSettings.put("dbName",dbNameInput.getText());
							mySQLSettings.put("userName",userInput.getText());
							this.ipAddress = addressInput.getText();
							this.portNumber = portInput.getText();
							this.dbName = dbNameInput.getText();
							this.userName = userInput.getText();
							this.pWord = passInput.getPassword();
							submitButton.notify();
						}
					}
				}
				else {
					JOptionPane.showMessageDialog(this,"Invalid Port Number. Must be of the form xxxx","Invalid Port Number",JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				JOptionPane.showMessageDialog(this,"Invalid IP address. Must be of the form xxx.xxx.xxx.xxx","Invalid IP Address",JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (aE.getSource().equals(cancelButton)){
			synchronized (submitButton) {
				submitButton.notify();
			}
		}
	}
}