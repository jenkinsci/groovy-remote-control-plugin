package org.jenkinsci.plugins.groovyremote;

import groovyx.remote.server.Receiver;
import groovyx.remote.transport.http.ContentType;
import hudson.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class GroovyRemotePlugin extends Plugin {

    private Receiver receiver;

    @Override
    public void start() throws Exception {
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("jenkins", Jenkins.getInstance());
        receiver = new Receiver(Jenkins.getInstance().getPluginManager().uberClassLoader, ctx);
    }

    public void doIndex(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
        if (validateRequest(req, res)) {
            configureSuccessfulResponse(res);
            execute(req.getInputStream(), res.getOutputStream());
        }
    }

    protected boolean validateRequest(StaplerRequest req, StaplerResponse res) throws IOException {
        if (!req.getMethod().equals("POST")) {
            res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "request must be a POST");
            return false;
        }
        if (!ContentType.COMMAND.getValue().equals(req.getContentType())) {
            res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only remote control commands can be sent");
            return false;
        }
        return true;
    }

    protected void configureSuccessfulResponse(StaplerResponse res) {
        res.setContentType((String) ContentType.RESULT.getValue());
    }

    protected void execute(InputStream input, OutputStream output) {
        receiver.execute(input, output);
    }
}
