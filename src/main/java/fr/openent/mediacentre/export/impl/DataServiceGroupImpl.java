package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceGroupImpl extends DataServiceBaseImpl implements DataService {

    DataServiceGroupImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, GROUPS_ROOT, GROUPS_FILE_PARAM, strDate);
    }

    /**
     * Export Data to folder
     * - Export Groups info
     * - Export Groups content (people into the groups)
     * - Export Groups fields of study     *
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getAndProcessGroupsInfoFromNeo4j(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> groupsResults) {
                if (validResponse(groupsResults, handler)) {

                    getAndProcessGroupsPersonFromNeo4j(new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> groupPersonResults) {
                            if (validResponse(groupPersonResults, handler)) {

                                getAndProcessGroupsFosFromNeo4j(new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(Either<String, JsonObject> groupFosResults) {
                                        if (validResponse(groupFosResults, handler)) {

                                            xmlExportHelper.closeFile();
                                            handler.handle(new Either.Right<String, JsonObject>(
                                                    new JsonObject().put(
                                                            FILE_LIST_KEY,
                                                            xmlExportHelper.getFileList()
                                                    )));

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
     * Process groups info, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessGroupsInfoFromNeo4j(final Handler<Either<String, JsonObject>> handler) {

        getGroupsInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> groupsResults) {
                if( validResponseNeo4j(groupsResults, handler) ) {
                    Either<String,JsonObject> result = processGroupsInfo( groupsResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Process groups content, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessGroupsPersonFromNeo4j(final Handler<Either<String, JsonObject>> handler) {

        getGroupsPersonFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> groupPersonResults) {
                if( validResponseNeo4j(groupPersonResults, handler) ) {
                    Either<String,JsonObject> result = processGroupPersonInfo( groupPersonResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Process groups fos, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessGroupsFosFromNeo4j(final Handler<Either<String, JsonObject>> handler) {

        getGroupsFosFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> groupFosResults) {
                if( validResponseNeo4j(groupFosResults, handler) ) {
                    Either<String,JsonObject> result = processGroupFosInfo( groupFosResults.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }




    /**
     * Get groups info from Neo4j
     * Get classes (or divisions)
     * Get user groups. Link groups to classes only for students
     * Get group external id if it exists, else get internal id
     * @param handler results
     */
    private void getGroupsInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String classQuery = "MATCH (c:Class)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "RETURN distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "split(c.externalId,\"$\")[1] as `" + GROUPS_CODE + "`, " +
                "c.name as `" + GROUPS_DESC + "`, " +
                "null as `" + GROUPS_DIVISION + "`, " +
                "\"" + GROUPS_DIVISION_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "` " +
                "UNION ";
        String groupsQuery = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->" +
                "(s:Structure)<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "OPTIONAL MATCH (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User) " +
                "WHERE u.profiles = ['Student'] " +
                "with s.UAI as uai, " +
                "coalesce(split(fg.externalId,\"$\")[1], fg.id) as id, " +
                "collect(split(c.externalId,\"$\")[1]) as dividlist, " +
                "fg.name as name " +
                "UNWIND (CASE dividlist WHEN [] then [null] else dividlist end) as divid " +
                "return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "id as `" + GROUPS_CODE + "`, " +
                "name as `" + GROUPS_DESC + "`, " +
                "divid as `" + GROUPS_DIVISION + "`, " +
                "\"" + GROUPS_GROUP_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "`";
        neo4j.execute(classQuery + groupsQuery, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups info
     * @param groups Array of groups from Neo4j
     */
    private Either<String,JsonObject> processGroupsInfo(JsonArray groups) {
        Either<String,JsonObject> event =  processSimpleArray(groups, GROUPS_NODE, GROUPS_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing groups infos : " + event.left().getValue());
        } else {
            return event;
        }

    }

    /**
     * Get groups content from Neo4j
     * Use external id for groups when available
     * @param handler results
     */
    private void getGroupsPersonFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String classQuery = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "coalesce(split(c.externalId,\"$\")[1], c.id) as `" + GROUPS_CODE + "` " +
                "UNION ";
        String groupsQuery = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "coalesce(split(fg.externalId,\"$\")[1], fg.id) as `" + GROUPS_CODE + "` ";
        neo4j.execute(classQuery + groupsQuery, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups content
     * @param groupPerson Array of group content from Neo4j
     */
    private Either<String,JsonObject> processGroupPersonInfo(JsonArray groupPerson) {
        Either<String,JsonObject> event =
                processSimpleArray(groupPerson, GROUPS_PERSON_NODE, GROUPS_PERSON_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing groups content : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Get groups fields of study from Neo4j
     * Use external id for groups when available
     * Field of study code may be prefixed by ACADEMY-
     * @param handler results
     */
    private void getGroupsFosFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "with u.id as uid, t.groups + t.classes as grouplist, sub.code as code, s.UAI as uai " +
                "unwind(grouplist) as group ";
        String dataReturn = "return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "uid as `" + PERSON_ID + "`, " +
                "split(group,\"$\")[1] as `" + GROUPS_CODE + "`, " +
                "coalesce(split(code,\"-\")[1], code) as `" + STUDYFIELD_CODE + "` " +
                "order by `" + PERSON_ID + "`, `" + STRUCTURE_UAI + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups subjects
     * @param groupSubject Array of group subjects from Neo4j
     */
    private Either<String,JsonObject> processGroupFosInfo(JsonArray groupSubject) {
        Either<String,JsonObject> event =
                processSimpleArray(groupSubject, GROUPS_SUBJECT_NODE, GROUPS_SUBJECT_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing groups fos : " + event.left().getValue());
        } else {
            return event;
        }
    }


}
