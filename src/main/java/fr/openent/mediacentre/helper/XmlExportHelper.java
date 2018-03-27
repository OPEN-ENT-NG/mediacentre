package fr.openent.mediacentre.helper;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface XmlExportHelper {

    void closeFile();

    void saveObject(String key, JsonObject entry);
}
