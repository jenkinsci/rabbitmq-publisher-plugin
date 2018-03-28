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
        Map<String, String> buildParameters = new HashMap<>();
        buildParameters.put("PARAM_1", "VALUE_1");
        buildParameters.put("PARAM_2", "VALUE_2");

        String message = "{\n\t\"field_1\":\"test\",\n\t\"field_2\":\"${param_1},\"\n\t\"field_3\":\"${param_2}\"\n}";

        String rawMessage = Utils.getRawMessage(buildParameters, message);


        Assertions.assertThat(rawMessage).isEqualTo("{\n\t\"field_1\":\"test\",\n\t\"field_2\":\"VALUE_1,\"\n\t\"field_3\":\"VALUE_2\"\n}");
    }

}