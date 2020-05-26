package com.dao.impl;

import com.dao.paymentDao;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IpaymentDao
  implements paymentDao
{
  public String payment(String OrdersID, String payment, String userno, String username)
  {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    PreparedStatement ps1 = null;
    ResultSet rs1 = null;
    PreparedStatement HistPs = null;
    ResultSet HistRs = null;
    Statement stmt = null;
    try
    {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      conn = DriverManager.getConnection(
        "jdbc:sqlserver://192.168.6.202:9999;databaseName=doright", 
        "doright", "doright001");
      if (conn == null) {
        System.out.println("链接失败！");
      }
      String sql1 = "select * from TblGetAdvance where Memory = ?";
      ps1 = conn.prepareStatement(sql1);
      ps1.setString(1, OrdersID);
      rs1 = ps1.executeQuery();
      if (!rs1.next())
      {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dfFormat = new SimpleDateFormat("yyMMdd");
        String sql = "select max(DID)DID from TblGetAdvance where DID like '" + dfFormat.format(date) + "%'";
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        String ID = "";
        while (rs.next())
        {
          String did = rs.getString("DID");
          if (did == null)
          {
            ID = dfFormat.format(date) + "0001";
          }
          else
          {
            int sum = Integer.valueOf(did.trim()).intValue() + 1;
            ID = String.valueOf(sum);
          }
        }
        String HistSql = "select B.UserNo,B.UserName from Java_Order_History A\tinner join dorightl.dbo.[Right] B on A.userno = B.userno\twhere HistoryID = ?";
        

        HistPs = conn.prepareStatement(HistSql);
        HistPs.setString(1, OrdersID);
        HistRs = HistPs.executeQuery();
        while (HistRs.next())
        {
          userno = HistRs.getString("UserNo").trim();
          username = HistRs.getString("UserName").trim();
        }
        String Insert_Sql = "Insert Into TblGetAdvance(DID,ClientID,ClientName,SumMoney,CreateUserID,CreateUserName,CreateDate,OperatorID,OperatorName,auditingflag,auditinguserid,auditingusername,auditingdate,flag,memory)Values ('" + 
          ID + "','" + userno + "','" + username + "','" + payment + "','" + userno + "','系统','" + df1.format(date) + "','" + userno + "','系统','1','" + userno + "','系统','" + df.format(date) + "','0','" + OrdersID + "')";
        
        conn.setAutoCommit(false);
        stmt = conn.createStatement();
        stmt.executeUpdate(Insert_Sql);
        
        conn.commit();
        conn.close();
        return "0";
      }
      if (rs1 != null) {
        rs1.close();
      }
      if (ps1 != null) {
        ps1.close();
      }
      return "1";
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (conn != null) {
        try
        {
          conn.close();
        }
        catch (Exception e2)
        {
          e2.printStackTrace();
        }
      }
      if (rs != null) {
        try
        {
          rs.close();
        }
        catch (SQLException e)
        {
          e.printStackTrace();
        }
      }
      if (ps != null) {
        try
        {
          ps.close();
        }
        catch (SQLException e)
        {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
}
