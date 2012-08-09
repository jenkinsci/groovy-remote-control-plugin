package org.jenkinsci.plugins.groovyremote;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.net.MalformedURLException;
import java.net.URL;

import static hudson.util.FormValidation.validateRequired;

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

        public FormValidation doCheckName(@QueryParameter String value) {
            return validateRequired(value);
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            try {
                new URL(value);
                return FormValidation.ok();
            } catch (MalformedURLException e) {
                return FormValidation.error(Messages.RemoteReceiver_malformed_url());
            }
        }
    }
}
