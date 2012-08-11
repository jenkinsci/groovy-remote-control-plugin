package org.jenkinsci.plugins.groovyremote

import spock.lang.*
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import groovyx.remote.transport.http.HttpTransport
import groovyx.remote.client.RemoteControl

class ControlJenkinsSpec extends Specification {

    @Rule
    JenkinsRule rule = new JenkinsRule()

    def remote

    def setup() {
        // disabled CSRF protection.
        rule.jenkins.crumbIssuer = null
        remote = new RemoteControl(new HttpTransport(rule.URL.toExternalForm() + 'plugin/groovy-remote/'))
    }

    def "return result"() {
        when:
        def result = remote {
            1 + 1
        }

        then:
        result == 2
    }

    def "passing variable"() {
        given:
        def name = 'kiy0taka'

        when:
        def result = remote {
            "Hello, ${name}!"
        }

        then:
        result == 'Hello, kiy0taka!'
    }

    def "use jenkins"() {
        when:
        def result = remote {
            jenkins.version.value
        }

        then:
        result == rule.jenkins.version.value
    }
}
