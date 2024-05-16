package io.kestra.plugin.googleworkspace.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Read a range from a google Sheets"
)
@Plugin(
    examples = {
        @Example(
            code = {
                "spreadsheetId: \"1Dkd3W0OQo-wxz1rrORLP7YGSj6EBLEg74fiTdbJUIQE\"",
                "range: \"Second One!A1:I\"",
                "store: true",
                "valueRender: FORMATTED_VALUE"
            }
        )
    }
)
public class ReadRange extends AbstractRead implements RunnableTask<ReadRange.Output> {
    @Schema(
        title = "The range to select"
    )
    private String range;

    @Override
    public ReadRange.Output run(RunContext runContext) throws Exception {
        Sheets service = this.connection(runContext);
        Logger logger = runContext.logger();

        ValueRange response = service.spreadsheets()
            .values()
            .get(
                runContext.render(spreadsheetId),
                runContext.render(range)
            )
            .set("valueRenderOption", this.valueRender.name())
            .set("dateTimeRenderOption", this.dateTimeRender.name())
            .execute();

        logger.info("Fetch {} rows from range '{}'", response.getValues().size(), response.getRange());

        List<Object> values = this.transform(response.getValues());

        Output.OutputBuilder builder = Output.builder()
            .size(values.size());

        if (this.fetch) {
            builder.rows(values);
        } else {
            builder.uri(runContext.storage().putFile(this.store(runContext, values)));
        }

        runContext.metric(Counter.of("rows", values.size()));

        return builder
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "List containing the fetched data",
            description = "Only populated if 'fetch' parameter is set to true."
        )
        private List<Object> rows;

        @Schema(
            title = "The size of the rows fetch"
        )
        private int size;

        @Schema(
            title = "The uri of store result",
            description = "Only populated if 'store' is set to true."
        )
        private URI uri;
    }

}
