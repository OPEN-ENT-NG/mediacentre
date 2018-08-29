package fr.openent.mediacentre.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface XmlExportHelper {

    /**
     * Close current xml file and save to disk
     */
    void closeFile();

    void saveObject(String key, JsonObject entry);

    JsonArray getFileList();
}
