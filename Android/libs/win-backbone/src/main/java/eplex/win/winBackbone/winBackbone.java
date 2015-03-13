package eplex.win.winBackbone;

//import com.squareup.otto.Bus;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

//import dagger.ObjectGraph;

/**
 * Created by paul on 8/6/14.
 */
public class winBackbone {

//    private static winBackbone instance = null;

    //Backbone handles a few things in the original JavaScript implementation
    //1. Register events and callbacks from each module
    //2. Retrieve a logger to send messages for logging
    //3. Retrieve a function for sending out messages- might be convered by Otto and Subscribe callbacks

//    Logger masterLog;
//    Bus bus;
//    ObjectGraph backboneGraph;

//    public static winBackbone getInstance() {
//        if(instance == null)
//            throw new IllegalArgumentException("Must configure winBackbone singleton instance initially.");
//
//        return instance;
//    }
//    public static winBackbone getInstance(Object loggerActivity, Object... modules) {
//        if(instance == null) {
//            instance = new winBackbone(loggerActivity, modules);
//        }
//        return instance;
//    }

    //create the backbone bus object
//    public winBackbone(Object loggerActivity, Object... modules)
//    {
//        //bus to shoot out events to!
//        bus = new Bus();
//
//        //graph to handle accessing objects from the backbone
//        backboneGraph = ObjectGraph.create(modules);
//
//        // SLF4J
//        masterLog = LoggerFactory.getLogger(loggerActivity.getClass());
//        masterLog.info("Backbone instantiated with main activity");
//    }

//    public Object getBackboneObject(Class objectClass)
//    {
//        return backboneGraph.get(objectClass);
//    }
//
//    public void configureBackbone(Map<String, Object> nameToObjects, Map<String, ObjectNode> globalConfig, Map<String, ObjectNode> localConfig) {
//        //configures all the objects we have
//        //send out configuration setting to each new object we create
//        Iterator moduleIterator = nameToObjects.entrySet().iterator();
//
//        for (Map.Entry<String, Object> modulePair : nameToObjects.entrySet())
//        {
//            System.out.println("Key = " + modulePair.getKey() + ", Value = " + modulePair.getValue());
//
//            Object moduleToRegister = modulePair.getValue();
//
//            if(modulePair.getValue().getClass() == String.class)
//            {
//                String moduleName = (String)(modulePair.getValue());
//                try {
//                    moduleToRegister = Class.forName(moduleName).newInstance();
//
//                }
//                catch (Exception e)
//                {
//                    //have to throw error into the logger
//                    masterLog.error("Exception: Failed to instantiate module.", e);
//                }
//            }
//            //else, we are already an object, simply register in the bus
//
//            //register this object inside the bus - where the wheel may not turn round and round!
//            bus.register(moduleToRegister);
//        }
//    }

}
