package io.kestra.plugin.googleworkspace.sheets;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.googleworkspace.drive.Delete;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
	title = "Deletes an spreadsheet file in Google Workspace"
)
@Plugin(
	examples = {
		@Example(
			title = "Deletes an spreadsheet in google workspace",
			code = {
				"type: io.kestra.plugin.googleworkspace.sheets.CreateSpreadsheet",
				"serviceAccount: \"{{ inputs.serviceAccount }}\"",
				"spreadsheetId: xxxxxxxxxxxxxxxxx"
			}
		)
	}
)
public class DeleteSpreadsheet extends AbstractSheet implements RunnableTask<DeleteSpreadsheet.Output> {

	@Schema(
		title = "Spreadsheet ID"
	)
	@NotNull
	@PluginProperty(dynamic = true)
	private String spreadsheetId;

	@Override
	public Output run(RunContext runContext) throws Exception {
		Sheets services = this.connection(runContext);

		String spreadsheetId = runContext.render(this.spreadsheetId);

		try {
			services.spreadsheets()
				.get(spreadsheetId)
				.execute();
		} catch (GoogleJsonResponseException exception) {
			throw new IllegalArgumentException("Spreadsheet not found: " + spreadsheetId);
		}

		Delete delete = Delete.builder()
			.fileId(spreadsheetId)
			.build();

		Delete.Output output = delete.run(runContext);

		return Output.builder()
			.spreadsheetId(output.getFileId())
			.build();
	}

	@Getter
	@Builder
	public static class Output implements io.kestra.core.models.tasks.Output {

		@Schema(
			title = "The spreadsheet id."
		)
		private String spreadsheetId;

	}

}
