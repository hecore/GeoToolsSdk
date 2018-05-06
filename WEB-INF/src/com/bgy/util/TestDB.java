package com.bgy.util;

import java.util.HashMap;
import java.util.Map;

import com.db.pool.ServiceBase;
import com.db.util.DBOper;

public class TestDB {
	public static void main(String[] args) throws Exception {
		ServiceBase sb=new ServiceBase();
		String sql="select loginname,username,workdepartment,lvshudepartment from am_user where gangwei<>'离退休人员' and gangwei<>'离院人员' and gangwei<>'离院人员' order by loginname";
		String[][] arrF = DBOper.getMulColReturnValue(sql);
		String usersql = "insert into yw_user(loginname,username,orgname)values(?,?,?)";
		for (int row=0;row<arrF.length;row++){
			String ln = arrF[row][0];
			String un = arrF[row][1];
			String wd = arrF[row][2];
			String ld = arrF[row][3];
			String orgname = "";
			if (PubFunction.isValidStr(wd) && !"null".equals(wd)){
				orgname = wd;
				System.out.println(ln + "=" + wd);
			}else if (PubFunction.isValidStr(ld)  && !"null".equals(ld)){
				orgname = ld;
				System.out.println(ln + "=" + ld);
			}else{
				sql = "SELECT orgid,orgname from am_org where orgid in(SELECT orgid from am_orguser where orgid<>'root' and loginname='" + ln + "')";
				String[][] arrM = DBOper.getMulColReturnValue(sql);
				if (arrM == null){
					orgname = "root";
					System.out.println(ln + "=root");
				}else{
					if (arrM.length == 1){
						orgname = arrM[0][1];
						System.out.println(ln + "=" + arrM[0][1]);
					}else{
						//属于也无所，有嵌套
						for (int i=0;i<arrM.length;i++){
							String oid = arrM[i][0]; //orgid 多个机构，可能属于下属单位主要是市政所
							sql = "select parentorgid from am_org where orgid='" + oid + "'";
							String pid = DBOper.getOneReturnValue(sql);
							if (PubFunction.isValidStr(pid) && pid.equals("root")){
								orgname = arrM[i][1];
								System.out.println(ln + "=" + arrM[i][1]);
							}
						}
					}
				}
			}
			if (PubFunction.isValidStr(orgname)){
				Map<Integer, Object> pm = new HashMap<Integer, Object>(); //参数
				pm.put(1, ln);
				pm.put(2, un);
				pm.put(3, orgname);
				boolean iskey = true;
				Long ID = sb.insert(usersql, pm, iskey);
				System.out.println(ID);
			}			
		}
		
		/*String sql = "insert into s_my_app(DirName,CurrentTime,Flag,Md5Key)values(?,?,?,?)";		
		Map<Integer, Object> pm = new HashMap<Integer, Object>(); //参数
		pm.put(1, "我的中国心");
		Date dt = new Date();
		pm.put(2, dt.getTime());
		pm.put(3, 1);
		pm.put(4, UUID.randomUUID().toString());		
		boolean iskey = true;
		Long ID = sb.insert(sql, pm, iskey);
		System.out.println(ID);*/				
	}

}
