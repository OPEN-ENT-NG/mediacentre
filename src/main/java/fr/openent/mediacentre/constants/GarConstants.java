package fr.openent.mediacentre.constants;

public class GarConstants {

    public static final String DEFAULT_CONTROL_GROUP = "GAR";

    public static final String STUDENT_ROOT = "men:GAR-ENT-Eleve";
    public static final String STUDENT_FILE_PARAM = "Eleve";
    public static final String STUDENT_PROFILE = "National_elv";
    public static final String STUDENT_NODE = "men:GAREleve";
    public static final String STUDENT_STUDYFIELD = "men:GAREleveEnseignement";

    public static final String DOCUMENTALIST_CODE = "DOC";
    public static final String DOCUMENTALIST_DESC= "DOCUMENTATION";


    public static final String TEACHER_ROOT = "men:GAR-ENT-Enseignant";
    public static final String TEACHER_FILE_PARAM = "Enseignant";
    public static final String TEACHER_PROFILE = "National_ens";
    public static final String DOCUMENTALIST_PROFILE = "National_doc";
    public static final String TEACHER_NODE = "men:GAREnseignant";
    public static final String TEACHER_POSITION = "men:GAREnsDisciplinesPostes";


    public static final String PERSON_ID = "men:GARPersonIdentifiant";
    public static final String PERSON_BIRTH_DATE = "men:GARPersonDateNaissance";
    public static final String PERSON_PATRO_NAME = "men:GARPersonNomPatro";
    public static final String PERSON_NAME = "men:GARPersonNom";
    public static final String PERSON_OTHER_NAMES = "men:GARPersonAutresPrenoms";
    public static final String PERSON_FIRST_NAME = "men:GARPersonPrenom";
    public static final String PERSON_MAIL = "men:GARPersonMail";
    public static final String PERSON_STRUCT_ATTACH = "men:GARPersonStructRattach";
    public static final String PERSON_STRUCTURE = "men:GARPersonEtab";
    public static final String PERSON_PROFILES = "men:GARPersonProfils";
    public static final String PERSON_PROFILE = "men:GARPersonProfil";
    public static final String PERSON_MEF = "men:GARPersonMEF";


    public static final String STRUCTURE_ROOT = "man:GAR-ENT-Etab";
    public static final String STRUCTURE_FILE_PARAM = "Etab";
    public static final String STRUCTURE_NODE = "man:GAREtab";
    public static final String STRUCTURE_UAI = "men:GARStructureUAI";
    public static final String STRUCTURE_CONTRACT = "men:GARStructureContrat";
    public static final String STRUCTURE_NAME = "men:GARStructureNomCourant";
    public static final String STRUCTURE_PHONE = "men:GARStructureTelephone";
    public static final String STRUCTURE_RATTACH = "men:GAREtablissementStructRattachFctl";

    public static final String GROUPS_ROOT = "man:GAR-ENT-Groupe";
    public static final String GROUPS_NODE = "man:GARGroupe";
    public static final String GROUPS_PERSON_NODE = "man:GARPersonGroupe";
    public static final String GROUPS_SUBJECT_NODE = "man:GAREnsClasseMatiere";
    public static final String GROUPS_FILE_PARAM = "Groupe";
    public static final String GROUPS_DIVISION_NAME = "DIVISION";
    public static final String GROUPS_GROUP_NAME = "GROUPE";
    public static final String GROUPS_CODE = "men:GARGroupeCode";
    public static final String GROUPS_DESC = "men:GARGroupeLibelle";
    public static final String GROUPS_STATUS = "men:GARGroupeStatut";
    public static final String GROUPS_DIVISION = "men:GARGroupeDivAppartenance";


    public static final String RESP_ROOT = "man:GAR-ENT-RespAff";
    public static final String RESP_NODE = "man:GARRespAff";
    public static final String RESP_ETAB = "man:GARRespAffEtab";
    public static final String RESP_FILE_PARAM = "RespAff";

    public static final String MEF_NODE = "men:GARMEF";
    public static final String MEF_CODE = "men:GARMEFCode";
    public static final String MEF_DESCRIPTION = "men:GARMEFLibelle";
    public static final String STUDYFIELD_NODE = "men:GARMatiere";
    public static final String STUDYFIELD_CODE = "men:GARMatiereCode";
    public static final String STUDYFIELD_DESC = "men:GARMatiereLibelle";
    public static final String POSITION_CODE = "men:GAREnsDisciplinePosteCode";


    public static final String[] STRUCTURE_NODE_MANDATORY = {STRUCTURE_UAI,STRUCTURE_NAME};
    public static final String[] MEF_NODE_MANDATORY = {STRUCTURE_UAI,MEF_CODE,MEF_DESCRIPTION};
    public static final String[] STUDYFIELD_NODE_MANDATORY = {STRUCTURE_UAI,STUDYFIELD_CODE,STUDYFIELD_DESC};
    public static final String[] GROUPS_NODE_MANDATORY = {GROUPS_CODE,STRUCTURE_UAI,GROUPS_DESC,GROUPS_STATUS};
    public static final String[] GROUPS_PERSON_NODE_MANDATORY = {GROUPS_CODE,STRUCTURE_UAI,PERSON_ID};
    public static final String[] GROUPS_SUBJECT_NODE_MANDATORY = {GROUPS_CODE,STRUCTURE_UAI,PERSON_ID,STUDYFIELD_CODE};
    public static final String[] STUDENT_NODE_MANDATORY = {PERSON_STRUCTURE,PERSON_ID,PERSON_PROFILES,
                                                            PERSON_NAME,PERSON_FIRST_NAME};
    public static final String[] PERSON_MEF_NODE_MANDATORY = {STRUCTURE_UAI,PERSON_ID,MEF_CODE};
    public static final String[] STUDENT_STUDYFIELD_NODE_MANDATORY = {STRUCTURE_UAI,PERSON_ID,STUDYFIELD_CODE,RESP_ETAB};

    public static final String[] TEACHER_NODE_MANDATORY = {PERSON_STRUCTURE,PERSON_ID,PERSON_PROFILES,
                                                            PERSON_NAME,PERSON_FIRST_NAME};
    public static final String[] RESP_NODE_MANDATORY = {PERSON_ID,PERSON_NAME,PERSON_FIRST_NAME};
}
