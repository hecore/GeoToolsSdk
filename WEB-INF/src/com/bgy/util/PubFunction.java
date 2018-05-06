package com.bgy.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;


/**
 *  数据类型相关处理工具类
 */
public class PubFunction{
	
    public static boolean isValidStr(String value){
    	return !StringUtils.isBlank(value);    	
    } 
    
    /**
     * 获取文件名，没有扩展名的文件名
     * @param filepath
     * @return
     */
	public static String getFileNameNoEx(String filepath){
    	File f =new File(filepath);  
    	String fileName = f.getName();
    	int ips = fileName.lastIndexOf(".");
    	String prefix = "";
    	if (ips > -1){
    		prefix = fileName.substring(fileName.lastIndexOf("."));
    	}
    	int num = prefix.length();//得到后缀名长度         
    	String fileOtherName=fileName.substring(0, fileName.length()-num);//得到文件名。去掉了后缀   
    	return fileOtherName.toUpperCase();
	}
	
	public static boolean setIniValue(String node, String value){		
		try {
			StringBuilder sb = new StringBuilder();
			List<String> list = FileUtils.readLines(new File("D:/FIndexDB/webProject/py/settings.ini"),"UTF-8");
			sb.append("[fs_info]\r\n");			
			for (String line:list){				
				if(line.indexOf("fs_info") > -1){
					continue;
				}
				if (line.indexOf(node) == 0){
					String nline = node + " = " + value;
					sb.append(nline + "\r\n");
				}else{
					sb.append(line + "\r\n");
				}				
			}			
			FileUtils.writeStringToFile(new File("D:/FIndexDB/webProject/py/settings.ini"), sb.toString(), "utf-8");
		    return true;
		} catch (IOException e) {
		    e.printStackTrace();
		    return false;
		}		
	}
	
	public static boolean setIniValue(String fname,String node, String value){
		String inifile = "D:/FIndexDB/webProject/py/" + fname;
		StringBuilder sb = new StringBuilder();
		try {			
			List<String> list = FileUtils.readLines(new File(inifile),"UTF-8");
			sb.append("[fs_info]\r\n");			
			for (String line:list){				
				if(line.indexOf("fs_info") > -1){
					continue;
				}
				if (line.indexOf(node) == 0){
					String nline = node + " = " + value;
					sb.append(nline + "\r\n");
				}else{
					sb.append(line + "\r\n");
				}				
			}			
			FileUtils.writeStringToFile(new File(inifile), sb.toString(), "utf-8");
		    return true;
		} catch (IOException e) {
		    e.printStackTrace();
		    return false;
		}		
	}
	
	/**
	 * 获取文本文件内容
	 * @param txtfile
	 * @return
	 */
	public static String getFileText(String txtfile) {
		try {
			String v = new String(Files.readAllBytes(Paths.get(txtfile)));
			return v;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	static class MyFilter implements FilenameFilter{    
        private String type;    
        public MyFilter(String type){    
            this.type = type;    
        }    
        public boolean accept(File dir,String name){    
            return name.endsWith(type);    
        }    
    }    	
    
	/**
	 * 
	 * @param filter .shp 过滤的扩展名
	 * @return
	 */
	public static String[] getDirAllFile(String fpath,String filter){
		File file = new File(fpath);
    	MyFilter mfilter = new MyFilter(filter); 
    	String[] flist = file.list(mfilter);
    	return flist;
	}
	
    public static void main(String[] args) {  
    	//PubFunction.setIniValue("servicename", "中国");
    	File file = new File("D:\\FIndexDB\\webProject\\Layer\\2017\\09\\19\\159");
    	MyFilter filter = new MyFilter(".shp"); 
    	String[] flist = file.list(filter);
    	for (String fn:flist){
    		System.out.println(fn);    		
    	}
    }  
    
}