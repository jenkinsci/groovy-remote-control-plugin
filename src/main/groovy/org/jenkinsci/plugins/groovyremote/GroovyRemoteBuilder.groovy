package org.jenkinsci.plugins.groovyremote;


import groovyx.remote.client.CommandGenerator
import groovyx.remote.client.RemoteControl
import groovyx.remote.client.Transport
import groovyx.remote.transport.http.HttpTransport
import hudson.CopyOnWrite
import hudson.Extension
import hudson.Launcher
import hudson.ProxyConfiguration
import hudson.model.AbstractBuild
import hudson.model.BuildListener
import hudson.model.Descriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import java.security.AccessController
import java.security.PrivilegedAction
import jenkins.model.Jenkins
import net.sf.json.JSONObject
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.SourceUnit
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.StaplerRequest

public class GroovyRemoteBuilder extends Builder {

    String remoteName;
    String script;

    @DataBoundConstructor
    public GroovyRemoteBuilder(String remoteName, String script) {
        this.remoteName = remoteName;
        this.script = script;
    }

    protected RemoteReceiver getRemote() {
        def remote = getDescriptor().remotes.find { it.name == remoteName }
        if (!remote) {
            throw new RuntimeException("No such remote receiver. [" + remoteName + "]")
        }
        remote
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        def cl = new BytesCashedGroovyClassLoader()
        def remote = new AppRemoteControl(new AppTransport(getRemote()), cl)
        try {
            def s = cl.parseClass(script).newInstance()
            s.binding = new Binding(out:listener.logger, jenkins:Jenkins.instance, remote:remote)
            s.run()
        } catch (e) {
            e.printStackTrace(listener.logger)
            return false
        }
        true
    }

    @Extension
    static final class DescriptorImpl extends Descriptor<Builder> {

        @CopyOnWrite
        private List<RemoteReceiver> remotes = new ArrayList<RemoteReceiver>();

        public DescriptorImpl() {
            load()
        }

        @Override
        public String getDisplayName() {
            "Execute Remote Groovy"
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            setRemotes(req.bindJSONToList(RemoteReceiver, json.get("remotes")))
            true
        }

        public List<RemoteReceiver> getRemotes() {
            remotes
        }

        public void setRemotes(List<RemoteReceiver> remotes) {
            this.remotes = remotes
            save()
        }

        public ListBoxModel doFillRemoteNameItems() {
            def model = new ListBoxModel()
            model.add("")
            remotes.inject(model) { m, r ->
                m.add r.name
            }
        }

        public FormValidation doCheckRemoteName(@QueryParameter String value) {
            FormValidation.validateRequired(value)
        }
    }

    static class AppTransport extends HttpTransport {

        def receiver

        public AppTransport(RemoteReceiver receiver) {
            super(receiver.url)
            this.receiver = receiver
        }

        @Override
        protected HttpURLConnection openConnection() {
            try {
                ProxyConfiguration.open(new URL(receiver.url))
            } catch (IOException e) {
                throw new RuntimeException(e)
            }
        }

        @Override
        protected Object configureConnection(HttpURLConnection connection) {
            receiver.headers?.each {
                connection.addRequestProperty(it.key, it.value)
            }
            connection
        }
    }

    static class AppRemoteControl extends RemoteControl {
        public AppRemoteControl(Transport transport, BytesCashedGroovyClassLoader classLoader) {
            super(transport, new RemoteCommandGenerator(classLoader))
        }
    }

    static class BytesCashedClassCollector extends GroovyClassLoader.ClassCollector {

        def cache = [:]

        protected BytesCashedClassCollector(GroovyClassLoader.InnerLoader cl, CompilationUnit unit, SourceUnit su) {
            super(cl, unit, su)
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected Class createClass(byte[] code, ClassNode classNode) {
            cache[classNode.name] = code
            return super.createClass(code, classNode)
        }
    }

    private static class BytesCashedGroovyClassLoader extends GroovyClassLoader {

        def classCollector;

        protected GroovyClassLoader.ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
            GroovyClassLoader.InnerLoader loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader.InnerLoader>() {
                public GroovyClassLoader.InnerLoader run() {
                    return new GroovyClassLoader.InnerLoader(BytesCashedGroovyClassLoader.this)
                }
            });
            classCollector = new BytesCashedClassCollector(loader, unit, su)
        }
    }

    private static class RemoteCommandGenerator extends CommandGenerator {

        public RemoteCommandGenerator(BytesCashedGroovyClassLoader cl) {
            super(cl)
        }

        protected byte[] getClassBytes(@SuppressWarnings("rawtypes") Class closureClass) {
            def classBytes = classLoader.classCollector.cache[closureClass.name]
            if (classBytes == null) {
                throw new IllegalStateException("Could not find class file for class [${closureClass.name}]");
            }
            classBytes
        }


    }
}
