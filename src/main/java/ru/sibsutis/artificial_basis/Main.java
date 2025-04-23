package ru.sibsutis.artificial_basis;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            SimplexProblem simplexProblem = SimplexInputReader.readFromFile("src/main/resources/extra.txt");
            SimplexSolver solver = new SimplexSolver(simplexProblem);
            solver.solve();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}