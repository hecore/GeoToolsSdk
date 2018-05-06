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
import com.bgy.util.DBUtils;

/**
 * 把图层发布成服务
 * @author Administrator
 *
 */
public class DeleteSdeTable extends HttpServlet{
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		boolean result = false;
		String layerid = request.getParameter("layer"); //对应  db_layer 表 ID 字段
		if (StringUtils.isNotBlank(layerid)){
			result = delSdeTable(layerid);
		}
		response.setContentType(CONTENT_TYPE); 
		PrintWriter out = response.getWriter();
		out.print(result);
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
	
	private boolean delSdeTable(String layer){
		Map<String,String> params = DBUtils._getConParams(); //服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"),params.get("instance"),params.get("user"),params.get("password"),params.get("port"));			
		db.setUp();						
		String dbtable = layer.toUpperCase();
		db.setSdeTableName(dbtable); //设置表，判断如果存在就删除，重新生成表，并插入数据
		boolean v = db.deleteTable();		
		db.tearDown();
		return v;
	}
}
