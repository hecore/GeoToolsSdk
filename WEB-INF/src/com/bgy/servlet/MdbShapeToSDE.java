package com.bgy.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.bgy.arcgis.sde.SDEGeoTool;
import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.bgy.util.PubFunction;

public class MdbShapeToSDE extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		String result = "";
		String shapepath = request.getParameter("shapepath"); //对应shape 文件夹
		if (StringUtils.isNotBlank(shapepath)){			
			result = parseMdb(shapepath);
		}else{
			result = "Layer ID is not null.";
		}
		response.setContentType(CONTENT_TYPE); 
		PrintWriter out = response.getWriter();
		out.print(result);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
		
	private String parseMdb(String shapepath){
		StringBuilder sb = new StringBuilder();
		Map<String,String> params = DBUtils.getConParams(); //服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"),params.get("instance"),params.get("user"),params.get("password"),params.get("port"));			
		db.setUp();		
		String[] shplist = PubFunction.getDirAllFile(shapepath,".shp");
		for (String shape:shplist){
			String shapefile = shapepath + "/" + shape;
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名	
			shapeu.createPrjFile();//必须设置ID 唯一编号，为了启动进度显示状态
			
			int iext = shape.lastIndexOf(".");
	    	String table = shape.substring(0, iext);
			String dbtable = table.toUpperCase();			
			db.setSdeTableName(dbtable); //设置表，判断如果存在就删除，重新生成表，并插入数据
			db.deleteTable();
			
			String v = db.exeImport(shapefile,dbtable);
			sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}		
	
}
