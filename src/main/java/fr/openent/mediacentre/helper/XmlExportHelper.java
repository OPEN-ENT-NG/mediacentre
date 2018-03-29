package fr.openent.mediacentre.helper;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface XmlExportHelper {

    /**
     * Close current xml file and save to disk
     */
    void closeFile();

    void saveObject(String key, JsonObject entry);
}
