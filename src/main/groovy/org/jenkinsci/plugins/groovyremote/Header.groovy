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

public class Header extends AbstractDescribableImpl<Header> {

    private String key;

    private String value;

    @DataBoundConstructor
    public Header(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Header> {

        @Override
        public String getDisplayName() {
            return "Header";
        }

        public FormValidation doCheckKey(@QueryParameter String value) {
            return validateRequired(value);
        }

        public FormValidation doCheckValue(@QueryParameter String value) {
            return validateRequired(value);
        }
    }

}
