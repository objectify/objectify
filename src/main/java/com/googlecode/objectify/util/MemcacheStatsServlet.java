/*
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.EntityMemcacheStats;
import com.googlecode.objectify.impl.EntityMemcacheStats.Stat;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Map;

/**
 * <p>If you are using the ObjectifyService static factory, you can mount this servlet to see the
 * memcache stats for an instance. This is nothing fancy, but it should give you an idea of what's
 * going on.</p>
 *
 * @author Jeff Schnitzer
 */
public class MemcacheStatsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private boolean requireAdminAuthentication = true;

    /**
     * If you aren't using ObjectifyService, you can extend the servlet and override this method.
     */
    protected EntityMemcacheStats getMemcacheStats() {
        return ObjectifyService.factory().getMemcacheStats();
    }

    public void init(ServletConfig config) throws ServletException {
        String requireAuthParam = config.getInitParameter("requireAdminAuthentication");
        if (requireAuthParam != null)
            this.requireAdminAuthentication = (("yes".equalsIgnoreCase(requireAuthParam)) || ("true".equalsIgnoreCase(requireAuthParam)));
    }

    private boolean requireAdminAuthentication(UserService userService, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!this.requireAdminAuthentication) {
            return true;
        }

        if (!userService.isUserLoggedIn()) {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
            return false;
        }

        if (!userService.isUserAdmin()) {
            resp.sendError(403);
            return false;
        }

        return true;
    }

    /** */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!this.requireAdminAuthentication(UserServiceFactory.getUserService(), req, resp)) {
            return;
        }

        Map<String, Stat> stats = getMemcacheStats().getStats();

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<html>");
        out.println("  <body>");
        out.println("    <table cellpadding='3' cellspacing='0' border='1'>");    // css? we don't need no stinkin' css
        out.println("      <tr>");
        out.println("        <th>Hits</th><th>Misses</th><th>Percent</th><th>Kind</th>");
        out.println("      </tr>");

        NumberFormat percentFmt = NumberFormat.getPercentInstance();
        percentFmt.setMaximumFractionDigits(2);

        for (Map.Entry<String, Stat> entry : stats.entrySet()) {
            out.println("<tr>");
            out.println("  <td>" + entry.getValue().getHits() + "</td>");
            out.println("  <td>" + entry.getValue().getMisses() + "</td>");
            out.println("  <td>" + percentFmt.format(entry.getValue().getPercent()) + "</td>");
            out.println("  <td>" + entry.getKey() + "</td>");
            out.println("</tr>");
        }

        out.println("    </table>");
        out.println("  </body>");
        out.println("</html>");
    }
}