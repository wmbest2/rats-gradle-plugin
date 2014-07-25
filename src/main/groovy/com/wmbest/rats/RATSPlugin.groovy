package com.wmbest.rats

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.TestServer;
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
    Project project;
    void apply(Project project) {
        this.project = project
        def hasApp = project.hasProperty('android')
        project.extensions.create("rats", RATSExtension);
        project.android.testServer(this);
    }

    String getName() {
        return "rats"
    }

    void uploadApks(@NonNull String variant, @NonNull File testApk, @Nullable File app) {
        def http = new HTTPBuilder(project.rats.server + "/api/run")

        http.getClient().getParams().setParameter("http.connection.timeout", project.rats.timeout)
        http.getClient().getParams().setParameter("http.socket.timeout", project.rats.timeout)

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
                entity.addPart('strict', new StringBody(booleanToString(project.rats.strict)))
                req.entity = entity

                response.success = { resp, json ->
                    String output = "${project.rats.server}/#/runs/${json.name}"
                    println "See ${output} for details"
                }

                response.'500' = { resp, json ->
                    String output = "${project.rats.server}/#/runs/${json.name}"
                    String failure = "Tests Failed on the Following devices:\n";
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
            http.shutdown();
        }
    }

    boolean isConfigured() {
        if (project != null && project.hasProperty('rats')
            && project.rats.server != null) {

            def http = new HTTPBuilder(project.rats.server + "/api/ping")

            http.getClient().getParams().setParameter("http.connection.timeout", project.rats.timeout)
            http.getClient().getParams().setParameter("http.socket.timeout", project.rats.timeout)

            try {
                boolean success = false;
                http.request(GET, TEXT) { req ->
                    response.success = { resp, json ->
                        success = true
                    }

                    response.failure = { resp, json ->
                        throw new GradleException("Couldn't reach rats server.  Check that it is connected and running the latest version.")
                    }
                }
                return success
            } catch(Exception  e) {
                throw new GradleException("Couldn't reach rats server.  Check that it is connected and running the latest version.")
            } finally {
                http.shutdown();
            }
        }
        return false
    }

    def booleanToString(def property) {
        property ? 'true' : 'false'
    }

}

class RATSExtension {
    String server = 'http://localhost:3000'
    Integer count
    Integer timeout
    String[] serials
    Boolean strict = false

    // Meta information
    String message
}
