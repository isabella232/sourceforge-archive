package com.mortbay.Jetty;
import com.mortbay.HTML.*;
import javax.servlet.*;

public class GenerateServlet extends GenericServlet
{
    public void service(ServletRequest request, ServletResponse response)
	 throws java.io.IOException
    {
	Page page = new Page("Generated HTML");

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
			      "GenerateServlet.java"));
	table.cell().attribute("COLSPAN","2");
	page.add(table);
	
	page.write(response.getOutputStream());
    }
}
