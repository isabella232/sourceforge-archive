import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class BounceServlet extends HttpServlet
{
    private final static String keyRecord = "BounceServlet";
    static long nTotal;
    static long tStart = System.currentTimeMillis();
    static long iLast = -1;
    static long[] nTotalWhen = new long[64];

    static final long getMinute() {
        return System.currentTimeMillis() / 60000;
    }

    private static final int getMinuteIndex(long minutes) {
        return (int)((64000 + getMinute() -  minutes) % 64);
    }

    final long getTotalWhen(long minutes) {
        return nTotalWhen[getMinuteIndex(minutes)];
    }

    final long getTotalForInterval(long minutes) {
        return nTotal - getTotalWhen(minutes);
    }

    private final class Record {
        HttpSession session;
        int count;
        long tLast;
        long dtCurrent;
        long dtMinimum;
        long dtMaximum;
        long dtAverage;

        Record(HttpSession session) {
            this.session = session;
            session.putValue(keyRecord,this);
            dtMinimum = 999999999;  // a really big value
        }

        void present(PrintWriter out) {
            out.println("<p>Response time (in milliseconds, this station only)");
            out.println("<table>");
            out.println("<tr><th>MINIMUM<td>"  + dtMinimum);
            out.println("<tr><th>AVERAGE<td>"  + dtAverage + "<th>CURRENT<td>" + dtCurrent);
            out.println("<tr><th>MAXIMUM<td>"  + dtMaximum);
            out.println("</table>");

            out.println("<p>Cumulative statistics");
            out.println("<table>");
            out.println("<tr><th>REQUESTS/HOUR<th>REQUESTS");
            out.println("<tr><td>" + (3600000 / dtAverage) + "<td>" + count + "<th>(this station)");

            long n = getTotalForInterval(1);
            out.println("<tr><td>" + (60 * n) + "<td>" + n + "<th>(last 1 minute)");
            n = getTotalForInterval(5);
            out.println("<tr><td>" + (12 * n) + "<td>" + n + "<th>(last 5 minutes)");
            n = getTotalForInterval(60);
            out.println("<tr><td>" + n + "<td>" + n + "<th>(last 60 minutes)");

            out.println("<tr><td>&nbsp;<td>" + nTotal + "<th>(total for server)");
            out.println("</table>");
        }

        void update() {
            long tNow = System.currentTimeMillis();
            if (0 == tLast) {
                tLast = tNow;
                return;
            }
            dtCurrent = tNow - tLast;
            tLast = tNow;
            if (0 == count) {
                count = 1;
                dtMinimum = dtCurrent;
                dtMaximum = dtCurrent;
                dtAverage = dtCurrent;
                return;
            }
            long dtTotal = dtAverage * count + dtCurrent;
            ++count;
            dtAverage = dtTotal / count;
            if (dtCurrent < dtMinimum) {
                dtMinimum = dtCurrent;
            }
            if (dtMaximum < dtCurrent) {
                dtMaximum = dtCurrent;
            }
            int i = getMinuteIndex(0);
            if (i != iLast) {
                iLast = i;
                nTotalWhen[i] = nTotal;
                //System.out.println("set total(" + i + ") to " + nTotal);
            }
        }
    }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException
    {
        HttpSession session = request.getSession(true);
        Record record = (Record) session.getValue(keyRecord);
        if (null == record) {
            record = new Record(session);
        }
        record.update();
        ++nTotal;

        response.setContentType("text/html");
        response.setHeader("Cache-Control","no-cache");
        PrintWriter out = response.getWriter();
        out.println(
            "<html><head>"
            + "<meta http-equiv=refresh content=0>"
            + "<title>Bounce</title></head>"
            + "<body>"
            );
        record.present(out);
        out.println(
            "</body></html>"
            );
    }
}

