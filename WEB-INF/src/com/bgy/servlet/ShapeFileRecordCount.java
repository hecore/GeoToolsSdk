package com.bgy.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.db.pool.ServiceBase;



public class ShapeFileRecordCount extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		String result = "";
		String layerid = request.getParameter("id"); //对应  db_layer 表 ID 字段
		if (StringUtils.isNotBlank(layerid)){
			putDBF(layerid);
			result = "0";
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
	
	private void putDBF(String layerid){
		String shapefile = DBUtils.getShapeFile(layerid);
		if (StringUtils.isNotBlank(shapefile)){
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名
			int recordCount =  shapeu.getDBFRecordCount();			
			insertIntoDBF(layerid,recordCount);
		}
	}
	
	/**
	 * 开始启动进度表，分析处理
	 */
	private int insertIntoDBF(String layerid,int recordcount){
		ServiceBase sb=new ServiceBase();
		String usersql = "insert into db_process(ID,ROWCOUNT,ROWIN,DFLAG,INFO)values(?,?,?,?,?)";
		Map<Integer, Object> pm = new HashMap<Integer, Object>(); //参数
		pm.put(1, layerid);
		pm.put(2, recordcount);
		pm.put(3, 0);
		pm.put(4, 0);
		pm.put(5, "");
		try {
			boolean  b = sb.insert(usersql, pm);
			if (b){
				return Integer.valueOf(layerid);
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
}
