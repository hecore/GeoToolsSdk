package com.bgy.hecore.sde;

import com.vividsolutions.jts.geom.Geometry;  
import com.vividsolutions.jts.operation.buffer.BufferOp;  
  
public class Buffers {  
  
    private GeometryFactory factory = GeometryFactory.getInstance();  
  
    public Geometry buildGeo(String str) {  
        return factory.buildGeo(str);  
    }  
  
    public static void main(String[] args) {  
        Buffers bs = new Buffers();  
        String line1 = "LINESTRING (0 0, 1 1, 2 2,3 3)";  
        Geometry g1 = bs.buildGeo(line1);  
        //方式(一)  
        Geometry g = g1.buffer(2);  
  
        ////方式(二) BufferOP  
        BufferOp bufOp = new BufferOp(g1);  
        bufOp.setEndCapStyle(BufferOp.CAP_BUTT);  
        Geometry bg = bufOp.getResultGeometry(2);  
    }  
    
    
}  
