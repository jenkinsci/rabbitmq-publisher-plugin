package fr.frogdevelopment.jenkins.plugins.mq;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class Utils {

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{?(?<param>\\w+)}?");

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * Transform a string with "_" to camelCase string, for Java convention.<br>
     * Ex : <ul>
     * <li>NB_DAYS => nbDays</li>
     * <li>MY_param => myParam</li>
     * <li>other_PARAM => otherParam</li>
     * </ul>
     *
     * @param value text to transform
     * @return processed text
     */
    static String toJava(String value) {
        // use an all lower case string
        StringBuilder sb = new StringBuilder(value.toLowerCase());

        int length = sb.length();
        for (int i = 0; i < length; i++) {
            if (sb.charAt(i) == '_') {
                sb.deleteCharAt(i);

                // handle last character is '_'
                if (i == length - 1) {
                    break;
                }

                sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
                length--;
            }
        }

        // handle 1st character is '_' => lower case new 1st character
        sb.replace(0, 1, String.valueOf(Character.toLowerCase(sb.charAt(0))));

        return sb.toString();
    }

    static String getRawMessage(Map<String, String> buildParameters, String message) {

        // resolving build data
        StringBuffer sb = new StringBuffer();

        Matcher matcher = PARAM_PATTERN.matcher(message);
        while (matcher.find()) {
            String param = matcher.group("param").toUpperCase();
            if (buildParameters.containsKey(param)) {
                String replacement = buildParameters.get(param);
                if (replacement != null) {
                    matcher.appendReplacement(sb, replacement);
                } else {
                    matcher.appendReplacement(sb, "null");
                }
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    static String getJsonMessage(Map<String, String> buildParameters, String message) {
        boolean hasError = false;

        // constructing JSON message
        JSONObject jsonObject = new JSONObject();

        String[] lines = message.split("\\r?\\n");
        if (lines.length > 0) {
            for (String line : lines) {
                String[] splitLine = line.split("=");
                if (splitLine.length == 2) {
                    String paramKey = splitLine[0];
                    String paramValue = splitLine[1];
                    if (StringUtils.isNotBlank(paramKey)) {
                        Matcher matcher = PARAM_PATTERN.matcher(paramValue);
                        if (matcher.find()) {
                            String param = matcher.group("param").toUpperCase();
                            if (buildParameters.containsKey(param)) {
                                paramValue = buildParameters.get(param);
                            }
                        }

                        LOGGER.info("\t- " + paramKey + "=" + paramValue);
                        if (paramValue != null) {
                            jsonObject.put(toJava(paramKey), paramValue);
                        } else {
                            jsonObject.put(toJava(paramKey), "null");
                        }
                    } else {
                        LOGGER.info("\t- Empty key for line : {}", line);
                        hasError = true;
                    }
                } else {
                    LOGGER.error("\t- Incorrect data format : {}", line);
                    hasError = true;
                }
            }
        }

        if (hasError) {
            throw new IllegalStateException("Incorrect data");
        }

        return jsonObject.toString();
    }
}
