package com.bgy.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PubArcPy {
	public static void main(String args[]){		
		//PubArcPy.createConAgs("192.168.0.110","admin", "arcgis");
		//PubArcPy.publishService("D:/FIndexDB/shapeFiles/北京POI数据/道路边线.shp","192.168.0.110");
		PubArcPy pp = new PubArcPy();
		String v = pp.mdbToSDE();
		System.out.println(v);
	}		
	
	public String mdbToSDE(){					
		String cmd = "python D:/FIndexDB/webProject/py/mdbToSde.py";
		String v = exeCmd(cmd);
		return v;
	}
	
	public String shpAppendToSde(){
		String cmd = "python D:/FIndexDB/webProject/py/shpAppendToSde.py";
		String v = exeCmd(cmd);
		return v;
	}
	
	private static String exeCmd(String cmd){
		String v = "";
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);  
			ByteArrayOutputStream resultOutStream = new ByteArrayOutputStream();  
			InputStream errorInStream = new BufferedInputStream(process.getErrorStream());  
			InputStream processInStream = new BufferedInputStream(process.getInputStream());  
			int num = 0;  
			byte[] bs = new byte[4096];  			 
			while((num=processInStream.read(bs))!=-1){  
				resultOutStream.write(bs,0,num);  
			}  
			while((num=errorInStream.read(bs))!=-1){  
				resultOutStream.write(bs,0,num);  
			} 
			String result=new String(resultOutStream.toByteArray());
			v = result;						
			errorInStream.close(); 
			errorInStream=null;  
			processInStream.close(); 
			processInStream=null;  
			resultOutStream.close(); 
			resultOutStream=null;  
		} catch (IOException E) {  
			E.printStackTrace();
			v = E.getMessage();
		}finally{
			if(process != null) {
				process.destroy();
				process=null;
			}
		}
		return v;
	}
}
