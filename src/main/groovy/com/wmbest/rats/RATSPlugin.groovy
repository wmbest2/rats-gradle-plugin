package com.wmbest.rats

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.builder.testing.api.TestException
import com.android.builder.testing.api.TestServer
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.MultipartEntity
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator

import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.JSON

import java.io.File

import javax.inject.Inject

class RATSPlugin extends TestServer implements Plugin<Project> {

    static final String CONNECTION_ERROR = "Couldn't reach rats server.  Check that it is connected and running the latest version."

    Project project
    void apply(Project project) {
        this.project = project
        def hasApp = project.hasProperty('android')
        project.extensions.create("rats", RATSExtension)
        project.android.testServer(this)
        project.tasks.create(name: "pingRatsServer") << {
            pingServer()
        }

        project.gradle.startParameter.getTaskNames().find { task ->
            if (task.equals("deviceCheck")) {
                project.tasks["preBuild"].dependsOn 'pingRatsServer'
                return true
            }
            if (task.startsWith("ratsUpload")) {
                project.tasks["preBuild"].dependsOn 'pingRatsServer'
                return true
            }
            return false
        }
    }

    String getName() {
        return "rats"
    }

    void uploadApks(@NonNull String variant, @NonNull File testApk, @Nullable File app) {
        def http = new HTTPBuilder(project.rats.server + "/api/run")

        if (project.rats.user != null && project.rats.password != null) {
            http.auth.basic project.rats.user, project.rats.password
        }

        http.getClient().getParams().setParameter("http.connection.timeout", project.rats.timeout)
        http.getClient().getParams().setParameter("http.socket.timeout", project.rats.timeout)
        http.getClient().getParams().setParameter("http.keepAlive", false)

        try {
            http.request(POST, JSON) { req ->

                MultipartEntity entity = new MultipartEntity()
                if (app != null) {
                    entity.addPart('apk', new FileBody(app))
                }

                entity.addPart('test-apk', new FileBody(testApk))
                if (project.rats.count != null) {
                    entity.addPart('count', new StringBody(String.valueOf(project.rats.count)))
                }
                if (project.rats.serials != null) {
                    entity.addPart('serials', new StringBody(project.rats.serials.join(',')))
                }
                if (project.rats.message != null) {
                    entity.addPart('message', new StringBody(project.rats.message))
                }
                entity.addPart('strict', new StringBody(booleanToString(project.rats.strict)))
                req.entity = entity

                response.success = { resp, json ->
                    String output = "${project.rats.server}/#/runs/${json.name}"
                    println "See ${output} for details"
                }

                response.'500' = { resp, json ->
                    String output = "${project.rats.server}/#/runs/${json.name}"
                    String failure = "Tests Failed on the Following devices:\n"
                    for (d in json.suites) {
                        if (d.errors > 0 || d.failures > 0) {
                            failure += "\t${d.device.manufacturer} ${d.device.model} with ${d.errors} Errors and ${d.failures} Failures\n"
                        }
                    }
                    failure += "\n\tSee ${output} for details"
                    throw new GradleException(failure)
                }
            }
        } finally {
            http.shutdown()
        }
    }

    boolean isConfigured() {
        return true
    }

    def booleanToString(def property) {
        property ? 'true' : 'false'
    }

    void pingServer() {
        if (project != null && project.hasProperty('rats') && project.rats.server != null) {

            def http = new HTTPBuilder(project.rats.server + "/api/ping")

            if (project.rats.user != null && project.rats.password != null) {
                http.auth.basic project.rats.user, project.rats.password
            }

            http.getClient().getParams().setParameter("http.connection.timeout", project.rats.timeout)
            http.getClient().getParams().setParameter("http.socket.timeout", project.rats.timeout)

            try {
                http.request(GET, TEXT) { req ->
                    response.success = { resp, json ->
                        // cool
                    }

                    response.failure = { resp, json ->
                        throw new GradleException(CONNECTION_ERROR)
                        // throw new GradleException("${CONNECTION_ERROR} (${resp})")
                    }
                }
            } catch(Exception  e) {
                throw new GradleException(CONNECTION_ERROR, e)
            } finally {
                http.shutdown()
            }
        }
    }
}

class RATSExtension {
    String server = 'http://localhost:3000'
    String user
    String password
    Integer count
    Integer timeout
    String[] serials
    Boolean strict = false

    // Meta information
    String message
}
