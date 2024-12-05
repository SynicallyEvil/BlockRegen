package nl.aurorion.blockregen.version;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * General deserializer for Node Data. All version specific parsers build a deserialized with different properties.
 *
 * @param <T> Implementation of NodeData to produce.
 */
@Log
public class NodeDataDeserializer<T extends NodeData> {

    public interface PropertyDeserializer<T extends NodeData> {
        void deserialize(T nodeData, String value);
    }

    private static final Pattern DATA_PATTERN = Pattern.compile("\\[(.*)]");
    private static final Pattern KEY_PATTERN = Pattern.compile("^(.*?)=");
    private Pattern propertyEqualsPattern = null;

    private final Map<String, PropertyDeserializer<T>> properties = new HashMap<>();

    public static <E extends Enum<E>> E tryParseEnum(String value, Class<E> clazz) {
        E face = ParseUtil.parseEnum(value.trim(), clazz);
        if (face == null) {
            // Fall back to ordinals (might be used for age for ex. on old versions where it's an enum)
            try {
                int id = Integer.parseInt(value.trim());
                return clazz.getEnumConstants()[id];
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(String.format("Invalid value '%s' for enum '%s'. Options: '%s'.", value, clazz.getSimpleName(),
                        Arrays.stream(clazz.getEnumConstants())
                                .map(v -> v.name() + " (" + v.ordinal() + ")")
                                .collect(Collectors.joining("', '"))));
            }
        }
        return face;
    }

    @NotNull
    public NodeDataDeserializer<T> property(@NotNull String key, @NotNull PropertyDeserializer<T> deserializer) {
        // Case-insensitive.
        properties.put(key.toLowerCase(), deserializer);
        return this;
    }

    @NotNull
    private Pattern generatePropertyEqualsPattern() {
        Set<String> properties = this.properties.keySet();
        // Allow property keys to be case-insensitive. Comfortable.
        // Match against specific keys instead of any keys. Use this to avoid having to solve URLs in values.
        return Pattern.compile(String.format("", String.join("|", properties)), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Deserialize NodeData from string form.
     * <p>
     * Parse each property and call the assigned property deserializer.
     *
     * @param nodeData NodeData to call the assigned property deserialized on.
     * @param input    String input to deserialize from.
     * @throws IllegalArgumentException If the input format is malformed in any way.
     */
    public void deserialize(@NotNull T nodeData, @NotNull String input) throws IllegalArgumentException {
        log.fine("Deserializing " + input);

        if (propertyEqualsPattern == null) {
            this.propertyEqualsPattern = generatePropertyEqualsPattern();
        }

        // Split out the [] section with actual data
        Matcher matcher = DATA_PATTERN.matcher(input);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Malformed node data syntax. Most likely missing ']'.");
        }

        String dataString = matcher.group(1);

        String[] dataParts = dataString.split(",");

        for (String dataPart : dataParts) {
            if (dataPart.trim().isEmpty()) {
                log.fine("Empty data part in '" + dataString + "'");
                continue;
            }

            Matcher keyMatcher = KEY_PATTERN.matcher(dataPart);

            if (!keyMatcher.find()) {
                throw new IllegalArgumentException(String.format("Malformed node data property part '%s'. Skipping.", dataPart));
            }

            String key = keyMatcher.group(1).substring(0, keyMatcher.end() - 1);
            String value = dataPart.substring(keyMatcher.end());

            log.fine("Key: " + key + ", value: " + value);

            PropertyDeserializer<T> deserializer = properties.get(key);

            if (deserializer == null) {
                throw new IllegalArgumentException(String.format("Unknown node data property %s in part %s. Valid properties: '%s'.", key, dataPart, String.join("', '", this.properties.keySet())));
            }

            try {
                deserializer.deserialize(nodeData, value);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Invalid value '%s' for property %s.", value, key), e);
            }
        }
    }
}
