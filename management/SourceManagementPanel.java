package management;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.ApplianceEditPanel;
import endUseWindow.ApplianceTable;
import endUseWindow.CircuitEditPanel;
import endUseWindow.CircuitTable;
import endUseWindow.GPOEditPanel;
import endUseWindow.GPOTable;
import endUseWindow.GasEditPanel;
import endUseWindow.GasTable;
import endUseWindow.HumidityEditPanel;
import endUseWindow.HumidityTable;
import endUseWindow.LightEditPanel;
import endUseWindow.LightTable;
import endUseWindow.MotionEditPanel;
import endUseWindow.MotionTable;
import endUseWindow.PhaseEditPanel;
import endUseWindow.PhaseTable;
import endUseWindow.RoomEditPanel;
import endUseWindow.RoomTable;
import endUseWindow.SiteTable;
import endUseWindow.Source;
import endUseWindow.TemperatureEditPanel;
import endUseWindow.TemperatureTable;
import endUseWindow.WaterEditPanel;
import endUseWindow.WaterTable;

public class SourceManagementPanel extends JPanel implements ListSelectionListener,ActionListener{
	private static final long serialVersionUID = 5421907525894521938L;

	//Main Panel
	//GridLayout mainPanelLayout = new GridLayout(2,1);
	//JPanel mainPanel = new JPanel(mainPanelLayout);
	
	//Management Panel
	JPanel managementPanel = new JPanel(new GridLayout(2,1,0,50));
	
	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Sites");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname"});
	JScrollPane siteScroll = new JScrollPane(siteTable);
	//JPanel siteButtonPanel = new JPanel(new FlowLayout());
	//JButton siteAddB = new JButton("Add");
	//JButton siteEditB = new JButton("Edit");
	//JButton siteRemB = new JButton("Remove");
	
	//Source Select Panel
	JPanel componentPanel = new JPanel(new BorderLayout());
	JPanel componentTitleP = new JPanel(new FlowLayout());
	JLabel componentLabel = new JLabel("Manage: ");
	JComboBox componentSelect = new JComboBox(Source.getManagementList()); //
	JPanel selectedComponentPanel = new JPanel(new BorderLayout());
	
	//Appliance Panel
	JPanel appliancePanel = new JPanel(new BorderLayout());
	ApplianceTable applianceTable = new ApplianceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Circuit ID","Room ID","Appliance Group","Appliance Type","Brand","Model"});
	
	//Circuit Panel
	JPanel circuitPanel = new JPanel(new BorderLayout());
	CircuitTable circuitTable = new CircuitTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Notes"});
	
	//Gas Panel
	JPanel gasPanel = new JPanel(new BorderLayout());
	GasTable gasTable = new GasTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Notes"});
	
	//GPO Panel
	JPanel gpoPanel = new JPanel(new BorderLayout());
	GPOTable gpoTable = new GPOTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"GPO ID","GPO Name","Circuit ID","Room ID","Notes"});
	
	//Humidity Panel
	JPanel humidityPanel = new JPanel(new BorderLayout());
	HumidityTable humidityTable = new HumidityTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Room ID","Notes"});
	
	//Light Panel
	JPanel lightPanel = new JPanel(new BorderLayout());
	LightTable lightTable = new LightTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Circuit ID","Room ID","Wattage"});
	
	//Motion Panel
	JPanel motionPanel = new JPanel(new BorderLayout());
	MotionTable motionTable = new MotionTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Room ID","Notes"});
	
	//Phase Panel
	JPanel phasePanel = new JPanel(new BorderLayout());
	PhaseTable phaseTable = new PhaseTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Notes"});
	
	//Room Panel
	JPanel roomPanel = new JPanel(new BorderLayout());
	RoomTable roomTable = new RoomTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Room ID","Room Number","Room Type","Area","Notes"});
	
	//Temperature Panel
	JPanel temperaturePanel = new JPanel(new BorderLayout());
	TemperatureTable temperatureTable = new TemperatureTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Room ID","Notes"});
	
	//Water Panel
	JPanel waterPanel = new JPanel(new BorderLayout());
	WaterTable waterTable = new WaterTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Notes"});
	
	Connection dbConn;
	
	public SourceManagementPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){		
		this.setLayout(new BorderLayout());
		managementPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
		
		//Appliance
		JScrollPane applianceScroll = new JScrollPane(applianceTable);
		ApplianceEditPanel applianceEditPanel = new ApplianceEditPanel(dbConn);
		JPanel applianceButtonPanel = applianceEditPanel.getApplianceButtonPanel(siteTable,applianceTable);
		appliancePanel.add(applianceScroll,BorderLayout.CENTER);
		appliancePanel.add(applianceButtonPanel,BorderLayout.SOUTH);
		
		//Circuit
		JScrollPane circuitScroll = new JScrollPane(circuitTable);
		CircuitEditPanel circuitEditPanel = new CircuitEditPanel(dbConn);
		JPanel circuitButtonPanel = circuitEditPanel.getCircuitButtonPanel(siteTable,circuitTable);
		circuitPanel.add(circuitScroll,BorderLayout.CENTER);
		circuitPanel.add(circuitButtonPanel,BorderLayout.SOUTH);
		
		//Gas
		JScrollPane gasScroll = new JScrollPane(gasTable);
		GasEditPanel gasEditPanel = new GasEditPanel(dbConn);
		JPanel gasButtonPanel = gasEditPanel.getGasButtonPanel(siteTable,gasTable);
		gasPanel.add(gasScroll,BorderLayout.CENTER);
		gasPanel.add(gasButtonPanel,BorderLayout.SOUTH);
		
		//GPO
		JScrollPane gpoScroll = new JScrollPane(gpoTable);
		GPOEditPanel gpoEditPanel = new GPOEditPanel(dbConn);
		JPanel gpoButtonPanel = gpoEditPanel.getGPOButtonPanel(siteTable,gpoTable);
		gpoPanel.add(gpoScroll,BorderLayout.CENTER);
		gpoPanel.add(gpoButtonPanel,BorderLayout.SOUTH);
		
		//Humidity
		JScrollPane humidityScroll = new JScrollPane(humidityTable);
		HumidityEditPanel humidityEditPanel = new HumidityEditPanel(dbConn);
		JPanel humidityButtonPanel = humidityEditPanel.getHumidityButtonPanel(siteTable,humidityTable);
		humidityPanel.add(humidityScroll,BorderLayout.CENTER);
		humidityPanel.add(humidityButtonPanel,BorderLayout.SOUTH);
		
		//Light
		JScrollPane lightScroll = new JScrollPane(lightTable);
		LightEditPanel lightEditPanel = new LightEditPanel(dbConn);
		JPanel lightButtonPanel = lightEditPanel.getLightButtonPanel(siteTable,lightTable);
		lightPanel.add(lightScroll,BorderLayout.CENTER);
		lightPanel.add(lightButtonPanel,BorderLayout.SOUTH);
		
		//Motion
		JScrollPane motionScroll = new JScrollPane(motionTable);
		MotionEditPanel motionEditPanel = new MotionEditPanel(dbConn);
		JPanel motionButtonPanel = motionEditPanel.getMotionButtonPanel(siteTable,motionTable);
		motionPanel.add(motionScroll,BorderLayout.CENTER);
		motionPanel.add(motionButtonPanel,BorderLayout.SOUTH);
		
		//Phase
		JScrollPane phaseScroll = new JScrollPane(phaseTable);
		PhaseEditPanel phaseEditPanel = new PhaseEditPanel(dbConn);
		JPanel phaseButtonPanel = phaseEditPanel.getPhaseButtonPanel(siteTable,phaseTable);
		phasePanel.add(phaseScroll,BorderLayout.CENTER);
		phasePanel.add(phaseButtonPanel,BorderLayout.SOUTH);
		
		//Room
		JScrollPane roomScroll = new JScrollPane(roomTable);
		RoomEditPanel roomEditPanel = new RoomEditPanel(dbConn);
		JPanel roomButtonPanel = roomEditPanel.getRoomButtonPanel(siteTable,roomTable);
		roomPanel.add(roomScroll,BorderLayout.CENTER);
		roomPanel.add(roomButtonPanel,BorderLayout.SOUTH);
		
		//Temperature
		JScrollPane temperatureScroll = new JScrollPane(temperatureTable);
		TemperatureEditPanel temperatureEditPanel = new TemperatureEditPanel(dbConn);
		JPanel temperatureButtonPanel = temperatureEditPanel.getTemperatureButtonPanel(siteTable,temperatureTable);
		temperaturePanel.add(temperatureScroll,BorderLayout.CENTER);
		temperaturePanel.add(temperatureButtonPanel,BorderLayout.SOUTH);
		
		//Water
		JScrollPane waterScroll = new JScrollPane(waterTable);
		WaterEditPanel waterEditPanel = new WaterEditPanel(dbConn);
		JPanel waterButtonPanel = waterEditPanel.getWaterButtonPanel(siteTable,waterTable);
		waterPanel.add(waterScroll,BorderLayout.CENTER);
		waterPanel.add(waterButtonPanel,BorderLayout.SOUTH);
		
		//Sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);
		//sitePanel.add(siteButtonPanel,BorderLayout.SOUTH);
		//siteButtonPanel.add(siteAddB);
		//siteButtonPanel.add(siteEditB);
		//siteButtonPanel.add(siteRemB);
		
		//Component Panel
		componentPanel.add(componentTitleP,BorderLayout.NORTH);
		componentTitleP.add(componentLabel);
		componentTitleP.add(componentSelect);
		componentPanel.add(selectedComponentPanel,BorderLayout.CENTER);
		selectedComponentPanel.add(appliancePanel,BorderLayout.CENTER);
		componentSelect.addActionListener(this);
		componentSelect.setEnabled(false);
		
		siteTable.siteListModel.addListSelectionListener(this);
		applianceTable.applianceListModel.addListSelectionListener(this);

		managementPanel.add(sitePanel);
		managementPanel.add(componentPanel);
		
		siteTable.updateList(dbConn);
		this.add(managementPanel,BorderLayout.CENTER);
	}

	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (siteTable.siteListModel.isSelectionEmpty()==false){
			componentSelect.setEnabled(true);
			if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
				if (componentSelect.getSelectedItem().equals("Appliances")){
					applianceTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Circuits")){
					circuitTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Gas")){
					gasTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				//else if (componentSelect.getSelectedItem().equals("GPOs")){
				//	gpoTable.update(dbConn, siteTable.getValueAt(siteTable.getSelectedRow(),0).toString());
				//}
				else if (componentSelect.getSelectedItem().equals("Humidity")){
					humidityTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Lights")){
					lightTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Motion")){
					motionTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Phases")){
					phaseTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Rooms")){
					roomTable.update(dbConn, siteTable.getValueAt(siteTable.getSelectedRow(),0).toString());
				}
				else if (componentSelect.getSelectedItem().equals("Temperatures")){
					temperatureTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
				else if (componentSelect.getSelectedItem().equals("Water")){
					waterTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				}
			}
		}
		else{
			componentSelect.setEnabled(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(componentSelect)){
			if (componentSelect.getSelectedItem().equals("Appliances")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(appliancePanel);
				applianceTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Circuits")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(circuitPanel);
				circuitTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Gas")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(gasPanel);
				gasTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			//else if (componentSelect.getSelectedItem().equals("GPOs")){
			//	selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
			//	selectedComponentPanel.add(gpoPanel);
			//	gpoTable.update(dbConn, siteTable.getValueAt(siteTable.getSelectedRow(),0).toString());
			//	selectedComponentPanel.revalidate();
			//}
			else if (componentSelect.getSelectedItem().equals("Humidity")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(humidityPanel);
				humidityTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Lights")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(lightPanel);
				lightTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Motion")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(motionPanel);
				motionTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Phases")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(phasePanel);
				phaseTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			}
			else if (componentSelect.getSelectedItem().equals("Rooms")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(roomPanel);
				roomTable.update(dbConn, siteTable.getValueAt(siteTable.getSelectedRow(),0).toString());
				selectedComponentPanel.revalidate();
			} 
			else if (componentSelect.getSelectedItem().equals("Temperatures")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(temperaturePanel);
				temperatureTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			} 
			else if (componentSelect.getSelectedItem().equals("Water")){
				selectedComponentPanel.remove(selectedComponentPanel.getComponent(0));
				selectedComponentPanel.add(waterPanel);
				waterTable.update(dbConn, siteTable.siteList.get(siteTable.getSelectedRow()));
				selectedComponentPanel.revalidate();
			} 
		}
	}

}
	

