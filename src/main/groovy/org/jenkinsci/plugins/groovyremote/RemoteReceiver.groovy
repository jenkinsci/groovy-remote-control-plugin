package org.jenkinsci.plugins.groovyremote;


import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import static hudson.util.FormValidation.validateRequired

public class RemoteReceiver extends AbstractDescribableImpl<RemoteReceiver> {

    String name
    String url
    List<Header> headers

    @DataBoundConstructor
    public RemoteReceiver(String name, String url, List<Header> headers) {
        this.name = name
        this.url = url
        this.headers = headers
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteReceiver> {

        @Override
        public String getDisplayName() {
            "Remote Receiver"
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            validateRequired(value)
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            try {
                value.toURL()
                FormValidation.ok()
            } catch (MalformedURLException e) {
                return FormValidation.error(Messages.RemoteReceiver_malformed_url())
            }
        }
    }
}
