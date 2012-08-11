package org.jenkinsci.plugins.groovyremote

import spock.lang.*
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import groovyx.remote.server.*
import groovyx.remote.client.*
import groovyx.remote.transport.http.*
import com.sun.net.httpserver.HttpServer
import java.util.concurrent.Executors

class ControlExternalAppSpec extends Specification {

    @Rule
    JenkinsRule rule = new JenkinsRule()

    def server

    def 'run groovy script'() {
        given:
        def job = rule.createFreeStyleProject()
        job.buildersList.add newBuilder('''
            println 'hello'
        ''')

        when:
        def build = job.scheduleBuild2(0).get()

        then:
        build.logFile.text.contains('hello')
    }

    def 'use jenkins instance'() {
        given:
        def job = rule.createFreeStyleProject()
        job.buildersList.add newBuilder('''
            println "Jenkins Version: ${jenkins.version}"
        ''')

        when:
        def build = job.scheduleBuild2(0).get()

        then:
        build.logFile.text.contains("Jenkins Version: ${rule.jenkins.version}")
    }

    def 'control remote application'() {
        given:
        createServer()
        def job = rule.createFreeStyleProject()
        job.buildersList.add newBuilder('''
            println "Jenkins Version: ${jenkins.version}"
            def result = remote {
                1 + 1
            }
            println "Remote result: ${result}"
        ''')

        when:
        def build = job.scheduleBuild2(0).get()

        then:
        build.logFile.text.contains("Jenkins Version: ${rule.jenkins.version}")
        build.logFile.text.contains("Remote result: 2")
    }

    private newBuilder(String script) {
        def builder = new GroovyRemoteBuilder('test', script)
        builder.descriptor.remotes << createRemoteReceiver()
        builder
    }

    private createRemoteReceiver() {
        new RemoteReceiver('test', "http://localhost:${server?.address?.port}")
    }

    private createServer() {
        def thisClassLoader = getClass().classLoader
        def neededURLsForServer = thisClassLoader.getURLs().findAll { it.path.contains("groovy-all") }
        def serverClassLoader = new URLClassLoader(neededURLsForServer as URL[], thisClassLoader.parent)
        def receiver = new Receiver(serverClassLoader)

        server = HttpServer.create(new InetSocketAddress(0), 1)
        server.createContext("/", new RemoteControlHttpHandler(receiver))
        server.executor = Executors.newSingleThreadExecutor()
        server.start()

        Thread.sleep(2000)
    }

    def cleanup() {
        server?.stop(0)
    }
}