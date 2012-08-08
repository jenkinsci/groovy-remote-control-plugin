package org.jenkinsci.plugins.groovyremote;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyClassLoader.InnerLoader;
import groovy.lang.Script;
import groovyx.remote.client.CommandGenerator;
import groovyx.remote.client.RemoteControl;
import groovyx.remote.client.Transport;
import groovyx.remote.transport.http.HttpTransport;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class GroovyRemoteBuilder extends Builder {

    private String remoteName;
    private String script;

    @DataBoundConstructor
    public GroovyRemoteBuilder(String remoteName, String script) {
        this.remoteName = remoteName;
        this.script = script;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public String getScript() {
        return script;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public void setScript(String script) {
        this.script = script;
    }

    protected RemoteReceiver getRemote() {
        for (RemoteReceiver remote : ((DescriptorImpl) getDescriptor()).getRemotes()) {
            if (remote.getName().equals(remoteName)) {
                return remote;
            }
        }
        throw new RuntimeException("No such remote receiver. [" + remoteName + "]");
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        BytesCashedGroovyClassLoader cl = new BytesCashedGroovyClassLoader();
        HttpTransport transport = new HttpTransport(getRemote().getUrl());
        RemoteControl remote = new AppRemoteControl(transport, cl);
        Binding binding = new Binding();
        binding.setProperty("out", listener.getLogger());
        binding.setProperty("jenkins", Jenkins.getInstance());
        binding.setProperty("remote", remote);
        try {
            Script s = (Script) cl.parseClass(script).newInstance();
            s.setBinding(binding);
            s.run();
        } catch (CompilationFailedException e) {
            listener.error(e.getMessage());
            return false;
        } catch (InstantiationException e) {
            listener.error(e.getMessage());
            return false;
        } catch (IllegalAccessException e) {
            listener.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {

        @CopyOnWrite
        private List<RemoteReceiver> remotes = new ArrayList<RemoteReceiver>();

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Execute Remote Groovy";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            setRemotes(req.bindJSONToList(RemoteReceiver.class, json.get("remotes")));
            return true;
        }

        public List<RemoteReceiver> getRemotes() {
            return remotes;
        }

        public void setRemotes(List<RemoteReceiver> remotes) {
            this.remotes = remotes;
            save();
        }

        public ListBoxModel doFillRemoteNameItems() {
            ListBoxModel m = new ListBoxModel();
            for (RemoteReceiver remote : remotes) {
                m.add(remote.getName());
            }
            return m;
        }
    }

    private static class AppRemoteControl extends RemoteControl {
        public AppRemoteControl(Transport transport, BytesCashedGroovyClassLoader classLoader) {
            super(transport, new RemoteCommandGenerator(classLoader));
        }
    }

    private static class BytesCashedClassCollector extends GroovyClassLoader.ClassCollector {

        private Map<String, byte[]> cache = new HashMap<String, byte[]>(); 

        protected BytesCashedClassCollector(InnerLoader cl, CompilationUnit unit, SourceUnit su) {
            super(cl, unit, su);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected Class createClass(byte[] code, ClassNode classNode) {
            cache.put(classNode.getName(), code);
            return super.createClass(code, classNode);
        }
    }

    private static class BytesCashedGroovyClassLoader extends GroovyClassLoader {

        private BytesCashedClassCollector classCollector;

        protected ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
            InnerLoader loader = AccessController.doPrivileged(new PrivilegedAction<InnerLoader>() {
                public InnerLoader run() {
                    return new InnerLoader(BytesCashedGroovyClassLoader.this);
                }
            });
            classCollector = new BytesCashedClassCollector(loader, unit, su);
            return classCollector;
        }
    }

    private static class RemoteCommandGenerator extends CommandGenerator {

        private BytesCashedGroovyClassLoader cl;

        public RemoteCommandGenerator(BytesCashedGroovyClassLoader cl) {
            super(cl);
            this.cl = cl;
        }

        protected byte[] getClassBytes(@SuppressWarnings("rawtypes") Class closureClass) {
            byte[] classBytes = cl.classCollector.cache.get(closureClass.getName());
            if (classBytes == null) {
                throw new IllegalStateException("Could not find class file for class [" + closureClass.getName() +"]");
            }
            return classBytes;
        }
    }
}
