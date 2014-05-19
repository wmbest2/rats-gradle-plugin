package com.wmbest.rats

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.ProjectConfigurationException
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class RATSPlugin implements Plugin<Project> {
    void apply(Project project) {
        def hasApp = project.plugins.hasPlugin(AppPlugin)
        def hasLib = project.plugins.hasPlugin(LibraryPlugin)
        if (!hasApp && !hasLib) {
            throw new ProjectConfigurationException("'android' or 'android-library' plugin required.")
        }

        project.extensions.create("rats", RATSExtension);

        final def variants
        if (hasApp) {
            variants = project.android.applicationVariants
        } else {
            variants = project.android.libraryVariants
        }

        variants.all { variant ->
            if (variant.getTestVariant()) {
                def name = variant.getName()
                name = name[0].toUpperCase() + name.substring(1)

                def task = variant.getTestVariant().getConnectedInstrumentTest()
                def uploadTask = project.task("remote${name}Test", type: RatsUploadTask)
                uploadTask.description = "Upload ${name} build to RATS server"
                uploadTask.group = "Verification"

                uploadTask.url = project.rats.server
                if (hasApp) {
                    uploadTask.apkFilename = variant.getOutputFile().getCanonicalPath()
                }

                uploadTask.testFilename = variant.getTestVariant().getOutputFile().getCanonicalPath()
                uploadTask.deviceCount = project.rats.count
                uploadTask.serials = project.rats.serials 
                uploadTask.strict = project.rats.strict
                if (project.rats.timeout) {
                    uploadTask.timeout = project.rats.timeout
                }
                uploadTask.dependsOn task.taskDependencies
            }
        }
    }
}

class RATSExtension {
    String server = 'http://localhost:3000'
    Integer count
    Integer timeout
    String[] serials
    Boolean strict = false
}
