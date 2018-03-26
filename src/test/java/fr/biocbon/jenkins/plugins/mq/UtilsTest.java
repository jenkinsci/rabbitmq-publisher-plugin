package fr.biocbon.jenkins.plugins.mq;

import fr.frogdevelopment.jenkins.plugins.mq.Utils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

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

}