package org.jenkinsci.plugins.groovyremote;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class RemoteReceiver extends AbstractDescribableImpl<RemoteReceiver> {

    private String name;

    private String url;

    @DataBoundConstructor
    public RemoteReceiver(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteReceiver> {

        @Override
        public String getDisplayName() {
            return "Remote Receiver";
        }
    }
}
