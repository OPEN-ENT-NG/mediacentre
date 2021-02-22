package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceStructureImpl1d extends DataServiceBaseImpl implements DataService {

    private PaginatorHelperImpl paginator;
    private JsonObject config;
    private String entId;
    private Boolean hasAcademyPrefix;
    private String source;

    DataServiceStructureImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        this.config = config;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, STRUCTURE_ROOT, STRUCTURE_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
        hasAcademyPrefix = this.config.containsKey("academy-prefix") && !"".equals(this.config.getString("academy-prefix").trim());
    }

    /**
     * Export Data to folder
     * - Export Structures info and build mapStructures with mapping between structures ID and UAI
     * - Export Structures Mefs
     * - Export Structures fields of study
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        getAndProcessStructuresInfo(0, structInfoResults -> {
            if (validResponse(structInfoResults, handler)) {
                xmlExportHelper.closeFile();
                handler.handle(new Either.Right<String, JsonObject>(
                        new JsonObject().put(
                                FILE_LIST_KEY,
                                xmlExportHelper.getFileList()
                        )));
            }
        });
    }

    /**
     * Process structure info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStructuresInfo(int skip, final Handler<Either<String, JsonObject>> handler) {
        getStucturesInfoFromNeo4j(skip, structResults -> {
            if (validResponseNeo4j(structResults, handler)) {
                Either<String, JsonObject> result = processStructuresInfo(structResults.right().getValue());

                if (structResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessStructuresInfo(skip + PaginatorHelperImpl.LIMIT, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceStructureImpl@getAndProcessStructuresInfo] Failed to process");
            }
        });
    }

    /**
     * Get structures infos from Neo4j
     *
     * @param handler results
     */
    private void getStucturesInfoFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure {source:'"+ this.source +"'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports ";
// Don't export optional attachment structure attribute
//                "OPTIONAL MATCH (g2:ManualGroup{name:\\\"\" + CONTROL_GROUP + \"\\\"})-[:DEPENDS]->(s2:Structure)<-[:HAS_ATTACHMENT]-(s:Structure) ";
        String dataReturn = "RETURN distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "s.name as `" + STRUCTURE_NAME + "`, " +
//                "collect(distinct s2.UAI)[0]  as `" + STRUCTURE_RATTACH + "`, " +
                "s.contract  as `" + STRUCTURE_CONTRACT + "`, " +
                "s.phone  as `" + STRUCTURE_PHONE + "`, " +
                //TODO GARStructureTelephone
                "s.externalId  as structid " +
                "order by " + "`" + STRUCTURE_UAI + "`";

        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStream(query, params, skip, handler);
    }

    /**
     * Process structures info
     * Update general map with mapping between structures ID and UAI
     *
     * @param structures Array of structures from Neo4j
     */
    private Either<String, JsonObject> processStructuresInfo(JsonArray structures) {
        try {
            //clean mapStructures before process structures.
            mapStructures.clear();
            for (Object o : structures) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;

                if (isMandatoryFieldsAbsent(structure, STRUCTURE_NODE_MANDATORY)) continue;

                updateMap(structure);
                xmlExportHelper.saveObject(STRUCTURE_NODE, structure);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing structures Info : " + e.getMessage());
        }
    }

    /**
     * Update mapStructures with ID and UAI of structure
     * Then remove ID from object
     *
     * @param structure object with structure info
     */
    private void updateMap(JsonObject structure) {
        String structId = structure.getString("structid");
        String structUAI = structure.getString(STRUCTURE_UAI);
        mapStructures.put(structId, structUAI);
        structure.remove("structid");
    }
}
