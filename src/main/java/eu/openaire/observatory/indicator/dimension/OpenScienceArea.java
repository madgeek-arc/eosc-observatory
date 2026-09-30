package eu.openaire.observatory.indicator.dimension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The Open Science topic areas of the national survey, with the survey question that answers each
 * kind of question about an area: the amount invested (in millions of Euros) and, for each
 * {@link InitiativeKind}, whether the country has that initiative (Yes/No). Question codes are the
 * composite field names of the "eosc-sb" survey models and are stable across survey years.
 */
public enum OpenScienceArea {

    OA_PUBLICATION("Open Access Publications", "Question56", "Question6", "Question7", "Question54"),
    FAIR_DATA("FAIR Data", "Question64", "Question14", "Question15", "Question62"),
    DATA_MANAGEMENT("Data Management", "Question60", "Question10", "Question11", "Question58"),
    OPEN_DATA("Open Data", "Question68", "Question18", "Question19", "Question66"),
    SOFTWARE("Open Software", "Question72", "Question22", "Question23", "Question70"),
    SERVICES("Services", "Question76", "Question26", "Question27", "Question74"),
    REPOSITORIES_CONNECT("Connecting repositories to EOSC", "Question80", "Question30", "Question31", "Question78"),
    DATA_STEWARDSHIP("Data Stewardship", "Question84", "Question34", "Question35", "Question82"),
    REPOSITORIES_PRESERVATION("Long-term Data Preservation", "Question88", "Question38", "Question39", "Question86"),
    TRAINING("Skills / Training", "Question92", "Question42", "Question43", "Question90"),
    ASSESSMENT("Incentives / Rewards for OS", "Question96", "Question46", "Question47", "Question94"),
    CITIZEN_SCIENCE("Citizen Science", "Question100", "Question50", "Question51", "Question98");

    /** The kinds of national initiative the survey asks about for every area. */
    public enum InitiativeKind {
        POLICY("Policy on %s"),
        FINANCIAL_STRATEGY("Financial Strategy on %s"),
        MONITORING_INITIATIVE("National Monitoring on %s");

        private final String labelPattern;

        InitiativeKind(String labelPattern) {
            this.labelPattern = labelPattern;
        }
    }

    /**
     * Initiative type of the specific policy on immediate open access to publications, a sub-question
     * of the open access publications policy that has no counterpart in the other areas.
     */
    public static final String IMMEDIATE_OA_POLICY = "IMMEDIATE_OA_POLICY";
    public static final String IMMEDIATE_OA_POLICY_QUESTION = "Question6.3";

    private final String label;
    private final String investmentQuestion;
    private final Map<InitiativeKind, String> initiativeQuestions;

    OpenScienceArea(String label, String investmentQuestion, String policyQuestion,
                    String financialStrategyQuestion, String monitoringQuestion) {
        this.label = label;
        this.investmentQuestion = investmentQuestion;
        this.initiativeQuestions = Map.of(
            InitiativeKind.POLICY, policyQuestion,
            InitiativeKind.FINANCIAL_STRATEGY, financialStrategyQuestion,
            InitiativeKind.MONITORING_INITIATIVE, monitoringQuestion);
    }

    public String label() {
        return label;
    }

    public String investmentQuestion() {
        return investmentQuestion;
    }

    public String initiativeQuestion(InitiativeKind kind) {
        return initiativeQuestions.get(kind);
    }

    /** The initiativeType member code of this area's initiative, e.g. {@code OPEN_DATA_POLICY}. */
    public String initiativeType(InitiativeKind kind) {
        return name() + "_" + kind.name();
    }

    public String initiativeTypeLabel(InitiativeKind kind) {
        return kind.labelPattern.formatted(label);
    }

    /** The initiativeType member codes of one kind across every area, in area order. */
    public static List<String> initiativeTypes(InitiativeKind kind) {
        return Stream.of(values()).map(area -> area.initiativeType(kind)).toList();
    }

    /** Survey question code by initiativeType member code, including {@link #IMMEDIATE_OA_POLICY}. */
    public static Map<String, String> initiativeQuestionCodes() {
        Map<String, String> codes = new LinkedHashMap<>();
        for (OpenScienceArea area : values()) {
            for (InitiativeKind kind : InitiativeKind.values()) {
                codes.put(area.initiativeType(kind), area.initiativeQuestion(kind));
            }
        }
        codes.put(IMMEDIATE_OA_POLICY, IMMEDIATE_OA_POLICY_QUESTION);
        return Collections.unmodifiableMap(codes);
    }

    /** Survey question code of the amount invested, by area code. */
    public static Map<String, String> investmentQuestionCodes() {
        Map<String, String> codes = new LinkedHashMap<>();
        for (OpenScienceArea area : values()) {
            codes.put(area.name(), area.investmentQuestion);
        }
        return Collections.unmodifiableMap(codes);
    }
}
