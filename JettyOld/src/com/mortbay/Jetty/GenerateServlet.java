package com.mortbay.Jetty;
import com.mortbay.HTML.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GenerateServlet extends HttpServlet
{
    public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws java.io.IOException
    {
        Page page = new Page("Generated HTML");

        page.add(new Heading(1,"Generated HTML Demo"));
        
        List list=new List(List.Unordered);
        list.add("Host = "+request.getRemoteHost());
        list.add("Date = "+new java.util.Date());

        TableForm form = new TableForm("/Dump");
        form.addTextField("Text","Text",25,"");
        form.addSelect("Select","Select",false,1)
            .add("Yes").add("No").add("Maybe");
        form.addColumn(15);
        form.addCheckbox("Check","Check",true);
        form.addButtonArea("Button");
        form.addButton("Button","Dump");
        
        Table table = new Table(2);
        table.newRow();
        table.addCell(new Image("/Images/mbLogoSmall.gif"));
        table.addCell(list).cell().bottom();
        table.newRow();
        table.addCell(form).cell().attribute("COLSPAN","2");
        table.newRow();
        table.newCell().nest(new Block(Block.Pre));
        table.add(new Include("src/com/mortbay/Jetty",
                              "GenerateServlet.java"));
        table.cell().attribute("COLSPAN","2");
        page.add(table);
        
        page.write(response.getWriter());
    }
}
