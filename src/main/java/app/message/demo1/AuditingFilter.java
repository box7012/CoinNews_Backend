package app.message.demo1;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class AuditingFilter extends GenericFilter{

    private final static Log log = LogFactory.getLog(AuditingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        long start = new Date().getTime();
        chain.doFilter(req, res);
        long elapsed = new Date().getTime();
        HttpServletRequest request = (HttpServletRequest) req;
        log.debug("Request[uri=" + request.getRequestURI() + ", method =" + request.getMethod() + "] completed in " + elapsed + "ms");
    }

}
