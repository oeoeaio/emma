package endUseWindow;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import javax.swing.JOptionPane;


public class Appliance extends Source{
	String sourceID;
	String sourceName;
	String circuitID;
	String roomID;
	String applianceGroup;
	String applianceType;
	String brand;
	String model;
	String serial;
	String connection;
	String control;
	String switchType;
	String display;
	String eps;
	String delay_start;
	String onW;
	String asW;
	String psW;
	String offW;
	String dsW;
	String yearOfPurchase;
	String usage;
	String usageUnits;
	String feature1;
	String feature2;
	String feature3;
	String feature4;
	String feature5;
	String notes;
	
	
	public Appliance(Site site,String sourceID,String sourceName,String circuitID,String roomID,String applianceGroup,String applianceType,String brand,String model,String serial,String connection,String control,String switchType,String display,String eps,String delayStart,String onW,String asW,String psW,String offW,String dsW,String yearOfPurchase,String usage,String usageUnits,String feature1,String feature2,String feature3,String feature4,String feature5,String notes){
		super(site,sourceID,sourceName,"Appliance","");
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.circuitID = (circuitID==null?"":circuitID);
		this.roomID = (roomID==null?"":roomID);
		this.applianceGroup = (applianceGroup==null?"":applianceGroup);
		this.applianceType = (applianceType==null?"":applianceType);
		this.brand = (brand==null?"":brand);
		this.model = (model==null?"":model);
		this.serial = (serial==null?"":serial);
		this.connection = (connection==null?"":connection);
		this.control = (control==null?"":control);
		this.switchType = (switchType==null?"":switchType);
		this.display = (display==null?"":display);
		this.eps = (eps==null?"":eps);
		this.delay_start = (delay_start==null?"":delay_start);
		this.onW = (onW==null?"":onW);
		this.asW = (asW==null?"":asW);
		this.psW = (psW==null?"":psW);
		this.offW = (offW==null?"":offW);
		this.dsW = (dsW==null?"":dsW);
		this.yearOfPurchase = (yearOfPurchase==null?"":yearOfPurchase);
		this.usage = (usage==null?"":usage);
		this.usageUnits = (usageUnits==null?"":usageUnits);
		this.feature1 = (feature1==null?"":feature1);
		this.feature2 = (feature2==null?"":feature2);
		this.feature3 = (feature3==null?"":feature3);
		this.feature4 = (feature4==null?"":feature4);
		this.feature5 = (feature5==null?"":feature5);
		this.notes = (notes==null?"":notes);
	}
	/*
	boolean equals(Appliance otherSource){
		if (this.sourceName.equals(otherSource.sourceName)
				&& this.sourceType.equals(otherSource.sourceType)
				&& this.measurementType.equals(otherSource.measurementType)
				&& this.location.equals(otherSource.location)){
			return true;
		}
		else{
			return false;
		}
	}*/
	
	public boolean isValid(){
		boolean isValid = false;
		if (sourceID.matches("^\\d{1,10}$")){
			if (sourceName.matches("^[\\w\\s\\-\\(\\)/]{0,20}$")){
				if (circuitID.matches("^\\d{1,10}$") || circuitID.equals("")){
					if (roomID.matches("^\\d{1,10}$") || roomID.equals("")){
						if (applianceGroup.equals("") || Arrays.asList(getApplianceGroups()).contains(applianceGroup)){
							if (applianceType.equals("") || Arrays.asList(getApplianceTypes(Arrays.asList(getApplianceGroups()).lastIndexOf(applianceGroup))).contains(applianceType)){
								if (brand.matches("^[\\w\\s\\Q!?@#$%^&*()[]{};:-+=/\\.,\\E]{0,30}$") || brand.equals("")){
									if (model.matches("^[\\w\\s\\Q!?@#$%^&*()[]{};:-+=/\\.\\E]{0,30}$") || model.equals("")){
										if (serial.matches("^[\\w\\s\\Q!?@#$%^&*()[]{};:-+=/\\.\\E]{0,30}$") || serial.equals("")){
											isValid = true;
										}
										else{
											JOptionPane.showMessageDialog(null,"The Serial '"+serial+"' is invalid.\r\nMax 30 alphanumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
										}
									}
									else{
										JOptionPane.showMessageDialog(null,"The Model '"+model+"' is invalid.\r\nMax 30 alphanumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
									}
								}
								else{
									JOptionPane.showMessageDialog(null,"The Brand '"+brand+"' is invalid.\r\nMax 30 alphanumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
								}
							}
							else{
								JOptionPane.showMessageDialog(null,"The Appliance Type '"+applianceType+"' is invalid.\r\nMax 30 alphanumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
							}
						}
						else{						
							JOptionPane.showMessageDialog(null,"The Appliance Group '"+applianceGroup+"' is invalid. Please select from the list.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
						}
					}
					else{
						JOptionPane.showMessageDialog(null,"The Room ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
					}
				}
				else{
					JOptionPane.showMessageDialog(null,"The Circuit ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The Source Name provided is invalid.\r\nMax 16 Alphnumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Source ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}	
	
	public static boolean addAppliance(Statement MySQL_Statement,LogWindow logWindow,String siteID,Appliance appliance) throws SQLException{
		if (appliance.isValid()){
			try{
				String updApplianceSQL = "INSERT INTO appliances (site_id,source_id,circuit_id,room_id,appliance_group,appliance_type,brand,model,serial_no) VALUES("+siteID+","+appliance.getSourceID()+","+(appliance.getCircuitID().equals("")?"NULL":appliance.getCircuitID())+","+(appliance.getRoomID().equals("")?"NULL":appliance.getRoomID())+","+(appliance.getApplianceGroup().equals("")?"NULL":"'"+appliance.getApplianceGroup()+"'")+","+(appliance.getApplianceType().equals("")?"NULL":"'"+appliance.getApplianceType()+"'")+","+(appliance.getBrand().equals("")?"NULL":"'"+appliance.getBrand()+"'")+","+(appliance.getModel().equals("")?"NULL":"'"+appliance.getModel()+"'")+","+(appliance.getSerial().equals("")?"NULL":"'"+appliance.getSerial()+"'")+")"; //adds specified information into the database
				MySQL_Statement.executeUpdate(updApplianceSQL);
				
				return true;
			}catch(SQLException sE){
				Source.removeSource(MySQL_Statement,siteID,appliance.sourceID);
				sE.printStackTrace();
				logWindow.println("Error occured when writing appliance information.");
				throw new SQLException();
			}
		}
		else{
			Source.removeSource(MySQL_Statement,siteID,appliance.sourceID);
			logWindow.println("Error occured when writing appliance information.");
			throw new SQLException();
		}
	}
	
	public String getSourceID(){
		return sourceID;
	}
	
	public String getSourceName(){
		return sourceName;
	}
	
	public String getCircuitID(){
		return circuitID;
	}
	
	public String getRoomID(){
		return roomID;
	}
	
	public String getApplianceGroup(){
		return applianceGroup;
	}
	
	public String getApplianceType(){
		return applianceType;
	}
	
	public String getBrand(){
		return brand;
	}
	
	public String getModel(){
		return model;
	}
	
	public String getSerial(){
		return serial;
	}

	public static String[] getApplianceGroups(){
		return new String[] {		
				"Air Conditioners",
				"Computers and Peripherals",
				"Cooking Appliances",
				"External Power Supplies",
				"Heating Appliances",
				"Home Cleaning Aids",
				"Home Entertainment",
				"Monitoring and Continuous Appliances",
				"Office Equipment",
				"Other Audio",
				"Personal Health and Hygiene Products",
				"Set Top Boxes",
				"Small Kitchen Appliances",
				"Telephones",
				"Televisions",
				"Tools",
				"Water Heaters",
				"Whitegoods",
				"Miscellaneous Appliances"
		};
	}
	
	public static String[] getApplianceTypes(int applianceGroup){
		if (!(applianceGroup>=0 && applianceGroup <=18)){
			return new String[] {""}; //no appliances since appliance Group is not set
		}
		else{
			String[][] applianceTypes = new String[][]{
				{"AC - Ducted","AC - Portable","AC - Split","AC - Window","Evaporative Cooler - Ducted","Evaporative Cooler - Portable","Evaporative Cooler - Split","Fan - Ceiling","Fan - Exhaust","Fan - Pedestal","Fan - Rangehood","Fan - Tower","Other"},
				{"Computer - Box","Computer - CRT Monitor","Computer - Home Entertainment Box","Computer - Integrated","Computer - Laptop","Computer - LCD Monitor","Computer - Speakers","External Hard Drive","Modem - ADSL","Modem - ADSL + Wireless","Modem - Cable","Modem - Dialup","Other","Printer - Inkjet","Printer - Laser","Router","Router - Wireless","Scanner","Switch","Other"},
				{"Cooktop - Electric","Cooktop - Electric/Gas Induction","Cooktop - Gas","Microwave - Convection","Microwave - Non Convection","Oven - Electric","Oven - Gas","Stove - All Electric","Stove - All Gas","Stove - Hob Electric/Oven Gas","Stove - Hob Gas/Oven Electric","Other"},
				{"External Power Supply"},
				{"Heater - Electric Ceramic","Heater - Electric Convection","Heater - Electric Fan","Heater - Electric Hyrdonic","Heater - Electric Oil","Heater - Electric Radiation","Heater - Electric Slab","Heater - Gas Convection","Heater - Gas Ducted","Heater - Gas Radiation","Heater - Gas Wall","Wood Combustion","Other"},
				{"Iron","Vacuum - Conventional","Vacuum - Ducted","Vacuum - Handheld","Other"},
				{"Aerial Booster","AV Receiver","DVD Player","DVD Recorder","DVD/Hard Disk Recorder","DVD/VCR","Exension Unit (Foxtel)","Games Console","Hard Disk Recorder/PVR","IPod Docking Station","Karaoke Player","Laser Disc","Speakers","Stereo - Amplifier","Stereo - CD Player","Stereo - Integrated","Stereo - Minidisc Player","Stereo - Other","Stereo - Portable","Stereo - Receiver","Stereo - Tape Deck","Stereo - Tuner","Stereo - Turntable","Subwoofer","VCR","Video Sender","Video Switch","Other"},
				{"Aerial","Automatic Gate","Clock","Clock Radio/Alarm Clock","Doorbell","Double Adaptor","Fish Tank - Bubbler","Fish Tank - Heater","Fish Tank - Pump","Insect Killer","Intercom","Oxygen Concentrator","Pool Control - Chlorinator","Pool Control - Filter","Pool Control - Pump","Pool Control - Solar","Pool Control - Timer","Powerboard","Pump","Remote Garage Door Opener","Security System","Smoke Alarm","Sprinkler System","Standby Switch","Surge Guard","Timer - Analogue","Timer - Digital","Uninterruptible Power Supply","Window Shutter","Other"},
				{"Electric Stapler","Electric Typewriter","Facsimile","Laminator","Multifunction Device","Photocopier","Shredder","Other"},
				{"Headphones","Headphones - Infrared Cordless","Radio","Other"},
				{"Air Freshener","Aromatheraphy","Epilator","Foot Spa","Hair - Clipper","Hair - Crimper","Hair - Curling Wand","Hair - Dryer","Hair - Electric Rollers","Hair - Straightener","Heated Towel Rail","Massage Unit","Shaver - Electric","Toothbrush - Electric","Vapouriser/Steamer","Ventilator","Waxing Unit","Other"},
				{"Set Top Box - Analogue","Set Top Box - Combination DVD Player","Set Top Box - Digital","Set Top Box - Pay TV","Other"},
				{"Breadmaker","Can Opener","Carving Knife","Coffee Grinder","Coffee Maker","De-corker","Deep Fryer","Dehydrator","Egg Cooker","Electric Grill","Electronic Scales","Espresso Machine","Fairy Floss Maker","Food Processor","Frying Pan","Grinder","Handheld Beater/Mixer","Handheld Blender","Ice Cream Maker","Juicer","Kettle","Meat Slicer","Mixer - Benchtop","Mixer - Handheld","Popcorn Maker","Rice Cooker","Sandwich Press","Slow Cooker/Crock Pot","Steamer","Steriliser","Toaster","Toaster Oven","Water Filter","Yogurt Maker","Other"},
				{"Answering Machine","Cordless Phone - Base Station","Cordless Phone - Base Station/Answering Machine","Cordless Phone - Extra Handset","Exchange","ISDN","Phone","Other"},
				{"Television - CRT","Television - LCD","Television - LED","Television - Plasma","Television - Projector","Television - Rear Projection","Other"},
				{"Angle Grinder","Benchgrinder","Benchsaw","Chainsaw","Circular Saw","Compressor","Electric Drill","Electric Lawnmower","Electric Whippersnipper","Engraver","Extraction Unit (fan)","Heat Gun","Jeweller's Motor","Jigsaw","Lathe","Leaf Blower","Mulcher","Plane","Potter's Wheel","Router (Tool)","Sander","Soldering Iron","Water Blaster","Welder","Other"},
				{"Water Heater - Electric","Water Heater - Gas","Water Heater - Heat Exchanger","Water Heater - Instantaneous Electric","Water Heater - Instantaneous Gas","Water Heater - Solar/Electric","Water Heater - Solar/Gas","Other"},
				{"Clothes Dryer","Clothes Washer - Combination","Clothes Washer - Front","Clothes Washer - Top","Dishwasher","Freezer","Refrigerator","Other"},
				{"Brewing Kit","Cash Register","CB Radio","Charger - Battery","Charger - CD Player","Charger - Digital Camera","Charger - Drill","Charger - Handheld Game","Charger - Mobile Phone Dock","Charger - Shaver","Charger - Toothbrush","Charger - Torch","Charger - Vacuum","Charger - Video Camera","Digital Picture Frame","EFTPOS","Electric Blanket","Electric Fence","Exercise Bike","Light - Novelty","Light Mirror","Light Table","Micro Film Viewer","Music - Amplifier","Music - Keyboard","Music - Sound Hub","Music - Sound Mixer","Overlocker","Pergola","Sewing Machine","Spa Heater","Treadmill","Vibrating Chair","Video Camera","Voice Recorder","Walking Machine","Waste Dispenser","Water Feature","Waterbed","Other"}
			};
			return applianceTypes[applianceGroup];
		}
	}
	
	public static String[] getConnectionValues(){
		return new String[] {
			"Plug",
			"Hardwired"
		};
	}
	
	public static String[] getControlValues(){
		return new String[] {
			"None",
			"Analogue",
			"Electronic"
			
		};
	}
	
	public static String[] getSwitchTypeValues(){
		return new String[] {
			"None",
			"Off",
			"Standby"
		};
	}
	
	public static String[] getYNValues(){
		return new String[] {
			"No",
			"Yes"
		};
	}
	
	public static String[] getUsageUnits(String applianceType){
		return new String[] {"Hours Per Day"};
		
		//if(applianceType.equals("")){
			
		//}
	}
	
	public static String[] getFeature1Values(String applianceType){
		return new String[] {"Screen"};
		
		//if(applianceType.equals("")){
			
		//}
	}
}
