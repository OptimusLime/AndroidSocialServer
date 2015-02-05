package api.NodeType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import eplex.win.FastCPPNJava.network.NodeType;

/**
 * Created by paul on 10/19/14.
 */
public class NodeTypeDeserializer extends JsonDeserializer<NodeType>
{
    @Override
    public NodeType deserialize(final JsonParser parser, final DeserializationContext context) throws IOException, JsonProcessingException
    {
        // then you'd do something like parser.getInt() or whatever to pull data off the parser
        return Enum.valueOf(NodeType.class, StringUtils.lowerCase(parser.getValueAsString()));
    }

}