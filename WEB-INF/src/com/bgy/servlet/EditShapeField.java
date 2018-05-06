package com.bgy.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.opengis.feature.simple.SimpleFeatureType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.db.util.DBOper;

/**
 * 把定义好的任务生成shapefile，供前端下载使用
 * @author Administrator
 *
 */
public class EditShapeField extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		String result = "";
		String taskid = request.getParameter("id"); //对应  db_layer 表 ID 字段
		if (StringUtils.isNotBlank(taskid)){
			String layerid = getLayerID(taskid);
			if (StringUtils.isNotBlank(layerid)){
				result = publishService(layerid,taskid);
			}else{
				result = "Layer ID is not null.";
			}
		}else{
			result = "Task ID is not null.";
		}		
		response.setContentType(CONTENT_TYPE); 
		PrintWriter out = response.getWriter();
		out.print(result);
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
	
	private String publishService(String layerid,String taskid){
		List<Map<String,Object>> fldList = getFieldList(layerid,taskid);//生成测试字段列表;
		List<Map<String,Object>> dataList = getDBFData(taskid);
		
		String shapefile = DBUtils.getShapeFile(layerid);
		ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名
		SimpleFeatureType st = shapeu.newFeatureType(layerid, fldList);		
		String sql = "SELECT LAYERALIAS from DB_LAYER WHERE ID=" + layerid;
		String alias = DBOper.getOneReturnValue(sql);
		String path =  "D:/FIndexDB/down/" + alias + "/";
		File f = new File(path);
		f.mkdirs();
		String destshape = path + layerid + ".shp";
		boolean v = shapeu.newShapeFile(destshape, layerid, st, dataList);		
		return v?"OK":"new shapefile fail";
	}
	
	private List<Map<String,Object>> getFieldList(String layerid,String taskid){
		String shapefile = DBUtils.getShapeFile(layerid);
		String sql = "SELECT LAYERNAME from DB_LAYER WHERE ID=" + layerid;
		String layername = DBOper.getOneReturnValue(sql);		
		if (StringUtils.isBlank(shapefile) || StringUtils.isBlank(layername)){	
			return null;
		}
		sql = "select FLDNAME,FLDTYPE,FLDLEN,FLDPRE FROM DB_FIELD WHERE LAYERID=" + layerid + " AND TASKID=" + taskid + " ORDER BY ID";
		String[][] vvlist = DBOper.getMulColReturnValue(sql);
		if (vvlist == null){
			return null;
		}
		List<Map<String,Object>> fldList = new ArrayList<Map<String,Object>>();
		int rows = vvlist.length;
		for (int index=0; index<rows; index++){
			Map<String,Object> mp = new HashMap<String,Object>();
			mp.put("name", vvlist[index][0]);
			String desc = vvlist[index][1];
			if (StringUtils.isNotBlank(desc)){
				if ("文本".equals(desc)){
					mp.put("type", 'C');
				}else if ("长整型".equals(desc)){
					mp.put("type", 'I');
				}else if ("双精度".equals(desc)){
					mp.put("type", 'F');
				}else if ("日期".equals(desc)){
					mp.put("type", 'D');
				}else{
					mp.put("type", 'C');
				}
			}else{
				mp.put("type", 'C');
			}
			mp.put("len", vvlist[index][2]);
			mp.put("pre", vvlist[index][3]);    	
			fldList.add(mp);
		}				
		return fldList;
	}
	
	
	private List<Map<String,Object>> getDBFData(String taskid){
		String sql = "SELECT FID,FCONTEXT from DB_EDIT_LAYER WHERE TASKID=" + taskid + " ORDER BY FID";
		String[][] vvlist = DBOper.getMulColReturnValue(sql);
		int rows = vvlist.length;
		List<Map<String,Object>> dList = new ArrayList<Map<String,Object>>();
		for (int index=0; index<rows; index++){
			String strJson = vvlist[index][1];			
			JSONObject jObj = JSON.parseObject(strJson);
			Map<String,Object> mp = new HashMap<String,Object>();
			for (Map.Entry<String, Object> entry : jObj.entrySet()){
				mp.put(entry.getKey(), entry.getValue());
			}
			dList.add(mp);
		}
		return dList;
	}
	
	private String getLayerID(String taskid){
		String sql = "SELECT LAYERID FROM DB_TASK WHERE ID=" + taskid;
		String layerid = DBOper.getOneReturnValue(sql);
		return layerid;
	}
}
