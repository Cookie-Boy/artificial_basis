package ru.sibsutis.artificial_basis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SimplexTable {
    private List<List<Fraction>> rows;
    private List<Fraction> zRow;
    private List<Fraction> mRow;
    private Pivot pivot;

    public void printFirstPhase(List<Integer> basis, int zCount, int artificialCount, int iteration) {
        System.out.printf("%nIteration: %d (Phase I):%n", iteration);
        System.out.println("Simplex-table:");

        List<String> headers = new ArrayList<>();
        headers.add("b.v.");
        headers.add("1");
        for (int i = 0; i < rows.getFirst().size() - 1; i++) {
            headers.add("x" + (i + 1));
        }
        printFormattedRow(headers);

        for (int i = 0; i < rows.size(); i++) {
            List<Fraction> row = rows.get(i);
            String bp;
            int basisIndex = basis.get(i);
            if (basisIndex < zCount + artificialCount) {
                bp = "x" + (basisIndex + 1);
            } else {
                bp = "x" + (basisIndex - zCount + 1);
            }

            List<String> rowStr = new ArrayList<>();
            rowStr.add(bp);
            rowStr.add(row.getLast().toString());
            for (int j = 0; j < row.size() - 1; j++) {
                rowStr.add(row.get(j).toString());
            }
            printFormattedRow(rowStr);
        }

        List<String> zStr = new ArrayList<>();
        zStr.add("Z");
        zStr.add(zRow.getLast().toString());
        for (int j = 0; j < zRow.size() - 1; j++) {
            zStr.add(zRow.get(j).toString());
        }
        printFormattedRow(zStr);

        // Строка M (если есть)
        if (!mRow.isEmpty()) {
            List<String> mStr = new ArrayList<>();
            mStr.add("M");
            mStr.add(mRow.getLast().toString());
            for (int j = 0; j < mRow.size() - 1; j++) {
                mStr.add(mRow.get(j).toString());
            }
            printFormattedRow(mStr);
        }

        if (pivot.getColumn() != null && pivot.getRow() != null) {
            System.out.printf("%nPivot column: x%d, pivot row: %d%n", pivot.getColumn() + 1, pivot.getRow() + 1);
        }
    }

    public void printSecondPhase(List<Integer> basis, int iteration) {
        System.out.printf("%nIteration %d (Phase II):%n", iteration);
        System.out.println("Simplex-table:");

        List<String> headers = new ArrayList<>();
        headers.add("b.v.");
        headers.add("1");
        for (int i = 0; i < rows.getFirst().size() - 1; i++) {
            headers.add("x" + (i + 1));
        }
        printFormattedRow(headers);

        for (int i = 0; i < rows.size(); i++) {
            List<Fraction> row = rows.get(i);
            String bp = "x" + (basis.get(i) + 1);
            List<String> rowStr = new ArrayList<>();
            rowStr.add(bp);
            rowStr.add(row.getLast().toString());
            for (int j = 0; j < row.size() - 1; j++) {
                rowStr.add(row.get(j).toString());
            }
            printFormattedRow(rowStr);
        }

        List<String> zStr = new ArrayList<>();
        zStr.add("Z");
        zStr.add(zRow.getLast().toString());
        for (int j = 0; j < zRow.size() - 1; j++) {
            zStr.add(zRow.get(j).toString());
        }
        printFormattedRow(zStr);

        if (pivot.getColumn() != null && pivot.getRow() != null) {
            System.out.printf("%nPivot column: x%d, pivot row: %d%n", pivot.getColumn() + 1, pivot.getRow() + 1);
        }
    }

    private void printFormattedRow(List<String> row) {
        for (String cell : row) {
            System.out.printf("%7s ", cell);
        }
        System.out.println();
    }
}
