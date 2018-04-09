package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceStructureImpl extends DataServiceBaseImpl implements DataService {


    DataServiceStructureImpl(Container container, String strDate) {
        super(container);
        xmlExportHelper = new XmlExportHelperImpl(container, STRUCTURE_ROOT, STRUCTURE_FILE_PARAM, strDate);
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

        getAndProcessStructuresInfo(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> structInfoResults) {
                if (validResponse(structInfoResults, handler)) {

                    getAndProcessStructuresMefs(new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> structMefsResults) {
                            if (validResponse(structMefsResults, handler)) {

                                getAndProcessStructuresFos(new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(Either<String, JsonObject> structFosResults) {
                                        if (validResponse(structFosResults, handler)) {

                                            xmlExportHelper.closeFile();
                                            handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));

                                        }
                                    }
                                });

                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Process structure info, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStructuresInfo(final Handler<Either<String, JsonObject>> handler) {

        getStucturesInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStructuresInfo( structResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Process structure mefs, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStructuresMefs(final Handler<Either<String, JsonObject>> handler) {

        getStucturesMefsFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStucturesMefs( structResults.right().getValue() );
                    handler.handle(result);
                }
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
                }
            }
        });
    }

    /**
     * Get structures infos from Neo4j
     * @param handler results
     */
    private void getStucturesInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) " +
                "OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure) ";
        String dataReturn = "RETURN distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "s.contract  as `" + STRUCTURE_CONTRACT + "`, " +
                "s.name as `" + STRUCTURE_NAME + "`, " +
                "s.phone  as `" + STRUCTURE_PHONE + "`, " +
                "s2.UAI  as `" + STRUCTURE_RATTACH + "`, " +
                "s.externalId  as structid " +
                "order by " + "`" + STRUCTURE_UAI + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process structures info
     * Update general map with mapping between structures ID and UAI
     * @param structures Array of structures from Neo4j
     */
    private Either<String,JsonObject> processStructuresInfo(JsonArray structures) {
        try {
            for (Object o : structures) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;
                updateMap(structure);

                if(isMandatoryFieldsAbsent(structure, STRUCTURE_NODE_MANDATORY))continue;
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
        structure.removeField("structid");
    }


    /**
     * Get structures mefs from Neo4j
     * For each structure :
     *      Each student has one mef attached
     *      Each teacher can have many mefs attached
     * @param handler results
     */
    private void getStucturesMefsFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String queryStudentsMefs = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->" +
                "(s:Structure)<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "where exists(n.module) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "n.module as `" + MEF_CODE + "`, " +
                "n.moduleName as `" + MEF_DESCRIPTION + "` " +
                "UNION ";
        String queryTeachersMefs = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->" +
                "(s:Structure)<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "where exists(n.modules) " +
                "with s,n " +
                "unwind n.modules as rows " +
                "with s, split(rows,\"$\") as modules " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "modules[1] as `" + MEF_CODE + "`, " +
                "modules[2] as `" + MEF_DESCRIPTION + "` ";
        neo4j.execute(queryStudentsMefs + queryTeachersMefs, new JsonObject(), validResultHandler(handler));
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
        String queryStructureFos = "MATCH (sub:Subject)-[SUBJECT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "with s, sub.label as label, split(sub.code,\"-\") as codelist " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "codelist[size(codelist)-1] as `" + STUDYFIELD_CODE + "`, " +
                "label as `" + STUDYFIELD_DESC + "` " +
                "order by `" + STRUCTURE_UAI + "` " +
                "UNION ";
        String queryStudentFos = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "where exists (u.fieldOfStudy) " +
                "with s, u.fieldOfStudy as fos, u.fieldOfStudyLabels as fosl " +
                "with s, " +
                "reduce(x=[], idx in range(0,size(fos)-1) | x + {code:fos[idx],label:fosl[idx]}) as rows " +
                "unwind rows as row " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "row.code as `" + STUDYFIELD_CODE + "`, " +
                "row.label as  `" + STUDYFIELD_DESC + "` " +
                "order by `" + STRUCTURE_UAI + "`";
        neo4j.execute(queryStructureFos + queryStudentFos, new JsonObject(), validResultHandler(handler));
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
