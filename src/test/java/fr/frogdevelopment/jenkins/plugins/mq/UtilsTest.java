package fr.frogdevelopment.jenkins.plugins.mq;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UtilsTest {

    @Test
    public void test_toCamelCase_withUnderscore() {
        Assertions.assertThat(Utils.toJava("WITH_UNDERSCORE")).isEqualTo("withUnderscore");
    }

    @Test
    public void test_toCamelCase_withoutUnderscore() {
        Assertions.assertThat(Utils.toJava("WITHOUTUNDERSCORE")).isEqualTo("withoutunderscore");
    }

    @Test
    public void test_toCamelCase_startWithUnderscore() {
        Assertions.assertThat(Utils.toJava("_START_WITH_UNDERSCORE")).isEqualTo("startWithUnderscore");
    }

    @Test
    public void test_toCamelCase_endWithUnderscore() {
        Assertions.assertThat(Utils.toJava("END_WITH_UNDERSCORE_")).isEqualTo("endWithUnderscore");
    }

    @Test
    public void test_toCamelCase_allLowerCase() {
        Assertions.assertThat(Utils.toJava("all_lower_case")).isEqualTo("allLowerCase");
    }

    @Test
    public void test_toCamelCase_allUpperCase() {
        Assertions.assertThat(Utils.toJava("ALL_UPPER_CASE")).isEqualTo("allUpperCase");
    }

    @Test
    public void test_getRawMessage() {
        // data
        Map<String, String> buildParameters = new HashMap<>();
        buildParameters.put("PARAM_1", "VALUE_1");
        buildParameters.put("PARAM_2", "VALUE_2");
        buildParameters.put("PARAM_EMPTY", "");
        buildParameters.put("PARAM_NULL", null);

        String message = "{\n" +
                "\t\"field_1\": \"test\",\n" +
                "\t\"field_2\": \"${PARAM_1}\",\n" +
                "\t\"field_3\": \"$PARAM_2\",\n" +
                "\t\"field_empty\": \"${PARAM_EMPTY}\",\n" +
                "\t\"field_null\": ${PARAM_NULL}\n" +
                "}";

        // call
        String rawMessage = Utils.getRawMessage(buildParameters, message);

        // assertions
        Assertions.assertThat(rawMessage).isEqualTo("{\n" +
                "\t\"field_1\": \"test\",\n" +
                "\t\"field_2\": \"VALUE_1\",\n" +
                "\t\"field_3\": \"VALUE_2\",\n" +
                "\t\"field_empty\": \"\",\n" +
                "\t\"field_null\": null\n" +
                "}");
    }

    @Test
    public void test_getJsonMessage() {
        // data
        Map<String, String> buildParameters = new HashMap<>();
        buildParameters.put("PARAM_1", "VALUE_1");
        buildParameters.put("PARAM_2", "VALUE_2");
        buildParameters.put("PARAM_EMPTY", "");
        buildParameters.put("PARAM_NULL", null);

        String message = "field_1=test\n" +
                "field_2=\"${PARAM_1}\"\n" +
                "field_3=\"$PARAM_2\"\n" +
                "field_empty=\"${PARAM_EMPTY}\"\n" +
                "field_null=${PARAM_NULL}";

        // call
        String jsonMessage = Utils.getJsonMessage(buildParameters, message);

        // assertions
        Assertions.assertThat(jsonMessage).isEqualTo("{\"field1\":\"test\",\"field2\":\"VALUE_1\",\"field3\":\"VALUE_2\",\"fieldEmpty\":\"\",\"fieldNull\":null}");
    }

}