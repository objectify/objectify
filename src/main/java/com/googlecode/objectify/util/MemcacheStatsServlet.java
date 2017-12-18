/*
 */

package com.googlecode.objectify.util;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.EntityMemcacheStats;
import com.googlecode.objectify.impl.EntityMemcacheStats.Stat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Map;

/**
 * <p>You can mount this servlet to see the memcache stats for an instance. This is nothing fancy,
 * but it should give you an idea of what's going on.</p>
 *  
 * @author Jeff Schnitzer
 */
public class MemcacheStatsServlet extends HttpServlet
{
	private static final long serialVersionUID = -5254845239323180573L;

	/**
	 * You can extend the servlet and override this method if you are doing something unusual with the factory.
	 */
	protected EntityMemcacheStats getMemcacheStats() {
		return ObjectifyService.factory().getMemcacheStats();
	}
	
	/** */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final Map<String, Stat> stats = getMemcacheStats().getStats();
		
		resp.setContentType("text/html");
		final PrintWriter out = resp.getWriter();
		
		out.println("<html>");
		out.println("  <body>");
		out.println("    <table cellpadding='3' cellspacing='0' border='1'>");	// css? we don't need no stinkin' css
		out.println("      <tr>");
		out.println("        <th>Hits</th><th>Misses</th><th>Percent</th><th>Kind</th>");
		out.println("      </tr>");

		final NumberFormat percentFmt = NumberFormat.getPercentInstance();
		percentFmt.setMaximumFractionDigits(2);
		
		for (final Map.Entry<String, Stat> entry: stats.entrySet()) {
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