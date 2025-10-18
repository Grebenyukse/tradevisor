package ru.grnk.tradevisor.calculate.strategies.fibo;

public class Main {

    public static void main(String[] args) {
// Arrange
        float[] lows = {100f,  99f,  98f,  97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};


        /**
         * 100% ┤------------------------------
         *  100% |                             |
         *   93% |                           |||
         *   87% |                          |||
         *   80% |                        ||||
         *   73% |                       |||
         *   67% |                     ||||
         *   60% |                    |||
         *   53% |                  ||||
         *   47% |        |||     ||||
         *   40% |       |||||   |||
         *   33% |     |||| |||||||
         *   27% |    |||     |||
         *   20% |  ||||       |
         *   13% | |||
         *    7% ||||
         *    0% ||
         *    0% └------------------------------
         *      0        5        10       15       20       25
         */
        float[] lowsTrendUpCorrection = {
                100f,101f,102f,103f,104f,105f,106f,107f,108f,109f,
                108f,107f,106f,105f,106f,107f,108f,109f,110f,111f,
                112f,113f,114f,115f,116f,117f,118f,119f,120f,121f
        };
        float[] highsTrendUpCorrection = {
                102f,103f,104f,105f,106f,107f,108f,109f,110f,111f,
                110f,109f,108f,107f,108f,109f,110f,111f,112f,113f,
                114f,115f,116f,117f,118f,119f,120f,121f,122f,123f
        };

        /**
         *  100% ┤------------------------------
         *  100% |  |
         *   93% | |||
         *   87% ||||||
         *   80% ||   |||
         *   73% |      |||
         *   67% |        |||
         *   60% |          |||
         *   53% |            |||
         *   47% |              |||
         *   40% |                |||
         *   33% |                  |||
         *   27% |                    |||
         *   20% |                      |||
         *   13% |                        |||
         *    7% |                          |||
         *    0% |                            ||
         *    0% └------------------------------
         *      0        5        10       15       20       25
         */
        float[] lowsHeadShoulders = {
                95f, 96f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f,
                89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f, 80f,
                79f, 78f, 77f, 76f, 75f, 74f, 73f, 72f, 71f, 70f
        };
        float[] highsHeadShoulders = {
                97f, 99f,101f, 99f, 97f, 95f, 94f, 93f, 92f, 91f,
                90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f,
                80f, 79f, 78f, 77f, 76f, 75f, 74f, 73f, 72f, 71f
        };

        /**
         *  100% ┤------------------------------
         *  100% ||
         *   93% ||||
         *   87% | |||
         *   80% |  |||         |
         *   73% |   ||||     |||||
         *   67% |     |||   ||| |||
         *   60% |      |||||||   ||||
         *   53% |        |||       |||
         *   47% |         |         |||
         *   40% |                    ||||
         *   33% |                      |||
         *   27% |                       ||||
         *   20% |                         |||
         *   13% |                          |||
         *    7% |                           |||
         *    0% |                             |
         *    0% └------------------------------
         *      0        5        10       15       20       25
         */
        float[] lowsTrendDownCorrection = {
                120f,119f,118f,117f,116f,115f,114f,113f,112f,111f,
                112f,113f,114f,115f,116f,115f,114f,113f,112f,111f,
                110f,109f,108f,107f,106f,105f,104f,103f,102f,101f
        };
        float[] highsTrendDownCorrection = {
                122f,121f,120f,119f,118f,117f,116f,115f,114f,113f,
                114f,115f,116f,117f,118f,117f,116f,115f,114f,113f,
                112f,111f,110f,109f,108f,107f,106f,105f,104f,103f
        };
        /**
         * 100% ┤------------------------------
         *  100% |  ||  ||  ||  ||  ||  ||  ||
         *   93% |  ||  ||  ||  ||  ||  ||  ||
         *   87% |  ||  ||  ||  ||  ||  ||  ||
         *   80% |  ||  ||  ||  ||  ||  ||  ||
         *   73% |  ||  ||  ||  ||  ||  ||  ||
         *   67% |||||||||||||||||||||||||||||||
         *   60% |||||||||||||||||||||||||||||||
         *   53% |||||||||||||||||||||||||||||||
         *   47% |||||||||||||||||||||||||||||||
         *   40% |||||||||||||||||||||||||||||||
         *   33% |||||||||||||||||||||||||||||||
         *   27% |||  ||  ||  ||  ||  ||  ||  ||
         *   20% |||  ||  ||  ||  ||  ||  ||  ||
         *   13% |||  ||  ||  ||  ||  ||  ||  ||
         *    7% |||  ||  ||  ||  ||  ||  ||  ||
         *    0% |||  ||  ||  ||  ||  ||  ||  ||
         *    0% └------------------------------
         *      0        5        10       15       20       25
         */
        float[] lowsSideways = {
                100f,100f,101f,101f,100f,100f,101f,101f,100f,100f,
                101f,101f,100f,100f,101f,101f,100f,100f,101f,101f,
                100f,100f,101f,101f,100f,100f,101f,101f,100f,100f
        };
        float[] highsSideways = {
                102f,102f,103f,103f,102f,102f,103f,103f,102f,102f,
                103f,103f,102f,102f,103f,103f,102f,102f,103f,103f,
                102f,102f,103f,103f,102f,102f,103f,103f,102f,102f
        };
        /**
         *  100% ┤----------------------------------------
         *  100% |                                       |
         *   93% |                   ||                |||
         *   87% |                  ||||              |||
         *   80% |                ||||||||          ||||
         *   73% |               |||    |||        |||
         *   67% |             ||||      ||||    ||||
         *   60% |            |||          |||  |||
         *   53% |          ||||            ||||||
         *   47% |        ||||                ||
         *   40% |       |||
         *   33% |     ||||
         *   27% |    |||
         *   20% |  ||||
         *   13% | |||
         *    7% ||||
         *    0% ||
         *    0% └----------------------------------------
         *      0        5        10       15       20       25       30       35
         */
        float[] lowsFib618 = {
                // 0‑100% (быстрый рост)
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f,
                90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                // откат до 61.8% (≈ 80 + 0.618*20 ≈ 92.36)
                99f,98f,97f,96f,95f,94f,93f,92f,91f,90f,
                // отскок вверх
                92f,93f,94f,95f,96f,97f,98f,99f,100f,101f
        };
        float[] highsFib618 = {
                // 0‑100% (рост)
                82f,83f,84f,85f,86f,87f,88f,89f,90f,91f,
                92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                // откат
                101f,100f,99f,98f,97f,96f,95f,94f,93f,92f,
                // отскок
                94f,95f,96f,97f,98f,99f,100f,101f,102f,103f
        };

        /**
         *  100% ┤----------------------------------------
         *  100% |                   ||
         *   93% |                 ||||||                |
         *   87% |                |||  |||              ||
         *   80% |               |||    |||            |||
         *   73% |             ||||      ||||        ||||
         *   67% |            |||          |||      |||
         *   60% |          ||||            ||||  ||||
         *   53% |         |||                || |||
         *   47% |        |||                  ||||
         *   40% |      ||||                    ||
         *   33% |     |||
         *   27% |   ||||
         *   20% |  |||
         *   13% | |||
         *    7% ||||
         *    0% ||
         *    0% └----------------------------------------
         *      0        5        10       15       20       25       30       35
         */
        float[] lowsFib382 = {
                // рост 0‑100%
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f,
                90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94f,93f,92f,91f,90f,
                // отскок
                88f,89f,90f,91f,92f,93f,94f,95f,96f,97f
        };
        float[] highsFib382 = {
                // рост
                82f,83f,84f,85f,86f,87f,88f,89f,90f,91f,
                92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                // откат
                101f,100f,99f,98f,97f,96f,95f,94f,93f,92f,
                // отскок
                90f,91f,92f,93f,94f,95f,96f,97f,98f,99f
        };

        /**
         *  100% ┤------------------------------
         *  100% |                   |
         *   93% |                  |||
         *   87% |                  |||
         *   80% |                 |||||
         *   73% |                |||||||
         *   67% |                |||||||
         *   60% |||             |||||||||
         *   53% ||||           |||||||||||
         *   47% | |||          |||||||||||
         *   40% |  ||||       |||||| ||||||
         *   33% |    |||     |||||     |||||
         *   27% |     ||||  |||||       |||||
         *   20% |       ||| |||           |||
         *   13% |        |||||             |||
         *    7% |         |||               |||
         *    0% |                             |
         *    0% └------------------------------
         */
        float[] lowsDoubleTop = {
                90f,89f,88f,87f,86f,85f,84f,83f,82f,81f,
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f,
                88f,87f,86f,85f,84f,83f,82f,81f,80f,79f
        };
        float[] highsDoubleTop = {
                92f,91f,90f,89f,88f,87f,86f,85f,84f,83f,
                82f,84f,86f,88f,90f,92f,94f,96f,98f,100f,
                98f,96f,94f,92f,90f,88f,86f,84f,82f,80f
        };
        /**
         * 100% ┤------------------------------
         *  100% |                             |
         *   93% |          |                 ||
         *   87% |         |||               |||
         *   80% |        |||||             |||
         *   73% |        || ||             ||
         *   67% |       ||| |||           |||
         *   60% |      |||   |||         |||
         *   53% |     |||     |||       |||
         *   47% |    |||       |||     |||
         *   40% |   |||         |||   |||
         *   33% |  |||           ||| |||
         *   27% |  ||             || ||
         *   20% | |||             |||||
         *   13% ||||               |||
         *    7% |||                 |
         *    0% ||
         *    0% └------------------------------
         *      0        5        10       15       20       25
         */
        float[] lowsDoubleBottom = {
                110f,111f,112f,113f,114f,115f,116f,117f,118f,119f,
                120f,119f,118f,117f,116f,115f,114f,113f,112f,111f,
                112f,113f,114f,115f,116f,117f,118f,119f,120f,121f
        };
        float[] highsDoubleBottom = {
                112f,113f,114f,115f,116f,117f,118f,119f,120f,121f,
                122f,121f,120f,119f,118f,117f,116f,115f,114f,113f,
                114f,115f,116f,117f,118f,119f,120f,121f,122f,123f
        };
        /**100% ┤------------------------------
         100% |                             |
         93% |                           |||
         87% |                          ||
         80% |                         ||
         73% |                       |||
         67% |                      ||
         60% |                    |||
         53% |                 ||||
         47% |         |||   ||||
         40% |       ||||||||||
         33% |      ||     |
         27% |    |||
         20% |   ||
         13% |  ||
         7% ||||
         0% ||
         0% └------------------------------
         *
         */
        float[] lowsFlag = {
                // резкое движение вверх
                50f,52f,54f,56f,58f,60f,62f,64f,66f,68f,
                // флаг‑коррекция (короткий диапазон)
                68f,67f,66f,65f,66f,67f,68f,69f,70f,71f,
                // продолжение роста
                72f,74f,76f,78f,80f,82f,84f,86f,88f,90f
        };
        float[] highsFlag = {
                // резкое движение вверх
                52f,54f,56f,58f,60f,62f,64f,66f,68f,70f,
                // флаг‑коррекция
                70f,69f,68f,67f,68f,69f,70f,71f,72f,73f,
                // продолжение роста
                74f,76f,78f,80f,82f,84f,86f,88f,90f,92f
        };
        // Печатаем график
        print(lowsFlag, highsFlag);
    }

    /** Height of the picture – 16 rows as required by the task. */
    private static final int ROWS = 16;

    /** Characters used in the picture. */
    private static final char VERTICAL   = '|';
    private static final char HORIZONTAL = '-';
    private static final char LEFT_TOP   = '┤';
    private static final char LEFT_BOTTOM = '└';

    /**
     * Prints the chart to {@link System#out}.
     *
     * @param lows  array of low values (must have the same length as {@code highs})
     * @param highs array of high values
     */
    public static void print(float[] lows, float[] highs) {
        // ---------- validation ----------
        if (lows == null || highs == null) {
            throw new IllegalArgumentException("Arrays must not be null");
        }
        if (lows.length != highs.length) {
            throw new IllegalArgumentException("Arrays must have the same length");
        }
        if (lows.length == 0) {
            System.out.println("Empty data");
            return;
        }

        final int width = lows.length;                 // number of columns
        char[][] canvas = new char[ROWS][width];        // empty picture

        // fill with spaces
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < width; c++) {
                canvas[r][c] = ' ';
            }
        }

        // ---------- 1. find global min/max ----------
        float globalMin = lows[0];
        float globalMax = highs[0];
        for (int i = 0; i < width; i++) {
            if (lows[i] < globalMin)  globalMin = lows[i];
            if (highs[i] > globalMax) globalMax = highs[i];
        }
        float range = globalMax - globalMin;
        if (range == 0f) {
            range = 1f; // avoid division by zero – all values are equal
        }

        // ---------- 2. fill columns with '|' ----------
        for (int col = 0; col < width; col++) {
            int lowRow  = priceToRow(lows[col],  globalMin, range);
            int highRow = priceToRow(highs[col], globalMin, range);

            // lowRow is always the larger index (lower on the screen)
            int from = Math.min(lowRow, highRow);
            int to   = Math.max(lowRow, highRow);

            for (int row = from; row <= to; row++) {
                canvas[row][col] = VERTICAL;
            }
        }

        // ---------- 3. build output ----------
        StringBuilder out = new StringBuilder("\n--- ASCII Range Chart ---\n");

        // 3.1 top border (100 %)
        out.append(String.format("%4s%% %c", "100", LEFT_TOP));
        for (int i = 0; i < width; i++) {
            out.append(HORIZONTAL);
        }
        out.append('\n');

        // 3.2 body rows – from 100 % down to 0 %
        for (int row = 0; row < ROWS; row++) {
            int percent = Math.round(100f * (ROWS - 1 - row) / (float) (ROWS - 1));
            out.append(String.format("%4d%% %c", percent, VERTICAL));

            for (int col = 0; col < width; col++) {
                out.append(canvas[row][col]);
            }
            out.append('\n');
        }

        // 3.3 bottom border (0 %)
        out.append(String.format("%4s%% %c", "0", LEFT_BOTTOM));
        for (int i = 0; i < width; i++) {
            out.append(HORIZONTAL);
        }
        out.append('\n');

        // 3.4 X‑axis index labels (every 5‑th column)
        out.append("     "); // space under the percent label column
        for (int col = 0; col < width; col++) {
            if (col % 5 == 0) {
                out.append(col);
                // fill the gap up to the next label
                int gap = Math.max(0, 5 - String.valueOf(col).length());
                for (int j = 0; j < gap; j++) {
                    out.append(' ');
                }
            } else {
                out.append(' ');
            }
        }
        out.append('\n');

        System.out.println(out);
    }

    /**
     * Converts a price to a row index in the canvas.
     *
     * @param price price value
     * @param min   global minimum of the whole series
     * @param range {@code max-min}
     * @return row number in the range {@code [0, ROWS-1]} (0 = top line)
     */
    private static int priceToRow(float price, float min, float range) {
        // relative position in the 0-1 interval
        float rel = (price - min) / range;
        // map to a row number and invert Y-axis (0 = top)
        int row = Math.round(rel * (ROWS - 1));
        return ROWS - 1 - row;
    }

}
