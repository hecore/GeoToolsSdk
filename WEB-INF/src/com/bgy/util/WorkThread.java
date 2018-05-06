package com.bgy.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.bgy.arcgis.sde.SDEGeoTool;
import com.bgy.arcgis.sde.ShapeUtils_1;
import com.db.util.DBOper;

public class WorkThread implements Runnable{	
	private int time_delay = 30*1000;
	private boolean isrunning = false;
	public void run() {
		while (true){			
			try {
				Thread.sleep(time_delay);
				if (isrunning){
					Thread.sleep(time_delay);
				}else{
					isrunning = true;
					pyShapeToSde();										
					isrunning = false;
				}
				
			} catch (Exception e) {
				isrunning = false;
				e.printStackTrace();
			}
		}
	}
	
	private void pyShapeToSde(){
		String sql = "SELECT ID,MDBPATH||'shape' FROM DB_MDB WHERE ISSDE=1";
		String[][] vvl =DBOper.getMulColReturnValue(sql);
		if (vvl == null){
			return;
		}
		File file = new File("D:/FIndexDB/mdb/layers.txt");
		StringBuilder sb = new StringBuilder();
		StringBuilder idlist = new StringBuilder();
		for (int index=0; index<vvl.length; index++){
			idlist.append(vvl[index][0] + ",");
			sb.append(vvl[index][1] + "\r\n");			
		}
		String data = sb.toString();
		String encoding = "utf-8";
		try {
			file.deleteOnExit();
			FileUtils.writeStringToFile(file, data, encoding);
			
			PubArcPy pp = new PubArcPy();
			String v = pp.mdbToSDE();
			System.out.println(v);
			if (v.indexOf("Finished shape to sde") > -1){
				String ids = idlist.toString();
				sql = "update db_mdb set issde=1 where id in(" + ids + "0)";
				DBOper.updateRecord(sql);
			}						
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void pyShapeUpToSde(){
//		String sql = "SELECT ID,MDBPATH||'shape' FROM DB_MDB WHERE ISSDE=1";
//		String[][] vvl =DBOper.getMulColReturnValue(sql);
//		if (vvl == null){
//			return;
//		}
		File file = new File("D:/FIndexDB/mdb/layers.txt");
		StringBuilder sb = new StringBuilder();
//		StringBuilder idlist = new StringBuilder();
//		for (int index=0; index<vvl.length; index++){
//			idlist.append(vvl[index][0] + ",");
//			sb.append(vvl[index][1] + "\r\n");			
//		}
		String data = sb.toString();
		String encoding = "utf-8";
		try {
			file.deleteOnExit();
			FileUtils.writeStringToFile(file, data, encoding);
			
			PubArcPy pp = new PubArcPy();
			String v = pp.mdbToSDE();
			System.out.println(v);
			if (v.indexOf("Finished shape to sde") > -1){
//				String ids = idlist.toString();
//				sql = "update db_mdb set issde=1 where id in(" + ids + "0)";
//				DBOper.updateRecord(sql);
			}						
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void runTask(){
		String[] idlist = getLayerList();
		if (idlist == null || idlist.length == 0){
			return;
		}
		String[] keywords = DBUtils.getOraKeyWord(); //获取oracle关键字		
		
		Map<String,String> params = DBUtils._getConParams(); //服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"),params.get("instance"),params.get("user"),params.get("password"),params.get("port"));			
		db.setUp();		
		for (String layerid:idlist){			
			putProcess(layerid); //记录进度总数
			Map<String,String> mpkv = new HashMap<String,String>(); 						
			boolean flag = DBUtils.isExsitsStandard(layerid); //获取shapefile字段映射关系，原始字段和标准字段的映射关系，如果没有建立标准则直接使用原始字段		
			if (flag){
				String v = DBUtils.getDBLayerDBF(layerid,mpkv,keywords);
				if (StringUtils.isBlank(v)){
					startDBLayer(db,layerid,mpkv);
				}else{
					setDBProcess(layerid,v,"-1");
				}
			}else{
				String v = DBUtils._getDBLayerDBF(layerid,mpkv,keywords);
				if (StringUtils.isBlank(v)){
					startDBLayer(db,layerid,mpkv);
				}else{
					setDBProcess(layerid,v,"-1");
				}
			}
		}		
		db.tearDown();
	}
	
	/**
	 * 启动图层分析
	 */
	private void startDBLayer(SDEGeoTool db,String layerid,Map<String,String> mpkv){
		String shapefile = DBUtils.getShapeFile(layerid);
		String sql = "SELECT LAYERNAME from DB_LAYER WHERE ID=" + layerid;
		String tablename = DBOper.getOneReturnValue(sql);
		if (StringUtils.isBlank(tablename)){
			setDBProcess(layerid,"DB_LAYER 表中图层名称不能为空","-1");
		}else{
			String dbtable = tablename.toUpperCase();
			db.setSdeTableName(dbtable); //设置表，判断如果存在就删除，重新生成表，并插入数据			
			db.deleteTable(); //直接删除后重新再次导入
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名	
			shapeu.initCoordSys();
			int row = db.saveShapeToSDE(shapeu,mpkv);			
			if (row > 0){
				writeDBProcess(layerid,row);
				//setCenterPoint(layerid);
			}			
		}
	}
	
	private String[] getLayerList(){
		String sql = "SELECT ID from DB_LAYER WHERE ISSDE=0";
		String[] v = DBOper.getColValues(sql);
		return v;
	}
	
	private void writeDBProcess(String layerid,int row){											
		String sql = "update db_process set ROWCOUNT=" + row + ",ROWIN=" + row + ",DFLAG=2 where ID=" + layerid;
		DBOper.updateRecord(sql);
		sql = "update db_layer set issde=1 where ID=" + layerid;
		DBOper.updateRecord(sql);
	}		
	
	private void setDBProcess(String layerid,String info,String flag){											
		String sql = "update db_process set DFLAG=" + flag + ",INFO='" + info + "' where ID=" + layerid;
		DBOper.updateRecord(sql);
	}		
	
	private void putProcess(String layerid){
		String sql = "DELETE FROM db_process where ID=" + layerid;
		DBOper.updateRecord(sql);
		sql = "insert into db_process(ID,ROWCOUNT,ROWIN,DFLAG) VALUES(" + layerid + ",0,0,1)";
		DBOper.updateRecord(sql);
	}
	
	/**
	 * 设置中中心坐标
	 * @param layerid
	 */
	private void setCenterPoint(String layerid){		
		String shapefile = DBUtils.getShapeFile(layerid);
		if (StringUtils.isNotBlank(shapefile)){
			String sql = "delete from db_yaosu where layerid=" + layerid;
			DBOper.updateRecord(sql);
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名
			shapeu.shapeToDB(layerid);			
		}
	}
	
}
