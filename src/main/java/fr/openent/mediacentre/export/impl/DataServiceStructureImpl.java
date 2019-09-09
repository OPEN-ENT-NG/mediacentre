package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceStructureImpl extends DataServiceBaseImpl implements DataService {

    private PaginatorHelperImpl paginator;
    private JsonObject config;

    DataServiceStructureImpl(JsonObject config, String strDate) {
        super(config);
        this.config = config;
        xmlExportHelper = new XmlExportHelperImpl(config, STRUCTURE_ROOT, STRUCTURE_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Structures info and build mapStructures with mapping between structures ID and UAI
     * - Export Structures Mefs
     * - Export Structures fields of study
     *
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getAndProcessStructuresInfo(structInfoResults -> {
            if (validResponse(structInfoResults, handler)) {

                getAndProcessStructuresMefs(structMefsResults -> {
                    if (validResponse(structMefsResults, handler)) {

                        getAndProcessStructuresFos(structFosResults -> {
                            if (validResponse(structFosResults, handler)) {

                                xmlExportHelper.closeFile();
                                handler.handle(new Either.Right<String, JsonObject>(
                                        new JsonObject().put(
                                                FILE_LIST_KEY,
                                                xmlExportHelper.getFileList()
                                        )));

                            }
                        });

                    }
                });
            }
        });
    }

    /**
     * Process structure info, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStructuresInfo(final Handler<Either<String, JsonObject>> handler) {

        getStucturesInfoFromNeo4j(structResults -> {
            if( validResponseNeo4j(structResults, handler) ) {
                Either<String,JsonObject> result = processStructuresInfo( structResults.right().getValue() );
                handler.handle(result);
            } else {
                log.error("[DataServiceStructureImpl@getAndProcessStructuresInfo] Failed to process");
            }
        });
    }

    /**
     * Process structure mefs, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStructuresMefs(final Handler<Either<String, JsonObject>> handler) {

        getStucturesMefsFromNeo4j(structResults -> {
            if( validResponseNeo4j(structResults, handler) ) {
                Either<String,JsonObject> result = processStucturesMefs( structResults.right().getValue() );
                handler.handle(result);
            } else {
                log.error("[DataServiceStructureImpl@getAndProcessStructuresMefs] Failed to process");
            }
        });
    }

    /**
     * Process structure fields of study, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStructuresFos(final Handler<Either<String, JsonObject>> handler) {

        getStucturesFosFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStucturesFos( structResults.right().getValue() );
                    handler.handle(result);
                } else {
                    log.error("[DataServiceStructureImple@getAndProcessStructureFos] Failed to process");
                }
            }
        });
    }

    /**
     * Get structures infos from Neo4j
     * @param handler results
     */
    private void getStucturesInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ManualGroup{name:\"" + CONTROL_GROUP + "\"}) ";
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

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures info
     * Update general map with mapping between structures ID and UAI
     * @param structures Array of structures from Neo4j
     */
    private Either<String,JsonObject> processStructuresInfo(JsonArray structures) {
        try {
            //clean mapStructures before process structures.
            mapStructures.clear();
            for (Object o : structures) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;

                if(isMandatoryFieldsAbsent(structure, STRUCTURE_NODE_MANDATORY)) continue;

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
     * @param structure object with structure info
     */
    private void updateMap(JsonObject structure) {
        String structId = structure.getString("structid");
        String structUAI = structure.getString(STRUCTURE_UAI);
        mapStructures.put(structId, structUAI);
        structure.remove("structid");
    }


    /**
     * Get structures mefs from Neo4j
     * For each structure :
     *      Each student has one mef attached
     *      Each teacher can have many mefs attached
     * @param handler results
     */
    private void getStucturesMefsFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String queryStudentsMefs = "MATCH (n:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:ManualGroup{name:\"" + CONTROL_GROUP + "\"}) " +
                "WHERE exists(n.module) AND  NOT(has(n.deleteDate)) AND NOT(HAS(n.disappearanceDate)) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "n.module as `" + MEF_CODE + "`, " +
                "n.moduleName as `" + MEF_DESCRIPTION + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + MEF_CODE + "` " +
                "UNION ";
        String queryTeachersMefs = "MATCH (n:User)-[:IN|DEPENDS*1..2]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:ManualGroup{name:\"" + CONTROL_GROUP + "\"}) " +
                "where exists(n.modules) and not has(n.deleteDate) AND NOT(HAS(n.disappearanceDate)) " +
                "with s,n " +
                "unwind n.modules as rows " +
                "with s, split(rows,\"$\") as modules " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "modules[1] as `" + MEF_CODE + "`, " +
                "modules[2] as `" + MEF_DESCRIPTION + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + MEF_CODE + "` ";

        String query = queryStudentsMefs + queryTeachersMefs;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures mefs
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String,JsonObject> processStucturesMefs(JsonArray mefs) {

        Either<String,JsonObject> event =  processSimpleArray(mefs, MEF_NODE, MEF_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing structures mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Get structures fields of study from Neo4j
     *      - Structure FOS codes may be prefixed by ACADEMY-
     *      - Students FOS codes and description are lists in two different fields and must be mapped
     * @param handler results
     */
    private void getStucturesFosFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String condition;
        boolean containsAcademyPrefix = this.config.containsKey("academy-prefix") && !"".equals(this.config.getString("academy-prefix").trim());
        if (containsAcademyPrefix) {
            condition = "CASE WHEN sub.code =~ '("+ this.config.getString("academy-prefix") +")-[A-Z0-9-]+' THEN substring(sub.code, size(head(split(sub.code,\"-\"))) + 1) ELSE sub.code END as codelist";
        } else {
            condition = "split(sub.code,\"-\") as codelist";
        }
        String queryStructureFos = "MATCH (sub:Subject)-[:SUBJECT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:ManualGroup{name:\"" + CONTROL_GROUP + "\"}) " +
                "with s, sub.label as label, " + condition +
                " return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                (containsAcademyPrefix ? "codelist" : "codelist[size(codelist)-1]") + " as `" + STUDYFIELD_CODE + "`, " +
                "label as `" + STUDYFIELD_DESC + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + STUDYFIELD_CODE + "` " +
                "UNION ";
        String queryStudentFos = "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:ManualGroup{name:\"" + CONTROL_GROUP + "\"}) " +
                "where exists (u.fieldOfStudy) AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
                "with s, u.fieldOfStudy as fos, u.fieldOfStudyLabels as fosl " +
                "with s, " +
                "reduce(x=[], idx in range(0,size(fos)-1) | x + {code:fos[idx],label:fosl[idx]}) as rows " +
                "unwind rows as row " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "row.code as `" + STUDYFIELD_CODE + "`, " +
                "row.label as  `" + STUDYFIELD_DESC + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + STUDYFIELD_CODE + "` " ;

        String query = queryStructureFos + queryStudentFos;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures fields of study
     * @param fos Array of fis from Neo4j
     */
    private Either<String,JsonObject>  processStucturesFos(JsonArray fos) {
        Either<String,JsonObject> event =  processSimpleArray(fos, STUDYFIELD_NODE, STUDYFIELD_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing structures fos : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
