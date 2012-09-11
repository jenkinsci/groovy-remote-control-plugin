package org.jenkinsci.plugins.groovyremote;


import groovyx.remote.server.Receiver
import groovyx.remote.transport.http.ContentType
import hudson.Plugin
import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse
import jenkins.model.Jenkins
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

public class GroovyRemotePlugin extends Plugin {

    def receiver

    @Override
    public void start() throws Exception {
        receiver = new Receiver(Jenkins.instance.pluginManager.uberClassLoader, [jenkins:Jenkins.instance])
    }

    public void doIndex(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
        if (validateRequest(req, res)) {
            configureSuccessfulResponse(res)
            execute(req.inputStream, res.outputStream)
        }
    }

    protected boolean validateRequest(StaplerRequest req, StaplerResponse res) throws IOException {
        if (req.method != "POST") {
            res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "request must be a POST")
            return false
        }
        if (ContentType.COMMAND.value != req.contentType) {
            res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only remote control commands can be sent")
            return false
        }
        true
    }

    protected void configureSuccessfulResponse(StaplerResponse res) {
        res.contentType = ContentType.RESULT.value
    }

    protected void execute(InputStream input, OutputStream output) {
        receiver.execute(input, output)
    }
}
