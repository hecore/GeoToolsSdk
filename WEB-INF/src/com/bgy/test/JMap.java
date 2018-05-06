package com.bgy.test;

import java.io.File;
import java.io.IOException;

import org.geotools.data.FileDataStore;  
import org.geotools.data.FileDataStoreFinder;  
import org.geotools.data.shapefile.ShapefileDataStore;  
import org.geotools.data.simple.SimpleFeatureSource;  
import org.geotools.map.FeatureLayer;  
import org.geotools.map.Layer;  
import org.geotools.map.MapContent;  
import org.geotools.styling.SLD;  
import org.geotools.styling.Style;  
import org.geotools.swing.JMapFrame;  
import org.geotools.swing.data.JFileDataStoreChooser;

public class JMap {
	  public static void main(String[] args)throws IOException {

          // TODO Auto-generated method stub

          File file =JFileDataStoreChooser.showOpenFile("shp", null);

          if(null==file){

                System.out.println("file==null");

          }

         

          FileDataStore fds=FileDataStoreFinder.getDataStore(file);

          SimpleFeatureSource sfs=fds.getFeatureSource();
         

          MapContent map=new MapContent();

          map.setTitle("GtDemoxx");

          Style style=SLD.createSimpleStyle(fds.getSchema());

          Layer lyr = new FeatureLayer(sfs,style);

          map.addLayer(lyr);

          JMapFrame.showMap(map);
          
     }
}
