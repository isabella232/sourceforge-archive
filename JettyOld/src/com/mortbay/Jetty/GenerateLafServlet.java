package com.mortbay.Jetty;
import com.mortbay.HTML.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GenerateLafServlet extends HttpServlet
{
    public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws java.io.IOException
    {
        Page page = Page.getPage(Page.getDefaultPageType(),
                                 request,response);
        
        page.title("Generated HTML");

        page.add(new Heading(1,"Generated HTML Demo"));
        
        List list=new List(List.Unordered);
        list.add("Host = "+request.getRemoteHost());
        list.add("Date = "+new java.util.Date());

        Form form = new Form("/Dump");
        form.add(new Input(Input.Text,"Text").size(60));
        
        Table table = new Table(2);
        table.newRow();
        table.addCell(new Image("/Images/mbLogoSmall.gif"));
        table.addCell(list).cell().bottom();
        table.newRow();
        table.addCell(form).cell().attribute("COLSPAN","2");
        table.newRow();
        table.newCell().nest(new Block(Block.Pre));
        table.add(new Include("src/com/mortbay/Jetty",
                              "GenerateLafServlet.java"));
        table.cell().attribute("COLSPAN","2");
        page.add(table);
        
        page.write(response.getWriter());
    }
}
