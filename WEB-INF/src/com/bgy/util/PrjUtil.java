package com.bgy.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


//坐标转换--point-->what we need
public class PrjUtil {
	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		//String dxf = "D:/tmp/lancun.dxf";
	}
		
	public static String ParseDXFFile(String dxf) {
		Map<String,Double> mp = getBoundData(dxf);
		if (mp != null && mp.size() > 0){
			Double minx = Double.valueOf(mp.get("min-x"));
			Double miny = Double.valueOf(mp.get("min-y"));
			Double maxx = Double.valueOf(mp.get("max-x"));
			Double maxy = Double.valueOf(mp.get("max-y"));			
			try{
				//double[] dstminxy = getPointToWKT(minx, miny);
				//double[] dstmaxxy = getPointToWKT(maxx, maxy);
				//String v = String.valueOf(dstminxy[0]) + "," + String.valueOf(dstminxy[1]) + "," + String.valueOf(dstmaxxy[0]) + "," + String.valueOf(dstmaxxy[1]);
				//System.out.println(v);
				String v = String.valueOf(minx) + "," + String.valueOf(miny) + "," + String.valueOf(maxx) + "," + String.valueOf(maxy);
				System.out.println(v);				
				return v;
			}catch(java.lang.IllegalArgumentException E){
				System.out.println(E.getMessage());
			}												
		}else{
			System.out.println("提取边界信息失败,有可能无法读取文件!");
		}
		return "error";
	}
	
	/**
     * 当浮点型数据位数超过10位之后，数据变成科学计数法显示。用此方法可以使其正常显示。
     * @param value
     * @return Sting
     */
    private static String formatFloatNumber(double value) {
        if(value != 0.00){
            java.text.DecimalFormat df = new java.text.DecimalFormat("########.00000");
            return df.format(value);
        }else{
            return "0.00";
        }

    }
    
    private static String formatFloatNumber(Double value) {
        if(value != null){
            if(value.doubleValue() != 0.00){
                java.text.DecimalFormat df = new java.text.DecimalFormat("########.00");
                return df.format(value.doubleValue());
            }else{
                return "0.00";
            }
        }
        return "";
    }
	
    private static double round(double v,int l){      
		 String result = formatFloatNumber(v);
		 return Double.valueOf(result);
	}  
	
    private static Map<String,Double> getBoundData(String dxf){
		Map<String,Double> mp = new HashMap<String,Double>();
		try {
            Parser kabejaParser=ParserBuilder.createDefaultParser();                       
            kabejaParser.parse(new FileInputStream(dxf),"GBK");
            DXFDocument dd = kabejaParser.getDocument();
            DXFLayer layer=dd.getDXFLayer("0");
            
            //dd.getDXFViewportIterator()
            //String bbs = dd.getBounds().toString();
            System.out.println(layer.toString());            
            double maxx = dd.getBounds().getMaximumX();
            double maxy = dd.getBounds().getMaximumY();
            double minx = dd.getBounds().getMinimumX();
            double miny = dd.getBounds().getMinimumY();                    
            mp.put("min-x", minx);
            mp.put("min-y", miny);
            mp.put("max-x", maxx);
            mp.put("max-y", maxy);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
		return mp;
    }
	
	/**
	 * 坐标转换
	 * @return
	 */
    private static double[] getPointToWKT(double x,double y){
		double[] dstProjec = {0.0000000000,0.0000000000};
		try {			
			String wkt = "PROJCS[\"Beijing_1954_3_Degree_GK_CM_111E\"," 
					+ "  GEOGCS[\"GCS_Beijing_1954\",DATUM[\"D_Beijing_1954\",SPHEROID[\"Krasovsky_1940\",6378245.0,298.3]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199]],"
			        + "  PROJECTION[\"Gauss_Kruger\"],PARAMETER[\"False_Easting\",13900.0],PARAMETER[\"False_Northing\",-4129200.0],PARAMETER[\"Central_Meridian\",112.5],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0],AUTHORITY[\"EPSG\",2434]]";			
			CoordinateReferenceSystem srcCRS = CRS.parseWKT(wkt);				
			MathTransform transform = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84,true);			
			double[] srcProjec = {x,y}; 	        
	        transform.transform(srcProjec, 0, dstProjec, 0, 1);	        
	        return dstProjec;
		} catch (FactoryException e) {
		} catch (TransformException e) {
		}
		return dstProjec;
	}

}
