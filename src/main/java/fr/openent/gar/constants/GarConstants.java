package fr.openent.gar.constants;

public class GarConstants {

    public static final String DEFAULT_CONTROL_GROUP = "GAR";

    public static final String STUDENT_ROOT = "men:GAR-ENT-Eleve";
    public static final String STUDENT_FILE_PARAM = "Eleve";
    public static final String STUDENT_PROFILE = "National_elv";
    public static final String STUDENT_NODE = "men:GAREleve";
    public static final String STUDENT_STUDYFIELD = "men:GAREleveEnseignement";

    //National_doc
    public static final String DOCUMENTALIST_CODE = "DOC";

    //National_dir
    public static final String DIRECTOR_CODE = "DIR";

    //National_evs ; personnel de vie scolaire travaillant dans l’établissement
    public static final String EDUCATION_CODE = "EDU";
    public static final String EDUCATION_ASSISTANT_CODE = "AED";
    public static final String ACCOMPAGNMENT_HANDICAP_CODE = "ASH";
    public static final String ACCOMPAGNEMENT_HANDICAP_2_CODE = "AES";
    public static final String ETRANGER_ASSISTANT_CODE = "ASE";
    public static final String SURVEILLANCE_CODE = "SUR";
    public static final String CPE_CODE = "CPE";

    //National_eta ; personnel administratif, technique ou d’encadrement travaillant dans personnel administratif, technique ou d’encadrement travaillant dans l’établissement
    public static final String ADMINISTRATION_CODE = "ADM";
    public static final String PERSONNEL_ADMINISTRATIF_CODE = "ADF";
    public static final String CHEF_DE_TRAVAUX_CODE = "CTR";
    public static final String PSYCHOLOGUE_CODE = "PSY";
    public static final String OUVRIER_CODE = "OUV";
    public static final String LABORATOIRE_CODE = "LAB";
    public static final String PERSONNEL_MEDICO_SOCIAUX_CODE = "MDS";
    public static final String PERSONNEL_TECHNIQUE_CODE = "TEC";
    public static final String READAPTATION_CODE = "ADA";
    public static final String CONSEILLER_FORMATION_CONTINUE_CODE = "CFC";
    public static final String CONSEILLER_ORIENTATION_CODE = "ORI";
    public static final String EDUCATEUR_INTERNAT_CODE = "EEI";
    public static final String APPRENTISSAGE_CODE = "APP";
    public static final String FORMATION_CONTINUE_ADULTES_CODE = "FCA";
    public static final String APPRENTI_CLASSIQUE_PROFESSEUR_CODE = "ACP";
    public static final String FORMATION_INSERTION_JEUNES_CODE = "FIJ";

    //National_col ; personnel de collectivité territoriale
    public static final String COLLECTIVITE_TERRITORIALE_CODE = "COL";

    //National_ens ; enseignant
    public static final String ENSEIGNANT_CODE = "ENS";

    public static final String TEACHER_ROOT = "men:GAR-ENT-Enseignant";
    public static final String TEACHER_FILE_PARAM = "Enseignant";
    public static final String TEACHER_PROFILE = "National_ens";
    public static final String DOCUMENTALIST_PROFILE = "National_doc";
    public static final String DIRECTOR_PROFILE = "National_dir";
    public static final String VIE_SCOLAIRE_PROFILE = "National_evs";
    public static final String PERSONNEL_ETAB_PROFILE = "National_eta";
    public static final String COLLECTIVITE_TERRITORIALE_PROFILE = "National_col";
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


    public static final String STRUCTURE_ROOT = "men:GAR-ENT-Etab";
    public static final String STRUCTURE_FILE_PARAM = "Etab";
    public static final String STRUCTURE_NODE = "men:GAREtab";
    public static final String STRUCTURE_UAI = "men:GARStructureUAI";
    public static final String STRUCTURE_CONTRACT = "men:GARStructureContrat";
    public static final String STRUCTURE_NAME = "men:GARStructureNomCourant";
    public static final String STRUCTURE_PHONE = "men:GARStructureTelephone";
    public static final String STRUCTURE_RATTACH = "men:GAREtablissementStructRattachFctl";

    public static final String GROUPS_ROOT = "men:GAR-ENT-Groupe";
    public static final String GROUPS_NODE = "men:GARGroupe";
    public static final String GROUPS_PERSON_NODE = "men:GARPersonGroupe";
    public static final String GROUPS_CLASS_SUBJECT_NODE = "men:GAREnsClasseMatiere";
    public static final String GROUPS_GROUP_SUBJECT_NODE = "men:GAREnsGroupeMatiere";
    public static final String GROUPS_FILE_PARAM = "Groupe";
    public static final String GROUPS_DIVISION_NAME = "DIVISION";
    public static final String GROUPS_GROUP_NAME = "GROUPE";
    public static final String GROUPS_CODE = "men:GARGroupeCode";
    public static final String GROUPS_DESC = "men:GARGroupeLibelle";
    public static final String GROUPS_STATUS = "men:GARGroupeStatut";
    public static final String GROUPS_DIVISION = "men:GARGroupeDivAppartenance";


    public static final String RESP_ROOT = "men:GAR-ENT-RespAff";
    public static final String RESP_NODE = "men:GARRespAff";
    public static final String RESP_ETAB = "men:GARRespAffEtab";
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
                                                            PERSON_NAME,PERSON_FIRST_NAME, PERSON_OTHER_NAMES};
    public static final String[] PERSON_MEF_NODE_MANDATORY = {STRUCTURE_UAI,PERSON_ID,MEF_CODE};
    public static final String[] STUDENT_STUDYFIELD_NODE_MANDATORY = {STRUCTURE_UAI,PERSON_ID,STUDYFIELD_CODE};

    public static final String[] TEACHER_NODE_MANDATORY = {PERSON_STRUCTURE,PERSON_ID,PERSON_PROFILES,
                                                            PERSON_NAME,PERSON_FIRST_NAME, PERSON_OTHER_NAMES};
    public static final String[] RESP_NODE_MANDATORY = {PERSON_ID,PERSON_NAME,PERSON_FIRST_NAME,PERSON_MAIL};
}
