package nl.aurorion.blockregen.util;

import java.util.Map;

// Implementation of a discrete random variable generator according to a probability function.
public class DiscreteGenerator<T> {
    private final Map<T, Double> probabilityFunction;

    private DiscreteGenerator(Map<T, Double> probabilityFunction) {
        this.probabilityFunction = probabilityFunction;
    }

    public static <T> DiscreteGenerator<T> fromProbabilityFunction(Map<T, Double> probabilityFunction) throws IllegalArgumentException {
        // check that all probabilities add up to 1
        double sum = probabilityFunction.values().stream().mapToDouble(e -> e).sum();
        // Using a lower eps for comparison has no significant effect on the probability.
        if (Math.abs(sum - 1.0) > 1E-10) {
            throw new IllegalArgumentException(String.format("Probability function must add to 1 (value: %.2f).", sum));
        }
        return new DiscreteGenerator<>(probabilityFunction);
    }

    public T next() {
        double s = Math.random();

        T choice = null;
        for (Map.Entry<T, Double> entry : probabilityFunction.entrySet()) {
            s = s - entry.getValue();
            choice = entry.getKey();

            if (s <= 0.0) {
                break;
            }
        }

        return choice;
    }
}
