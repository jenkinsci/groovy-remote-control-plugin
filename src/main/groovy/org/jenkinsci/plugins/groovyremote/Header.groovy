package org.jenkinsci.plugins.groovyremote;


import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import static hudson.util.FormValidation.validateRequired

public class Header extends AbstractDescribableImpl<Header> {

    String key
    String value

    @DataBoundConstructor
    public Header(String key, String value) {
        this.key = key
        this.value = value
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Header> {

        @Override
        public String getDisplayName() {
            "Header"
        }

        public FormValidation doCheckKey(@QueryParameter String value) {
            validateRequired(value)
        }

        public FormValidation doCheckValue(@QueryParameter String value) {
            validateRequired(value)
        }
    }

}
