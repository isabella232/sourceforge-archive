package com.mortbay.Jetty;
import com.mortbay.HTTP.*;
import com.mortbay.HTML.*;
import java.io.*;
import javax.servlet.http.*;

public class UploadServlet extends HttpServlet
{
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
         throws java.io.IOException
    {
        Page page = Page.getPage(Page.getDefaultPageType(),
                                 request,response);
        
        page.title("Upload File Demo");
        page.add(new Heading(1,"Upload File Demo"));
        
        MultiPartRequest mpr = new MultiPartRequest(request);
        if (mpr.contains("TF"))
        {
            String filename = mpr.getFilename("TF");
            LineNumberReader in = new LineNumberReader(
                new InputStreamReader(mpr.getInputStream("TF"),"UTF8"));
            int lines=0;
            while (in.readLine()!=null)
                lines++;
            
            page.add("Filename \"<FONT FACE=courier>"+filename+
                     "</FONT>\" has "+lines+" lines<P>");
        }
        else
            page.add("No file uploaded.");
        
        page.write(response.getWriter());
    }
};
