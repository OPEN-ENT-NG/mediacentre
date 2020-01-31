package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResult;

public class DataServiceStudentImpl extends DataServiceBaseImpl implements DataService{
    private PaginatorHelperImpl paginator;

    DataServiceStudentImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, STUDENT_ROOT, STUDENT_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
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
                } else {
                    log.error("[DataServiceStudentImpl@getAndProcessStudentsInfo] Failed to process");
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
                } else {
                    log.error("[DataServiceStudentImpl@getAndProcessStudentsMefs] Failed to process");
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
                } else {
                    log.error("[DataServiceStudentImpl@getAndProcessStudentsFos] Failed to process");
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
        String query = "match (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure), " +
                "(p:Profile)<-[:HAS_PROFILE]-(pg:ProfileGroup) " +
                "where p.name = 'Student' AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) AND HAS(s.exports) AND 'GAR' IN s.exports " +
                "OPTIONAL MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(sr:Structure) WHERE HAS(s.exports) AND 'GAR' IN s.exports " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) ";
        String dataReturn = "return distinct " +
                "u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.otherNames, [u.firstName]) as `" + PERSON_OTHER_NAMES + "`, " +
                //TODO GARPersonCivilite
                "sr.UAI as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "collect(distinct s.UAI) as profiles " +
                "order by " + "`" + PERSON_ID + "`";


        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
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

                processProfiles(student, STUDENT_PROFILE, null);

                if(isMandatoryFieldsAbsent(student, STUDENT_NODE_MANDATORY)) {
                    log.warn("Mediacentre : mandatory attribut for Student : " + student);
                    continue;
                }

                reorganizeNodes(student);

                xmlExportHelper.saveObject(STUDENT_NODE, student);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing students Info : " + e.getMessage());
        }
    }

    /**
     * XSD specify precise order for xml tags
     * @param student
     */
    private void reorganizeNodes(JsonObject student) {
        JsonObject personCopy = student.copy();
        student.clear();
        student.put(PERSON_ID, personCopy.getValue(PERSON_ID));
        student.put(PERSON_PROFILES, personCopy.getValue(PERSON_PROFILES));
        student.put(PERSON_PATRO_NAME, personCopy.getValue(PERSON_PATRO_NAME));
        student.put(PERSON_NAME, personCopy.getValue(PERSON_NAME));
        student.put(PERSON_FIRST_NAME, personCopy.getValue(PERSON_FIRST_NAME));
        student.put(PERSON_OTHER_NAMES, personCopy.getValue(PERSON_OTHER_NAMES));
        //TODO GARPersonCivilite
        student.put(PERSON_STRUCT_ATTACH, personCopy.getValue(PERSON_STRUCT_ATTACH));
        student.put(PERSON_STRUCTURE, personCopy.getValue(PERSON_STRUCTURE));
        student.put(PERSON_BIRTH_DATE, personCopy.getValue(PERSON_BIRTH_DATE));
    }

    /**
     * Get students mefs from Neo4j
     * @param handler results
     */
    private void getStudentsMefFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)";
        String dataReturn = "WHERE head(u.profiles) = 'Student'" +
                "AND HAS(s.exports) AND 'GAR' IN s.exports " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) "+
                "AND u.module  <>\"\""+
                "RETURN DISTINCT "+
                    "s.UAI as `" + STRUCTURE_UAI + "`, " +
                    "u.id as `" + PERSON_ID + "`, " +
                    "u.module as `" + MEF_CODE + "` " +
                "ORDER BY " + "`" + STRUCTURE_UAI + "`, `" + PERSON_ID + "`, `" + MEF_CODE + "` ";


        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
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


        //get all GAR structure UAI

        String query1 = "MATCH (s:Structure)" +
                "WHERE 'GAR' IN s.exports " +
                "RETURN s.UAI as UAI";

        neo4j.execute(query1, new JsonObject(), res -> {
            if (res.body() != null && res.body().containsKey("result")) {
                JsonArray garUAIs = res.body().getJsonArray("result");

                List<String> UAIs = new ArrayList<String>();
                garUAIs.forEach((entry) -> {
                    if (entry instanceof JsonObject) {
                        JsonObject field = (JsonObject) entry;
                        UAIs.add(field.getString("UAI", ""));
                    }
                });

                getStudentsFosByUAI(UAIs, 0, new JsonArray(), handler);
            }
        });
    }

    private void getStudentsFosByUAI(List<String> UAIs, int index, JsonArray finalResult, Handler<Either<String, JsonArray>> handler){
        String query = "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "WHERE head(u.profiles) = 'Student' AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) AND HAS(s.exports) " +
                " AND 'GAR' IN s.exports "+
                " AND s.UAI = {uai}";
        String dataReturn = "with u,s " +
                "unwind u.fieldOfStudy as fos " +
                "return distinct " +
                "s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "toUpper(fos) as `" + STUDYFIELD_CODE + "` " +
                "order by " + "`" + STRUCTURE_UAI + "`, `" + PERSON_ID + "`, `" + STUDYFIELD_CODE + "` ";

        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject()
                .put("uai", UAIs.get(index))
                .put("limit", paginator.LIMIT);
        int finalIndex = index + 1;
        paginator.neoStreamList(query, params, new JsonArray(), 0, resultNeo -> {
            if (resultNeo.isRight()) {
                finalResult.addAll(resultNeo.right().getValue());
                if(UAIs.size() > finalIndex){
                    getStudentsFosByUAI( UAIs, finalIndex, finalResult, handler);
                }
                else {
                    handler.handle(new Either.Right<>(finalResult));
                }
            }
            else {
                log.error("[DataServiceStudentImpl@getAndProcessStudentsInfo] Failed to process");
            }
        });
    };



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
