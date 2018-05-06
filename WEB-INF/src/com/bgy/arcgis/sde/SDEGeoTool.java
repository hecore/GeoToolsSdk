package com.bgy.arcgis.sde;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.geotools.arcsde.ArcSDEDataStoreFactory;
import org.geotools.arcsde.data.ArcSDEDataStore;
import org.geotools.arcsde.session.ISession;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

import com.esri.sde.sdk.client.SeExtent;
import com.esri.sde.sdk.client.SeLayer;
import com.esri.sde.sdk.client.SeTable;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class SDEGeoTool {
	private String ARCSDE_SERVER = "192.168.169.6";
	private String ARCSDE_INSTANCE = "SDE";
	private String ARCSDE_USER = "SDE";
	private String ARCSDE_PWD = "sde";
	private String ARCSDE_PORT = "5151";
	
	/*
     * Convenient constants for the type of feature geometry in the shapefile
     */
    //private enum GeomType { POINT, LINE, POLYGON };
	
	/**
     * A datastore factory set up with the {@link #workingParams}
     */
    private ArcSDEDataStoreFactory dsFactory = null;
    
    private ArcSDEDataStore dsDataStore = null;
    
    
    /**
     * A set of datastore parameters that are meant to work
     */
    private Map<String, Serializable> workingParams = new HashMap<String,Serializable>();;
    
	private String sdeTableName;

	public void setSdeTableName(String sdeTableName){
		this.sdeTableName = sdeTableName;
	}
	
	public SDEGeoTool(String server,String instance,String user,String password,String port){
		this.dsFactory = new ArcSDEDataStoreFactory();
		this.ARCSDE_SERVER = server;
		this.ARCSDE_INSTANCE = instance;
		this.ARCSDE_USER = user;
		this.ARCSDE_PWD = password;
		this.ARCSDE_PORT = port; 
	}	
	
	public void setUp(){
		this.workingParams = getConParams();
		try {
			if (dsDataStore == null){
				dsDataStore = (ArcSDEDataStore)this.dsFactory.createDataStore(this.workingParams);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tearDown(){
		if (dsDataStore != null){
			dsDataStore.dispose();
			dsFactory = null;
		}
	}
	
	public ArcSDEDataStore getArcSDEDS(){
		try {
			if (dsDataStore == null){
				dsDataStore = (ArcSDEDataStore)this.dsFactory.createDataStore(this.workingParams);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dsDataStore;
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	private Map<String,Serializable> getConParams(){
		Map<String,Serializable> params = new HashMap<String,Serializable>();
		params.put("dbtype","arcsde");
		params.put("server",ARCSDE_SERVER);
		params.put("port",ARCSDE_PORT);
		params.put("instance",ARCSDE_INSTANCE);
		params.put("user",ARCSDE_USER);
		params.put("password",ARCSDE_PWD);
		params.put("create spatial index", Boolean.TRUE); 			
		return params;
	}		
	
	/**
	 * 获取所有数据库表
	 * @return
	 */
	public String[] getTableNames(){
		try{			
			ArcSDEDataStore ds = getArcSDEDS();			
			String[] tns = ds.getTypeNames();
			return tns;
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return null;
	}			
    
	public boolean isExistTypeName(String typename){
		boolean b = false;
		String[] tns = getTableNames();
		for (String name:tns){
			if (name.toUpperCase().equals("SDE." + typename)){
				b = true;
				break;
			}
		}
		return b;
	}
	
	public boolean deleteTable(){		
		try {
			ArcSDEDataStore ds = getArcSDEDS();
			ISession sdeSession = ds.getSession(Transaction.AUTO_COMMIT);
			SeTable table = sdeSession.getTable(this.sdeTableName);
			table.delete();
			return true;
        } catch (Exception e) {            
        	//e.printStackTrace();
        }
		return false;
	}
	
	/**
	 * 清空表数据
	 */
	public boolean truncateTable(){		 
		try {
			ArcSDEDataStore ds = getArcSDEDS();
			ISession sdeSession = ds.getSession(Transaction.AUTO_COMMIT);
			SeTable table = sdeSession.getTable(this.sdeTableName);
			table.truncate();
			return true;
        } catch (Exception e) {            
        	e.printStackTrace();
        }
		return false;	 		
	}
	
	/**
	 * 获取绑定了表的数据库
	 * @param TYPE
	 * @param typename
	 * @return
	 */
	private ArcSDEDataStore newArcSdeDS(final SimpleFeatureType TYPE){
		ArcSDEDataStore datastore = null;
		try {
			datastore = getArcSDEDS();			
			Map<String, String> hints = new HashMap<String, String>();
			hints.put("configuration.keyword", "");	        
			datastore.createSchema(TYPE,hints);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return datastore;	
	}		
	
	/**
	 * shapefile 不带扩展名的完整路径
	 * @param shapefile
	 */
	public void saveDefaultShapeToSDE(ShapeUtils_1 shapeu){
		List<String> fields = new ArrayList<String>(); //所有字段		
		final SimpleFeatureType TYPE  = shapeu.createFeatureType(this.sdeTableName,fields);
		ArcSDEDataStore datastore = newArcSdeDS(TYPE);
		try{
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = datastore.getFeatureWriter("SDE." + this.sdeTableName, Transaction.AUTO_COMMIT);			
			shapeu.getShapePropertis(writer, fields);			
			writer.close();				
			BoundingBox bs = shapeu.getBounding();
			setBounds(datastore,bs);
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
	}		
	
	/**
	 * 标准字段映射关系类
	 * @param shapefile
	 * @param mpf
	 */
	public int saveShapeToSDE(ShapeUtils_1 shapeu,Map<String,String> mpf){
		int row = 0;
		final SimpleFeatureType TYPE  = shapeu.newFeatureType(this.sdeTableName,mpf);
		ArcSDEDataStore datastore = newArcSdeDS(TYPE);
		try{
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = datastore.getFeatureWriter("SDE." + this.sdeTableName, Transaction.AUTO_COMMIT);			
			row = shapeu.getShapePropertis(writer, mpf);
			writer.close();			
			BoundingBox bs = shapeu.getBounding();
			setBounds(datastore,bs);
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return row;
	}
	
	public String exeImport(String shapepath,String typename){		
		String sdepath = System.getenv("SDEHOME");
		System.out.println("zhix ");
		if (StringUtils.isBlank(sdepath)){
			return "尚未配置 SDEHOME 环境变量";
		}
		String cmd = sdepath + "/bin/shp2sde -o create -l " + 
				typename + ",shape -f " + shapepath + " -a all -i 5151:esri_sde -u " + 
				ARCSDE_USER + " -p " + ARCSDE_PWD + " -s " + ARCSDE_SERVER;
		cmd = sdepath + "/bin/shp2sde -o create -l " + 
				typename + ",shape -f " + shapepath + " -a all -i sde:oracle -u " + 
				ARCSDE_USER + " -p " + ARCSDE_PWD + " -s " + ARCSDE_SERVER;
		StringBuilder sb = new StringBuilder();
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);  
			ByteArrayOutputStream resultOutStream = new ByteArrayOutputStream();  
			InputStream errorInStream = new BufferedInputStream(process.getErrorStream());  
			InputStream processInStream = new BufferedInputStream(process.getInputStream());  
			int num = 0;  
			byte[] bs = new byte[4096];  
			while((num=errorInStream.read(bs))!=-1){  
				resultOutStream.write(bs,0,num);  
			}  
			while((num=processInStream.read(bs))!=-1){  
				resultOutStream.write(bs,0,num);  
			}  
			String result=new String(resultOutStream.toByteArray());  
			System.out.println(result);
			errorInStream.close(); 
			errorInStream=null;  
			processInStream.close(); 
			processInStream=null;  
			resultOutStream.close(); 
			resultOutStream=null;  
		} catch (Exception E) {  
			E.printStackTrace();
			sb.append(E.getMessage());
		}finally{  
			if(process!=null) process.destroy();  
			process=null;  
		}
		return sb.toString();
	}
	
	private void setBounds(ArcSDEDataStore datastore,BoundingBox bs){
		if (bs == null){
			return;
		}
		try {
			ISession sdeSession = datastore.getSession(Transaction.AUTO_COMMIT);
        	SeLayer layer = sdeSession.getLayer(this.sdeTableName);
			SeExtent ext = new SeExtent(bs.getMinX(),bs.getMinY(),bs.getMaxX(),bs.getMaxY());
            layer.setExtent(ext);            
            layer.alter();                    	
        } catch (Exception e) {
			e.printStackTrace();
		}				
    }		
	
	public boolean testConn(){
		if (dsDataStore == null){
			return false;
		}else{
			return true;
		}			
    }
	
	/**
	 * 获取几何类型
	 */
	/*private GeomType getGeometryType(FeatureSource featureSource) {
		GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
        Class<?> clazz = geomDesc.getType().getBinding();
        GeomType geometryType;
        if (Polygon.class.isAssignableFrom(clazz) || MultiPolygon.class.isAssignableFrom(clazz)) {
            geometryType = GeomType.POLYGON;
        } else if (LineString.class.isAssignableFrom(clazz) || MultiLineString.class.isAssignableFrom(clazz)) {
            geometryType = GeomType.LINE;
        } else {
            geometryType = GeomType.POINT;
        }
        return geometryType;
    }*/
	
	/**
	 * 获取几何类型
	 */
	@SuppressWarnings("rawtypes")
	private String getGeomType(FeatureSource featureSource) {
		GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
        Class<?> clazz = geomDesc.getType().getBinding();
        String geometryType = "";
        if (Polygon.class.isAssignableFrom(clazz) || MultiPolygon.class.isAssignableFrom(clazz)) {
            geometryType ="POLYGON";
        } else if (LineString.class.isAssignableFrom(clazz) || MultiLineString.class.isAssignableFrom(clazz)) {
            geometryType = "LINE";
        } else {
            geometryType = "POINT";
        }
        return geometryType;
    }
	
	/**
	 * 获取所有Tables以及其几何类型
	 * @return
	 */
	public String[] getAllTableInfo(){
		try{			
			ArcSDEDataStore ds = getArcSDEDS();									
			String[] tns = ds.getTypeNames();
			String[] v = new String[tns.length];
			for (int index=0;index<tns.length;index++){
				String typeName = tns[index];
				//ds.getSchema(typeName).getGeometryDescriptor().
				String geomtype = getGeomType(ds.getFeatureSource(typeName));
				v[index] = typeName + ":" + geomtype;
			}
			return v;
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return null;
	}	
	
	/**
	 * 创建shapefile，数据来源与ArcSDE数据库
	 * @param filepath = D:/shapefile/  不带文件名，只是路径
	 */
	public boolean writeShapeFile(String filepath) {
		boolean result = false;
		try {
			ArcSDEDataStore datastore = getArcSDEDS();			
			datastore.getSchema("SDE." + this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = datastore.getFeatureSource("SDE." + this.sdeTableName);
			FeatureCollection<SimpleFeatureType,SimpleFeature> FSS = FS.getFeatures();
			SimpleFeatureType type = FSS.getSchema();						
			int fldlen = type.getAttributeCount(); //获取对应的字段信息，字段需要一一对应哦
			String[] fieldArr = new String[fldlen];
			for (int i=0;i<fldlen;i++){
				fieldArr[i] = type.getType(i).getName().toString();
			}						
			//创建shape文件对象  
	        File file = new File(filepath + this.sdeTableName + ".shp");  
	        Map<String, Serializable> params = new HashMap<String, Serializable>();  
	        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL() );  
	        ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);  	        
	        ds.createSchema(type);//定义图形信息和属性信息    
	        ds.setCharset(Charset.forName("utf-8")); 			
			FeatureIterator<SimpleFeature> iterator = FSS.features();
		    try {
		        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(this.sdeTableName, Transaction.AUTO_COMMIT);  		        
		    	while( iterator.hasNext() ){
		            SimpleFeature feature = iterator.next();		            
			        SimpleFeature newfeature = writer.next();
			        for (String fld:fieldArr){
			        	Object objatrr = feature.getAttribute(fld);
			        	if ("SHAPE".equals(fld.toUpperCase())){
			        		Geometry defGeo = (Geometry)feature.getDefaultGeometry();
							defGeo.normalize();
			        		newfeature.setAttribute("the_geom", defGeo);
			        	}else{
			        		newfeature.setAttribute(fld, objatrr);
			        	}			        	
			        }			        			        			        
			        writer.write(); 
		        }
		    	writer.close();  
		    	ds.dispose();
		    	result = true;
		    }
		    finally {
		        iterator.close();
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	} 
	
	private FeatureJSON newFJSON(){
		FeatureJSON fjson = new FeatureJSON(new GeometryJSON(19));
		fjson.setEncodeNullValues(true);
    	fjson.setEncodeFeatureCollectionCRS(true);	    	
    	fjson.setEncodeFeatureCollectionBounds(true);
    	return fjson;
	}
	
	private void querySql(String where,String jsonfile,String wktPolygon){				
		try {
			ArcSDEDataStore datastore = getArcSDEDS();			
			datastore.getSchema("SDE." + this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = datastore.getFeatureSource("SDE." + this.sdeTableName);
			//Filter filter = CQL.toFilter(where);
			//Filter filter = CQL.toFilter("CONTAINS(SHAPE," + wktPolygon + ")");
			Filter filter = CQL.toFilter("BBOX(SHAPE, 1584.375311703014,42323.01397149698,19345.83798503689,55782.28618989271)");
			Query query = new Query("SDE." + this.sdeTableName, filter);			
			FeatureCollection<SimpleFeatureType,SimpleFeature> FSS = FS.getFeatures(query);
			FeatureJSON fjson = newFJSON();
			fjson.writeFeatureCollection(FSS, jsonfile);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}		
	
	/**
	 * 获取查询结果，生成数据集
	 * @param bbox
	 * @return
	 */
	private FeatureCollection<SimpleFeatureType,SimpleFeature> getQueryFC(String bbox){
		try{
			ArcSDEDataStore datastore = getArcSDEDS();			
			datastore.getSchema("SDE." + this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = datastore.getFeatureSource("SDE." + this.sdeTableName);
			Filter filter = CQL.toFilter("BBOX(SHAPE, " + bbox + ")");
			Query query = new Query("SDE." + this.sdeTableName, filter);			
			FeatureCollection<SimpleFeatureType,SimpleFeature> FSS = FS.getFeatures(query);
			return FSS;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 根据边界查询并生成矢量文件输出
	 * @param typename
	 * @param bbox
	 * @param shapepath
	 */
	public void queryBBox(String bbox,String filepath){
		FeatureCollection<SimpleFeatureType,SimpleFeature> FSS = getQueryFC(bbox);
		if (FSS == null){
			return;
		}
		try {			
			SimpleFeatureType type = FSS.getSchema();
	        File file = new File(filepath + this.sdeTableName + ".shp");  
	        Map<String, Serializable> params = new HashMap<String, Serializable>();  
	        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL() );  
	        ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);  	        
	        ds.createSchema(type);//定义图形信息和属性信息    
	        ds.setCharset(Charset.forName("utf-8"));	        
			FeatureIterator<SimpleFeature> iterator = FSS.features();
		    
			int fldlen = type.getAttributeCount(); //获取对应的字段信息，字段需要一一对应哦
			String[] fieldArr = new String[fldlen];
			for (int i=0;i<fldlen;i++){
				fieldArr[i] = type.getType(i).getName().toString();
			}			
			try {
		        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriterAppend(this.sdeTableName, Transaction.AUTO_COMMIT);  		        
		    	while( iterator.hasNext() ){
		            SimpleFeature feature = iterator.next();		            
			        SimpleFeature newfeature = writer.next();			             					        
	        		for (String fld:fieldArr){
			        	Object objatrr = feature.getAttribute(fld);
			        	if ("SHAPE".equals(fld.toUpperCase())){
			        		Geometry defGeo = (Geometry)feature.getDefaultGeometry();
							defGeo.normalize();
			        		newfeature.setAttribute("the_geom", defGeo);
			        	}else{
			        		newfeature.setAttribute(fld, objatrr);
			        	}			        	
			        }	        			        		
	        		writer.write(); 
		        }
		    	writer.close();  
		    	ds.dispose();
		    }
		    finally {
		        iterator.close();
		    }
	        	      
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}		
	
	public Map<String, Long> querySql(String bbox,String field){				
		try {
			ArcSDEDataStore datastore = getArcSDEDS();			
			datastore.getSchema("SDE." + this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = datastore.getFeatureSource("SDE." + this.sdeTableName);
			Filter filter = CQL.toFilter("BBOX(SHAPE, " + bbox + ")");
			Query query = new Query("SDE." + this.sdeTableName, filter);			
			FeatureCollection<SimpleFeatureType,SimpleFeature> FSS = FS.getFeatures(query);			
			FeatureIterator<SimpleFeature> iterator = FSS.features();			
			String[] item = new String[FSS.size()];
			int row = 0;
			Map<String, Long> result = null;
			try {				 
				while( iterator.hasNext() ){
		            SimpleFeature feature = iterator.next();
		            Object vvt = feature.getAttribute(field);
		            if (vvt == null){
		            	item[row] = "null";
		            }else{
		            	item[row] = String.valueOf(vvt);
		            }		            
		            row ++;
		        }
				List<String> items = Arrays.asList(item);				
				result = items.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		    }finally {
		        iterator.close();
		    }
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {				
		SDEGeoTool db = new SDEGeoTool("192.168.169.6","sde","SDE","sde","5151");
		db.setUp();
		boolean v = db.testConn();
		if (v){
			System.out.println("连接成功");
		}else{
			System.out.println("连接失败");
		}
		db.setSdeTableName("HB_STHX_PY");											
		String bbox = "3654.826116343778,68469.67220061366,16391.03290335938,89232.05505696498";						
		Map<String, Long> vv = db.querySql(bbox,"GKFQLX");
		String value = vv.toString();
		System.out.println(value);
		db.tearDown();
	}
	
	/**
	 * 标准字段映射关系类,增量部分
	 * @param shapefile
	 * @param mpf
	 */
	public int _saveShapeToSDE(ShapeUtils_1 shapeu,List<Map<String, String>> mpf){
		int row = 0;		
		try{
			ArcSDEDataStore datastore = getArcSDEDS();	
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = datastore.getFeatureWriter("SDE." + this.sdeTableName, Transaction.AUTO_COMMIT);			
			row = shapeu._getShapePropertis(writer, mpf);
			writer.close();			
			//BoundingBox bs = shapeu.getBounding();
			//setBounds(datastore,bs);
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return row;
	}

	public String _saveShape2ToSDE(ShapeUtils_1 shapeu, List<Map<String, String>> mpf) {
		int row = 0;		
		try{
			ArcSDEDataStore datastore = getArcSDEDS();	
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = datastore.getFeatureWriter("SDE." + this.sdeTableName, Transaction.AUTO_COMMIT);			
			row = shapeu.__getShapePropertis(writer, mpf);
			writer.close();			
			//BoundingBox bs = shapeu.getBounding();
			//setBounds(datastore,bs);
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return row+"";
	}

	public void setCharset(Charset forName) {
		// TODO Auto-generated method stub
		ArcSDEDataStore datastore = getArcSDEDS();
		
	}
}
