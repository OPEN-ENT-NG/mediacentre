package fr.openent.mediacentre.helper.impl;

import fr.openent.mediacentre.helper.XmlExportHelper;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XmlExportHelperImpl implements XmlExportHelper {

    private final Integer MAX_NODES;

    private Element currentElement = null;
    private final String ROOT;
    private Document currentDoc = null;
    private final Logger log;
    private int nbElem = 0;
    private int fileIndex = 0;
    private final String exportDir;
    private final String FILE_PREFIX;

    public XmlExportHelperImpl(Container container, String root, String fileParamName) {
        ROOT = root;
        initNewFile();
        this.log = container.logger();
        MAX_NODES = container.config().getInteger("max-nodes", 10000);
        exportDir = container.config().getString("export-path", "");
        String idEnt = container.config().getString("id-ent", "");
        String strDate = new SimpleDateFormat("yyyyMMdd_").format(new Date());
        FILE_PREFIX = idEnt + "_GAR-ENT_Complet_" + strDate + fileParamName + "_";
    }

    private void addAttributesToRootNode() {
        currentElement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        currentElement.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        currentElement.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        currentElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        currentElement.setAttribute("Version", "1.0");
        currentElement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
    }

    @Override
    public void closeFile() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(currentDoc);
            StreamResult result = new StreamResult(new File(exportDir + getExportFileName(fileIndex)));
            transformer.transform(source, result);
        } catch (TransformerException tfe) {
            log.error(tfe.getMessage());
        }
    }

    @Override
    public void saveObject(String key, JsonObject entry) {
        saveObject(key, entry, currentElement);
        testNumberOfOccurrences();
    }


    private void saveObject(String key, JsonObject entry, Element curElt) {
        Element objectElement = currentDoc.createElement(key);
        for(String jsonKey : entry.getFieldNames()) {
            Object o = entry.getValue(jsonKey);
            saveUnknownObject(jsonKey, o, objectElement);
        }
        curElt.appendChild(objectElement);
        nbElem++;
    }

    private void saveString(String key, String value, Element curElt) {
        Element elem = currentDoc.createElement(key);
        elem.appendChild(currentDoc.createTextNode(value));
        curElt.appendChild(elem);
        nbElem++;
    }

    private void saveArray(String key, JsonArray array, Element curElt) {
        for(Object o : array) {
            saveUnknownObject(key, o, curElt);
        }
    }

    private void saveUnknownObject(String key, Object o, Element curElt) {
        if(o instanceof String) {
            saveString(key, (String)o, curElt);
        }
        if(o instanceof JsonObject) {
            saveObject(key, (JsonObject)o, curElt);
        }
        if(o instanceof JsonArray) {
            saveArray(key, (JsonArray)o, curElt);
        }
        if(o instanceof Number) {
            String strNumber = o.toString();
            saveString(key, strNumber, curElt);
        }
    }

    private void initNewFile(){
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        // root elements
        currentDoc = docBuilder.newDocument();
        currentElement = currentDoc.createElement(ROOT);
        currentDoc.appendChild(currentElement);
        nbElem = 1;
        addAttributesToRootNode();
    }

    private void testNumberOfOccurrences() {
        if (nbElem >= MAX_NODES) {
            closeFile();
            log.info("Students" + fileIndex + " saved");
            fileIndex++;
            initNewFile();
        }
    }

    /**
     * @param fileIndex : it is a number put at the end
     * @return name of the file to save
     */
    private String getExportFileName(int fileIndex){
        String formattedIndex = String.format ("%04d", fileIndex);
        return FILE_PREFIX + formattedIndex + ".xml";
    }

}
