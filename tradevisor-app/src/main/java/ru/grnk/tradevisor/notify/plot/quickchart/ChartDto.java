package ru.grnk.tradevisor.notify.plot.quickchart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartDto {
    private String type;
    private ChartData data;
    private ChartOptions options;

    @Builder
    public static record ChartData(
            List<String> labels,
            List<ChartDataset> datasets
    ) {
        @Builder
        public static record ChartDataset(
                String label,
                String yAxisID,
                List<Dataset> data,
                OhlcColor color,
                String type,
                String backgroundColor,
                String borderColor,
                Integer borderWidth,
                Integer pointRadius
        ) {
            @Builder
            public static record Dataset(long x, Float y, Float o, Float h, Float l, Float c) {
            }

            @Builder
            public static record OhlcColor(String up, String down, String unchanged) {
            }
        }
    }

    @Builder
    public static record ChartOptions(
            Boolean responsive,
            Boolean maintainAspectRatio,
            ChartScales scales,
            ChartPlugins plugins
    ) {
        @Builder
        public static record ChartScales(
                ChartOptionsScale x,
                ChartOptionsScale y1,
                ChartOptionsScale y2,
                ChartOptionsScale y3,
                ChartOptionsScale y4,
                ChartOptionsScale y5,
                ChartOptionsScale y6,
                ChartOptionsScale y7,
                ChartOptionsScale y8,
                ChartOptionsScale y9,
                ChartOptionsScale y10
        ) {
            @Builder
            public static record ChartOptionsScale(
                    ChartAdapters adapters,
                    ChartTime time,
                    String stack,
                    Integer stackWeight,
                    Integer weight,
                    Boolean display
            ) {
                @Builder
                public static record ChartAdapters(ChartZone date) {
                    @Builder
                    public static record ChartZone(String zone) {
                    }
                }

                @Builder
                public static record ChartTime(String unit, Integer stepSize, DisplayFormats displayFormats) {
                    @Builder
                    public static record DisplayFormats(String day, String month) {
                    }
                }
            }
        }

        @Builder
        public static record ChartPlugins(Legend legend, Title title, ChartAnnotation annotation) {
            @Builder
            public static record Legend(Boolean display) {
            }

            @Builder
            public static record Title(Boolean display, String text, Font font) {
                @Builder
                public static record Font(Integer size) {
                }
            }

            @Builder
            public static record ChartAnnotation(List<ChartAnnotationItem> annotations) {
                @Builder
                public static record ChartAnnotationItem(
                        String type,
                        String mode,
                        Float yMin,
                        Float yMax,
                        Long xMax,
                        Long xMin,
                        String borderColor,
                        Label label,
                        ArrowHeads arrowHeads
                ) {
                    @Builder
                    public static record Label(
                            Boolean enabled,
                            String color,
                            String backgroundColor,
                            String content,
                            String position,
                            Padding padding
                    ) {
                        @Builder
                        public static record Padding(Integer bottom) {
                        }
                    }

                    @Builder
                    public static record ArrowHeads(Start start) {
                        @Builder
                        public static record Start(Boolean enabled) {
                        }
                    }
                }
            }
        }
    }
}
