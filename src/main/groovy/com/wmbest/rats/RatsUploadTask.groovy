package com.wmbest.rats

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException

import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON

class RatsUploadTask extends DefaultTask {

    static final String CONTENT_TYPE = 'multipart/form-data'

    @Input String url
    @Optional String apkFilename
    @Input String testFilename
    @Optional List<String> serials
    @Optional Integer deviceCount
    @Optional Integer timeout = 20 * 60 * 1000;
    @Input Boolean strict = false
    @Input Boolean replace = false

    @TaskAction
    void upload() {
        def http = new HTTPBuilder(url + "/api/run")

        http.getClient().getParams().setParameter("http.connection.timeout", timeout)
        http.getClient().getParams().setParameter("http.socket.timeout", timeout)

        try {
            http.request(POST, JSON) { req ->

                MultipartEntity entity = new MultipartEntity()
                if (apkFilename != null) {
                    entity.addPart('apk', new FileBody(new File(apkFilename)))
                }

                entity.addPart('test-apk', new FileBody(new File(testFilename)))
                if (deviceCount != null) {
                    entity.addPart('count', new StringBody(String.valueOf(deviceCount)))
                }
                if (serials != null) {
                    entity.addPart('serials', new StringBody(serials.join(',')))
                }
                entity.addPart('strict', new StringBody(booleanToString(strict)))
                req.entity = entity

                response.success = { resp, json ->
                    String output = "${url}/#/runs/${json.name}"
                    println "See ${output} for details"
                }

                response.'500' = { resp, json ->
                    String output = "${url}/#/runs/${json.name}"
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

    def booleanToString(def property) {
        property ? 'true' : 'false'
    }
}
