package api.NeatGenome;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eplex.win.FastNEATJava.genome.NeatGenome;

/**
 * Created by paul on 10/19/14.
 */
public class NeatGenomeDeserializer extends JsonDeserializer<NeatGenome>{

    @Override
    public NeatGenome deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {


//        HashMap map = //jp.readv.readValue(new StringReader(test), HashMap.class);

//        JsonNode node = jp.getCodec().readTree(jp);


        return null;//new NeatGenome();
    }
}
