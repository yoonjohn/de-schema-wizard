package com.deleidos.sw;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class HelpRedirect extends HttpServlet{
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException {
      response.setStatus(response.SC_MOVED_TEMPORARILY);
      response.setHeader("Location", "assets/help/Default.htm");
    } // doGet
} // HelpRedirect
