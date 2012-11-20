package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import endUseWindow.LogWindow;

public class getSNDates extends Thread{
	
	LogWindow logWindow;
	File[] processFiles;

	getSNDates(LogWindow logWindow,File[] processFiles){
		this.logWindow = logWindow;
		this.processFiles = processFiles;
	}
	
	public void run(){
		BufferedReader inputStream = null;
		BufferedWriter outputStream = null;
		try {
			outputStream = new BufferedWriter(new FileWriter("datesAndSerials.csv"));
			outputStream.append("FileName,FileID,Meter Serial,Start Date,Start Data,End Date\r\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			
			for (int i=0;i<processFiles.length;i++){
				inputStream = new BufferedReader(new FileReader(processFiles[i]));
				int rowCounter = 0;
				String line = "";
				String meterSerial = "";
				String fileName = "";
				Long[] datesArray = new Long[3];
				SimpleDateFormat outputDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					while ((line = inputStream.readLine()) != null) {
						line = line.replace("   "," ");
						line = line.replace("  "," ");
						line = line.replace("	"," ");
						line = line.replace(","," ");
						String[] splitline = line.split(" ");
						if (rowCounter > 10 && meterSerial.equals("")){
							logWindow.println("Unable to determine meter serial number for file: "+processFiles[i].getName()+"\r\nNo data will be written from this file.");
							System.out.println("aslkjdfhakjuhdfgkasdjhf");
							break;
						}
						else if (rowCounter > 10 && fileName.equals("")){
							logWindow.println("Unable to determine file name for file: "+processFiles[i].getName()+"\r\nNo data will be written from this file.");
							System.out.println("SDFGSGHSFGHSRTH");
							break;
						}
						else if (splitline.length==3 
						&& (Pattern.matches("^\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1}$",splitline[0])
						|| Pattern.matches("^\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2}$",splitline[0])) 
						&& Pattern.matches("^\\d{1,2}:\\d\\d:\\d\\d$",splitline[1])){
							if (Pattern.matches("^-?\\d{1,4}(.\\d{1,2}){0,1}$",splitline[2])){ //data point
								try{
									SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
									if (datesArray[1] == null){
										datesArray[1] = dateFormat.parse(splitline[0]+" "+splitline[1]).getTime();
										if (datesArray[0] == null){datesArray[0] = datesArray[1];} //if no overflow at start
											System.out.println(datesArray[0]);
											System.out.println(datesArray[1]);
											datesArray[2] = tail(processFiles[i]);
											System.out.println(datesArray[2]);
										if (datesArray[2] == null){
											logWindow.println("ERROR: could not find end date for file: "+processFiles[i].getName());
										}
										else{
											break;
										}
									}
								} catch (ParseException e) {
									datesArray[1] = null;
									e.printStackTrace();
								}
							}
							else if (splitline[2].equals("overflow")){
								try{
									SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
									if (datesArray[0] == null){
										datesArray[0] = dateFormat.parse(splitline[0]+" "+splitline[1]).getTime();
									}
								} catch (ParseException e) {
									datesArray[0] = null;
									e.printStackTrace();
								}
							}
						}
						else if (rowCounter <= 10 && line.matches("^.{0,50} \\d{8}$")){
							meterSerial = splitline[splitline.length-1];
							logWindow.println("Meter Serial found for file: "+processFiles[i].getName()+" (meterSN: "+meterSerial+")");
						}
						else if (rowCounter <= 10 && line.matches("^.{0,50} [MLH]{2}\\d{4}$")){
							fileName = splitline[splitline.length-1];
							logWindow.println("File ID found for file: "+processFiles[i].getName()+" (fileID: "+fileName+")");
						}
						rowCounter++;
					}
					outputStream.append(processFiles[i].getName()+","+fileName+","+meterSerial+","+(datesArray[0]==null?"?????":outputDate.format(datesArray[0]))+","+(datesArray[1]==null?"?????":outputDate.format(datesArray[1]))+","+(datesArray[2]==null?"?????":outputDate.format(datesArray[2]))+"\r\n");
					logWindow.println("Done.");
				} catch (IOException err){
					logWindow.println("Unable to read from file: "+processFiles[i].getName()+"\r\nNo data will be written from this file.");
					System.err.println("IOException: "+err.getMessage());
				} finally {
					if (inputStream != null){
						try {
							inputStream.close();
						} catch (IOException err) {
							System.err.println("IOException: "+err.getMessage());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if (inputStream != null){
				try {
					inputStream.close();
				} catch (IOException err) {
					System.err.println("IOException: "+err.getMessage());
				}
			}
		}
		try {
			outputStream.close();
		} catch (IOException e) {
			System.err.println("IOException: "+e.getMessage());
		}
	}
	
	public Long tail( File file ) {
	    try {
	        RandomAccessFile fileHandler = new RandomAccessFile( file, "r" );
	        long fileLength = file.length() - 1;
	        StringBuilder sb = new StringBuilder();

	        for( long filePointer = fileLength; filePointer != -1; filePointer-- ) {
	            fileHandler.seek( filePointer );
	            int readByte = fileHandler.readByte();

	            if( readByte == 0xA ) {
	                if( filePointer == fileLength ) {
	                    continue;
	                } else {
	                    break;
	                }
	            } else if( readByte == 0xD ) {
	                if( filePointer == fileLength - 1 ) {
	                    continue;
	                } else {
	                    break;
	                }
	            }

	            sb.append( ( char ) readByte );
	        }
	        
	        fileHandler.close();

	        String lastLine = sb.reverse().toString();
	        
	        System.out.println(lastLine);
	        lastLine = lastLine.replace("   "," ");
	        lastLine = lastLine.replace("  "," ");
	        lastLine = lastLine.replace("	"," ");
	        lastLine = lastLine.replace(","," ");
			String[] splitline = lastLine.split(" ");
			if (splitline.length==3 
			&& Pattern.matches("^\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1}$",splitline[0])
			|| Pattern.matches("^\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2}$",splitline[0])){
				if (Pattern.matches("^\\d{1,2}:\\d\\d:\\d\\d$",splitline[1])
				&& Pattern.matches("^\\d{1,4}(.\\d{1,2}){0,1}$",splitline[2])){
					
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
					try{
						return dateFormat.parse(splitline[0]+" "+splitline[1]).getTime();
					} catch (ParseException e) {
						e.printStackTrace();
						return null;
					}
				}
				else{
					return null;
				}
			}
			else {
				return null;
			}
	    } catch( java.io.FileNotFoundException e ) {
	        e.printStackTrace();
	        return null;
	    } catch( java.io.IOException e ) {
	        e.printStackTrace();
	        return null;
	    }
	}

	
	
}
