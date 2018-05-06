package com.bgy.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.db.pool.ServiceBase;
import com.db.util.DBOper;



public class ShapeFileToDbf extends HttpServlet{	
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
			List<Map<String,String>> lmap =  shapeu.getShapeFields();			
			for (int i=0;i<lmap.size();i++){
				Map<String,String> mpv = lmap.get(i);
				String fldname = mpv.get("fname");
				String fldtype = mpv.get("ftype");
				String fsize = mpv.get("fsize");
				String fldpre = mpv.get("fdeci");
				insertIntoDBF(layerid,fldname,fldtype,fsize,fldpre);
			}
			String geomtype = shapeu.getLayerGeomType();//获取图形几何类型，更新到 DB_LAYER 表中
			String sql = "UPDATE DB_LAYER SET LAYERTYPE='" + geomtype + "' WHERE ID=" + layerid;
			DBOper.updateRecord(sql);
		}
	}
	
	/**
	 * 开始启动进度表，分析处理
	 */
	private int insertIntoDBF(String layerid,String fldname,String fldtype,String fsize,String fldpre){
		String seq = DBUtils.getDBSeq("db_layer_dbf");
		ServiceBase sb=new ServiceBase();
		String usersql = "insert into db_layer_dbf(ID,LAYERID,FLDNAME,FLDTYPE,FLDLEN,FLDPRE)values(?,?,?,?,?,?)";
		Map<Integer, Object> pm = new HashMap<Integer, Object>(); //参数
		pm.put(1, seq);
		pm.put(2, layerid);
		pm.put(3, fldname);
		pm.put(4, fldtype);
		pm.put(5, fsize);
		pm.put(6, fldpre);
		try {
			boolean  b = sb.insert(usersql, pm);
			if (b){
				return Integer.valueOf(seq);
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
}
