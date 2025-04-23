package ru.sibsutis.artificial_basis;

import java.io.*;
import java.util.*;

public class SimplexInputReader {
    public static SimplexProblem readFromFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String goalStr = reader.readLine().strip().toUpperCase();
        Goal goal = Goal.valueOf(goalStr);

        List<Fraction> zCoefficients = Arrays.stream(reader.readLine().split("\\s+"))
                .map(Fraction::parseFraction)
                .toList();

        int equationCount = Integer.parseInt(reader.readLine().strip());
        List<Equation> equations = new ArrayList<>();

        for (int i = 0; i < equationCount; i++) {
            String[] parts = reader.readLine().split("\\s+");
            List<Fraction> coefficients = Arrays.stream(parts, 0, parts.length - 2)
                    .map(Fraction::parseFraction)
                    .toList();
            String sign = parts[parts.length - 2];
            Fraction result = Fraction.parseFraction(parts[parts.length - 1]);
            equations.add(new Equation(coefficients, sign, result));
        }

        reader.close();
        return new SimplexProblem(goal, zCoefficients, equations);
    }
}