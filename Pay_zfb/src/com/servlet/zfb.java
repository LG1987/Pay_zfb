package com.servlet;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class zfb
  extends HttpServlet
{
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    request.setCharacterEncoding("utf-8");
    response.setContentType("text/html;charset=utf-8");
    String out_trade_no = request.getParameter("HistoryID");
    String total_amount = request.getParameter("UnitPrice");
    String url = "https://openapi.alipay.com/gateway.do";
    String appid = "2017121800947897";
    String ss = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCOoshFL85vZyGB6GrJSi3n64ri6DwrIZJoi+XNRJ7NMZsvD6+rzhNjV18zeN2lR517Xidqy8eDUSG8ciOFru26CSova6wRA18dkNdd+wZCSIELsRQNGVLFCI/mkQpw01XF6ngFYLnC7vbEEC0eS1pc9GqrL8+H1fmLJFbYdejPERN25O/jXkaeY9GkQJ2TyADlBQISsl6EK6VFIKzc3A6Bf6YA9Ny/AA2gGyYvvgBBepb9ZTpDeJBxRYtbh75tYuAIbu7jSaeZjcddRTgKaB7po5Li8Qn0N2Py9iw4Stwa4AD8iPePS7ua9PqJpW2Fd8+NuhjC0Yo7eurH9PF8yDi7AgMBAAECggEAEahDJJV7d70Ln9pWxd5h+nDlO5vGPwyj5D9VTmjtQ7edX2EQvTMAoW6igWu3/Ir2CtyoAoeBhlcX1w1GKnlyQnJ5/27T2TNSpgVVM/QsXS4+k2Tzd3+oCG476i4QXdE7sAmLnBbSSDhu4eyiuTpY0GMNhW2gwk8R2lDAU4GdNWzXYRpxYdTI9/tM6cYqO/R8epwvKDVfRAdPQ1O4OTtMiZTwalkzCQ1kbLpkDQ32JYTUZrMfEzyW88VwB0DI5/2DTw0Gmx5FVs6Xz+1uE4Rd7cNQ3HysO8y8nevxzhDHdktGcnC+s079lJb/HaCdicBy0KIgiYqazwyDAw1NIeUNsQKBgQDH2yFWfWszNJiotd3aAWni/PGZbRkDeHL1iQZDS0Qr0v0Of7QJ4pxzBQR64AQE3YC9yCKot+teHWqLagY7QS5nf/S3Q6RKKKPmYdHxg2/gb+6euH+FRr+ENtgDOkuh8RgsW4P/S2OG3iQeWHZSDr92bZLPgkxlwMxuUACMtCm/GQKBgQC2tJpXqXhuO9+kVnMLs3oXf3vkdBrvJiVRlpXuGSpmPDyq5taxyLeeMv8rD4tu3TVUseINEaLmZYN5d/8HxVs1xypNE3RAiO21e+Zy1MTD0dyhHwEBJPceieWvwp/6zMRN/us/o68gtr0+HfHCsJ9nAcS8l+vHDtGiwfidu6T08wKBgBrznC1TwI8pOqUV0ZNCuYrY3aP5KHRlW+tszSFqFwlp7KJ16kui+cIxHQeNCj21uVTeln32P5Fzmhiu9aQsCBwBD3lfb9poXKIG8w8E0KONtLpG2zuX7N/hnYPh6q0uHDpvJP/apNNdRbWp7Frojj1kTXYadj8KnwcJVumiqjaRAoGAecfJtLKmLjvEZsKkrmDz+WOlDfKB3XO8njRUPT8S7tAp0/RHmHrkQRNW17nlw4C4wnpK5VEjFpUFXFqqZ+CZWjAltvhHnZhncXdjdJjzil6Aw/hpb/zWpsldipicj8wa8K4A2gtdeN21KAlHkvCtXzhjjR1qL+VlwCmwcFqZqhcCgYAVrrrsbKlc5apDT3QqNgux6m25JxcToiihEK6qkS+prIWiWsu87pJ1irlcwpuzEhCeXE7EYhhqWM+kc/QjbktBfLDrTFDEJlFbw3MIqkZhQO4w3BlVsqheaBGVVGm9eSKySl3MpNNb8Nk3hnRB8sfbd7IVla+XSitm0A3Qv9T5TA==";
    String gs = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjqLIRS/Ob2chgehqyUot5+uK4ug8KyGSaIvlzUSezTGbLw+vq84TY1dfM3jdpUede14nasvHg1EhvHIjha7tugkqL2usEQNfHZDXXfsGQkiBC7EUDRlSxQiP5pEKcNNVxep4BWC5wu72xBAtHktaXPRqqy/Ph9X5iyRW2HXozxETduTv415GnmPRpECdk8gA5QUCErJehCulRSCs3NwOgX+mAPTcvwANoBsmL74AQXqW/WU6Q3iQcUWLW4e+bWLgCG7u40mnmY3HXUU4Cmge6aOS4vEJ9Ddj8vYsOErcGuAA/Ij3j0u7mvT6iaVthXfPjboYwtGKO3rqx/TxfMg4uwIDAQAB";
    AlipayClient alipayClient = new DefaultAlipayClient(url, appid, ss, 
      "json", "utf-8", gs, "RSA2");
    AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
    alipayRequest.setReturnUrl("http://shop.doright.com.cn:51886/ShopPing_Malls/TrReZfbPay.jsp?HistoryID=" + out_trade_no);
    alipayRequest.setNotifyUrl("http://shop.doright.com.cn:51886/ShopPing_Malls/TrReZfbPay.jsp?HistoryID=" + out_trade_no);
    String subject = "加盟补货";
    String body = "加盟商城补货,订单号:" + out_trade_no;
    String passback_params = URLEncoder.encode("jiamengbuhuo&shuliang=10", "UTF-8");
    alipayRequest
      .setBizContent("{    \"out_trade_no\":\"" + 
      out_trade_no + "\"," + 
      "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," + 
      "    \"total_amount\":" + total_amount + "," + 
      "    \"subject\":\"" + subject + "\"," + 
      "    \"body\":\"" + body + "\"," + 
      "    \"passback_params\":\"" + passback_params + "\"," + 
      "    \"extend_params\":{" + 
      "    \"sys_service_provider_id\":\"2088821713723172\"" + 
      "    }" + "  }");
    String form = "";
    try
    {
      form = ((AlipayTradePagePayResponse)alipayClient.pageExecute(alipayRequest)).getBody();
    }
    catch (AlipayApiException e)
    {
      e.printStackTrace();
    }
    response.setContentType("text/html;charset=UTF-8");
    response.getWriter().write(form);
    response.getWriter().flush();
    response.getWriter().close();
  }
  
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    request.setCharacterEncoding("utf-8");
    response.setContentType("text/html;charset=utf-8");
    String out_trade_no = request.getParameter("HistoryID");
    String total_amount = request.getParameter("UnitPrice");
    String url = "https://openapi.alipay.com/gateway.do";
    String appid = "2017121800947897";
    String ss = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCOoshFL85vZyGB6GrJSi3n64ri6DwrIZJoi+XNRJ7NMZsvD6+rzhNjV18zeN2lR517Xidqy8eDUSG8ciOFru26CSova6wRA18dkNdd+wZCSIELsRQNGVLFCI/mkQpw01XF6ngFYLnC7vbEEC0eS1pc9GqrL8+H1fmLJFbYdejPERN25O/jXkaeY9GkQJ2TyADlBQISsl6EK6VFIKzc3A6Bf6YA9Ny/AA2gGyYvvgBBepb9ZTpDeJBxRYtbh75tYuAIbu7jSaeZjcddRTgKaB7po5Li8Qn0N2Py9iw4Stwa4AD8iPePS7ua9PqJpW2Fd8+NuhjC0Yo7eurH9PF8yDi7AgMBAAECggEAEahDJJV7d70Ln9pWxd5h+nDlO5vGPwyj5D9VTmjtQ7edX2EQvTMAoW6igWu3/Ir2CtyoAoeBhlcX1w1GKnlyQnJ5/27T2TNSpgVVM/QsXS4+k2Tzd3+oCG476i4QXdE7sAmLnBbSSDhu4eyiuTpY0GMNhW2gwk8R2lDAU4GdNWzXYRpxYdTI9/tM6cYqO/R8epwvKDVfRAdPQ1O4OTtMiZTwalkzCQ1kbLpkDQ32JYTUZrMfEzyW88VwB0DI5/2DTw0Gmx5FVs6Xz+1uE4Rd7cNQ3HysO8y8nevxzhDHdktGcnC+s079lJb/HaCdicBy0KIgiYqazwyDAw1NIeUNsQKBgQDH2yFWfWszNJiotd3aAWni/PGZbRkDeHL1iQZDS0Qr0v0Of7QJ4pxzBQR64AQE3YC9yCKot+teHWqLagY7QS5nf/S3Q6RKKKPmYdHxg2/gb+6euH+FRr+ENtgDOkuh8RgsW4P/S2OG3iQeWHZSDr92bZLPgkxlwMxuUACMtCm/GQKBgQC2tJpXqXhuO9+kVnMLs3oXf3vkdBrvJiVRlpXuGSpmPDyq5taxyLeeMv8rD4tu3TVUseINEaLmZYN5d/8HxVs1xypNE3RAiO21e+Zy1MTD0dyhHwEBJPceieWvwp/6zMRN/us/o68gtr0+HfHCsJ9nAcS8l+vHDtGiwfidu6T08wKBgBrznC1TwI8pOqUV0ZNCuYrY3aP5KHRlW+tszSFqFwlp7KJ16kui+cIxHQeNCj21uVTeln32P5Fzmhiu9aQsCBwBD3lfb9poXKIG8w8E0KONtLpG2zuX7N/hnYPh6q0uHDpvJP/apNNdRbWp7Frojj1kTXYadj8KnwcJVumiqjaRAoGAecfJtLKmLjvEZsKkrmDz+WOlDfKB3XO8njRUPT8S7tAp0/RHmHrkQRNW17nlw4C4wnpK5VEjFpUFXFqqZ+CZWjAltvhHnZhncXdjdJjzil6Aw/hpb/zWpsldipicj8wa8K4A2gtdeN21KAlHkvCtXzhjjR1qL+VlwCmwcFqZqhcCgYAVrrrsbKlc5apDT3QqNgux6m25JxcToiihEK6qkS+prIWiWsu87pJ1irlcwpuzEhCeXE7EYhhqWM+kc/QjbktBfLDrTFDEJlFbw3MIqkZhQO4w3BlVsqheaBGVVGm9eSKySl3MpNNb8Nk3hnRB8sfbd7IVla+XSitm0A3Qv9T5TA==";
    String gs = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjqLIRS/Ob2chgehqyUot5+uK4ug8KyGSaIvlzUSezTGbLw+vq84TY1dfM3jdpUede14nasvHg1EhvHIjha7tugkqL2usEQNfHZDXXfsGQkiBC7EUDRlSxQiP5pEKcNNVxep4BWC5wu72xBAtHktaXPRqqy/Ph9X5iyRW2HXozxETduTv415GnmPRpECdk8gA5QUCErJehCulRSCs3NwOgX+mAPTcvwANoBsmL74AQXqW/WU6Q3iQcUWLW4e+bWLgCG7u40mnmY3HXUU4Cmge6aOS4vEJ9Ddj8vYsOErcGuAA/Ij3j0u7mvT6iaVthXfPjboYwtGKO3rqx/TxfMg4uwIDAQAB";
    AlipayClient alipayClient = new DefaultAlipayClient(url, appid, ss, 
      "json", "utf-8", gs, "RSA2");
    AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
    alipayRequest.setReturnUrl("http://shop.doright.com.cn:51886/ShopPing_Malls/TrReZfbPay.jsp");
    alipayRequest.setNotifyUrl("http://shop.doright.com.cn:51886/ShopPing_Malls/TrReZfbPay.jsp");
    String subject = "加盟补货";
    String body = "加盟商城补货,订单号:" + out_trade_no;
    String passback_params = URLEncoder.encode("jiamengbuhuo&shuliang=10", "UTF-8");
    alipayRequest
      .setBizContent("{    \"out_trade_no\":\"" + 
      out_trade_no + "\"," + 
      "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," + 
      "    \"total_amount\":" + total_amount + "," + 
      "    \"subject\":\"" + subject + "\"," + 
      "    \"body\":\"" + body + "\"," + 
      "    \"passback_params\":\"" + passback_params + "\"," + 
      "\t   \"timeout_express\":\"15m\"," + 
      "    \"extend_params\":{" + 
      "    \"sys_service_provider_id\":\"2088821713723172\"" + 
      "    }" + "  }");
    String form = "";
    try
    {
      form = ((AlipayTradePagePayResponse)alipayClient.pageExecute(alipayRequest)).getBody();
    }
    catch (AlipayApiException e)
    {
      e.printStackTrace();
    }
    response.setContentType("text/html;charset=UTF-8");
    response.getWriter().write(form);
    response.getWriter().flush();
    response.getWriter().close();
  }
}
