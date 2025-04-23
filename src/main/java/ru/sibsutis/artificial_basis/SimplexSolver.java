package ru.sibsutis.artificial_basis;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SimplexSolver {
    private Goal goal;
    private List<Fraction> zCoefficients;
    private List<Equation> equations;
    private int originalVarsCount;

    private List<Integer> artificialVars;
    private boolean hasMRow;
    private List<Integer> basis;
    private SimplexTable simplexTable;

    private int iteration;

    public SimplexSolver(SimplexProblem simplexProblem) {
        this.goal = simplexProblem.getGoal();
        this.zCoefficients = new ArrayList<>(simplexProblem.getZCoefficients());
        this.equations = new ArrayList<>(simplexProblem.getEquations());
        this.artificialVars = new ArrayList<>();
        this.originalVarsCount = zCoefficients.size();
        toCanonicalForm();
        printCanonicalForm();
        addArtificialVars();
        buildInitialSimplexTable();
        this.iteration = 1;
    }

    private void toCanonicalForm() {
        if (goal == Goal.MIN) {
            zCoefficients = new ArrayList<>(zCoefficients.stream()
                    .map(c -> c.multiply(-1))
                    .toList());
        }

        int sluckVarsCount = 0;
        int maxLength = 0;

        for (Equation equation : equations) {
            Fraction result = equation.getResult();
            List<Fraction> coefficients = new ArrayList<>(equation.getCoefficients());
            String sign = equation.getSign();

            if (result.getNumerator().intValue() < 0) {
                coefficients = new ArrayList<>(coefficients.stream()
                        .map(c -> c.multiply(-1))
                        .toList());
                result = result.multiply(-1);
                sign = sign.equals("<=") ? ">=" : sign.equals(">=") ? "<=" : "=";
                equation.setSign(sign);
            }

            for (int i = 0; i < sluckVarsCount; i++) {
                coefficients.add(Fraction.ZERO);
            }

            if (sign.equals("<=")) {
                coefficients.add(Fraction.ONE);
                zCoefficients.add(Fraction.ZERO);
                sluckVarsCount++;
            } else if (sign.equals(">=")) {
                coefficients.add(Fraction.MINUS_ONE);
                zCoefficients.add(Fraction.ZERO);
                sluckVarsCount++;
            }

            equation.setCoefficients(coefficients);
            equation.setResult(result);

            maxLength = Math.max(coefficients.size(), maxLength);
        }

        for (Equation equation : equations) {
            List<Fraction> coefficients = equation.getCoefficients();
            for (int i = coefficients.size(); i < maxLength; i++) {
                coefficients.add(Fraction.ZERO);
            }
        }
    }

    private void addArtificialVars() {
        int artificialStart = zCoefficients.size();

        for (int i = 0; i < equations.size(); i++) {
            final int curr = i;
            boolean hasBasis = false;
            Equation equation = equations.get(i);
            List<Fraction> row = equation.getCoefficients();

            for (int j = 0; j < row.size(); j++) {
                if (!row.get(j).equals(Fraction.ONE)) continue;

                final int col = j;
                if (equations.stream()
                        .filter(eq -> equations.indexOf(eq) != curr)
                        .allMatch(eq -> eq.getCoefficients().get(col).equals(Fraction.ZERO))) {
                    hasBasis = true;
                    break;
                }
            }

            if (!hasBasis) {
                artificialVars.add(artificialStart + artificialVars.size());
            }
        }
    }

    private void buildInitialSimplexTable() {
        basis = new ArrayList<>();
        List<List<Fraction>> rows = new ArrayList<>();

        for (Equation equation : equations) {
            List<Fraction> simplexTableRow = new ArrayList<>(equation.getCoefficients());
            simplexTableRow.add(equation.getResult());
            rows.add(simplexTableRow);
        }

        int artificialIndex = 0;
        for (int i = 0; i < equations.size(); i++) {
            final int curr = i;
            boolean hasBasis = false;

            for (int j = 0; j < zCoefficients.size(); j++) {
                final int col = j;
                if (!rows.get(i).get(col).equals(Fraction.ONE)) continue;

                if (rows.stream()
                        .filter(row -> rows.indexOf(row) != curr)
                        .allMatch(row -> row.get(col).equals(Fraction.ZERO))) {
                    hasBasis = true;
                    basis.add(j);
                    break;
                }
            }

            if (!hasBasis) {
                final int insertPos = zCoefficients.size() + artificialIndex;
                for (List<Fraction> row : rows) {
                    row.add(insertPos, Fraction.ZERO);
                }
                rows.get(i).set(insertPos, Fraction.ONE);
                basis.add(insertPos);
                artificialIndex++;
            }
        }

        List<Fraction> zRow;
        if (goal == Goal.MAX) {
            zRow = new ArrayList<>(zCoefficients);
        } else {
            zRow = new ArrayList<>(zCoefficients.stream()
                    .map(c -> c.multiply(-1))
                    .toList());
        }

        while (zRow.size() < rows.getFirst().size() - 1) {
            zRow.add(Fraction.ZERO);
        }
        zRow.add(Fraction.ZERO);

        List<Fraction> mRow = new ArrayList<>(
                Collections.nCopies(rows.getFirst().size(), Fraction.ZERO));

        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (var >= zCoefficients.size()) { // !
                for (int j = 0; j < mRow.size(); j++) {
                    mRow.set(j, mRow.get(j).subtract(rows.get(i).get(j)));
                }
            }
        }

        for (int var : artificialVars) {
            if (var < mRow.size()) {
                mRow.set(var, Fraction.ZERO);
            }
        }

        hasMRow = true;

        for (int col = rows.getFirst().size() - 2; col >= 0; col--) {
            final int column = col;
            if (rows.stream().allMatch(row -> row.get(column).equals(Fraction.ZERO))) {
                rows.forEach(row -> row.remove(column));
                basis = new ArrayList<>(basis.stream()
                        .map(b -> b > column ? b - 1 : b)
                        .toList());
            }
        }

        this.simplexTable = new SimplexTable(rows, zRow, mRow, new Pivot());
    }

    private void updateMRow() {
        if (!hasMRow || simplexTable.getRows().isEmpty()) return;

        List<Fraction> mRow = new ArrayList<>();
        int width = simplexTable.getRows().getFirst().size();

        for (int j = 0; j < width; j++) {
            mRow.add(Fraction.ZERO);
        }

        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (artificialVars.contains(var)) {
                List<Fraction> row = simplexTable.getRows().get(i);
                for (int j = 0; j < width; j++) {
                    mRow.set(j, mRow.get(j).subtract(row.get(j)));
                }
            }
        }

        for (int var : artificialVars) {
            if (var < mRow.size()) {
                mRow.set(var, Fraction.ZERO);
            }
        }

        simplexTable.setMRow(mRow);
    }

    private void performPivotOperation() {
        Pivot pivot = simplexTable.getPivot();
        int row = pivot.getRow();
        int col = pivot.getColumn();
        List<List<Fraction>> rows = simplexTable.getRows();
        List<Fraction> zRow = simplexTable.getZRow();
        List<Fraction> mRow = simplexTable.getMRow();
        List<Fraction> pivotRow = rows.get(row);
        Fraction pivotValue = pivotRow.get(col);

        // Делим всю строку на опорный элемент
        List<Fraction> newRow = new ArrayList<>();
        for (Fraction value : pivotRow) {
            newRow.add(value.divide(pivotValue));
        }
        rows.set(row, newRow);

        // Обновляем все остальные строки
        for (int i = 0; i < rows.size(); i++) {
            if (i == row) continue;

            List<Fraction> currentRow = rows.get(i);
            Fraction factor = currentRow.get(col);
            List<Fraction> updatedRow = new ArrayList<>();

            for (int j = 0; j < currentRow.size(); j++) {
                Fraction updatedValue = currentRow.get(j).subtract(factor.multiply(newRow.get(j)));
                updatedRow.add(updatedValue);
            }

            rows.set(i, updatedRow);
        }

        List<Fraction> updatedZRow = new ArrayList<>();
        Fraction factor = zRow.get(col);
        for (int j = 0; j < zRow.size(); j++) {
            Fraction updatedValue = zRow.get(j).subtract(factor.multiply(newRow.get(j)));
            updatedZRow.add(updatedValue);
        }
        simplexTable.setZRow(updatedZRow);

        List<Fraction> updatedMRow = new ArrayList<>();
        factor = mRow.get(col);
        for (int j = 0; j < mRow.size(); j++) {
            Fraction updatedValue = mRow.get(j).subtract(factor.multiply(newRow.get(j)));
            updatedMRow.add(updatedValue);
        }
        simplexTable.setMRow(updatedMRow);

        // Обновляем базис
        basis.set(row, col);
        updateMRow();
    }

    private void findPivot() {
        List<List<Fraction>> rows = simplexTable.getRows();
        // Исключаем столбец результатов (1)
        List<Fraction> zRow = simplexTable.getZRow().subList(0, simplexTable.getZRow().size() - 1);

        // Используем M-строку, если она ещё есть
        if (hasMRow) {
            List<Fraction> mRow = simplexTable.getMRow().subList(0, simplexTable.getMRow().size() - 1);
            if (mRow.stream().anyMatch(f -> f.compareTo(Fraction.ZERO) < 0)) {
                int col = Integer.MAX_VALUE;
                Fraction minFraction = Fraction.MAX_VALUE;
                for (int i = 0; i < mRow.size(); i++) {
                    if (mRow.get(i).compareTo(minFraction) < 0) {
                        minFraction = mRow.get(i);
                        col = i;
                    }
                }

                if (col == Integer.MAX_VALUE) {
                    simplexTable.setPivot(new Pivot(null, null)); // Решение не ограничено
                    return;
                }

                //  Поиск ведущей строки — минимальное положительное отношение b_i / a_ij
                int row = Integer.MAX_VALUE;
                minFraction = Fraction.MAX_VALUE;
                for (int i = 0; i < rows.size(); i++) {
                    Fraction a_ij = rows.get(i).get(col);
                    if (a_ij.compareTo(Fraction.ZERO) > 0) {
                        Fraction b_i = rows.get(i).getLast();
                        Fraction result = b_i.divide(a_ij);
                        if (result.compareTo(minFraction) < 0) {
                            minFraction = result;
                            row = i;
                        }
                    }
                }

                if (row == Integer.MAX_VALUE) {
                    simplexTable.setPivot(new Pivot(null, null)); // Решение не ограничено
                    return;
                }

                simplexTable.setPivot(new Pivot(row, col));
                return;
            }
        }

        // Иначе используем Z-строку
        if (zRow.stream().anyMatch(f -> f.compareTo(Fraction.ZERO) < 0)) {
            int col = Integer.MAX_VALUE;
            Fraction minFraction = Fraction.MAX_VALUE;
            for (int i = 0; i < zRow.size(); i++) {
                if (zRow.get(i).compareTo(minFraction) < 0) {
                    minFraction = zRow.get(i);
                    col = i;
                }
            }

            if (col == Integer.MAX_VALUE) {
                simplexTable.setPivot(new Pivot(null, null)); // Решение не ограничено
                return;
            }

            //  Поиск ведущей строки — минимальное положительное отношение b_i / a_ij
            int row = Integer.MAX_VALUE;
            minFraction = Fraction.MAX_VALUE;
            for (int i = 0; i < rows.size(); i++) {
                Fraction a_ij = rows.get(i).get(col);
                if (a_ij.compareTo(Fraction.ZERO) > 0) {
                    Fraction b_i = rows.get(i).getLast();
                    Fraction result = b_i.divide(a_ij);
                    if (result.compareTo(minFraction) < 0) {
                        minFraction = result;
                        row = i;
                    }
                }
            }

            if (row == Integer.MAX_VALUE) {
                simplexTable.setPivot(new Pivot(null, null)); // Решение не ограничено
                return;
            }

            simplexTable.setPivot(new Pivot(row, col));
            return;
        }

        simplexTable.setPivot(new Pivot(null, null));
    }

    private void removeArtificialVars() {
        List<Integer> colsToRemove = new ArrayList<>();
        for (int var : artificialVars) {
            if (!basis.contains(var)) {
                colsToRemove.add(var);
            }
        }

        // Удаление столбцов в обратном порядке
        colsToRemove.sort(Collections.reverseOrder());
        for (int col : colsToRemove) {
            for (List<Fraction> row : simplexTable.getRows()) {
                row.remove(col);
            }
            basis.replaceAll(b -> b > col ? b - 1 : b);
        }

        // Обновление списка искусственных переменных
        artificialVars.removeIf(var -> !basis.contains(var));

        // Удаление M-строки если нужно
        if (hasMRow && simplexTable.getMRow() != null) {
            List<Fraction> mRow = simplexTable.getMRow();
            System.out.println("Delete the M-row: " + mRow);
            mRow.clear();
            hasMRow = false;
        }
    }

    private void restoreOriginalZRow() {
        List<Fraction> zRow = new ArrayList<>(
                Collections.nCopies(simplexTable.getRows().getFirst().size(), Fraction.ZERO));

        // Восстановление исходных коэффициентов
        int objSize = zCoefficients.size();
        for (int j = 0; j < Math.min(objSize, zRow.size() - 1); j++) {
            zRow.set(j, zCoefficients.get(j).multiply(-1));
        }

        // Корректировка по базису
        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (var < objSize) {
                Fraction coef = zCoefficients.get(var);
                List<Fraction> tableRow = simplexTable.getRows().get(i);
                for (int j = 0; j < zRow.size(); j++) {
                    zRow.set(j, zRow.get(j).add(coef.multiply(tableRow.get(j))));
                }
            }
        }

        simplexTable.setZRow(zRow);
    }

    private boolean isOptimal() {
        List<Fraction> zRow = simplexTable.getZRow();
        return zRow.stream()
                .limit(zRow.size() - 1)
                .noneMatch(f -> f.compareTo(Fraction.ZERO) < 0);
    }

    private void performPivotOperationPhaseTwo() {
        List<List<Fraction>> rows = simplexTable.getRows();
        int row = simplexTable.getPivot().getRow();
        int col = simplexTable.getPivot().getColumn();
        List<Fraction> pivotRow = new ArrayList<>(rows.get(row));
        Fraction pivotVal = pivotRow.get(col);

        List<Fraction> newRow = new ArrayList<>();
        for (Fraction value : pivotRow) {
            newRow.add(value.divide(pivotVal));
        }
        rows.set(row, newRow);

        // Обновление остальных строк
        for (int i = 0; i < rows.size(); i++) {
            if (i == row) continue;

            List<Fraction> currentRow = rows.get(i);
            Fraction factor = currentRow.get(col);
            List<Fraction> updatedRow = new ArrayList<>();

            for (int j = 0; j < currentRow.size(); j++) {
                Fraction result = currentRow.get(j).subtract(factor.multiply(newRow.get(j)));
                updatedRow.add(result);
            }

            rows.set(i, updatedRow);
        }

        // Обновление базиса
        basis.set(row, col);
        restoreOriginalZRow();
    }

    public void solve() {
        // Phase I
        while (true) {
            updateMRow();
            List<Fraction> mRow = simplexTable.getMRow();

            if (mRow.stream()
                    .limit(mRow.size() - 1)
                    .allMatch(f -> f.equals(Fraction.ZERO))) {

                break;
            }

            if (mRow.stream()
                    .limit(mRow.size() - 1)
                    .allMatch(x -> x.compareTo(Fraction.ZERO) >= 0)) {

                System.out.println("\nThe system of constraints is inconsistent: only positive values remain in the M-row, and the solution is impossible.");
                return;
            }

            findPivot();
            if (simplexTable.getPivot().getRow() == null || simplexTable.getPivot().getColumn() == null) {
                System.out.println("\nThere is no feasible solution or the solution is not constrained.");
                return;
            }

            simplexTable.printFirstPhase(basis, zCoefficients.size(), artificialVars.size(), iteration);
            performPivotOperation();
//            if (simplexTable.getMRow().stream() // Опять смотрим на mRow
//                    .limit(mRow.size() - 1)
//                    .allMatch(x -> x.compareTo(Fraction.ZERO) >= 0)) {
//
//                simplexTable.setPivot(new Pivot());
//            }
            iteration++;
        }

        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (!simplexTable.getRows().get(i).getLast().equals(Fraction.ZERO) && artificialVars.contains(var)) {
                System.out.println("No feasible solution (artificial variables remain in the basis with non-zero values)");
                return;
            }
        }

        // Phase II
        removeArtificialVars();
        restoreOriginalZRow();
        simplexTable.setPivot(new Pivot());

        System.out.print("\nPhase I is complete. Moving on to Phase II (optimization of the original objective function)\n");
        simplexTable.printSecondPhase(basis, iteration);

        while (!isOptimal()) {
            findPivot();
            Integer row = simplexTable.getPivot().getRow();
            Integer col = simplexTable.getPivot().getColumn();

            if (row == null || col == null) {
                System.out.println("The optimal solution is unattainable.");
                return;
            }

            performPivotOperationPhaseTwo();
            iteration++;
            simplexTable.printSecondPhase(basis, iteration);
        }

        System.out.println("\nIteration " + iteration + "(first optimal solution):");
        simplexTable.printSecondPhase(basis, iteration);
        List<Fraction> firstSolution = getCurrentSolution();
        printSolution();

        // Ищем альтернативные решения
        List<Fraction> zRow = simplexTable.getZRow();
        List<Integer> nonBasic = new ArrayList<>();
        for (int j = 0; j < zRow.size() - 1; j++) {
            if (!basis.contains(j)) {
                nonBasic.add(j);
            }
        }

        List<Integer> alternativeCols = new ArrayList<>();
        for (int col : nonBasic) {
            if (col < zRow.size() && zRow.get(col).equals(Fraction.ZERO)) {
                alternativeCols.add(col);
            }
        }

        List<Fraction> secondSolution = null;
        for (int col : alternativeCols) {
            List<Fraction> ratios = new ArrayList<>();
            int minIdx = Integer.MAX_VALUE;
            Fraction minFraction = Fraction.MAX_VALUE;
            for (int i = 0; i < simplexTable.getRows().size(); i++) {
                List<Fraction> row = simplexTable.getRows().get(i);
                Fraction aij = row.get(col);
                if (aij.compareTo(Fraction.ZERO) > 0) {
                    Fraction ratio = row.getLast().divide(row.get(col));
                    ratios.add(ratio);
                    if (ratio.compareTo(minFraction) < 0) {
                        minFraction = ratio;
                        minIdx = i;
                    }
                }
            }

            if (ratios.isEmpty()) continue;

            int row = minIdx;
            List<List<Fraction>> savedRows = new ArrayList<>(simplexTable.getRows());
            List<Fraction> savedZRow = new ArrayList<>(simplexTable.getZRow());
            List<Integer> savedBasis = new ArrayList<>(basis);
            int savedIteration = iteration;

            simplexTable.setPivot(new Pivot(row, col));
            simplexTable.printSecondPhase(basis, iteration);
            performPivotOperationPhaseTwo();
            iteration++;

            System.out.printf("%nИтерация %d (альтернативное оптимальное решение):%n", iteration);
            simplexTable.printSecondPhase(basis, iteration);
            secondSolution = getCurrentSolution();
            printSolution();

            simplexTable.setRows(savedRows);
            simplexTable.setZRow(savedZRow);
            this.basis = savedBasis;
            this.iteration = savedIteration;
            break;
        }

        if (secondSolution != null) {
            System.out.println("\nThere are infinitely many optimal solutions.");
            System.out.println("The general form of the solutions can be represented as:");
            System.out.println("λ * X₁ + (1-λ) * X₂, where 0 ≤ λ ≤ 1");
            System.out.printf("X₁ = %s%n", formatSolution(firstSolution));
            System.out.printf("X₂ = %s%n", formatSolution(secondSolution));

            Fraction z = simplexTable.getZRow().getLast();
            if (goal == Goal.MIN) {
                z = z.multiply(-1);
            }

            List<String> varExprs = getGeneralSolution(firstSolution, secondSolution);
            System.out.println("\nGeneral solution in open form:");
            System.out.println("(" + String.join(", ", varExprs) + ")");
            System.out.printf("Z = %s (the same for all solutions)%n", z);
        } else {
            System.out.println("\nThere is only one solution.");
        }
    }

    private List<String> getGeneralSolution(List<Fraction> firstSolution, List<Fraction> secondSolution) {
        List<String> varExprs = new ArrayList<>();
        for (int i = 0; i < firstSolution.size(); i++) {
            Fraction x1 = firstSolution.get(i);
            Fraction x2 = secondSolution.get(i);
            Fraction delta = x1.subtract(x2);

            if (delta.equals(Fraction.ZERO)) {
                varExprs.add(x2.toString());
            } else if (x2.equals(Fraction.ZERO)) {
                varExprs.add(delta + "λ");
            } else {
                String sign = delta.compareTo(Fraction.ZERO) > 0 ? "+" : "-";
                varExprs.add(String.format("%s %s %sλ", x2, sign, delta.abs()));
            }
        }
        return varExprs;
    }

    private List<Fraction> getCurrentSolution() {
        List<List<Fraction>> rows = simplexTable.getRows();
        if (rows.isEmpty()) return Collections.emptyList();

        int numVars = rows.getFirst().size() - 1;
        List<Fraction> solution = new ArrayList<>(Collections.nCopies(numVars, Fraction.ZERO));

        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (var < numVars) {
                solution.set(var, rows.get(i).getLast());
            }
        }

        return solution.subList(0, originalVarsCount);
    }

    private String formatSolution(List<Fraction> solution) {
        return "(" + solution.stream()
                .map(Fraction::toString)
                .collect(Collectors.joining(", ")) + ")";
    }

    private void printCanonicalForm() {
        System.out.println("\nCanonical form:\n");

        for (Equation equation : equations) {
            List<Fraction> coefficients = equation.getCoefficients();
            Fraction b = equation.getResult();

            List<String> terms = getTerms(coefficients);

            String equationStr = String.join(" ", terms);
            if (equationStr.startsWith("+ ")) {
                equationStr = equationStr.substring(2);
            }
            equationStr = equationStr.replace("+ -", "- ");

            System.out.printf("%s = %s%n", equationStr, b);
        }

        List<String> zTerms = getTerms(zCoefficients);

        String zEquation = String.join(" ", zTerms);
        if (zEquation.startsWith("+ ")) {
            zEquation = zEquation.substring(2);
        }
        zEquation = zEquation.replace("+ -", "- ");

        System.out.println("\nZ-function (for simplex-method):");
        System.out.printf("Z = %s%n%n", zEquation);
    }

    private List<String> getTerms(List<Fraction> zCoefficients) {
        List<String> terms = new ArrayList<>();
        for (int j = 0; j < zCoefficients.size(); j++) {
            Fraction coeff = zCoefficients.get(j);
            if (coeff.equals(Fraction.ZERO)) continue;

            String sign = coeff.getNumerator().intValue() > 0 ? "+" : "-";
            if (coeff.abs().equals(Fraction.ONE)) {
                terms.add(String.format("%s x%d", sign, j + 1));
            } else {
                terms.add(String.format("%s %sx%d", sign, coeff.abs(), j + 1));
            }
        }
        return terms;
    }

    public void printSolution() {
        System.out.println("\nOptimal solution:");

        List<List<Fraction>> rows = simplexTable.getRows();
        if (rows.isEmpty()) {
            System.out.println("The table is empty.");
            return;
        }

        int numVars = rows.getFirst().size() - 1; // без свободного члена
        List<Fraction> solution = new ArrayList<>(Collections.nCopies(numVars, Fraction.ZERO));

        // Заполняем значения базисных переменных
        for (int i = 0; i < basis.size(); i++) {
            int var = basis.get(i);
            if (var < numVars) {
                Fraction value = rows.get(i).getLast();
                solution.set(var, value);
            }
        }

        // Выводим значения переменных
        for (int i = 0; i < solution.size(); i++) {
            System.out.printf("x%d = %s%n", i + 1, solution.get(i));
        }

        // Вычисляем значение Z
        Fraction zValue = simplexTable.getZRow().getLast();
        if (goal == Goal.MIN) {
            zValue = zValue.multiply(-1);
        }

        System.out.printf("%nZ = %s%n", zValue);
    }
}
