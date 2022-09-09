pipelineJob("$jobPath") {
    definition {
        cps {
            script(readFileFromWorkspace('$jobFile'))
            sandbox()
        }
    }

    logRotator {
        numToKeep(30)
    }
}

