import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class HelloWorldServlet extends HttpServlet
{

    public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException
    {
        response.setContentType("text/html");
        ServletOutputStream os = response.getOutputStream();
        os.println(
            "<html>"
            + "<head><title>Hello World</title></head>"
            + "<body>"
            + "<h1>Hello World</h1>"
            + "</body></html>"
            );
    }

    public String getServletInfo() {
        return "Create a page that says <i>Hello World</i> and send it back";
    }

    public HelloWorldServlet() {
    }
}
