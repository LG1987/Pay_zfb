package com.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.dao.impl.IpaymentDao;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class zfb_cx
  extends HttpServlet
{
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    request.setCharacterEncoding("UTF-8");
    response.setContentType("text/html;charset=UTF-8");
    String url = "https://openapi.alipay.com/gateway.do";
    String appid = "2017121800947897";
    String ss = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCOoshFL85vZyGB6GrJSi3n64ri6DwrIZJoi+XNRJ7NMZsvD6+rzhNjV18zeN2lR517Xidqy8eDUSG8ciOFru26CSova6wRA18dkNdd+wZCSIELsRQNGVLFCI/mkQpw01XF6ngFYLnC7vbEEC0eS1pc9GqrL8+H1fmLJFbYdejPERN25O/jXkaeY9GkQJ2TyADlBQISsl6EK6VFIKzc3A6Bf6YA9Ny/AA2gGyYvvgBBepb9ZTpDeJBxRYtbh75tYuAIbu7jSaeZjcddRTgKaB7po5Li8Qn0N2Py9iw4Stwa4AD8iPePS7ua9PqJpW2Fd8+NuhjC0Yo7eurH9PF8yDi7AgMBAAECggEAEahDJJV7d70Ln9pWxd5h+nDlO5vGPwyj5D9VTmjtQ7edX2EQvTMAoW6igWu3/Ir2CtyoAoeBhlcX1w1GKnlyQnJ5/27T2TNSpgVVM/QsXS4+k2Tzd3+oCG476i4QXdE7sAmLnBbSSDhu4eyiuTpY0GMNhW2gwk8R2lDAU4GdNWzXYRpxYdTI9/tM6cYqO/R8epwvKDVfRAdPQ1O4OTtMiZTwalkzCQ1kbLpkDQ32JYTUZrMfEzyW88VwB0DI5/2DTw0Gmx5FVs6Xz+1uE4Rd7cNQ3HysO8y8nevxzhDHdktGcnC+s079lJb/HaCdicBy0KIgiYqazwyDAw1NIeUNsQKBgQDH2yFWfWszNJiotd3aAWni/PGZbRkDeHL1iQZDS0Qr0v0Of7QJ4pxzBQR64AQE3YC9yCKot+teHWqLagY7QS5nf/S3Q6RKKKPmYdHxg2/gb+6euH+FRr+ENtgDOkuh8RgsW4P/S2OG3iQeWHZSDr92bZLPgkxlwMxuUACMtCm/GQKBgQC2tJpXqXhuO9+kVnMLs3oXf3vkdBrvJiVRlpXuGSpmPDyq5taxyLeeMv8rD4tu3TVUseINEaLmZYN5d/8HxVs1xypNE3RAiO21e+Zy1MTD0dyhHwEBJPceieWvwp/6zMRN/us/o68gtr0+HfHCsJ9nAcS8l+vHDtGiwfidu6T08wKBgBrznC1TwI8pOqUV0ZNCuYrY3aP5KHRlW+tszSFqFwlp7KJ16kui+cIxHQeNCj21uVTeln32P5Fzmhiu9aQsCBwBD3lfb9poXKIG8w8E0KONtLpG2zuX7N/hnYPh6q0uHDpvJP/apNNdRbWp7Frojj1kTXYadj8KnwcJVumiqjaRAoGAecfJtLKmLjvEZsKkrmDz+WOlDfKB3XO8njRUPT8S7tAp0/RHmHrkQRNW17nlw4C4wnpK5VEjFpUFXFqqZ+CZWjAltvhHnZhncXdjdJjzil6Aw/hpb/zWpsldipicj8wa8K4A2gtdeN21KAlHkvCtXzhjjR1qL+VlwCmwcFqZqhcCgYAVrrrsbKlc5apDT3QqNgux6m25JxcToiihEK6qkS+prIWiWsu87pJ1irlcwpuzEhCeXE7EYhhqWM+kc/QjbktBfLDrTFDEJlFbw3MIqkZhQO4w3BlVsqheaBGVVGm9eSKySl3MpNNb8Nk3hnRB8sfbd7IVla+XSitm0A3Qv9T5TA==";
    String gs = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj693zf5Tmy1RTyodBhFAzVDQi7zKm7dDsTiED7kq2F5a1hEbf80lN69tPjX78u451x3TRvC9KNtPTcMJvUdIQmZBVnhOruZ65/nyBZFEaXfa30iyzbX68HIQulpMI9Eu3MaqiS0/LEp718WyPOPkzZyb6IYjmqFWYFw8UE3upG6O+9v3bd/4dCAYDoJjaFkfRpnjk2sJwZgUmFfZbanWOa/b1HVEtYWBW1GSKhiOWIr5NZqpPGIrMbsmi+P7EBpT6amx+kVXOzNuMUJuoci60lNOycX2BM5A4p/BsIkZwDpBfcMGE3yE4OmIJipbkZTaHGHDFlp4G5RfQeMZz4n+owIDAQAB";
    AlipayClient alipayClient = new DefaultAlipayClient(url, appid, ss, 
      "json", "UTF-8", gs, "RSA2");
    AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
    String out_trade_no = request.getParameter("OrderID");
    alipayRequest.setBizContent("{\"out_trade_no\":\"" + 
      out_trade_no + "\"" + 
      "}");
    String result = "";
    try
    {
      result = ((AlipayTradeQueryResponse)alipayClient.execute(alipayRequest)).getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    //dcavfegfydbvjjj
    JSONObject jsonObject = JSON.parseObject(result);
    JSONObject jsonObject2 = JSON.parseObject(jsonObject.getString("alipay_trade_query_response"));
    if (jsonObject2.getString("code").equals("40004"))
    {
      response.setContentType("text/html;charset=UTF-8");
      response.getWriter().write("1," + jsonObject2.getString("sub_msg"));
      response.getWriter().flush();
      response.getWriter().close();
    }
    else if (jsonObject2.getString("code").equals("10000"))
    {
      IpaymentDao ipaymentDao = new IpaymentDao();
      String JgResult = ipaymentDao.payment(jsonObject2.getString("out_trade_no"), jsonObject2.getString("total_amount"), request.getParameter("UserNo"), request.getParameter("UserName"));
      if (JgResult.equals("0"))
      {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("0," + jsonObject2.getString("out_trade_no") + "&" + jsonObject2.getString("total_amount"));
        response.getWriter().flush();
        response.getWriter().close();
      }
      else
      {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("2," + jsonObject2.getString("out_trade_no") + "&" + jsonObject2.getString("total_amount"));
        response.getWriter().flush();
        response.getWriter().close();
      }
    }
  }
  
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    request.setCharacterEncoding("UTF-8");
    response.setContentType("text/html;charset=UTF-8");
    String url = "https://openapi.alipay.com/gateway.do";
    String appid = "2017121800947897";
    String ss = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCOoshFL85vZyGB6GrJSi3n64ri6DwrIZJoi+XNRJ7NMZsvD6+rzhNjV18zeN2lR517Xidqy8eDUSG8ciOFru26CSova6wRA18dkNdd+wZCSIELsRQNGVLFCI/mkQpw01XF6ngFYLnC7vbEEC0eS1pc9GqrL8+H1fmLJFbYdejPERN25O/jXkaeY9GkQJ2TyADlBQISsl6EK6VFIKzc3A6Bf6YA9Ny/AA2gGyYvvgBBepb9ZTpDeJBxRYtbh75tYuAIbu7jSaeZjcddRTgKaB7po5Li8Qn0N2Py9iw4Stwa4AD8iPePS7ua9PqJpW2Fd8+NuhjC0Yo7eurH9PF8yDi7AgMBAAECggEAEahDJJV7d70Ln9pWxd5h+nDlO5vGPwyj5D9VTmjtQ7edX2EQvTMAoW6igWu3/Ir2CtyoAoeBhlcX1w1GKnlyQnJ5/27T2TNSpgVVM/QsXS4+k2Tzd3+oCG476i4QXdE7sAmLnBbSSDhu4eyiuTpY0GMNhW2gwk8R2lDAU4GdNWzXYRpxYdTI9/tM6cYqO/R8epwvKDVfRAdPQ1O4OTtMiZTwalkzCQ1kbLpkDQ32JYTUZrMfEzyW88VwB0DI5/2DTw0Gmx5FVs6Xz+1uE4Rd7cNQ3HysO8y8nevxzhDHdktGcnC+s079lJb/HaCdicBy0KIgiYqazwyDAw1NIeUNsQKBgQDH2yFWfWszNJiotd3aAWni/PGZbRkDeHL1iQZDS0Qr0v0Of7QJ4pxzBQR64AQE3YC9yCKot+teHWqLagY7QS5nf/S3Q6RKKKPmYdHxg2/gb+6euH+FRr+ENtgDOkuh8RgsW4P/S2OG3iQeWHZSDr92bZLPgkxlwMxuUACMtCm/GQKBgQC2tJpXqXhuO9+kVnMLs3oXf3vkdBrvJiVRlpXuGSpmPDyq5taxyLeeMv8rD4tu3TVUseINEaLmZYN5d/8HxVs1xypNE3RAiO21e+Zy1MTD0dyhHwEBJPceieWvwp/6zMRN/us/o68gtr0+HfHCsJ9nAcS8l+vHDtGiwfidu6T08wKBgBrznC1TwI8pOqUV0ZNCuYrY3aP5KHRlW+tszSFqFwlp7KJ16kui+cIxHQeNCj21uVTeln32P5Fzmhiu9aQsCBwBD3lfb9poXKIG8w8E0KONtLpG2zuX7N/hnYPh6q0uHDpvJP/apNNdRbWp7Frojj1kTXYadj8KnwcJVumiqjaRAoGAecfJtLKmLjvEZsKkrmDz+WOlDfKB3XO8njRUPT8S7tAp0/RHmHrkQRNW17nlw4C4wnpK5VEjFpUFXFqqZ+CZWjAltvhHnZhncXdjdJjzil6Aw/hpb/zWpsldipicj8wa8K4A2gtdeN21KAlHkvCtXzhjjR1qL+VlwCmwcFqZqhcCgYAVrrrsbKlc5apDT3QqNgux6m25JxcToiihEK6qkS+prIWiWsu87pJ1irlcwpuzEhCeXE7EYhhqWM+kc/QjbktBfLDrTFDEJlFbw3MIqkZhQO4w3BlVsqheaBGVVGm9eSKySl3MpNNb8Nk3hnRB8sfbd7IVla+XSitm0A3Qv9T5TA==";
    String gs = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj693zf5Tmy1RTyodBhFAzVDQi7zKm7dDsTiED7kq2F5a1hEbf80lN69tPjX78u451x3TRvC9KNtPTcMJvUdIQmZBVnhOruZ65/nyBZFEaXfa30iyzbX68HIQulpMI9Eu3MaqiS0/LEp718WyPOPkzZyb6IYjmqFWYFw8UE3upG6O+9v3bd/4dCAYDoJjaFkfRpnjk2sJwZgUmFfZbanWOa/b1HVEtYWBW1GSKhiOWIr5NZqpPGIrMbsmi+P7EBpT6amx+kVXOzNuMUJuoci60lNOycX2BM5A4p/BsIkZwDpBfcMGE3yE4OmIJipbkZTaHGHDFlp4G5RfQeMZz4n+owIDAQAB";
    AlipayClient alipayClient = new DefaultAlipayClient(url, appid, ss, 
      "json", "UTF-8", gs, "RSA2");
    AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
    String out_trade_no = request.getParameter("OrderID");
    alipayRequest.setBizContent("{\"out_trade_no\":\"" + 
      out_trade_no + "\"" + 
      "}");
    String result = "";
    try
    {
      result = ((AlipayTradeQueryResponse)alipayClient.execute(alipayRequest)).getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    JSONObject jsonObject = JSON.parseObject(result);
    JSONObject jsonObject2 = JSON.parseObject(jsonObject.getString("alipay_trade_query_response"));
    if (jsonObject2.getString("code").equals("40004"))
    {
      response.setContentType("text/html;charset=UTF-8");
      response.getWriter().write(jsonObject2.getString("sub_code"));
      response.getWriter().flush();
      response.getWriter().close();
    }
    else if (jsonObject2.getString("code").equals("10000"))
    {
      response.setContentType("text/html;charset=UTF-8");
      if (jsonObject2.getString("trade_status").equalsIgnoreCase("TRADE_SUCCESS")) {
        response.getWriter().write(jsonObject2.getString("total_amount") + "&" + jsonObject2.getString("trade_status"));
      } else {
        response.getWriter().write(jsonObject2.getString("trade_status"));
      }
      response.getWriter().flush();
      response.getWriter().close();
    }
  }
}
