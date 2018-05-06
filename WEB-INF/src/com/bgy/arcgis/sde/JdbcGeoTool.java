package com.bgy.arcgis.sde;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
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
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class JdbcGeoTool {
	private String ARCSDE_SERVER = "192.168.169.6";
	private String ARCSDE_INSTANCE = "ORCL";
	private String ARCSDE_USER = "SDE";
	private String ARCSDE_PWD = "sde";
	private String ARCSDE_PORT = "1521";
	
	/*
     * Convenient constants for the type of feature geometry in the shapefile
     */
    //private enum GeomType { POINT, LINE, POLYGON };
	
	/**
     * A datastore factory set up with the {@link #workingParams}
     */
    
    private DataStore dsDataStore = null;
    
    
    /**
     * A set of datastore parameters that are meant to work
     */
    private Map<String, Serializable> workingParams = new HashMap<String,Serializable>();;
    
	private String sdeTableName;

	public void setSdeTableName(String sdeTableName){
		this.sdeTableName = sdeTableName;
	}
	
	public JdbcGeoTool(){}
	
	public JdbcGeoTool(String server,String instance,String user,String password,String port){
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
				dsDataStore = DataStoreFinder.getDataStore(this.workingParams);
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}				
		
	}
	
	public void tearDown(){
		if (dsDataStore != null){
			dsDataStore.dispose();
		}
	}
	
	/**
	 * 获取 ARCSDE 连接参数
	 * @return
	 */
	private Map<String,Serializable> getConParams(){
		Map<String,Serializable> params = new HashMap<String,Serializable>();				
		params.put(JDBCDataStoreFactory.DBTYPE.key,  "oracle");
		params.put(JDBCDataStoreFactory.HOST.key,  ARCSDE_SERVER); //主机名，IP地址
		params.put(JDBCDataStoreFactory.PORT.key,  ARCSDE_PORT);   //一般1521
		params.put(JDBCDataStoreFactory.DATABASE.key,  ARCSDE_INSTANCE); //服务名
		params.put(JDBCDataStoreFactory.USER.key,  ARCSDE_USER);
		params.put(JDBCDataStoreFactory.PASSWD.key,  ARCSDE_PWD);		
		return params;
	}		
	
	/**
	 * 获取所有数据库表
	 * @return
	 */
	public String[] getTableNames(){
		try{			
			String[] tns = dsDataStore.getTypeNames();
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
			if (name.toUpperCase().equals(typename)){
				b = true;
				break;
			}
		}
		return b;
	}
	
	public boolean deleteTable(){		
		try {
			dsDataStore.removeSchema(this.sdeTableName);
			return true;
        } catch (Exception e) {            
        	e.printStackTrace();
        }
		return false;
	}
	
	/**
	 * shapefile 不带扩展名的完整路径
	 * @param shapefile
	 */
	public void saveDefaultShapeToSDE(ShapeUtils_1 shapeu){
		List<String> fields = new ArrayList<String>(); //所有字段		
		final SimpleFeatureType TYPE  = shapeu.createFeatureType(this.sdeTableName,fields);		
		try{
			dsDataStore.createSchema(TYPE);
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = dsDataStore.getFeatureWriterAppend(this.sdeTableName, Transaction.AUTO_COMMIT);			
			shapeu.getShapePropertis(writer, fields);			
			writer.close();				
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
		try{
			dsDataStore.createSchema(TYPE);
			FeatureWriter<SimpleFeatureType,SimpleFeature> writer = dsDataStore.getFeatureWriterAppend(this.sdeTableName, Transaction.AUTO_COMMIT);			
			row = shapeu.getShapePropertis(writer, mpf);
			writer.close();			
	    } catch (Exception e) {  
	        e.printStackTrace();	       
	    }
		return row;
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
			String[] tns = dsDataStore.getTypeNames();
			String[] v = new String[tns.length];
			for (int index=0;index<tns.length;index++){
				String typeName = tns[index];
				String geomtype = getGeomType(dsDataStore.getFeatureSource(typeName));
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
			dsDataStore.getSchema(this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = dsDataStore.getFeatureSource(this.sdeTableName);
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
		} catch (Exception e) {
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
			dsDataStore.getSchema(this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = dsDataStore.getFeatureSource(this.sdeTableName);
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
			dsDataStore.getSchema(this.sdeTableName);
			FeatureSource<SimpleFeatureType,SimpleFeature> FS = dsDataStore.getFeatureSource(this.sdeTableName);
			Filter filter = CQL.toFilter("BBOX(SHAPE, " + bbox + ")");
			Query query = new Query(this.sdeTableName, filter);			
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
		    try {
		        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(this.sdeTableName, Transaction.AUTO_COMMIT);  		        
		    	while( iterator.hasNext() ){
		            SimpleFeature feature = iterator.next();		            
			        SimpleFeature newfeature = writer.next();
			        newfeature.setAttributes(feature.getAttributes());
			        Geometry defGeo = (Geometry)feature.getDefaultGeometry();
	        		newfeature.setAttribute("the_geom", defGeo);	        		
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
	
	public static void main(String[] args) {
		JdbcGeoTool db = new JdbcGeoTool();
		db.setUp();
		boolean v = db.testConn();
		if (v){
			System.out.println("连接成功");
		}else{
			System.out.println("连接失败");
		}
		db.setSdeTableName("P24风景名胜_POINT");
					
		//Filter pointInPolygon = CQL.toFilter("CONTAINS(THE_GEOM, POINT(1 2))");		
	    //String wktPoint = "POINT(103.83489981581 33.462715497945)";  
	    //String wktLine = "LINESTRING(108.32803893589 41.306670233001,99.950999898452 25.84722546391)";  
	    //wktPolygon = "POLYGON((100.02715479879 32.168082192159,102.76873121104 37.194305614622,107.0334056301 34.909658604412,105.96723702534 30.949603786713,100.02715479879 32.168082192159))";  
	    //String wktPolygon1 = "POLYGON((96.219409781775 32.777321394882,96.219409781775 40.240501628236,104.82491352023001 40.240501628236,104.82491352023001 32.777321394882,96.219409781775 32.777321394882))";  		
		//db.querySql("NAME_ LIKE '%天%'","D:/log/tmp.geojson",wktPolygon);
		String bbox = "1584.375311703014,42323.01397149698,19345.83798503689,55782.28618989271";
		db.queryBBox(bbox, "D:/tmp/DEM/");
		db.tearDown();
	}
}
