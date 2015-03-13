package edu.eplex.AsyncEvolution.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import edu.eplex.AsyncEvolution.api.NodeType.NodeTypeDeserializer;
import eplex.win.FastCPPNJava.network.NodeType;

/**
 * Created by paul on 10/19/14.
 */
public class CustomJSONMapper {
    public static ObjectMapper CustomWINDeserializer()
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
//        module.addDeserializer(HomeAPIObject.class, new HomeAPIDeserializer());
        module.addDeserializer(NodeType.class, new NodeTypeDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
