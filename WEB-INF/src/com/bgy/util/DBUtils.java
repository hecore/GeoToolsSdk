package com.bgy.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.db.util.DBOper;

public class DBUtils {
	
	/**
	 * 获取表的序列
	 * @param table
	 * @return
	 */
	public static String getDBSeq(String table){
		String name = "SEQ_" + table.toUpperCase();
		String sql = "select " + name + ".nextval from dual";
		String v = DBOper.getOneReturnValue(sql);
		return v;
	}
		
	public static String[] getOraKeyWord(){
		String sql = "SELECT DISTINCT KEYWORD from V$RESERVED_WORDS WHERE KEYWORD IS NOT NULL";
		String[] strArray = DBOper.getColValues(sql);		 
		return strArray;
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	public static Map<String,String> getConParams(String serverid){
		String sql = "SELECT SDEIP,SDEPORT,SDENAME,SDELOGINNAME,SDEPWD from DB_SERVER WHERE ID=" + serverid;
		String[][] vls = DBOper.getMulColReturnValue(sql);
		Map<String,String> params = new HashMap<String,String>();
		params.put("server",vls[0][0]);
		params.put("port",vls[0][1]);
		params.put("instance",vls[0][2]);
		params.put("user",vls[0][3]);
		params.put("password",vls[0][4]);
		return params;
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	public static Map<String,String> _getConParams(){
		String sql = "SELECT SDEIP,SDEPORT,SDENAME,SDELOGINNAME,SDEPWD from DB_SERVER";
		String[][] vls = DBOper.getMulColReturnValue(sql);
		Map<String,String> params = new HashMap<String,String>();
		params.put("server",vls[0][0]);
		params.put("port",vls[0][1]);
		params.put("instance",vls[0][2]);
		params.put("user",vls[0][3]);
		params.put("password",vls[0][4]);
		return params;
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	public static Map<String,String> getConParams(){
		String sql = "select SDEIP,SDEPORT,SDENAME,SDELOGINNAME,SDEPWD from DB_SERVER";
		String[][] vls = DBOper.getMulColReturnValue(sql);
		Map<String,String> params = new HashMap<String,String>();
		params.put("server",vls[0][0]);
		params.put("port",vls[0][1]);
		params.put("instance",vls[0][2]);
		params.put("user",vls[0][3]);
		params.put("password",vls[0][4]);
		return params;
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	public static Map<String,String> getServiceParams(String id){
		String sql = "SELECT IP,LOGINNAME,PWD,PORT FROM DB_SERVICE WHERE ID=" + id;
		String[][] vls = DBOper.getMulColReturnValue(sql);
		Map<String,String> params = new HashMap<String,String>();
		params.put("server",vls[0][0]);
		params.put("user",vls[0][1]);
		params.put("pwd",vls[0][2]);
		params.put("port",vls[0][3]);
		return params;
	}
	
	public static String getShapeFile(String layerid){
		String sql = "SELECT SHAPEPATH||'/'||ID||'.shp' from DB_LAYER WHERE ID=" + layerid;
		String shapefile = DBOper.getOneReturnValue(sql);
		return shapefile;
	}
	
	public static String getShpPath(String layerid){
		String sql = "SELECT SHAPEPATH from DB_LAYER WHERE ID=" + layerid;
		String shapefile = DBOper.getOneReturnValue(sql);
		return shapefile;
	}
	
	/**
	 * 是否已经建立的标准
	 * @return
	 */
	public static boolean isExsitsStandard(String layerid){
		String sql = "select COUNT(ID) from db_layer_dbf where STANDNAME IS NOT NULL AND LAYERID=" + layerid; 
		String v = DBOper.getOneReturnValue(sql);
		if (StringUtils.isBlank(v) || "0".equals(v)){
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * 获取字段映射对照表，然后可以插入到SDE数据库中
	 */
	public static String getDBLayerDBF(String layerid,Map<String,String> mpkv,String[] keywords){
		String sql = "select FLDNAME,STANDNAME from db_layer_dbf where STANDNAME IS NOT NULL AND LAYERID=" + layerid;
		String[][] lvs = DBOper.getMulColReturnValue(sql);
		int rowlen = lvs.length;
		String result = "";
		for (int i=0;i<rowlen;i++){
			String key  = lvs[i][0].toUpperCase();
			String value= lvs[i][1].toUpperCase(); //标准字段，不能为oracle 缺省字段哦
			for (String name:keywords){
				if (value.equals(name)){
					value = value + "_";
					break;
				}
			}
			mpkv.put(key,value);
		}
		return result;
	}
	
	/**
	 * 获取字段映射对照表，然后可以插入到SDE数据库中
	 */
	public static String _getDBLayerDBF(String layerid,Map<String,String> mpkv,String[] keywords){
		String sql = "select FLDNAME,FLDNAME from db_layer_dbf where LAYERID=" + layerid;
		String[][] lvs = DBOper.getMulColReturnValue(sql);
		int rowlen = lvs.length;
		String result = "";
		for (int i=0;i<rowlen;i++){
			String key  = lvs[i][0].toUpperCase();
			String value= lvs[i][1].toUpperCase(); //标准字段，不能为oracle 缺省字段哦
			for (String name:keywords){
				if (value.equals(name)){
					value = value + "_";
					break;
				}				
			}			
			mpkv.put(key,value);
		}
		return result;
	}
	
	/**
	 * 获取字段映射对照表，然后可以插入到SDE数据库中
	 */
	public static String getStandard(String layerid,Map<String,String> mpkv){
		String sql = "select FLDNAME,FLDTYPE from db_standard where topicid=" + layerid;
		String[][] lvs = DBOper.getMulColReturnValue(sql);
		int rowlen = lvs.length;
		String result = "";
		for (int i=0;i<rowlen;i++){
			String fld  = lvs[i][0];
			String type = lvs[i][1]; //标准字段，不能为oracle 缺省字段哦			
			mpkv.put(fld,type);
		}
		return result;
	}
}
