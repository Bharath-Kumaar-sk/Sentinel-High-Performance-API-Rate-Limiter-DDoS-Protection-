package com.sentinel;

import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

//import org.springframework.scripting.ScriptSource;
//import org.springframework.scripting.support.ResourceScriptSource;


@Configuration
public class RedisConfig {
    //tells the spring to create this object and manage it
    @Bean
    public RedisScript<Long> rateLimitScript() {
        //script source -> where our Lua script loaded from
        //resource script -> warps the resource file into script source
        //class path resource -> provides the file at the location.
        //ScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("token_bucket.lua"));

        //directly take the resource
        ClassPathResource classPathResource = new ClassPathResource("token_bucket.lua");
        //load the lua file, prepare for execution, expect a Long return value
        return RedisScript.of(classPathResource, Long.class);
    }
}
