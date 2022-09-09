package com.telushealth.thcp.pipeline.common.util

import groovy.json.JsonSlurperClassic 
import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS

/**
 * Helper class for Json functions
 */
class JsonUtil {

    @NonCPS
    static Object parseJsonText (String jsonText) {

        JsonSlurperClassic slurper = new JsonSlurperClassic()
        return slurper.parseText(jsonText)
    }

    static String toJson (Object object) {

        return JsonOutput.prettyPrint(JsonOutput.toJson(object))
    }

    static String generateSourceAmiManifest (Object packerManifest) {

        def amiId = packerManifest.builds[0].artifact_id.split(':')[1]
        def manifest = [
            sourceImageId: amiId
        ]
        return JsonOutput.toJson(manifest)
    }

    static String generateDockerManifest (Map vars) {
        def manifest = [
            sourceImageId: vars.docker_image,
            targetImageId: vars.aws_ecr_repository + ':' + vars.aws_ecr_image_tag
        ]
        return JsonOutput.toJson(manifest)
    }
}