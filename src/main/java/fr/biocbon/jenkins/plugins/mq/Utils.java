package fr.biocbon.jenkins.plugins.mq;

abstract class Utils {

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
}
