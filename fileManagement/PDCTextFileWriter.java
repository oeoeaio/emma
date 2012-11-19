package fileManagement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

public class PDCTextFileWriter {
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
	
	void writeHeaderToFile(BufferedWriter outputStream,String[] concHeaderValues,ArrayList<String[]> ctModHeaderValues,ArrayList<String[]> wModHeaderValues){
		try {
			outputStream.write(concHeaderValues[1]+",Time,Volts,Volts,Volts");
			
			for (int i=0;i<ctModHeaderValues.size();i++){
				outputStream.write(",");
				for (int j=0;j<6;j++){
					outputStream.write(ctModHeaderValues.get(i)[0]);
					if (j!=5){outputStream.write(",");}
				}
			}
			for (int i=0;i<wModHeaderValues.size();i++){
				//outputStream.write(",");
				//for (int j=0;j<6;j++){
				//	outputStream.write(wModHeaderValues.get(i)[0]);
				//	if (j!=5){outputStream.write(",");}
				//}
				outputStream.write(",");
				String[] wSensorSNs = wModHeaderValues.get(i)[4].split(",");
				for (int j=0;j<wSensorSNs.length;j++){
					outputStream.write(wSensorSNs[j]);
					if (j!=wSensorSNs.length-1){outputStream.write(",");}
				}
			}
			outputStream.write("\r\n");
			outputStream.write(concHeaderValues[0]+",hh:mm:ss,Ph 1,Ph 2,Ph 3");
			for (int i=0;i<ctModHeaderValues.size();i++){
				outputStream.write(",");
				String[] ctModChannelNames = ctModHeaderValues.get(i)[3].split(",");
				for (int j=0;j<ctModChannelNames.length;j++){
					outputStream.write(ctModChannelNames[j]);
					if (j!=ctModChannelNames.length-1){outputStream.write(",");}
				}
			}
			for (int i = 0;i<wModHeaderValues.size();i++){
				outputStream.write(",");
				String[] wModChannelNames = wModHeaderValues.get(i)[3].split(",");
				for (int j=0;j<wModChannelNames.length;j++){
					outputStream.write(wModChannelNames[j]);
					if (j!=wModChannelNames.length-1){outputStream.write(",");}
				}
			}
			outputStream.write("\r\n");
			outputStream.write("Channels,hh:mm:ss,Ph 1,Ph 2,Ph 3");
			for (int i=0;i<ctModHeaderValues.size();i++){
				outputStream.write(",");
				String[] ctModChannels = ctModHeaderValues.get(i)[6].split(",");
				for (int j=0;j<ctModChannels.length;j++){
					outputStream.write(ctModChannels[j]);
					if (j!=ctModChannels.length-1){outputStream.write(",");}
				}
			}
			for (int i = 0;i<wModHeaderValues.size();i++){
				outputStream.write(",");
				String[] wModChannels = wModHeaderValues.get(i)[5].split(",");
				for (int j=0;j<wModChannels.length;j++){
					outputStream.write(wModChannels[j]);
					if (j!=wModChannels.length-1){outputStream.write(",");}
				}
			}
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void writeDataToFile(BufferedWriter outputStream,ArrayList<Long> concDatesDataBlock,double[][] concPhasesDataBlock,double[][] ctModDataBlocks,double[][] wModDataBlocks){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		timeFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		try {
			System.out.println(concDatesDataBlock.size()+" "+ctModDataBlocks.length);
			for (int j=0;j<concDatesDataBlock.size();j++){
				outputStream.write("\r\n");
            	outputStream.write(dateFormatter.format(concDatesDataBlock.get(j))+",");
            	outputStream.write(timeFormatter.format(concDatesDataBlock.get(j))+",");
            	outputStream.write(concPhasesDataBlock[0][j]+","+concPhasesDataBlock[1][j]+","+concPhasesDataBlock[2][j]+",");

            	for (int i=0;i<ctModDataBlocks.length;i++){
            		outputStream.write((ctModDataBlocks[i][j]==-123.456 ? "" : ctModDataBlocks[i][j])+"");
            		outputStream.write(",");
            	}
            	for (int i=0;i<wModDataBlocks.length;i++){
            		outputStream.write((wModDataBlocks[i][j]==-123.456 ? "" : wModDataBlocks[i][j])+"");
            		if (i!=wModDataBlocks.length-1){ outputStream.write(",");}
            	}
			}
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
