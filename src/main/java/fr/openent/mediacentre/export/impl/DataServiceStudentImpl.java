package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.mediacentre.constants.GarConstants.*;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceStudentImpl extends DataServiceBaseImpl implements DataService{

    DataServiceStudentImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, STUDENT_ROOT, STUDENT_FILE_PARAM, strDate);
    }

    /**
     * Export Data to folder
     * - Export Students identities
     * - Export Students Mefs
     * - Export Students fields of study
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getAndProcessStudentsInfo(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> studentsResult) {
                if(validResponse(studentsResult, handler)) {

                    getAndProcessStudentsMefs(new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> mefsResult) {
                            if(validResponse(mefsResult, handler)) {

                                getAndProcessStudentsFos(new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> modulesResult) {
                                            if(validResponse(modulesResult, handler)) {

                                                xmlExportHelper.closeFile();
                                                handler.handle(new Either.Right<String, JsonObject>(
                                                        new JsonObject().put(
                                                                FILE_LIST_KEY,
                                                                xmlExportHelper.getFileList()
                                                        )));
                                            }
                                        }
                                    }
                                );
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Process students info, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStudentsInfo(final Handler<Either<String, JsonObject>> handler) {

        getStudentsInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStudentsInfo( structResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }



    /**
     * Process students mefs, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStudentsMefs(final Handler<Either<String, JsonObject>> handler) {

        getStudentsMefFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStudentsMefs( structResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }



    /**
     * Process students fos, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessStudentsFos(final Handler<Either<String, JsonObject>> handler) {

        getStudentsFosFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> structResults) {
                if( validResponseNeo4j(structResults, handler) ) {
                    Either<String,JsonObject> result = processStudentsFos( structResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Get students infos from Neo4j
     * Set fields as requested by xsd, except for structures
     * @param handler results
     */
    private void getStudentsInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "match (u:User)-[IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}), " +
                "(p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup) " +
                "where p.name = 'Student' " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(sr:Structure) ";
        String dataReturn = "return distinct u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.otherNames as `" + PERSON_OTHER_NAMES + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "sr.UAI as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "collect(distinct s.UAI) as profiles " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process students info
     * Add structures in arrays to match xsd
     * @param students Array of students from Neo4j
     */
    private Either<String,JsonObject> processStudentsInfo(JsonArray students) {
        try {
            for(Object o : students) {
                if(!(o instanceof JsonObject)) continue;

                JsonObject student = (JsonObject) o;
                JsonArray profiles = student.getJsonArray("profiles", null);
                if(profiles == null || profiles.size() == 0) {
                    log.warn("Mediacentre : Student with no profile for export, id "
                            + student.getString("u.id", "unknown"));
                    continue;
                }
                if(isMandatoryFieldsAbsent(student, STUDENT_NODE_MANDATORY))continue;

                processProfiles(student, STUDENT_PROFILE, null);
                xmlExportHelper.saveObject(STUDENT_NODE, student);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing students Info : " + e.getMessage());
        }
    }

    /**
     * Get students mefs from Neo4j
     * @param handler results
     */
    private void getStudentsMefFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-" +
                "(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) ";
        String dataReturn = "where p.name = 'Student' " +
                "return distinct u.id as `" + PERSON_ID + "`, " +
                "u.module as `" + MEF_CODE + "`, " +
                "s.UAI as `" + STRUCTURE_UAI + "` " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process mefs info
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String,JsonObject> processStudentsMefs(JsonArray mefs) {
        Either<String,JsonObject> event =  processSimpleArray(mefs, PERSON_MEF, PERSON_MEF_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing students mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Get students fields of study from Neo4j
     * @param handler results
     */
    private void getStudentsFosFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)" +
                "-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "where p.name = 'Student' ";
        String dataReturn = "with u,s " +
                "unwind u.fieldOfStudy as fos " +
                "return distinct u.id as `" + PERSON_ID + "`, " +
                "s.UAI as `" + STRUCTURE_UAI + "`, " +
                "fos as `" + STUDYFIELD_CODE + "` " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process fields of study info
     * @param fos Array of fieldsOfStudy from Neo4j
     */
    private Either<String,JsonObject> processStudentsFos(JsonArray fos) {
        Either<String,JsonObject> event =  processSimpleArray(fos, STUDENT_STUDYFIELD, STUDENT_STUDYFIELD_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing students fos : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
