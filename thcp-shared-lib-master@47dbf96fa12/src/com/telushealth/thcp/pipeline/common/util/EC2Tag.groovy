package com.telushealth.thcp.pipeline.common.util

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')

class EC2Tag
{
    String name;
    
    String value;
}
